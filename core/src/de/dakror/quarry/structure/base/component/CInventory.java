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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.badlogic.gdx.utils.Array;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ElectricConveyorCore;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class CInventory extends Component implements IStorage {
    int size, count;
    int outputDock;

    HashMap<ItemType, Integer> inventory;
    float pumpOutDelay;
    boolean isTubeAtOutput;
    float pumpOutSpeed;

    public CInventory() {
        inventory = new HashMap<>();
        pumpOutDelay = Const.DEFAULT_PUMP_OUT_DELAY;
        pumpOutSpeed = Const.DEFAULT_PUMP_OUT_DELAY;
    }

    public CInventory(int size) {
        this(size, -1);
    }

    public CInventory(int size, int outputDock) {
        this();
        this.size = size;
        this.outputDock = outputDock;
    }

    public CInventory setPumpOutSpeed(float speed) {
        this.pumpOutSpeed = speed;
        return this;
    }

    public float getPumpOutSpeed() {
        return pumpOutSpeed;
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
    public int getOutput() {
        return outputDock;
    }

    @Override
    public void onPlacement() {
        if (structure.layer != null && outputDock > -1) updateOutput();
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (structure.layer == null) return;

        if (structure.layer.fake && size < Integer.MAX_VALUE) size = Integer.MAX_VALUE;
        if (outputDock == -1) return;

        if (dirtyBounds.touches(structure)) {
            updateOutput();
        }

        if (gameSpeed == 0) return;

        pumpOutDelay -= deltaTime * gameSpeed;
        if (pumpOutDelay <= 0) {
            pumpOutDelay = pumpOutSpeed;
            if (!isTubeAtOutput) return;
            pumpOutItems();
        }
    }

    protected void updateOutput() {
        Dock output = outputDock == -1 ? null : structure.getDocks()[outputDock];
        if (output != null) {
            Structure<?> s = structure.layer.getStructure(structure.x + output.x + output.dir.dx, structure.y + output.y + output.dir.dy);
            isTubeAtOutput = s instanceof Conveyor;
            if (s instanceof ElectricConveyorCore && ((ElectricConveyorCore) s).getDirection().isPerpendicular(output.dir))
                isTubeAtOutput = false;
        }
    }

    public void clear() {
        inventory.clear();
        count = 0;
    }

    @Override
    public boolean isEmpty() {
        return count == 0;
    }

    @Override
    public boolean hasSpace() {
        return count < size;
    }

    @Override
    public float getFillRatio() {
        return Math.min(1, count / (float) size);
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
    public boolean add(ItemType item, int amount) {
        if (count + amount > size) return false;
        if (amount < 0) return false;
        addUnsafe(item, amount);
        return true;
    }

    @Override
    public boolean remove(ItemType item, int amount) {
        if (count < amount) return false;
        int i = get(item);
        if (i < amount) return false;
        count -= amount;

        int num = i - amount;

        if (num > 0)
            inventory.put(item, num);
        else inventory.remove(item);

        return true;
    }

    @Override
    public int removeWithRest(ItemType item, int amount) {
        if (count == 0) return amount;
        int i = get(item);

        int dif = Math.min(i, amount);
        count -= dif;
        int num = i - dif;

        inventory.put(item, Math.max(0, num));

        return amount - dif;
    }

    @Override
    public void addUnsafe(ItemType item, int amount) {
        int i = get(item);
        count += amount;
        inventory.put(item, i + amount);
    }

    @Override
    public int addWithRest(ItemType item, int amount) {
        if (amount < 0) return amount;
        if (count == size) return amount;

        int dif = Math.min(amount, size - count);
        count += dif;
        inventory.put(item, get(item) + dif);

        return amount - dif;
    }

    @Override
    public int get(ItemType item) {
        Integer i = inventory.get(item);
        if (i == null) return 0;
        return i;
    }

    /** Returns amount for the first matching itemtype with the same value but difference in meta
     * @param item
     * @return
     */
    @Override
    public Amount[] getSimilar(ItemType item) {
        Array<Amount> amounts = new Array<>(Amount.class);
        int base = item.value & 0xff;
        for (Map.Entry<ItemType, Integer> e : inventory.entrySet()) {
            if ((e.getKey().value & 0xff) == base) amounts.add(new Amount(e.getKey(), e.getValue()));
        }

        return amounts.toArray();
    }

    @Override
    public Amount[] get(ItemCategory cat) {
        Array<Amount> amounts = new Array<>(Amount.class);
        for (Map.Entry<ItemType, Integer> e : inventory.entrySet()) {
            if (e.getKey().categories.contains(cat)) {
                amounts.add(new Amount(e.getKey(), e.getValue()));
            }
        }

        return amounts.toArray();
    }

    @Override
    public int getSum(ItemCategory cat) {
        int count = 0;
        for (Map.Entry<ItemType, Integer> e : inventory.entrySet()) {
            if (e.getKey().categories.contains(cat)) {
                count += e.getValue();
            }
        }
        return count;
    }

    public Set<Map.Entry<ItemType, Integer>> getAll() {
        return inventory.entrySet();
    }

    @Override
    public void saveData(Builder b) {
        b.Compound();
        Util.NBTwriteInventory(b, inventory);
        if (outputDock > -1) b
                .Float("pumpDelay", pumpOutDelay)
                .Byte("output", (byte) (isTubeAtOutput ? 1 : 0));
        b.End();
    }

    @Override
    public void loadData(CompoundTag tag) {
        int c = 0;
        try {
            c = Util.NBTreadInventory(tag, inventory);
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
        count = tag.Int("count", c);
        if (outputDock > -1) pumpOutDelay = tag.Float("pumpDelay", Const.DEFAULT_PUMP_OUT_DELAY);

        isTubeAtOutput = tag.Byte("output", (byte) 0) == 1;
    }

    protected void pumpOutItems() {
        Dock output = outputDock == -1 ? null : structure.getDocks()[outputDock];
        if (output == null) return;

        for (Iterator<Map.Entry<ItemType, Integer>> iter = inventory.entrySet().iterator(); iter.hasNext();) {
            Map.Entry<ItemType, Integer> e = iter.next();
            if (structure.layer.addItemEntity(e.getKey(), structure, output, structure)) {
                structure.setItemNotifications();

                int v = e.getValue();

                e.setValue(v - 1);
                count--;
                if (e.getValue() <= 0) iter.remove();

                // only pump out the first item, so we break
                break;
            }
        }
    }

    @Override
    public Component clone() {
        return new CInventory(size, outputDock).setPumpOutSpeed(pumpOutSpeed);
    }

    @Override
    public boolean canAccept(ItemType item) {
        return true;
    }

    @Override
    public boolean remove(ItemCategory category, int amount) {
        int left = amount;
        for (Iterator<Entry<ItemType, Integer>> iter = inventory.entrySet().iterator(); iter.hasNext();) {
            Entry<ItemType, Integer> e = iter.next();
            if (e.getKey().categories.contains(category)) {
                left = removeWithRest(e.getKey(), left);

                if (inventory.get(e.getKey()) == 0) iter.remove();

                if (left == 0) return true;
            }
        }
        return false;
    }

    @Override
    public void init() {}
}
