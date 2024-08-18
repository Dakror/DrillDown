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

package de.dakror.quarry.structure.logistics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.IntMap.Entry;

import de.dakror.common.libgdx.Pair;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.ItemEntity;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;

/**
 * @author Maximilian Stark | Dakror
 */
public class Hopper extends ConveyorBridge {

    public static final ConveyorSchema classSchema = new ConveyorSchema(0, StructureType.Hopper, true, 1, 1,
            "hopper",
            new Items(ItemType.Wood, 8, ItemType.Stone, 8), null);

    static TextureRegion tex = Quarry.Q.atlas.findRegion("structure_conveyor_we");
    static TextureRegion pad = Quarry.Q.atlas.findRegion("structure_conveyor_pad1");

    Structure<?> target;

    int blockerSlot, deciderSlot, minSlot, maxSlot, dirSlot, invDirSlot;

    public Hopper(int x, int y) {
        super(x, y, classSchema);
    }

    public Hopper(int x, int y, Direction direction, boolean prev, ConveyorSchema schema) {
        super(x, y, direction, prev, schema);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (!fromLoading && layer != null) {
            updateTarget();
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateTarget();
    }

    protected void updateTarget() {
        target = layer.getStructure(x + dir.dx, y + dir.dy);

        dirSlot = Layer.getStartingSlot(dir);
        invDirSlot = Layer.getStartingSlot(dir.inv());
        minSlot = Math.min(dirSlot, invDirSlot);
        maxSlot = Math.max(dirSlot, invDirSlot);

        deciderSlot = dirSlot + (Const.ITEMS_PER_CONVEYOR / 2 - 1) * (invDirSlot == maxSlot ? 1 : -1);
        blockerSlot = invDirSlot - (Const.ITEMS_PER_CONVEYOR / 2 - 1) * (invDirSlot == maxSlot ? 1 : -1);
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        spriter.add(pad, x * Const.TILE_SIZE, y * Const.TILE_SIZE + Const.TUBE_OFFSET, Const.Z_TUBES,
                Const.TILE_SIZE / 2, s / 2, 10, s, 1, 1, getDirection2().rot - 180);

        spriter.add(tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES,
                Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, dir.rot);

        spriter.add(getSchema().tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES,
                Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, dir.rot + 90);
    }

    @Override
    public boolean isItemSlotFree(int slot) {
        Direction d = getDirection2();
        if (d == Direction.East && slot == Const.ITEMS_PER_CONVEYOR) return false;
        if (d == Direction.West && slot == Const.ITEMS_PER_CONVEYOR * 2 - 1) return false;
        if (d == Direction.North && slot == 0) return false;
        if (d == Direction.South && slot == Const.ITEMS_PER_CONVEYOR - 1) return false;

        return super.isItemSlotFree(slot);
    }

    private boolean isQueueFree() {
        int dir = dirSlot == minSlot ? 1 : -1;

        for (int i = 0; i < Const.ITEMS_PER_CONVEYOR / 2 - 1; i++) {
            if (!isItemSlotFree(blockerSlot + i * dir) && items[blockerSlot + i * dir].dir == getDirection2()) return false;
        }

        return true;
    }

    @Override
    protected void updateItemWithin(ItemEntity e, boolean chain) {
        if (e.slot == deciderSlot && target != null && !target.canAccept(e.item, x, y, dir)) {
            if (isQueueFree()) {
                e.dir = getDirection2();
            } else {
                e.dir = null;
            }
        }

        super.updateItemWithin(e, chain);
    }

    @Override
    protected void notifyNeighbors(boolean chain) {
        if (structures[2] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(dir))) {
            ((Conveyor) structures[2]).itemChanges = true;
            if (chain) ((Conveyor) structures[2]).updateItems(0, 1, true, false);
        }
    }

    public boolean isValidRotation(Direction dir) {
        Layer l = layer;
        if (l == null) l = Game.G.layer;
        Structure<?> s = l.getStructure(x + dir.dx, y + dir.dy);

        if (Game.G.pasteMode) {
            // XXX this is slow as fuck
            for (Entry<Pair<Structure<?>, CompoundTag>> pair : Game.G.copyStructures.entries()) {
                Structure<?> q = pair.value.getKey();
                if (q.x <= x + dir.dx && q.x + q.getWidth() > x + dir.dx
                        && q.y <= y + dir.dy && q.y + q.getHeight() > y + dir.dy) {
                    s = q;
                    break;
                }
            }
        }

        if (s == null || s.getDocks().length == 0) return false;

        for (Dock d : s.getDocks()) {
            if (s.isNextToDock(x, y, dir, d) && d.type == DockType.ItemIn) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setRotation(Direction direction) {
        if (isValidRotation(direction) || layer == null)
            super.setRotation(direction);
    }

    @Override
    public float getItemZ(ItemType type, Direction direction) {
        return Const.Z_ITEMS;
    }

    @Override
    public Direction getCurrentDirection(ItemType item, Direction prev, float currentZ, int slot) {
        if (target != null && target.canAccept(item, x, y, dir)
                && slot >= Math.min(dirSlot, invDirSlot) && slot <= Math.max(dirSlot, invDirSlot))
            return dir;
        return getDirection2();
    }

    @Override
    public Direction getNextDirection(ItemType item, Direction prev, float currentZ, int slot) {
        if (target != null && target.canAccept(item, x, y, dir))
            return dir;
        return getDirection2();
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (dirtyBounds.touches(this)) {
            updateTarget();
        }
    }

    @Override
    public Object clone() {
        return new Hopper(x, y, dir, dir2Prev, classSchema);
    }
}
