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

import java.util.HashMap;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.FluidType;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.FluidTubeStructure.FluidTubeSchema;
import de.dakror.quarry.structure.logistics.Valve;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.CopyRegionHelper;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class FluidTubeStructure extends Structure<FluidTubeSchema> implements ITube {
    public static class FluidTubeSchema extends Schema {
        public final int maxFluid;

        public FluidTubeSchema(int version, StructureType type, int maxFluid, int width, int height, String tex, Items buildCosts, Sfx sfx) {
            super(version, type, true, width, height, tex, buildCosts, sfx);

            this.maxFluid = maxFluid;
            flags(Flags.Draggable, Flags.NoDustEffect);
            loudness = 0.5f;
        }
    }

    protected static final HashMap<Class<? extends FluidTubeStructure>, TextureRegion[]> texCache = new HashMap<>();

    // indices in tex cache array
    private static final int TEX_N = 0;
    private static final int TEX_EW = 1;
    private static final int TEX_SW = 2;
    private static final int TEX_ESW = 3;
    private static final int TEX_NESW = 4;

    static final float decupple = 1f;

    static final int size = Const.TILE_SIZE - 2 * Const.TUBE_OFFSET;

    protected int fluidLevel;
    protected FluidType fluid;
    protected ItemType fluidItem;
    protected float delay;

    // gets resolved into #structures in postLoad
    protected int[] structs;
    protected Structure<?>[] structures = new Structure[4];
    protected DockType[] types = new DockType[4];

    // determines when to cut the connection to or from Docks (only visually) gets renewed when acceptFluid is called
    protected float[] decuppleTime = new float[4];

    Table ui;
    Container<Table> container;
    ItemType uiType;

    public FluidTubeStructure(int x, int y, FluidTubeSchema schema) {
        super(x, y, schema);

        initTextures();
    }

    private void initTextures() {
        if (texCache.containsKey(getClass())) return;

        texCache.put(getClass(), new TextureRegion[] {
                Quarry.Q.atlas.findRegion("structure_" + getSchema().texName + "_n"),
                Quarry.Q.atlas.findRegion("structure_" + getSchema().texName + "_ew"),
                Quarry.Q.atlas.findRegion("structure_" + getSchema().texName + "_sw"),
                Quarry.Q.atlas.findRegion("structure_" + getSchema().texName + "_esw"),
                Quarry.Q.atlas.findRegion("structure_" + getSchema().texName + "_nesw")
        });
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (fluidLevel >= getSchema().maxFluid) return false;
        if (fluid != null) return item == fluidItem;
        else return isAllowedFluid(item);
    }

    protected abstract boolean isAllowedFluid(ItemType i);

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (fluidLevel >= getSchema().maxFluid) return amount;
        if (fluid != null && item != fluidItem) return amount;

        if (fluid == null) {
            if (!isAllowedFluid(item)) return amount;
            fluid = Item.fluid((byte) item.value);
            fluidItem = item;
        }

        if (delay <= 0) delay = fluid.viscosity;
        int old = fluidLevel;
        fluidLevel = Math.min(getSchema().maxFluid, fluidLevel + amount);

        if (clicked) updateUI();

        return Math.max(0, old + amount - fluidLevel);
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (dirtyBounds.touches(this) || layer == null) {
            updateStructures();
        }

        if (fluidLevel <= 0 && fluid != null) {
            fluidLevel = 0;
            fluid = null;
            fluidItem = null;
            delay = 0;
            updateUI();
        }
        if (fluid == null || gameSpeed == 0) return;

        delay -= deltaTime * gameSpeed;

        if (delay <= 0) {
            for (int i = 0; i < 4; i++) {
                if (structures[i] != null && types[i] != DockType.FluidOut && structures[i].canAccept(fluidItem, x, y, Direction.values[i])) {
                    int dif = 0;
                    if (structures[i] instanceof FluidTubeStructure && !(structures[i] instanceof Valve)) {
                        if (((FluidTubeStructure) structures[i]).fluidLevel < fluidLevel) {
                            dif = fluidLevel - ((FluidTubeStructure) structures[i]).fluidLevel;
                        }
                    } else {
                        dif = fluidLevel;
                    }

                    dif = (int) Math.ceil(dif * fluid.pressure);

                    if (dif > 0) {
                        fluidLevel = fluidLevel - dif + structures[i].acceptFluid(fluidItem, dif, this);
                        decuppleTime[i] = decupple;
                    }
                }
            }

            delay = fluid.viscosity;

            if (clicked) updateUI();
        }

        for (int i = 0; i < 4; i++) {
            if (decuppleTime[i] > 0) {
                decuppleTime[i] -= deltaTime * gameSpeed;
                if (decuppleTime[i] <= 0) {
                    decuppleTime[i] = 0;
                }
            }
        }
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        if (!fromLoading && layer == null) {
            updateStructures();
        }
    }

    protected void updateStructures() {
        Layer l = layer == null ? Game.G.layer : layer;
        for (Direction d : Direction.values) {
            Structure<?> s = Game.G.activeStructureTrail.get((x + d.dx) * l.height + (y + d.dy));
            if (s == null)
                s = l.getStructure(x + d.dx, y + d.dy);

            DockType type = null;

            if (s != null && s.getDocks().length > 0) {
                boolean any = false;
                for (Dock dock : s.getDocks()) {
                    if (s.isNextToDock(x, y, d, dock) && (dock.type == DockType.FluidIn || dock.type == DockType.FluidOut)) {
                        type = dock.type;
                        any = true;
                        break;
                    }
                }
                if (!any) s = null;
            } else if (s == null || !(s instanceof FluidTubeStructure) || (s instanceof Valve && ((Valve) s).getDirection() == d)) {
                s = null;
                type = null;
            }

            types[d.ordinal()] = type;
            structures[d.ordinal()] = s;
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (fluidLevel > 0) {
            float width = Math.max(1, 27.5f * (fluidLevel / (float) getSchema().maxFluid));

            float o = (Const.TILE_SIZE - width) / 2;

            spriter.add(fluid.icon, x * Const.TILE_SIZE + o, y * Const.TILE_SIZE + o, Const.Z_TUBES + 0.01f, width, width);

            // TODO: make prettier (textures get stretched)
            if (structures[0] != null || decuppleTime[0] > 0) {
                spriter.add(fluid.icon, x * Const.TILE_SIZE + o, y * Const.TILE_SIZE + (Const.TILE_SIZE - width) / 2 + width, Const.Z_TUBES + 0.01f, width, o);
            }
            if (structures[1] != null || decuppleTime[1] > 0) {
                spriter.add(fluid.icon, x * Const.TILE_SIZE + o + width, y * Const.TILE_SIZE + o, Const.Z_TUBES + 0.01f, o, width);
            }
            if (structures[2] != null || decuppleTime[2] > 0) {
                spriter.add(fluid.icon, x * Const.TILE_SIZE + o, y * Const.TILE_SIZE, Const.Z_TUBES + 0.01f, width, o);
            }
            if (structures[3] != null || decuppleTime[3] > 0) {
                spriter.add(fluid.icon, x * Const.TILE_SIZE, y * Const.TILE_SIZE + o, Const.Z_TUBES + 0.01f, o, width);
            }
        }
    }

    @Override
    public void saveData(Builder b) {
        super.saveData(b);

        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            str[i] = structures[i] == null ? -1 : structures[i].x * layer.height + structures[i].y;
        }
        b.IntArray("structs", str);

        if (fluidItem != null) {
            b.Short("fluid", fluidItem.value);
            b.Short("level", (short) fluidLevel);
        }
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);

        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            str[i] = structures[i] == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, structures[i].x * layer.height + structures[i].y);
        }
        b.IntArray("structs", str);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        try {
            int[] str = tag.IntArray("structs");
            for (int i = 0; i < 4; i++) {
                this.structures[i] = CopyRegionHelper.getStructure(this, pasteRegion, str[i]);
            }
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);

        try {
            structs = tag.IntArray("structs");
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }

        short flu = tag.Short("fluid", (short) 0);
        if (flu != 0) {
            try {
                fluidItem = Item.get(flu);
                if (fluidItem != null)
                    fluid = Item.fluid((byte) fluidItem.value);
                fluidLevel = tag.Short("level");
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }

    @Override
    public void postLoad() {
        if (structs != null) {
            for (int i = 0; i < 4; i++)
                structures[i] = (structs[i] == -1 ? null : layer.getStructure(structs[i]));
            structs = null;
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<>();
        }
        if (ui == null || uiType != fluidItem) {
            uiType = fluidItem;
            if (uiType != null) {
                ui = GameUi.createResourceTable(32, Quarry.Q.skin, uiType, GameUi.formatResourceAmount(fluidLevel / 1000f, true) + "L");
                container.setActor(ui);
            } else {
                ui = null;
                container.setActor(null);
            }
        } else {
            ((Label) ui.getChildren().get(1)).setText(GameUi.formatResourceAmount(fluidLevel / 1000f, true) + "L");
            ((TextTooltip) ui.getListeners().get(0)).getActor().setText(GameUi.formatResourceAmount(fluidLevel / 1000f, true) + "L " + uiType.title);

        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(container).grow();
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        int x = this.x * Const.TILE_SIZE + Const.TUBE_OFFSET;
        int y = this.y * Const.TILE_SIZE + Const.TUBE_OFFSET;

        TextureRegion tex = getSchema().tex;
        int rot = 0;

        TextureRegion[] seg = texCache.get(getClass());

        if (structures[0] != null && structures[1] != null && structures[2] != null && structures[3] != null) {
            tex = seg[TEX_NESW];

            for (Direction d : Direction.values) {
                spriter.add(seg[TEX_EW], x + size * d.dx, y + size * d.dy, Const.Z_TUBES, size / 2, size / 2, Const.TUBE_OFFSET, size, 1, 1, d.rot);
            }
        } else {
            int prio = 0;

            for (Direction d : Direction.values) {
                if (structures[d.ordinal()] != null && structures[d.inv().ordinal()] != null && (structures[d.prev().ordinal()] != null || structures[d.next().ordinal()] != null)) {
                    prio = 3;
                    tex = seg[TEX_ESW];
                    rot = d.rot + (structures[d.prev().ordinal()] != null ? 180 : 0);
                } else if (prio <= 2 && structures[d.ordinal()] != null && structures[d.inv().ordinal()] != null) {
                    prio = 2;
                    tex = seg[TEX_EW];
                    rot = d.rot;
                } else if (prio <= 1 && structures[d.ordinal()] != null && structures[d.next().ordinal()] != null) {
                    prio = 1;
                    tex = seg[TEX_SW];
                    rot = d.rot + 90;
                } else if (prio <= 0 && structures[d.ordinal()] != null) {
                    prio = 0;
                    tex = seg[TEX_N];
                    rot = d.rot - 90;
                }

                if (structures[d.ordinal()] != null) {
                    spriter.add(seg[TEX_EW], x + size * d.dx, y + size * d.dy, Const.Z_TUBES, size / 2, size / 2, Const.TUBE_OFFSET, size, 1, 1, d.rot);
                }
            }
        }
        if (tex != null) spriter.add(tex, x, y, Const.Z_TUBES, size / 2, size / 2, size, size, 1, 1, rot);
    }
}
