/*******************************************************************************
 * Copyright 2017 Maximilian Stark | Dakror <mail@dakror.de>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.dakror.quarry.structure.base.component;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.FluidTubeStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.logistics.Valve;
import de.dakror.quarry.util.Bounds;

/**
 * @author Maximilian Stark | Dakror
 */
public class CTank extends Component implements IStorage {
    ItemType fluid;
    int count, size;

    int maxOutput;

    float remainingDelay;
    float pumpOutDelay;

    int outputDock;
    Structure<?> outputTube;

    boolean hasTubeAtOutput;
    boolean outputEnabled;
    boolean draw;

    public CTank(int size) {
        this(size, -1);
    }

    public CTank(int size, int outputDock) {
        this(size, outputDock, outputDock > -1);
    }

    public CTank(int size, int outputDock, boolean outputEnabled) {
        this.outputDock = outputDock;
        this.size = size;
        pumpOutDelay = Const.DEFAULT_PUMP_OUT_DELAY;
        remainingDelay = pumpOutDelay;

        maxOutput = Const.DEFAULT_PUMP_OUT_MAX_FLUID;
        draw = true;
        this.outputEnabled = outputEnabled;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void setOutput(int outputDock) {
        this.outputDock = outputDock;
    }

    @Override
    public void init() {}

    public CTank setPumpOutDelay(float pumpOutDelay) {
        this.pumpOutDelay = pumpOutDelay;
        remainingDelay = pumpOutDelay;
        return this;
    }

    public CTank setMaxOutput(int output) {
        this.maxOutput = output;
        return this;
    }

    @Override
    public void onPlacement() {
        if (structure.layer != null && outputDock > -1) updateOutput();
    }

    @Override
    public int getOutput() {
        return outputDock;
    }

    public ItemType getFluid() {
        return fluid;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (structure.layer == null) return;

        if (structure.layer.fake && size < Integer.MAX_VALUE) size = Integer.MAX_VALUE;
        if (count <= 0) {
            fluid = null;
            count = 0;
            remainingDelay = 0;
        }

        if (outputDock == -1) return;

        if (dirtyBounds.touches(structure)) {
            updateOutput();
        }

        if (!outputEnabled || fluid == null || gameSpeed == 0) return;

        remainingDelay -= deltaTime * gameSpeed;
        if (remainingDelay <= 0) {
            remainingDelay = pumpOutDelay;
            if (outputTube == null || fluid == null) return;

            int dif = Math.min(maxOutput, count);

            count = count - dif + outputTube.acceptFluid(fluid, dif, structure);
        }
    }

    public boolean hasOutputTube() {
        return outputTube != null;
    }

    public boolean isOutputEnabled() {
        return outputEnabled;
    }

    public void setOutputEnabled(boolean outputEnabled) {
        this.outputEnabled = outputEnabled;
    }

    protected void updateOutput() {
        Dock output = outputDock == -1 ? null : structure.getDocks()[outputDock];
        if (output == null) return;
        Structure<?> s = structure.layer.getStructure(structure.x + output.x + output.dir.dx, structure.y + output.y + output.dir.dy);
        outputTube = s instanceof FluidTubeStructure ? s : null;
        if (s instanceof Valve) {
            Valve v = (Valve) s;
            // disable wrong side of valve
            if (output.dir == v.getDirection()) outputTube = null;
        }
    }

    @Override
    public void postLoad() {
        if (hasTubeAtOutput)
            updateOutput();
    }

    @Override
    public boolean canAccept(ItemType item) {
        if (fluid != null && fluid != item) return false;
        if (count >= size) return false;
        if (!item.categories.contains(ItemCategory.Fluid)) return false;

        return true;
    }

    @Override
    public int addWithRest(ItemType item, int amount) {
        if (fluid != null && fluid != item) return amount;
        if (this.count >= size) return amount;
        if (!item.categories.contains(ItemCategory.Fluid)) return amount;

        fluid = item;
        int dif = Math.min(amount, size - count);
        this.count += dif;

        return amount - dif;
    }

    @Override
    public int removeWithRest(ItemType item, int amount) {
        if (fluid == null || fluid != item) return amount;
        if (this.count - amount >= size) return amount;
        if (!item.categories.contains(ItemCategory.Fluid)) return amount;

        fluid = item;
        int dif = Math.min(amount, count);

        removeUnsafe(dif);

        return amount - dif;
    }

    public void removeUnsafe(int amount) {
        this.count -= amount;
        if (this.count <= 0) {
            this.fluid = null;
            this.count = 0;
        }
    }

    @Override
    public void addUnsafe(ItemType item, int amount) {
        fluid = item;
        this.count += amount;
    }

    @Override
    public boolean hasSpace() {
        return count < size;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public void saveData(Builder b) {
        b.Compound();
        if (fluid != null) {
            b
                    .Short("fluid", fluid.value)
                    .Int("amount", count);
        }
        b
                .Byte("output", (byte) (outputTube != null ? 1 : 0))
                .Byte("pump", (byte) (outputEnabled ? 1 : 0));

        b.End();
    }

    @Override
    public void loadData(CompoundTag tag) {
        short flu = tag.Short("fluid", (short) 0);
        if (flu != 0) {
            fluid = Item.get(flu);
            count = tag.Int("amount", 0);
            if (count == 0) fluid = null;
        }
        hasTubeAtOutput = tag.Byte("output", (byte) 0) == 1;

        outputEnabled = tag.Byte("pump", (byte) 0) == 1;
    }

    @Override
    public Component clone() {
        CTank tank = new CTank(size, outputDock, outputEnabled);
        tank.maxOutput = maxOutput;
        tank.pumpOutDelay = pumpOutDelay;

        return tank;
    }

    @Override
    public float getFillRatio() {
        return count / (float) size;
    }

    @Override
    public boolean add(ItemType item, int amount) {
        return false;
    }

    @Override
    public boolean remove(ItemType item, int amount) {
        if (amount > count || fluid == null) return false;

        if (item == fluid) {
            count -= amount;
            if (count == 0) fluid = null;
            return true;
        }
        return false;
    }

    @Override
    public int get(ItemType item) {
        if (item == fluid) return count;
        return 0;
    }

    @Override
    public Amount[] getSimilar(ItemType item) {
        if (fluid != null && Item.base(item) == Item.base(fluid))
            return new Amount[] { new Amount(fluid, count) };
        return null;
    }

    @Override
    public Amount[] get(ItemCategory cat) {
        if (fluid != null && fluid.categories.contains(cat))
            return new Amount[] { new Amount(fluid, count) };
        return null;
    }

    @Override
    public boolean remove(ItemCategory category, int amount) {
        if (fluid != null && fluid.categories.contains(category)) {
            return remove(fluid, amount);
        }
        return false;
    }

    @Override
    public int getSum(ItemCategory cat) {
        if (fluid != null && fluid.categories.contains(cat))
            return count;
        return 0;
    }
}
