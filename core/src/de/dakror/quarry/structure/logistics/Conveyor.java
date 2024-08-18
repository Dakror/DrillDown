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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pools;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
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
import de.dakror.quarry.structure.base.IRotatable;
import de.dakror.quarry.structure.base.ITube;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.logistics.Conveyor.ConveyorSchema;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.CopyRegionHelper;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Conveyor extends Structure<ConveyorSchema> implements IRotatable, ITube {
    public static class ConveyorSchema extends Schema {
        TextureRegion texWE;
        TextureRegion texNE;
        TextureRegion texNSE;
        TextureRegion texWNE;
        TextureRegion texWNSE;

        public ConveyorSchema(int version, StructureType type, boolean clickable, int width, int height, String tex, Items buildCosts, Sfx sfx, Dock... docks) {
            super(version, type, clickable, width, height, tex, buildCosts, sfx, docks);
            texWE = Quarry.Q.atlas.findRegion("structure_" + tex + "_we");
            texNE = Quarry.Q.atlas.findRegion("structure_" + tex + "_ne");
            texNSE = Quarry.Q.atlas.findRegion("structure_" + tex + "_nse");
            texWNE = Quarry.Q.atlas.findRegion("structure_" + tex + "_wne");
            texWNSE = Quarry.Q.atlas.findRegion("structure_" + tex + "_wnse");
        }
    }

    public static final ConveyorSchema classSchema = new ConveyorSchema(0, StructureType.Conveyor, true, 1, 1, "conveyor", new Items(ItemType.Stone, 1), null)
            .flags(Flags.Draggable, Flags.NoDustEffect)
            .loudness(0.5f);

    protected Direction dir;

    final static int s = Const.TILE_SIZE - 2 * Const.TUBE_OFFSET;
    final static int w = Const.TUBE_OFFSET + 6;

    public static float k = Const.TILE_SIZE / (float) Const.ITEMS_PER_CONVEYOR;

    Structure<?>[] structures = new Structure[4];

    final Object itemLock = new Object();

    /**
     *        N:0
     *          1
     *          2
     * W:7 8 9 10/3 11 12 E:13
     *          4
     *          5
     *        S:6
     */

    ItemEntity[] items = new ItemEntity[Const.ITEMS_PER_CONVEYOR * 2];

    int itemCount = 0;

    boolean itemChanges;

    boolean notification;

    // gets resolved into #structures in postLoad
    // 0 from, 1 side1, 2 side2, 3 to
    int[] structs;

    public Conveyor(int x, int y) {
        super(x, y, classSchema);
        dir = Direction.East;
    }

    protected Conveyor(int x, int y, ConveyorSchema schema) {
        super(x, y, schema);
        dir = Direction.East;
    }

    public Conveyor(int x, int y, Direction direction) {
        super(x, y, classSchema);
        dir = direction;
    }

    public Structure<?> getStructureInDirection(Direction dir) {
        if (dir == this.dir.inv()) return structures[0];
        else if (dir == this.dir.next()) return structures[1];
        else if (dir == this.dir.prev()) return structures[2];
        else if (dir == this.dir) return structures[3];
        else return null;
    }

    public boolean moveItem(ItemEntity e, Direction dir, int deltaSlot, boolean chain) {
        //        System.out.println("move " + e.x + ":" + e.y + "-" + e.slot + ": " + e.item);
        if (dir == null) {
            synchronized (itemLock) {
                if (isItemSlotFree(e.slot + deltaSlot)) {
                    items[e.slot] = null;
                    items[e.slot + deltaSlot] = e;
                    e.lastSlot = e.slot;
                    e.slot += deltaSlot;
                    e.x = x;
                    e.y = y;
                    itemChanges = true;
                    notifyNeighbors(chain);

                    return true;
                } else {
                    return false;
                }
            }
        } else {
            Structure<?> s = getStructureInDirection(dir);
            if (s instanceof Conveyor) {
                Conveyor c = (Conveyor) s;

                synchronized (itemLock) {
                    synchronized (c.itemLock) {
                        if (c.isItemSlotFree(e.slot + deltaSlot)) {
                            items[e.slot] = null;
                            c.items[e.slot + deltaSlot] = e;
                            e.lastSlot = e.slot;
                            e.slot += deltaSlot;
                            e.x = c.x;
                            e.y = c.y;
                            itemCount--;
                            c.itemCount++;

                            itemChanges = true;
                            c.itemChanges = true;
                            notifyNeighbors(chain);

                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    public boolean isItemSlotFree(int slot) {
        if (slot == Const.ITEMS_PER_CONVEYOR / 2 || slot == Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2) {
            return items[Const.ITEMS_PER_CONVEYOR / 2] == null && items[Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2] == null;
        } else {
            return items[slot] == null;
        }
    }

    boolean touches;

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        this.touches = dirtyBounds.touches(this);

        if (touches || layer == null) {
            updateStructures();
        }
    }

    public void updateItems(double deltaTime, int gameSpeed, boolean chain) {
        updateItems(deltaTime, gameSpeed, touches, chain);
    }

    protected void updateItemWithin(ItemEntity e, boolean chain) {
        if (e.dir == Direction.South || e.dir == Direction.North) {
            if (e.slot < Const.ITEMS_PER_CONVEYOR) {
                if (moveItem(e, null, e.dir.dy, chain)) {
                    e.interp--;
                }
            } else if (e.slot != Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2) {
                if (moveItem(e, null, e.slot > Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2 ? -1 : 1, chain)) {
                    e.interp--;
                }
            } else if (moveItem(e, null, -Const.ITEMS_PER_CONVEYOR + e.dir.dy, chain)) {
                e.interp--;
            }
        } else if (e.dir == Direction.West || e.dir == Direction.East) {
            if (e.slot >= Const.ITEMS_PER_CONVEYOR) {
                if (moveItem(e, null, e.dir.dx, chain)) {
                    e.interp--;
                }
            } else if (e.slot != Const.ITEMS_PER_CONVEYOR / 2) {
                if (moveItem(e, null, e.slot > Const.ITEMS_PER_CONVEYOR / 2 ? -1 : 1, chain)) {
                    e.interp--;
                }
            } else if (moveItem(e, null, Const.ITEMS_PER_CONVEYOR + e.dir.dx, chain)) {
                e.interp--;
            }
        }
    }

    public void updateItems(double deltaTime, int gameSpeed, boolean touch, boolean chain) {
        if (!itemChanges && !touch && !notification) {
            return;
        }

        itemChanges = false;
        // update items
        synchronized (itemLock) {
            for (int i = 0; i < items.length; i++) {
                ItemEntity e = items[i];

                if (e == null) continue;

                // why does this even occur
                if (e.item == null) {
                    System.out.println("Deleting nonsense item");
                    items[i] = null;
                    itemChanges = true;
                    Pools.free(e);
                    continue;
                }
                if (e.dir == null && !notification && !touch) {
                    continue;
                }

                if (touch || notification) {
                    Direction nd = getCurrentDirection(e.item, e.dir, e.z, e.slot);
                    float nz = getItemZ(e.item, nd);
                    if (nd != e.dir || nz != e.z) {
                        itemChanges = true;
                        e.dir = nd;
                        e.z = nz;
                    }
                }

                if (e.interp >= 1.0) {
                    boolean atEdge = false;

                    if (e.slot == 0 && e.dir == Direction.South) {
                        atEdge = true;
                    } else if (e.slot == Const.ITEMS_PER_CONVEYOR - 1 && e.dir == Direction.North) {
                        atEdge = true;
                    } else if (e.slot == Const.ITEMS_PER_CONVEYOR && e.dir == Direction.West) {
                        atEdge = true;
                    } else if (e.slot == Const.ITEMS_PER_CONVEYOR * 2 - 1 && e.dir == Direction.East) {
                        atEdge = true;
                    } else {
                        updateItemWithin(e, chain);
                    }

                    if (atEdge || e.dir == null) {
                        Structure<?> s = this;
                        if (e.dir != null) {
                            s = getStructureInDirection(e.dir);
                        }

                        boolean blocked = false;

                        if (atEdge && e.dir != null) {
                            if (e.slot == 0) {
                                if (moveItem(e, e.dir, Const.ITEMS_PER_CONVEYOR - 1, chain)) {
                                    e.interp--;
                                } else {
                                    blocked = true;
                                }
                            } else if (e.slot == Const.ITEMS_PER_CONVEYOR - 1) {
                                if (moveItem(e, e.dir, -(Const.ITEMS_PER_CONVEYOR - 1), chain)) {
                                    e.interp--;
                                } else {
                                    blocked = true;
                                }
                            } else if (e.slot == Const.ITEMS_PER_CONVEYOR) {
                                if (moveItem(e, e.dir, Const.ITEMS_PER_CONVEYOR - 1, chain)) {
                                    e.interp--;
                                } else {
                                    blocked = true;
                                }
                            } else if (e.slot == Const.ITEMS_PER_CONVEYOR * 2 - 1) {
                                if (moveItem(e, e.dir, -(Const.ITEMS_PER_CONVEYOR - 1), chain)) {
                                    e.interp--;
                                } else {
                                    blocked = true;
                                }
                            }
                        }

                        if (s instanceof Conveyor) {
                            if (!blocked) {
                                //                                System.out.println(x + ":" + y + "-" + i + " " + "!blocked " + e.x + ":" + e.y + "-" + e.slot + ": " + e.item);

                                e.dir = ((Conveyor) s).getNextDirection(e.item, e.dir, e.z, e.slot);
                                e.z = ((Conveyor) s).getItemZ(e.item, e.dir);
                            }
                        } else if (s != null && s.canAccept(e.item, e.x, e.y, e.dir) && s.acceptItem(e.item, e.src, e.dir)) {

                            //                            System.out.println(x + ":" + y + "-" + i + " " + "consume " + e.x + ":" + e.y + "-" + e.slot + ": " + e.item);

                            items[e.slot] = null;
                            Pools.free(e);
                            itemCount--;
                            itemChanges = true;
                            notifyNeighbors(chain);
                            continue;
                        } else {
                            e.dir = null;
                        }
                    }
                } else {
                    e.interp += deltaTime * Const.ITEM_SPEED * gameSpeed;
                    itemChanges = true;
                }
            }
        }

        //notification = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (itemCount > 0) {
            for (ItemEntity e : items) {
                if (e != null && e.src != null) {
                    if (!e.src.isDestroyed())
                        e.src.putBack(e.item, 1);
                    items[e.slot] = null;
                    Pools.free(e);
                }
            }
        }
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        int flag = 0;
        for (int i = 0; i < 4; i++)
            if (structures[i] != null) flag |= (1 << i);

        TextureRegion tex = getSchema().tex;
        int flip = 1;
        switch (flag) {
            case 0b0001:
            case 0b1001:
            case 0b1000:
                tex = schema.texWE;
                break;
            case 0b0100:
            case 0b1100:
                tex = schema.texNE;
                break;
            case 0b0010:
            case 0b1010:
                tex = schema.texNE;
                flip = -1;
                break;
            case 0b0101:
            case 0b1101:
                tex = schema.texWNE;
                break;
            case 0b0011:
            case 0b1011:
                tex = schema.texWNE;
                flip = -1;
                break;
            case 0b0110:
            case 0b1110:
                tex = schema.texNSE;
                break;
            case 0b0111:
            case 0b1111:
                tex = schema.texWNSE;
                break;

        }

        spriter.add(tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_TUBES, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, flip, dir.rot);
    }

    protected void notifyNeighbors(boolean chain) {
        if (structures[0] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(dir))) {
            ((Conveyor) structures[0]).itemChanges = true;
            if (chain) ((Conveyor) structures[0]).updateItems(0, 1, true, false);
        }
        if (structures[1] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(dir.prev()))) {
            ((Conveyor) structures[1]).itemChanges = true;
            if (chain) ((Conveyor) structures[1]).updateItems(0, 1, true, false);
        }
        if (structures[2] instanceof Conveyor && isItemSlotFree(Layer.getStartingSlot(dir.next()))) {
            ((Conveyor) structures[2]).itemChanges = true;
            if (chain) ((Conveyor) structures[2]).updateItems(0, 1, true, false);
        }
    }

    static TextureRegion caret;

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (clicked || (Game.G.activeStructure != null && Game.G.activeStructure == this && getSchema().type == StructureType.Hopper)) {
            if (caret == null) {
                caret = Quarry.Q.atlas.findRegion("caret_down");
            }
            spriter.add(caret, (x + 0.5f) * Const.TILE_SIZE - 6, (y + 0.5f) * Const.TILE_SIZE - 2.5f, Const.Z_STATES, 6, 2.5f, 12, 5, 1, 1, dir.rot + 90);
        }

        if (itemCount > 0)
            drawItems(spriter);

        if (Game.DRAW_DEBUG) {
            synchronized (itemLock) {
                shaper.set(ShapeType.Line);

                for (ItemEntity e : items) {
                    if (e == null) continue;

                    shaper.setColor(e.z == Const.Z_ITEMS ? Color.GREEN : Color.YELLOW);
                    float ix = 0, iy = 0;
                    if (e.slot < Const.ITEMS_PER_CONVEYOR) {
                        ix = (e.x + 0.5f) * Const.TILE_SIZE - k / 2;
                        iy = (e.y) * Const.TILE_SIZE + (e.slot) * k;
                    } else {
                        ix = (e.x) * Const.TILE_SIZE + (e.slot - Const.ITEMS_PER_CONVEYOR) * k;
                        iy = (e.y + 0.5f) * Const.TILE_SIZE - k / 2;
                    }

                    if (e.slot == Const.ITEMS_PER_CONVEYOR / 2)
                        shaper.circle(ix + k / 2, iy + k / 2, k / 2, 16);
                    else shaper.rect(ix + 1, iy + 1, k - 2, k - 2);

                    if (e.dir == null) {
                        shaper.x(ix + k / 2, iy + k / 2, k / 4);
                    } else {
                        shaper.line(ix + k / 2, iy + k / 2, ix + k / 2 + k / 3 * 2 * e.dir.dx, iy + k / 2 + k / 3 * 2 * e.dir.dy);
                    }
                }
            }
        }
    }

    public void drawItems(SpriteRenderer spriter) {
        synchronized (itemLock) {
            for (ItemEntity e : items) {
                if (e == null) continue;
                float ix = 0, iy = 0;

                Direction dir = null;
                if (e.dir != null && e.lastSlot > -1) {
                    dir = e.dir;
                    int x = e.x;
                    int y = e.y;

                    if (Math.abs(e.lastSlot - e.slot) > 1) {
                        if (e.lastSlot == 0 && e.slot == Const.ITEMS_PER_CONVEYOR - 1) {
                            y++;
                            dir = Direction.South;
                        } else if (e.lastSlot == Const.ITEMS_PER_CONVEYOR - 1 && e.slot == 0) {
                            y--;
                            dir = Direction.North;
                        } else if (e.lastSlot == Const.ITEMS_PER_CONVEYOR * 2 - 1 && e.slot == Const.ITEMS_PER_CONVEYOR) {
                            x--;
                            dir = Direction.East;
                        } else if (e.lastSlot == Const.ITEMS_PER_CONVEYOR && e.slot == Const.ITEMS_PER_CONVEYOR * 2 - 1) {
                            x++;
                            dir = Direction.West;
                        }
                    } else if ((dir == Direction.South || dir == Direction.North) && e.lastSlot >= Const.ITEMS_PER_CONVEYOR && e.lastSlot != Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2) {
                        dir = e.lastSlot > Const.ITEMS_PER_CONVEYOR + Const.ITEMS_PER_CONVEYOR / 2 ? Direction.West : Direction.East;
                    } else if ((dir == Direction.West || dir == Direction.East) && e.lastSlot < Const.ITEMS_PER_CONVEYOR && e.lastSlot != Const.ITEMS_PER_CONVEYOR / 2) {
                        dir = e.lastSlot > Const.ITEMS_PER_CONVEYOR / 2 ? Direction.South : Direction.North;
                    }

                    if (e.lastSlot < Const.ITEMS_PER_CONVEYOR) {
                        ix = (x + 0.5f) * Const.TILE_SIZE - 12;
                        iy = (y) * Const.TILE_SIZE + (e.lastSlot) * Conveyor.k - (24 - Conveyor.k) / 2;
                    } else {
                        ix = (x) * Const.TILE_SIZE + (e.lastSlot - Const.ITEMS_PER_CONVEYOR) * Conveyor.k - (24 - Conveyor.k) / 2;
                        iy = (y + 0.5f) * Const.TILE_SIZE - 12;
                    }

                    //                    float prog = Math.min(1, e.interp);
                    double prog = MathUtils.clamp(e.interp, 0, 1);

                    ix += Conveyor.k * prog * dir.dx;
                    iy += Conveyor.k * prog * dir.dy;
                } else {
                    if (e.slot < Const.ITEMS_PER_CONVEYOR) {
                        ix = (e.x + 0.5f) * Const.TILE_SIZE - 12;
                        iy = (e.y) * Const.TILE_SIZE + (e.slot) * Conveyor.k - (24 - Conveyor.k) / 2;
                    } else {
                        ix = (e.x) * Const.TILE_SIZE + (e.slot - Const.ITEMS_PER_CONVEYOR) * Conveyor.k - (24 - Conveyor.k) / 2;
                        iy = (e.y + 0.5f) * Const.TILE_SIZE - 12;
                    }
                }

                float newZ = e.z + ix / (layer.width * Const.TILE_SIZE) - iy / (layer.height * Const.TILE_SIZE);

                if (e.item.stackable != null) {
                    spriter.add(e.item.stackable.icon, ix + 5, iy + 5, newZ, 14, 14);
                }

                spriter.add(e.item.icon, ix, iy, newZ, 24, 24);
            }
        }
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        if (!fromLoading) {
            updateStructures();
        }
    }

    public void setItemNotification() {
        notification = true;
    }

    protected Structure<?> getNeighbor(Direction dir) {
        Layer l = layer == null ? Game.G.layer : layer;

        Structure<?> s = Game.G.activeStructureTrail.get((x + dir.dx) * l.height + (y + dir.dy));
        if (s == null)
            s = l.getStructure(x + dir.dx, y + dir.dy);

        if (s != null && isDockOrConveyor(s, dir)) {
            return s;
        }

        return null;
    }

    protected void updateStructures() {
        Structure<?> from = getNeighbor(dir.inv());

        if (from == null) {
            structures[0] = from;
        } else if (from instanceof Conveyor) {
            if (((Conveyor) from).getDirection() == dir
                    || (from instanceof ConveyorBridge && ((ConveyorBridge) from).getDirection2() == dir)) {
                structures[0] = from;
            } else {
                structures[0] = null;
            }
        } else {
            structures[0] = from;
        }

        Structure<?> to = getNeighbor(dir);
        if (to == null
                || (to instanceof Conveyor && ((Conveyor) to).getDirection() == dir.inv())
                || (to instanceof ConveyorBridge && ((ConveyorBridge) to).getDirection2() == dir.inv())
                || (to instanceof ElectricConveyorCore && ((ElectricConveyorCore) to).getDirection() != dir
                        && ((ElectricConveyorCore) to).getDirection() != dir.inv())) {
            structures[3] = null;
        } else {
            structures[3] = to;
        }

        // update side structures
        if (this instanceof ElectricConveyorCore) {
            structures[1] = null;
            structures[2] = null;
        } else {
            Direction d = dir.next();
            Structure<?> s = getNeighbor(d);
            if (s != null && isDockOrInputConveyor(s, d)) structures[1] = s;
            else structures[1] = null;

            d = dir.prev();
            s = getNeighbor(d);
            if (s != null && isDockOrInputConveyor(s, d)) structures[2] = s;
            else structures[2] = null;
        }
    }

    private boolean isDockOrConveyor(Structure<?> s, Direction dir) {
        if (s instanceof Conveyor) {
            return true;
        } else {
            for (Dock d : s.getDocks())
                if (s.x + d.x == x + dir.dx && s.y + d.y == y + dir.dy && d.dir == dir.inv() && canConnectToDock(d))
                    return true;
        }
        return false;
    }

    private boolean isDockOrInputConveyor(Structure<?> s, Direction dir) {
        if (s instanceof Conveyor) {
            if (s.getSchema().type == StructureType.Conveyor
                    || s.getSchema().type == StructureType.ElectricConveyor) {
                return ((Conveyor) s).getDirection() == dir.inv();
            } else if (s.getSchema().type == StructureType.ConveyorBridge || s.getSchema().type == StructureType.Hopper) {
                return ((ConveyorBridge) s).getDirection() == dir.inv()
                        || ((ConveyorBridge) s).getDirection2() == dir.inv();
            } else if (s.getSchema().type == StructureType.ElectricConveyorCore) {
                return ((Conveyor) s).getDirection() == dir.inv();
            }
        } else {
            for (Dock d : s.getDocks())
                if (s.x + d.x == x + dir.dx && s.y + d.y == y + dir.dy && d.dir == dir.inv() && canConnectToDock(d))
                    return true;
        }
        return false;
    }

    protected boolean canConnectToDock(Dock d) {
        return d.type == DockType.ItemIn || d.type == DockType.ItemOut;
    }

    @Override
    public void rotate() {
        int i = (dir.ordinal() + 1) % 4;
        if (i < 0) i = 4 + i;
        setRotation(Direction.values[i]);
    }

    @Override
    public void setRotation(Direction direction) {
        dir = direction;

        if (layer == null) updateStructures();
        else setDirty();
    }

    @Override
    public Direction getDirection() {
        return dir;
    }

    public Direction getCurrentDirection(ItemType item, Direction prev, float currentZ, int slot) {
        return dir;
    }

    public Direction getNextDirection(ItemType item, Direction prev, float currentZ, int slot) {
        return dir;
    }

    public int getItemCount() {
        return itemCount;
    }

    public ItemEntity[] getItems() {
        return items;
    }

    public boolean addItemEntity(ItemType value, int lastSlot, int slot, Direction dir, Structure<?> src) {
        synchronized (itemLock) {
            if (items[slot] != null) return false;

            ItemEntity e = Pools.obtain(ItemEntity.class);
            e.item = value;
            e.x = x;
            e.y = y;
            e.z = getItemZ(value, dir);
            e.dir = dir;
            e.src = src;
            e.lastSlot = lastSlot;
            e.slot = slot;

            Game.G.addSeenResource(value);
            items[slot] = e;

            itemCount++;
            itemChanges = true;

            return true;
        }
    }

    public float getItemZ(ItemType type, Direction direction) {
        return Const.Z_ITEMS;
    }

    @Override
    public Object clone() {
        return new Conveyor(x, y, dir);
    }

    @Override
    public void postLoad() {
        if (structs != null) {
            for (int i = 0; i < 4; i++)
                if (structs[i] > -1) structures[i] = layer.getStructure(structs[i]);
            structs = null;
        }

        for (ItemEntity e : items) {
            if (e != null) e.postLoad();
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Byte("dir", (byte) dir.ordinal());

        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            str[i] = structures[i] == null ? -1 : structures[i].x * layer.height + structures[i].y;
        }
        b.IntArray("structs", str);

        b.List("Items", TagType.Compound);
        synchronized (itemLock) {
            for (ItemEntity e : items)
                if (e != null)
                    e.save(b);
        }
        b.End();
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);

        int[] str = new int[4];
        for (int i = 0; i < 4; i++) {
            str[i] = structures[i] == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, structures[i].x * layer.height + structures[i].y);
        }
        b
                .Byte("dir", (byte) dir.ordinal())
                .IntArray("structs", str);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        try {
            dir = Direction.values[tag.Byte("dir", (byte) 0)];
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
        dir = Direction.values[tag.Byte("dir", (byte) 0)];
        structs = tag.IntArray("structs", null);

        for (Tag t : tag.List("Items", TagType.Compound).data) {
            try {
                ItemEntity e = Pools.obtain(ItemEntity.class);
                e.load((CompoundTag) t);
                items[e.slot] = e;
                itemChanges = true;
                itemCount++;
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }
}
