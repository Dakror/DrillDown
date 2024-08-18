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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.FluidTubeStructure;
import de.dakror.quarry.structure.base.IRotatable;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class Valve extends FluidTubeStructure implements IRotatable {
    public static final FluidTubeSchema classSchema = new FluidTubeSchema(0, StructureType.Valve, 10000, 1, 1,
            "valve", new Items(ItemType.SteelTube, 4, ItemType.SteelIngot, 8), null)
                    .sciences(ScienceType.OilProcessing, ScienceType.Routers)
                    .removeFlags(Flags.Draggable);

    static final TextureRegion top = Quarry.Q.atlas.findRegion("structure_valve_top");

    // direction of middle knob
    Direction dir;
    Direction flowDir;

    ImageButton ui;

    public Valve(int x, int y) {
        super(x, y, classSchema);
        dir = Direction.South;
        flowDir = Direction.North;
    }

    @Override
    public void rotate() {
        dir = dir.next();
        flowDir = flowDir.next();
        if (ui != null) ui.getImage().setRotation(flowDir.rot - Direction.South.rot);
        updateFlowDir();
        setDirty();
    }

    private void updateFlowDir() {
        if (ui != null) {
            ui.getImage().setOrigin(ui.getImage().getPrefWidth() / 2, ui.getImage().getPrefHeight() / 2);
            ui.getImage().setRotation(flowDir.rot - Direction.South.rot);
            ui.setChecked(flowDir == dir.inv());
        }
        updateStructures();
    }

    @Override
    protected void updateStructures() {
        Layer l = layer == null ? Game.G.layer : layer;

        for (Direction d : Direction.values) {
            if (d == dir.inv() || flowDir == dir.inv()) {
                types[d.ordinal()] = null;
                structures[d.ordinal()] = null;
            } else {
                Structure<?> s = Game.G.activeStructureTrail.get((x + d.dx) * l.height + (y + d.dy));
                if (s == null)
                    s = l.getStructure(x + d.dx, y + d.dy);

                DockType type = DockType.FluidOut;

                if (s != null && s.getDocks().length > 0) {
                    boolean any = false;
                    for (Dock dock : s.getDocks()) {
                        if (s.isNextToDock(x, y, d, dock)
                                && ((d == flowDir && dock.type == DockType.FluidIn)
                                        || dock.type == DockType.FluidOut)) {
                            type = dock.type;
                            any = true;
                            break;
                        }
                    }
                    if (!any) {
                        type = null;
                        s = null;
                    }
                } else if (s == null/* || !(s.getClass().equals(getClass()) || (s instanceof Valve && d == ((Valve) s).getDirection().inv()))*/) {
                    s = null;
                    type = null;
                }

                if (d == flowDir) {
                    type = DockType.FluidIn;
                }

                types[d.ordinal()] = type;
                structures[d.ordinal()] = s;
            }
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateStructures();
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return item.categories.contains(ItemCategory.Fluid)
                && Item.base(item) != ItemType._MoltenMetal
                && dir != flowDir.inv()
                && dir != this.dir;
    }

    @Override
    public void setRotation(Direction direction) {
        dir = direction;
        updateFlowDir();
        setDirty();
    }

    @Override
    public Direction getDirection() {
        return dir;
    }

    @Override
    public Object clone() {
        Valve valve = (Valve) super.clone();
        valve.dir = dir;
        valve.flowDir = flowDir;
        return valve;
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        if (ui == null) {
            ui = new ImageButton(Quarry.Q.skin, "flow_dir");
            ui.pad(16);
            ui.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    flowDir = flowDir.next();
                    updateFlowDir();
                }
            });
            updateFlowDir();
        }

        content.add(ui).expand();
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        spriter.add(getSchema().tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES,
                Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, dir.rot + 90);
        spriter.add(top, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES + 0.1f,
                Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, dir.rot + 90);
    }

    @Override
    public void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);

        try {
            dir = Direction.values[tag.Byte("dir")];
            flowDir = Direction.values[tag.Byte("flow")];
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    public void saveData(Builder b) {
        super.saveData(b);
        b
                .Byte("dir", (byte) dir.ordinal())
                .Byte("flow", (byte) flowDir.ordinal());
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b
                .Byte("dir", (byte) dir.ordinal())
                .Byte("flow", (byte) flowDir.ordinal());
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        try {
            dir = Direction.values[tag.Byte("dir")];
            flowDir = Direction.values[tag.Byte("flow")];
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    protected boolean isAllowedFluid(ItemType i) {
        return i.categories.contains(ItemCategory.Fluid) && Item.base(i) != ItemType._MoltenMetal;
    }
}
