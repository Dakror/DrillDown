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

import de.dakror.common.Callback;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ElectricConveyorCore;
import de.dakror.quarry.util.Bounds;

// @Refactor: pretty much the same code as in ctank

/**
 * @author Maximilian Stark | Dakror
 */
public class CSingleInventory extends Component implements IStorage {
    ItemType item;
    int count, size;

    float remainingDelay;
    float pumpOutDelay;

    int outputDock;

    boolean hasTubeAtOutput;
    boolean outputEnabled;
    boolean draw;
    boolean showFullState;

    Callback<ItemType> pumpOutCallback;

    public CSingleInventory(int size) {
        this(size, -1);
    }

    public CSingleInventory(int size, int outputDock) {
        this(size, outputDock, outputDock > -1);
    }

    public CSingleInventory(int size, int outputDock, boolean outputEnabled) {
        this.outputDock = outputDock;
        this.size = size;
        pumpOutDelay = Const.DEFAULT_PUMP_OUT_DELAY;
        remainingDelay = pumpOutDelay;

        draw = true;
        this.outputEnabled = outputEnabled;
        showFullState = true;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public void setOutput(int outputDock) {
        this.outputDock = outputDock;
    }

    public CSingleInventory setPumpOutDelay(float pumpOutDelay) {
        this.pumpOutDelay = pumpOutDelay;
        remainingDelay = pumpOutDelay;
        return this;
    }

    public CSingleInventory setPumpOutCallback(Callback<ItemType> callback) {
        pumpOutCallback = callback;
        return this;
    }

    public CSingleInventory setShowFullState(boolean showState) {
        this.showFullState = showState;
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

    public ItemType getItem() {
        return item;
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
            item = null;
            count = 0;
        }

        if (outputDock == -1) return;

        if (dirtyBounds.touches(structure)) {
            updateOutput();
        }

        if (!outputEnabled || item == null || gameSpeed == 0) return;

        remainingDelay -= deltaTime * gameSpeed;
        if (remainingDelay <= 0) {
            Dock output = structure.getDocks()[outputDock];
            remainingDelay = pumpOutDelay;
            if (!hasTubeAtOutput || item == null) return;

            if (structure.layer.addItemEntity(item, structure, output, structure)) {
                count--;

                if (pumpOutCallback != null) pumpOutCallback.call(item);

                if (count <= 0) {
                    item = null;
                    count = 0;
                }
            }
        }
    }

    public boolean isOutputEnabled() {
        return outputEnabled;
    }

    public void setOutputEnabled(boolean outputEnabled) {
        this.outputEnabled = outputEnabled;
    }

    protected void updateOutput() {
        if (structure.getIndex() == 50565) {
            int i = 0;
        }
        Dock output = outputDock == -1 ? null : structure.getDocks()[outputDock];
        if (output != null) {
            Structure<?> s = structure.layer.getStructure(structure.x + output.x + output.dir.dx, structure.y + output.y + output.dir.dy);
            hasTubeAtOutput = s instanceof Conveyor;
            if (s instanceof ElectricConveyorCore && ((ElectricConveyorCore) s).getDirection().isPerpendicular(output.dir))
                hasTubeAtOutput = false;
        }
    }

    @Override
    public void postLoad() {
        if (hasTubeAtOutput && outputEnabled)
            updateOutput();
    }

    @Override
    public boolean canAccept(ItemType item) {
        if (this.item != null && this.item != item) return false;
        if (count >= size) return false;
        if (item.categories.contains(ItemCategory.Fluid)) return false;

        return true;
    }

    @Override
    public int addWithRest(ItemType item, int amount) {
        if (this.item != null && this.item != item) return amount;
        if (this.count >= size) return amount;
        if (item.categories.contains(ItemCategory.Fluid)) return amount;

        this.item = item;
        int dif = Math.min(amount, size - count);
        this.count += dif;

        return amount - dif;
    }

    @Override
    public int removeWithRest(ItemType item, int amount) {
        if (this.item != null && this.item != item) return amount;
        if (this.count - amount >= size) return amount;
        if (item.categories.contains(ItemCategory.Fluid)) return amount;

        this.item = item;
        int dif = Math.min(amount, count);

        removeUnsafe(dif);

        return amount - dif;
    }

    public void removeUnsafe(int amount) {
        this.count -= amount;
        if (this.count <= 0) {
            this.item = null;
            this.count = 0;
        }
    }

    @Override
    public void addUnsafe(ItemType item, int amount) {
        this.item = item;
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
        if (item != null) {
            b
                    .Short("item", item.value)
                    .Int("amount", count);
        }
        b
                .Byte("output", (byte) (hasTubeAtOutput ? 1 : 0))
                .Byte("pump", (byte) (outputEnabled ? 1 : 0));

        b.End();
    }

    @Override
    public void loadData(CompoundTag tag) {
        short flu = tag.Short("item", (short) 0);
        if (flu != 0) {
            item = Item.get(flu);
            count = tag.Int("amount", 0);
            if (count == 0) this.item = null;
        }
        hasTubeAtOutput = tag.Byte("output", (byte) 0) == 1;

        outputEnabled = tag.Byte("pump", (byte) 0) == 1;
    }

    @Override
    public Component clone() {
        CSingleInventory tank = new CSingleInventory(size, outputDock, outputEnabled);
        tank.pumpOutDelay = pumpOutDelay;

        return tank;
    }

    @Override
    public float getFillRatio() {
        return count / (float) size;
    }

    @Override
    public boolean add(ItemType item, int amount) {
        if (this.item != null && this.item != item) return false;
        if (this.count >= size) return false;
        if (item.categories.contains(ItemCategory.Fluid)) return false;

        this.item = item;
        this.count += amount;
        return true;
    }

    @Override
    public boolean remove(ItemType item, int amount) {
        if (amount > count || this.item == null) return false;
        if (item == this.item) {
            count -= amount;
            if (count == 0) this.item = null;
            return true;
        }
        return false;
    }

    @Override
    public int get(ItemType item) {
        if (item == this.item) return count;
        return 0;
    }

    @Override
    public Amount[] getSimilar(ItemType item) {
        if (this.item != null && Item.base(item) == Item.base(this.item))
            return new Amount[] { new Amount(this.item, count) };
        return null;
    }

    @Override
    public Amount[] get(ItemCategory cat) {
        if (this.item != null && this.item.categories.contains(cat))
            return new Amount[] { new Amount(this.item, count) };
        return null;
    }

    @Override
    public int getSum(ItemCategory cat) {
        if (this.item != null && this.item.categories.contains(cat))
            return count;
        return 0;
    }

    @Override
    public boolean remove(ItemCategory category, int amount) {
        if (this.item != null && this.item.categories.contains(category)) {
            return remove(this.item, amount);
        }
        return false;
    }

    @Override
    public void init() {}

}
