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

package de.dakror.quarry.structure.logistics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.IFlippable;
import de.dakror.quarry.structure.base.IItemZModifier;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class ConveyorBridge extends Conveyor implements IItemZModifier, IFlippable {
    public static final ConveyorSchema classSchema = new ConveyorSchema(0, StructureType.ConveyorBridge, true, 1, 1, "conveyorbridge", new Items(ItemType.Stone, 4, ItemType.Scaffolding, 2), null)
            .flags(Flags.NoDustEffect);

    static final TextureRegion tex2 = Quarry.Q.atlas.findRegion("structure_conveyor_we");
    static final TextureRegion tex3 = Quarry.Q.atlas.findRegion("structure_conveyorbridge3");

    protected boolean dir2Prev;

    public ConveyorBridge(int x, int y) {
        this(x, y, Direction.South, classSchema);
    }

    protected ConveyorBridge(int x, int y, ConveyorSchema schema) {
        this(x, y, Direction.South, schema);
    }

    public ConveyorBridge(int x, int y, Direction direction, ConveyorSchema schema) {
        super(x, y, schema);
        dir = direction;
        dir2Prev = true;
    }

    public ConveyorBridge(int x, int y, Direction direction, boolean prev, ConveyorSchema schema) {
        super(x, y, schema);
        dir = direction;
        dir2Prev = prev;
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        spriter.add(getSchema().tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, getDirection2().rot);

        spriter.add(tex2, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES + 0.01f,
                Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, dir.rot);

        spriter.add(tex3, x * Const.TILE_SIZE + Const.TUBE_OFFSET, y * Const.TILE_SIZE + Const.TUBE_OFFSET, Const.Z_STRUCTURES - 0.1f,
                s / 2, s / 2, s, s, 1, 1, dir.rot);
    }

    /**
     * structures 
     * 0 = +dir
     * 1 = +dir2
     * 2 = -dir
     * 3 = -dir2
     */
    @Override
    protected void updateStructures() {
        Layer l = layer == null ? Game.G.layer : layer;

        Structure<?> s = Game.G.activeStructureTrail.get((x + dir.dx) * l.height + (y + dir.dy));
        if (s == null) s = l.getStructure(x + dir.dx, y + dir.dy);

        structures[0] = s;

        s = Game.G.activeStructureTrail.get((x - dir.dx) * l.height + (y - dir.dy));
        if (s == null) s = l.getStructure(x - dir.dx, y - dir.dy);

        structures[2] = s;

        Direction d2 = getDirection2();
        s = Game.G.activeStructureTrail.get((x + d2.dx) * l.height + (y + d2.dy));
        if (s == null) s = l.getStructure(x + d2.dx, y + d2.dy);

        structures[1] = s;

        s = Game.G.activeStructureTrail.get((x - d2.dx) * l.height + (y - d2.dy));
        if (s == null) s = l.getStructure(x - d2.dx, y - d2.dy);

        structures[3] = s;
    }

    @Override
    protected void notifyNeighbors(boolean chain) {
        if (structures[2] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(dir))) {
            ((Conveyor) structures[2]).itemChanges = true;
            if (chain) ((Conveyor) structures[2]).updateItems(0, 1, true, false);
        }
        if (structures[3] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(getDirection2()))) {
            ((Conveyor) structures[3]).itemChanges = true;
            if (chain) ((Conveyor) structures[3]).updateItems(0, 1, true, false);
        }
    }

    @Override
    public boolean isItemSlotFree(int slot) {
        return items[slot] == null;
    }

    @Override
    public Structure<?> getStructureInDirection(Direction dir) {
        if (dir == this.dir) return structures[0];
        else if (dir == getDirection2()) return structures[1];
        else if (dir == this.dir.inv()) return structures[2];
        else if (dir == getDirection2().inv()) return structures[3];
        else return null;
    }

    @Override
    public Direction getNextDirection(ItemType item, Direction prev, float currentZ, int slot) {
        if (prev != null) {
            if (prev == dir.inv() || prev == getDirection2().inv()) return prev.inv();
            return prev;
        }

        if (currentZ == Const.Z_ITEMS) return getDirection2();
        else return dir;
    }

    @Override
    public Direction getCurrentDirection(ItemType item, Direction prev, float currentZ, int slot) {
        return getNextDirection(item, prev, currentZ, slot);
    }

    public Direction getDirection2() {
        if (dir2Prev) return dir.prev();
        return dir.next();
    }

    @Override
    public float getItemZ(ItemType type, Direction direction) {
        if (Math.abs(direction.dx) == Math.abs(dir.dx)) return Const.Z_CABLES - 0.1f;
        else return Const.Z_ITEMS;
    }

    @Override
    public void flip() {
        dir2Prev = !dir2Prev;
        setDirty();
    }

    @Override
    public Object clone() {
        return new ConveyorBridge(x, y, dir, dir2Prev, classSchema);
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Byte("prev", (byte) (dir2Prev ? 1 : 0));
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        dir2Prev = tag.Byte("prev", (byte) 1) == 1;
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("prev", (byte) (dir2Prev ? 1 : 0));
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        dir2Prev = tag.Byte("prev", (byte) 1) == 1;
    }
}
