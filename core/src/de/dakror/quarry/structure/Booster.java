/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;

import de.dakror.common.BiCallback;
import de.dakror.common.libgdx.Pair;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Booster extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.Booster, true, 3, 3,
            "booster",
            new Items(ItemType.AdvancedMachineFrame, 1, ItemType.BronzePlate, 150, ItemType.Dynamo, 5, ItemType.Glass, 80, ItemType.Battery, 20),
            new Sfx("booster" + Const.SFX_FORMAT),
            new Dock(0, 1, Direction.West, DockType.ItemIn),
            new Dock(1, 0, Direction.South, DockType.ItemIn),
            new Dock(2, 1, Direction.East, DockType.ItemIn), new Dock(1, 2, Direction.North, DockType.ItemIn))
                    .sciences(ScienceType.Boosting)
                    .button(new ButtonDef("icon_network", "button.network", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            ((Booster) data).meshMode = on;
                            ((Booster) data).updateMesh();
                            if (((Booster) data).clicked) ((Booster) data).updateUI();
                        }
                    }))
                    .button(new ButtonDef("symb_ff", "button.boost", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            ((Booster) data).setBoosting(on);
                        }
                    }))
                    .flags(Flags.NoDustEffect, Flags.NotRotatable);

    public static final float BOOST_RADIUS = 9.5f;
    public static final int BOOST_FACTOR = 3;
    public static final int CONSUME_PER_SECOND = 1000;

    boolean meshMode;
    boolean boosting;

    boolean boostActive;

    double boostTime;

    Table ui;
    Container<Table> container;

    HashSet<Booster> nearby;
    HashSet<Booster> mesh;
    HashSet<Pair<Booster, Booster>> meshEdges;
    Rectangle myBoostArea;

    final Set<Rectangle> rectangles = new TreeSet<>(new Comparator<Rectangle>() {
        @Override
        public int compare(Rectangle a, Rectangle b) {
            return Float.compare(a.x, b.x) == 0 ? Float.compare(a.y, b.y) : Float.compare(a.x, b.x);
        }
    });
    final FloatArray boostAreaTmp = new FloatArray();
    FloatArray boostArea;
    ShortArray triangleIndices;
    Booster meshHost;

    public Booster(int x, int y) {
        super(x, y, classSchema);
        nearby = new HashSet<>();
        mesh = new HashSet<>();
        meshEdges = new HashSet<>();
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (item.categories.contains(ItemCategory.Fluid)) return false;

        for (Dock d : getDocks())
            if (d.type == DockType.ItemIn)
                if (isNextToDock(x, y, dir, d)) return true;
        return false;
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        boostTime += item.worth;
        updateUI();

        return true;
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 0)
            return meshMode;
        else if (buttonIndex == 1)
            return isBoosting();
        return false;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (Game.G.activeStructure == this || clicked) {
            shaper.set(ShapeType.Filled);
            if (meshMode) {
                for (Rectangle r : meshHost.rectangles) {
                    shaper.setColor(1, 0.498f, 0.314f, 0.3f);
                    shaper.rect(r.x * Const.TILE_SIZE, r.y * Const.TILE_SIZE, BOOST_RADIUS * 2 * Const.TILE_SIZE, BOOST_RADIUS * 2 * Const.TILE_SIZE);
                }
            }
            shaper.setColor(1, 0.498f, 0.314f, 0.2f);
            shaper.rect((x + getWidth() / 2f - BOOST_RADIUS) * Const.TILE_SIZE, (y + getHeight() / 2f - BOOST_RADIUS) * Const.TILE_SIZE, BOOST_RADIUS * 2 * Const.TILE_SIZE, BOOST_RADIUS * 2 * Const.TILE_SIZE);
        }
        shaper.set(ShapeType.Filled);
        if ((Game.G.activeStructure == this || clicked) && meshMode) {
            shaper.setColor(1, 0.5f, 0, 1);
            shaper.rect((x + getWidth() / 2f) * Const.TILE_SIZE - 8, (y + getHeight() / 2f) * Const.TILE_SIZE - 8, 16, 16);

            for (Pair<Booster, Booster> p : meshEdges) {
                shaper.setColor(1, 0.5f, 0, 1);
                shaper.rectLine((p.getKey().x + p.getKey().getWidth() / 2f) * Const.TILE_SIZE, (p.getKey().y + p.getKey().getHeight() / 2f) * Const.TILE_SIZE,
                        (p.getVal().x + p.getVal().getWidth() / 2f) * Const.TILE_SIZE, (p.getVal().y + p.getVal().getHeight() / 2f) * Const.TILE_SIZE, 8);
            }

            shaper.setColor(1, 0.75f, 0.25f, 1);
            shaper.rect((x + getWidth() / 2f) * Const.TILE_SIZE - 4, (y + getHeight() / 2f) * Const.TILE_SIZE - 4, 8, 8);

            for (Booster b : mesh) {
                shaper.setColor(1, 0.75f, 0.25f, 1);
                shaper.rect((b.x + b.getWidth() / 2f) * Const.TILE_SIZE - 4, (b.y + b.getHeight() / 2f) * Const.TILE_SIZE - 4, 8, 8);
            }

            for (Pair<Booster, Booster> p : meshEdges) {
                shaper.setColor(1, 0.75f, 0.25f, 1);
                shaper.rectLine((p.getKey().x + p.getKey().getWidth() / 2f) * Const.TILE_SIZE, (p.getKey().y + p.getKey().getHeight() / 2f) * Const.TILE_SIZE,
                        (p.getVal().x + p.getVal().getWidth() / 2f) * Const.TILE_SIZE, (p.getVal().y + p.getVal().getHeight() / 2f) * Const.TILE_SIZE, 4);
            }
        }

        /*if (Game.DRAW_DEBUG) {
            for (Booster b : nearby) {
                shaper.setColor(0, 0.5f, 0.5f, 1);
                shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                        (b.x + b.getWidth() / 2f) * Const.TILE_SIZE, (b.y + b.getHeight() / 2f) * Const.TILE_SIZE, 12);
            }
            if (meshHost != null) {
                shaper.setColor(1, 0, 0.5f, 1f);
                shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                        (meshHost.x) * Const.TILE_SIZE, (meshHost.y) * Const.TILE_SIZE, 8);
            }
        }*/
    }

    public boolean isBoosting() {
        return (meshMode && meshHost != null && meshHost.boosting) || (!meshMode && boosting);
    }

    public boolean isBoostActive() {
        return (!meshMode && boostActive) || (meshMode && meshHost != null && meshHost.boostActive);
    }

    @Override
    public void setNearbyBooster(Booster booster) {
        if (booster != null)
            nearby.add(booster);

        // keep array sanitized
        for (Iterator<Booster> iter = nearby.iterator(); iter.hasNext();) {
            Booster b = iter.next();
            if (layer.getStructure(b.getX() * layer.height + b.getY()) != b) iter.remove();
        }
    }

    public void updateMesh() {
        if (!meshMode) {
            for (Booster b : mesh) {
                b.updateMesh();
            }
            mesh.clear();
            meshEdges.clear();
            meshHost = null;
            rectangles.clear();
        } else {
            mesh.clear();
            meshEdges.clear();
            meshHost = this;
            meshHost.rectangles.clear();
            addToMeshRecursively(this);
            for (Booster b : mesh) {
                b.mesh.clear();
                b.addToMeshRecursively(b);
                b.meshHost = meshHost;
            }
        }
    }

    protected void addToMeshRecursively(Booster base) {
        for (Booster b : base.nearby) {
            if (b != this && b.meshMode && !mesh.contains(b)) {
                mesh.add(b);

                Pair<Booster, Booster> p1 = new Pair<Booster, Booster>().set(base, b);
                Pair<Booster, Booster> p2 = new Pair<Booster, Booster>().set(b, base);

                if (!meshEdges.contains(p1) && !meshEdges.contains(p2))
                    meshEdges.add(p1);

                if (b.getX() * layer.height + b.getY() < meshHost.getX() * layer.height + meshHost.getY()) {
                    meshHost = b;
                }

                addToMeshRecursively(b);
            }
        }
    }

    public void setBoosting(boolean boosting) {
        if (meshMode) {
            if (this != meshHost) meshHost.setBoosting(boosting);
            else this.boosting = boosting;
        } else {
            this.boosting = boosting;
        }
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (!fromLoading && layer != null) updateStructures(false);

        myBoostArea = new Rectangle(
                (int) (x + getWidth() / 2f - BOOST_RADIUS),
                (int) (y + getHeight() / 2f - BOOST_RADIUS),
                BOOST_RADIUS * 2,
                BOOST_RADIUS * 2);
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateStructures(false);
        updateMesh();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        updateStructures(true);
        if (meshMode) {
            meshMode = false;
            updateMesh();
        }
    }

    protected void updateStructures(boolean delete) {
        if (layer == null) return;
        int x1 = (int) (x + getWidth() / 2f - BOOST_RADIUS);
        int y1 = (int) (y + getHeight() / 2f - BOOST_RADIUS);
        for (int x = 0; x < (int) (BOOST_RADIUS * 2); x++) {
            for (int y = 0; y < (int) (BOOST_RADIUS * 2); y++) {
                Structure<?> s = layer.getStructure(x + x1, y + y1);
                if (s != null && (delete || s.getNearbyBooster() == null)) s.setNearbyBooster(delete ? null : this);
            }
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (isBoostActive() && gameSpeed > 0) playSfx();
        else pauseSfx();

        if (gameSpeed == 0)
            return;

        if (boosting && !meshMode && boostTime > 0) {
            boostTime -= gameSpeed * deltaTime * CONSUME_PER_SECOND;
            boostActive = boostTime > 0;
        } else if (meshMode && meshHost == this && boosting) {
            // find any booster with time on it and deduct one
            if (boostTime > 0) {
                boostTime = Math.max(0, boostTime - gameSpeed * deltaTime * CONSUME_PER_SECOND);

                boostActive = true;
            } else {
                boolean any = false;
                for (Booster b : mesh) {
                    if (b.boostTime > 0) {
                        b.boostTime = Math.max(0, b.boostTime - gameSpeed * deltaTime * CONSUME_PER_SECOND);
                        any = true;
                        break;
                    }
                }

                boostActive = any;
            }
        } else {
            boostActive = false;
        }

        if (isBoostActive() && clicked) updateUI();
    }

    @Override
    public void postUpdate(Bounds dirtyBounds) {
        super.postUpdate(dirtyBounds);

        if (dirtyBounds.hasAnyFlag(Bounds.Flags.CONSTRUCTION | Bounds.Flags.DESTRUCTION) && dirtyBounds.intersects((int) (x + getWidth() / 2f - BOOST_RADIUS),
                (int) (y + getHeight() / 2f - BOOST_RADIUS), (int) (BOOST_RADIUS * 2), (int) (BOOST_RADIUS * 2))) {
            updateStructures(false);
        }
    }

    private void updateUI() {
        if (meshMode && mesh.contains(Game.G.ui.currentClickedStructure)) {
            ((Booster) Game.G.ui.currentClickedStructure).updateUI();
            return;
        }

        if (container == null) {
            container = new Container<>();
        }

        long time = Math.round(boostTime / CONSUME_PER_SECOND * 1000);

        if (meshMode) {
            long sum = 0;
            for (Booster b : mesh) {
                sum += Math.round(b.boostTime / CONSUME_PER_SECOND * 1000);
            }
            time += sum;
        }

        if (ui == null) {
            ui = GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_time"),
                    String.format("%d:%02d:%02d.%03dh", time / 3600_000, (time % 3600_000) / 60_000, time % 60_000 / 1000, time % 1000));
            container.setActor(ui);
        } else {
            ((Label) ui.getChildren().get(1)).setText(String.format("%d:%02d:%02d.%03dh", time / 3600_000, (time % 3600_000) / 60_000, time % 60_000 / 1000, time % 1000));
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);
        updateUI();
        content.add(container).grow();
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Byte("mesh", meshMode ? 1 : (byte) 0)
                .Byte("boost", boosting ? 1 : (byte) 0)
                .Double("time", boostTime);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        meshMode = tag.Byte("mesh", (byte) 0) == 1;
        boosting = tag.Byte("boost", (byte) 0) == 1;
        boostTime = tag.Double("time", 0);
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("mesh", meshMode ? 1 : (byte) 0);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        meshMode = tag.Byte("mesh", (byte) 0) == 1;
        updateStructures(false);
        updateMesh();
    }
}
