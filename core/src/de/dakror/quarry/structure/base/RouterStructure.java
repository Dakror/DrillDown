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
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.RouterStructure.RouterSchema;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ConveyorBridge;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class RouterStructure extends Structure<RouterSchema> {
    public static class RouterSchema extends Schema {
        public RouterSchema(int version, StructureType type, boolean clickable, String tex, Items buildCosts, Sfx sfx) {
            super(version, type, clickable, 1, 1,
                    tex,
                    buildCosts,
                    sfx,
                    new Dock(0, 0, Direction.North, DockType.ItemIn), new Dock(0, 0, Direction.East, DockType.ItemIn), new Dock(0, 0, Direction.South, DockType.ItemIn), new Dock(0, 0, Direction.West, DockType.ItemIn));
            flags(Flags.NotRotatable);
        }

    }

    protected ItemType currentItem;
    protected Structure<?> currentSource;
    protected Direction currentSourceDirection;
    protected Conveyor[] tubes;
    protected int[] tubeIdx;
    protected boolean stuck;

    protected RouterStructure(int x, int y, RouterSchema schema) {
        super(x, y, schema);
        tubes = new Conveyor[4];
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return currentItem == null && !item.categories.contains(ItemCategory.Fluid);
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        if (currentItem != null || item.categories.contains(ItemCategory.Fluid)) return false;

        currentItem = item;
        currentSource = source;
        currentSourceDirection = dir.inv();
        return true;
    }

    protected void updateTubes() {
        for (int i = 0; i < 4; i++) {
            Direction d = Direction.values[i];
            Structure<?> s = layer.getStructure(x + d.dx, y + d.dy);
            tubes[i] = s != null && ((s.getSchema().type == StructureType.Conveyor && ((Conveyor) s).getDirection() != d.inv())
                    || (s.getSchema().type == StructureType.Hopper && ((Conveyor) s).getDirection() == d)
                    || (s.getSchema().type == StructureType.ElectricConveyor && ((Conveyor) s).getDirection() != d.inv())
                    || (s.getSchema().type == StructureType.ElectricConveyorCore && ((Conveyor) s).getDirection() == d)
                    || (s.getSchema().type == StructureType.ConveyorBridge
                            && (((ConveyorBridge) s).getDirection() != d.inv() && ((ConveyorBridge) s).getDirection2() != d.inv()))) ? (Conveyor) s : null;
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();

        tubes = new Conveyor[4];
        for (int i = 0; i < 4; i++) {
            tubes[i] = tubeIdx[i] == -1 ? null : (Conveyor) layer.getStructure(tubeIdx[i]);
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (stuck) {
            drawFullState(spriter);
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (layer != null && (dirtyBounds.touches(this) || tubes == null)) {
            updateTubes();
        }

        if (currentItem != null && gameSpeed > 0) {
            if (dispatchItem()) {
                stuck = false;
                currentItem = null;
                currentSource = null;
                setItemNotifications();
            } else {
                stuck = true;
            }
        }
    }

    protected abstract boolean dispatchItem();

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        currentItem = Item.get(tag.Short("item", (short) 0));
        tubeIdx = tag.IntArray("tubes", new int[4]);
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);

        tubeIdx = new int[4];
        for (int i = 0; i < 4; i++)
            tubeIdx[i] = tubes[i] != null ? tubes[i].x * layer.height + tubes[i].y : -1;

        b.Short("item", currentItem != null ? currentItem.value : 0)
                .IntArray("tubes", tubeIdx);

    }
}
