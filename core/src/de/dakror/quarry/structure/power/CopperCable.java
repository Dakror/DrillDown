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

package de.dakror.quarry.structure.power;

import java.util.Arrays;
import java.util.HashSet;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.ConnectionType;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.CopyRegionHelper;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class CopperCable extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.CopperCable, false, 1, 1,
            "copper_cable", new Items(ItemType.CopperWire, 1), null)
                    .sciences(ScienceType.Electricity)
                    .flags(Flags.Draggable, Flags.NoDustEffect, Flags.NotRotatable);

    static final TextureRegion ew = Quarry.Q.atlas.findRegion("structure_copper_cable_ew");
    static final TextureRegion es = Quarry.Q.atlas.findRegion("structure_copper_cable_es");
    static final int plus = (Const.TILE_SIZE + Const.CABLE_WIDTH) / 2;
    static final int minus = (Const.TILE_SIZE - Const.CABLE_WIDTH) / 2;

    ConnectionType[] dirs = new ConnectionType[4];
    int directions = 0;

    Structure<?>[] structures = new Structure[4];

    CopperCable cachedEnd;

    // gets resolved into #structures in postLoad
    int[] structs;

    public CopperCable(int x, int y) {
        super(x, y, classSchema);
        Arrays.fill(dirs, ConnectionType.No);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        if (layer == null) {
            directions = 0;
            Arrays.fill(dirs, ConnectionType.No);
            Arrays.fill(structures, null);
            updateFacing(true);
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        boolean hasFlag = dirtyBounds.hasAnyFlag(Bounds.Flags.CABLE | Bounds.Flags.POWERNODE);

        if (hasFlag) {
            cachedEnd = null;
        }

        if (layer == null) {
            updateFacing(true);
        } else if (dirtyBounds.touches(this)) {
            updateFacing(true);
        }
    }

    @Override
    public void postLoad() {
        if (structs != null) {
            for (int i = 0; i < 4; i++) {
                structures[i] = (structs[i] == -1 ? null : layer.getAny(structs[i], false));

                // Cleanup for old save files <v93
                if (dirs[i] == ConnectionType.No) {
                    structures[i] = null;
                }
            }

            structs = null;
        }

        updateFacing(true);
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        if (directions > 0) {
            int off = (directions < 2 ? Const.CABLE_WIDTH / 2 : 0);
            for (Direction d : Direction.values) {
                if (dirs[d.ordinal()] != ConnectionType.No) {
                    int mx = x * Const.TILE_SIZE, my = y * Const.TILE_SIZE, rot = 0;
                    switch (d) {
                        case East:
                            mx += plus + off;
                            my += minus;
                            break;
                        case North:
                            mx += plus;
                            my += plus + off;
                            rot = 90;
                            break;
                        case South:
                            rot = -90;
                            mx += minus;
                            my += minus - off;
                            break;
                        case West:
                            my += minus;
                            break;
                        default:
                            break;
                    }

                    spriter.add(ew, mx, my, Const.Z_CABLES, 0, 0, minus - off, Const.CABLE_WIDTH, 1, 1, rot);
                }
            }

            int mx = x * Const.TILE_SIZE, my = y * Const.TILE_SIZE, rot = 0;

            TextureRegion filler = null;

            if (dirs[0] != ConnectionType.No && dirs[1] != ConnectionType.No) {
                filler = es;
                mx += plus;
                my += minus;
                rot = 90;
            }

            if (dirs[1] != ConnectionType.No && dirs[2] != ConnectionType.No) {
                filler = es;
                mx += minus;
                my += minus;
                rot = 0;
            }

            if (dirs[0] != ConnectionType.No && dirs[3] != ConnectionType.No) {
                filler = es;
                mx += plus;
                my += plus;
                rot = 180;
            }

            if (dirs[2] != ConnectionType.No && dirs[3] != ConnectionType.No) {
                filler = es;
                mx += minus;
                my += plus;
                rot = -90;
            }

            if (dirs[0] != ConnectionType.No && dirs[2] != ConnectionType.No) {
                mx = x * Const.TILE_SIZE + plus;
                my = y * Const.TILE_SIZE + minus;
                filler = ew;
                rot = 90;
            }

            if (dirs[1] != ConnectionType.No && dirs[3] != ConnectionType.No) {
                mx = x * Const.TILE_SIZE + minus;
                my = y * Const.TILE_SIZE + minus;
                filler = ew;
            }

            if (filler != null)
                spriter.add(filler, mx, my, Const.Z_CABLES, 0, 0, Const.CABLE_WIDTH, Const.CABLE_WIDTH, 1, 1, rot);
        }
        if (directions <= 1) {
            int w = Const.TILE_SIZE - 2 * Const.CABLE_OFFSET;
            spriter.add(getSchema().tex, x * Const.TILE_SIZE + Const.CABLE_OFFSET, y * Const.TILE_SIZE + Const.CABLE_OFFSET, Const.Z_CABLES + .1f, w, w);
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        /*if (Game.DRAW_DEBUG) {
            if (cachedEnd != null) {
                shaper.set(ShapeType.Filled);
                shaper.setColor(Color.YELLOW);
                shaper.rectLine((x) * Const.TILE_SIZE, (y) * Const.TILE_SIZE,
                        (cachedEnd.x + 0.5f) * Const.TILE_SIZE, (cachedEnd.y + 0.5f) * Const.TILE_SIZE, 2);
            } else {
                shaper.set(ShapeType.Filled);
                shaper.setColor(Color.YELLOW);
                shaper.circle((x + 0.5f) * Const.TILE_SIZE, (y + 0.5f) * Const.TILE_SIZE, 2);
            }
        }*/
    }

    public boolean connectTo(Structure<?> s, Direction myDirectionToS) {
        if (structures[myDirectionToS.ordinal()] != null &&
                structures[myDirectionToS.ordinal()].equals(s)) return true;

        if (directions == 2) return false;

        ConnectionType old = dirs[myDirectionToS.ordinal()];

        dirs[myDirectionToS.ordinal()] = ConnectionType.Open;
        structures[myDirectionToS.ordinal()] = s;
        if (old == ConnectionType.No) {
            directions++;
        }
        setDirty();

        return true;
    }

    public void updateFacing(boolean propagate) {
        boolean changed = false;

        // remove directions that are not present anymore and update current ones
        for (Direction d : Direction.values) {
            int ord = d.ordinal();
            Structure<?> s = null;
            if (layer == null) {
                s = Game.G.activeStructureTrail.get((x + d.dx) * Game.G.layer.height + (y + d.dy));
            } else if (s == null) {
                s = layer.getAny(x + d.dx, y + d.dy, false);
            }
            if (s == this || (s != null && !(s instanceof CopperCable) && s.getSchema().powerDocks == 0)) {
                s = null;
            }

            // we dont want to add any structures
            if (structures[ord] == null) continue;

            if (s == null) {
                // structure is gone
                changed = true;
                structures[ord] = null;
                // decrement connection count, since s is empty

                dirs[ord] = ConnectionType.No;
                directions = Math.max(0, directions - 1);
            } else if (structures[ord] != s) {
                // different structure
                ConnectionType t = getConnection(s, d);

                if (dirs[ord] != ConnectionType.No) {
                    if (t == ConnectionType.No) {
                        // new building, but without connection
                        structures[ord] = null;
                        dirs[ord] = ConnectionType.No;
                        directions = Math.max(0, directions - 1);
                        changed = true;
                    } else {
                        // new building replacing the old one
                        structures[ord] = s;
                        dirs[ord] = t;
                    }
                }
            } else {
                // update a pre-existing coppercable over which we've just built a new cable
                if (propagate && structures[ord] instanceof CopperCable && structures[ord].layer != null) {
                    ((CopperCable) s).updateFacing(false);
                }

                // same structure, but maybe not same condition
                ConnectionType t = getConnection(s, d);

                if (dirs[ord] != ConnectionType.No && t == ConnectionType.No) {
                    // changed, now closed
                    dirs[ord] = t;
                    directions = Math.max(0, directions - 1);
                    structures[ord] = null;
                    changed = true;

                    // cascade the change
                    if (s instanceof CopperCable && propagate && layer != null)
                        ((CopperCable) s).updateFacing(false);
                }
            }
        }

        if (directions < 2) {
            // first run only cables (to prefer cables)
            o: for (int i = 0; i < 2; i++) {
                for (Direction d : Direction.values) {
                    if (directions >= 2) break o;

                    int ord = d.ordinal();
                    Structure<?> s = structures[ord];
                    if (s == null) {
                        if (layer == null) {
                            s = Game.G.activeStructureTrail.get((x + d.dx) * Game.G.layer.height + (y + d.dy));
                        } else if (s == null) {
                            s = layer.getAny(x + d.dx, y + d.dy, false);
                        }
                    }

                    ConnectionType p = dirs[ord];

                    if ((i == 0 && s instanceof CopperCable) || (i == 1 && s != null)) {
                        ConnectionType t = getConnection(s, d);

                        // unchanged, skip connection
                        if (p == t) continue;

                        // changed, now open
                        if (t != ConnectionType.No) {
                            if (!(s instanceof CopperCable)
                                    || ((CopperCable) s).connectTo(this, d.inv())) {
                                dirs[ord] = t;
                                structures[ord] = s;
                                if (p == ConnectionType.No) {
                                    directions++;
                                }
                                changed = true;
                            }
                        }
                        /*if (s instanceof CopperCable && propagate && layer != null)
                            ((CopperCable) s).updateFacing(false);*/
                    }
                }
            }
        }

        if (changed) {
            setDirty();
        }
    }

    public int getDirections() {
        return directions;
    }

    public CopperCable walkToEnd() {
        if (cachedEnd != null)
            return cachedEnd;

        CopperCable c = this;
        int prevDirection = -1;

        HashSet<CopperCable> seen = new HashSet<>();

        o: while (c != null) {
            for (int i = 0; i < 4; i++) {
                if (i != prevDirection
                        && c.dirs[i] == ConnectionType.Open
                        && c.structures[i] instanceof CopperCable
                        && !Game.G.activeStructureTrail.containsValue(c.structures[i], true)
                        && !seen.contains(c)) {
                    seen.add(c);
                    c = (CopperCable) c.structures[i];
                    prevDirection = (i + 2) % 4;
                    continue o;
                }
            }
            break;
        }

        cachedEnd = c;
        return c;
    }

    protected ConnectionType getConnection(Structure<?> s, Direction d) {
        if (s.getSchema().type == StructureType.CopperCable) {
            CopperCable c = (CopperCable) s;
            if (c.dirs[d.inv().ordinal()] == ConnectionType.Open) {
                return ConnectionType.Open;
            } else if (c.dirs[d.inv().ordinal()] == ConnectionType.Dock) {
                return ConnectionType.No;
            } else if (c.directions < 2) {
                return ConnectionType.Open;
            } else {
                return ConnectionType.No;
            }
        } else {
            for (Dock k : s.getDocks()) {
                if ((k.type == DockType.Power)
                        && s.x + k.x + k.dir.dx == x && s.y + k.y + k.dir.dy == y && d == k.dir.inv())
                    return ConnectionType.Dock;
            }
            return ConnectionType.No;
        }
    }

    public Structure<?>[] getStructures() {
        return structures;
    }

    @Override
    public void saveData(Builder b) {
        super.saveData(b);
        byte[] dir = new byte[4];
        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            dir[i] = dirs[i].value;
            str[i] = structures[i] == null ? -1 : structures[i].x * layer.height + structures[i].y;
        }
        b
                .ByteArray("dirs", dir)
                .IntArray("structs", str);

    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);

        byte[] dir = new byte[4];
        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            int tx = x + Direction.values[i].dx;
            int ty = y + Direction.values[i].dy;
            if (tx < copyRegion[0] || ty < copyRegion[1] || tx >= copyRegion[0] + copyRegion[2] || ty >= copyRegion[1] + copyRegion[3]) {
                dir[i] = ConnectionType.No.value;
                str[i] = -1;
            } else {
                dir[i] = dirs[i].value;
                str[i] = structures[i] == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, structures[i].x * layer.height + structures[i].y);
            }
        }
        b
                .ByteArray("dirs", dir)
                .IntArray("structs", str);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        try {
            byte[] dir = tag.ByteArray("dirs");
            for (int i = 0; i < 4; i++) {
                dirs[i] = Dock.connectionTypes[dir[i]];
            }
            int[] str = tag.IntArray("structs");
            for (int i = 0; i < 4; i++) {
                this.structures[i] = CopyRegionHelper.getAny(this, pasteRegion, str[i], false);
            }
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }

        directions = 0;
        for (ConnectionType b : dirs) {
            if (b != ConnectionType.No) directions++;
        }

        cachedEnd = null;
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        try {
            byte[] dir = tag.ByteArray("dirs");
            for (int i = 0; i < 4; i++) {
                dirs[i] = Dock.connectionTypes[dir[i]];
            }
            structs = tag.IntArray("structs");
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }

        directions = 0;
        for (ConnectionType b : dirs) {
            if (b != ConnectionType.No) directions++;
        }
    }
}
