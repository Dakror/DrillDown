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

package de.dakror.quarry.game;

import com.badlogic.gdx.utils.Pool.Poolable;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.util.Savable;

/**
 * @author Maximilian Stark | Dakror
 */
public class ItemEntity implements Poolable, Savable {
    public ItemType item;
    public int x, y, slot, lastSlot;
    public float z;
    public double interp;
    public Direction dir;
    public Structure<?> src;
    public int srcIndex;
    public int srcLayer;

    @Override
    public void reset() {
        interp = 0;
        item = null;
        src = null;
        x = 0;
        y = 0;
        slot = 0;
        lastSlot = -1;
        z = Const.Z_ITEMS;
        dir = null;
        srcIndex = -1;
    }

    @Override
    public void save(Builder b) {
        b
                .Compound()
                .Short("value", item.value)
                .Int("x", x)
                .Int("y", y)
                .Int("slot", slot)
                .Int("lastSlot", lastSlot)
                .Double("interp", interp);

        if (z != Const.Z_ITEMS)
            b.Float("z", z);

        if (dir == null)
            b.Byte("dir", (byte) -1);
        else b.Byte("dir", (byte) dir.ordinal());
        if (src == null)
            b.Int("src", -1);
        else {
            b
                    .Int("src", src.x * src.layer.height + src.y)
                    .Int("layer", src.layer.index);
        }

        b.End();
    }

    public void load(CompoundTag tag) throws NBTException {
        item = Item.get(tag.Short("value"));
        x = tag.Int("x");
        y = tag.Int("y");
        slot = tag.Int("slot");
        lastSlot = tag.Int("lastSlot", -1);
        z = tag.Float("z", Const.Z_ITEMS);

        try {
            interp = tag.Double("interp") % 1.0;
        } catch (NBTException e) {
            interp = tag.Float("interp") % 1.0f;
        }

        int d = tag.Byte("dir");
        if (d > -1)
            dir = Direction.values[d];
        srcIndex = tag.Int("src", -1);
        srcLayer = tag.Int("layer", 0);
    }

    public void postLoad() {
        if (srcIndex > -1) {
            Layer l = Game.G.getLayer(srcLayer);
            if (l != null)
                src = l.getStructure(srcIndex);
        }
    }
}
