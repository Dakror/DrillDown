/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure.base;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class StorageStructure extends Structure<Schema> {
    protected boolean refundStorage;

    protected StorageStructure(int x, int y, Schema schema) {
        super(x, y, schema);
    }

    public boolean isEmpty() {
        return ((IStorage) components[0]).isEmpty();
    }

    public int getSpace() {
        return ((IStorage) components[0]).getSize() - ((IStorage) components[0]).getCount();
    }

    public boolean isRefundStorage() {
        return refundStorage;
    }

    public void setRefundStorage(boolean refundStorage) {
        this.refundStorage = refundStorage;
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        return addToInventory(item, 1, source);
    }

    public boolean addToInventory(ItemType item, int amount, Structure<?> source) {
        boolean result = ((IStorage) components[0]).add(item, amount);
        if (result) {
            if (!layer.fake) Game.G.addResources(item, amount, false);
            setItemNotifications();
            onItemCountChanged(item);
        }
        return result;
    }

    // used by Quarry.java
    public int addToInventoryWithRest(ItemType item, int amount) {
        int result = ((IStorage) components[0]).addWithRest(item, amount);
        setItemNotifications();
        onItemCountChanged(item);
        return result;
    }

    // used by Quarry.java
    public int removeFromInventoryWithRest(ItemType item, int amount) {
        int result = ((IStorage) components[0]).removeWithRest(item, amount);
        setItemNotifications();
        onItemCountChanged(item);
        return result;
    }

    @Override
    public boolean putBack(ItemType item, int amount) {
        if (!layer.fake) Game.G.addResources(item, amount, false);
        ((IStorage) components[0]).addUnsafe(item, amount);
        setItemNotifications();
        return true;
    }

    protected abstract void onItemCountChanged(ItemType item);

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (!((IStorage) components[0]).hasSpace())
            drawFullState(spriter);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        refundStorage = tag.Byte("refund", (byte) 0) == 1;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Byte("refund", (byte) (refundStorage ? 1 : 0));
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("refund", (byte) (refundStorage ? 1 : 0));
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        refundStorage = tag.Byte("refund", (byte) 0) == 1;
    }
}
