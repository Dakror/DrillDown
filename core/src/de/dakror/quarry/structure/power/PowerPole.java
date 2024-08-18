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

package de.dakror.quarry.structure.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Dock;
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
public class PowerPole extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.PowerPole, true, 1,
            1,
            "powerpole", new Items(ItemType.SteelPlate, 25, ItemType.TinPlate, 15, ItemType.SteelCable, 60, ItemType.Brick, 20), null)
                    .sciences(ScienceType.HighPower)
                    .flags(Flags.NotRotatable);

    static final float range = 5.5f;

    PowerPoleGhost aGhost, bGhost;

    PowerPole cachedEnd;

    Structure<?> a, b;

    int aIndex, bIndex, aGhostIndex, bGhostIndex;

    public PowerPole(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void postLoad() {
        super.postLoad();

        if (aIndex > -1) a = layer.getStructure(aIndex);
        if (bIndex > -1) b = layer.getStructure(bIndex);
        if (aGhostIndex > -1) {
            try {
                aGhost = (PowerPoleGhost) layer.getStructure(aGhostIndex);
            } catch (Exception e) {
                aGhost = null;
                a = null;
            }
        }
        if (bGhostIndex > -1) {
            try {
                bGhost = (PowerPoleGhost) layer.getStructure(bGhostIndex);
            } catch (Exception e) {
                bGhost = null;
                b = null;
            }
        }
    }

    public void cleanConnections() {
        if (layer == null) return;

        if (a != null && layer.getStructure(a.x * layer.height + a.y) != a) {
            a = null;
            if (aGhost != null) {
                layer.removeStructure(aGhost);
                aGhost = null;
            }
        }
        if (b != null && layer.getStructure(b.x * layer.height + b.y) != b) {
            b = null;
            if (bGhost != null) {
                layer.removeStructure(bGhost);
                bGhost = null;
            }
        }
    }

    protected void tryToConnectTo(Structure<?> s) {
        if (s == null || s == this || (a != null && b != null) || s == a || s == b) return;

        if (s instanceof PowerPole) {
            PowerPole p = (PowerPole) s;
            if (p.a != null && p.b != null) return;
            p.cleanConnections();

            if (p.a == null) {
                if (a == null) {
                    p.a = this;
                    a = p;
                } else {
                    p.a = this;
                    b = p;
                }
            } else {
                if (a == null) {
                    p.b = this;
                    a = p;
                } else {
                    p.b = this;
                    b = p;
                }
            }
        } else if (s.getSchema().powerDocks > 0) {
            float minDistance = range;
            Dock dock = null;
            for (Dock d : s.getDocks()) {
                if (d.type == DockType.BigPower && layer.getAny(s.x + d.x + d.dir.dx, s.y + d.y + d.dir.dy, true) == null) {
                    float dist = Vector2.dst(s.x + d.x + d.dir.dx, s.y + d.y + d.dir.dy, x, y);
                    if (dist <= minDistance) {
                        minDistance = dist;
                        dock = d;
                    }
                }
            }

            if (dock != null) {
                if (a == null) {
                    a = s;
                    aGhost = new PowerPoleGhost(s.x + dock.x + dock.dir.dx, s.y + dock.y + dock.dir.dy, this, s);
                    layer.addStructure(aGhost);
                    return;
                } else {
                    b = s;
                    bGhost = new PowerPoleGhost(s.x + dock.x + dock.dir.dx, s.y + dock.y + dock.dir.dy, this, s);
                    layer.addStructure(bGhost);
                    return;
                }
            }
        }
    }

    protected void updateConnections() {
        if (layer == null) return;

        cleanConnections();

        if (b == null || a == null) {
            o: for (int i = 1; i < range; i++) {
                for (int x = -i; x <= i; x++) {
                    for (int y = -i; y <= i; y++) {
                        if (Math.sqrt(x * x + y * y) <= i - 0.5f || Math.sqrt(x * x + y * y) > i + 0.5f) continue;
                        if (b != null && a != null) break o;

                        Structure<?> s = layer.getStructure((this.x + x) * layer.height + this.y + y);
                        if (s != null) tryToConnectTo(s);
                    }
                }
            }
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (!dirtyBounds.isEmpty()) {
            cachedEnd = null;
            updateConnections();
        }
    }

    public PowerPole walkToEnd() {
        if (cachedEnd != null)
            return cachedEnd;

        PowerPole active = this, prev = null;
        while (active != null) {
            if (active.a instanceof PowerPole && active.a != prev) {
                prev = active;
                active = (PowerPole) active.a;
            } else if (active.b instanceof PowerPole && active.b != prev) {
                prev = active;
                active = (PowerPole) active.b;
            } else {
                break;
            }
        }

        active.cachedEnd = this;
        cachedEnd = active;
        return active;
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        updateConnections();
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (Game.G.activeStructure == this || clicked) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(1, 0.498f, 0.314f, 0.2f);
            shaper.circle((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE, range * Const.TILE_SIZE);
        }

        if (a != null) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(0.33f, 0.33f, 0.33f, 1);

            if (aGhost == null)
                shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                        (a.x + a.getWidth() / 2f) * Const.TILE_SIZE, (a.y + a.getHeight() / 2f) * Const.TILE_SIZE, 4);
            else shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                    (aGhost.x + aGhost.getWidth() / 2f) * Const.TILE_SIZE, (aGhost.y + aGhost.getHeight() / 2f) * Const.TILE_SIZE, 4);
        }

        if (b != null) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(0.33f, 0.33f, 0.33f, 1);

            if ((b instanceof PowerPole && ((PowerPole) b).b == this))
                shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                        (b.x + b.getWidth() / 2f) * Const.TILE_SIZE, (b.y + b.getHeight() / 2f) * Const.TILE_SIZE, 4);
            else if (bGhost != null) {
                shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                        (bGhost.x + bGhost.getWidth() / 2f) * Const.TILE_SIZE, (bGhost.y + bGhost.getHeight() / 2f) * Const.TILE_SIZE, 4);
            }
        }

        if (Game.DRAW_DEBUG) {
            if (cachedEnd != null) {
                shaper.setColor(Color.YELLOW);
                shaper.rectLine((x + 0.5f) * Const.TILE_SIZE, (y + 0.5f) * Const.TILE_SIZE,
                        (cachedEnd.x + 0.5f) * Const.TILE_SIZE, (cachedEnd.y + 0.5f) * Const.TILE_SIZE, 3);
            } else {
                shaper.setColor(Color.YELLOW);
                shaper.circle((x + 0.5f) * Const.TILE_SIZE, (y + 0.5f) * Const.TILE_SIZE, 3);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (aGhost != null) {
            layer.removeStructure(aGhost);
        }
        if (bGhost != null) {
            layer.removeStructure(bGhost);
        }
    }

    public Structure<?> getA() {
        return a;
    }

    public PowerPoleGhost getAGhost() {
        return aGhost;
    }

    public Structure<?> getB() {
        return b;
    }

    public PowerPoleGhost getBGhost() {
        return bGhost;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);

        b
                .Int("a", a == null ? -1 : a.x * layer.height + a.y)
                .Int("b", this.b == null ? -1 : this.b.x * layer.height + this.b.y)
                .Int("aGhost", aGhost == null ? -1 : aGhost.x * layer.height + aGhost.y)
                .Int("bGhost", bGhost == null ? -1 : bGhost.x * layer.height + bGhost.y);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);

        aIndex = tag.Int("a", -1);
        bIndex = tag.Int("b", -1);
        aGhostIndex = tag.Int("aGhost", -1);
        bGhostIndex = tag.Int("bGhost", -1);
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);

        b
                .Int("a", a == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, a.x * layer.height + a.y))
                .Int("b", this.b == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, this.b.x * layer.height + this.b.y))
                .Int("aGhost", aGhost == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, aGhost.x * layer.height + aGhost.y))
                .Int("bGhost", bGhost == null ? -1 : CopyRegionHelper.indexGlobalToRelative(copyRegion, bGhost.x * layer.height + bGhost.y));
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);

        this.a = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("a", 0));
        this.b = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("b", 0));
        Structure<?> s = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("aGhost", 0));
        if (s instanceof PowerPoleGhost) aGhost = (PowerPoleGhost) s;
        else aGhost = null;
        s = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("bGhost", 0));
        if (s instanceof PowerPoleGhost) bGhost = (PowerPoleGhost) s;
        else bGhost = null;
    }
}
