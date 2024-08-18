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

package de.dakror.quarry.scenes;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Base64Coder;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.dakror.common.Callback;
import de.dakror.common.debug.Delta;
import de.dakror.common.libgdx.ChangeNotifier;
import de.dakror.common.libgdx.ChangeNotifier.Event.Type;
import de.dakror.common.libgdx.EditorCameraControl;
import de.dakror.common.libgdx.Pair;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.audio.AmbientSound;
import de.dakror.common.libgdx.io.ByteArrayFileHandle;
import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.CompressionType;
import de.dakror.common.libgdx.io.NBT.ListTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.math.AStar;
import de.dakror.common.libgdx.math.AStar.Network;
import de.dakror.common.libgdx.math.AStar.Visitor;
import de.dakror.common.libgdx.render.BatchDelegate;
import de.dakror.common.libgdx.render.DepthSpriter;
import de.dakror.common.libgdx.ui.ColorUtil;
import de.dakror.common.libgdx.ui.GameScene;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Chunk;
import de.dakror.quarry.game.Generator;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.LoadingCompat;
import de.dakror.quarry.game.Science;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.game.Tile.TileMeta;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.game.power.Edge;
import de.dakror.quarry.game.power.PowerGrid;
import de.dakror.quarry.game.power.PowerNetwork;
import de.dakror.quarry.game.power.PowerNetwork.NetworkStrength;
import de.dakror.quarry.structure.Boiler;
import de.dakror.quarry.structure.DistillationColumn;
import de.dakror.quarry.structure.Refinery;
import de.dakror.quarry.structure.ShaftDrillHead;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.FluidTubeStructure;
import de.dakror.quarry.structure.base.IRotatable;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StorageStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.base.component.CSingleInventory;
import de.dakror.quarry.structure.logistics.BrickChannel;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ConveyorBridge;
import de.dakror.quarry.structure.logistics.ElectricConveyorCore;
import de.dakror.quarry.structure.logistics.Filter;
import de.dakror.quarry.structure.logistics.Hopper;
import de.dakror.quarry.structure.logistics.ItemLift;
import de.dakror.quarry.structure.logistics.ItemLiftBelow;
import de.dakror.quarry.structure.logistics.TubeShaft;
import de.dakror.quarry.structure.logistics.TubeShaftBelow;
import de.dakror.quarry.structure.power.CableShaft;
import de.dakror.quarry.structure.power.CableShaftBelow;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.structure.power.GasTurbine;
import de.dakror.quarry.structure.power.HighPowerShaft;
import de.dakror.quarry.structure.power.HighPowerShaftBelow;
import de.dakror.quarry.structure.power.PowerPole;
import de.dakror.quarry.structure.power.SolarPanel;
import de.dakror.quarry.structure.power.SolarPanelOutlet;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.structure.producer.AirPurifier;
import de.dakror.quarry.structure.producer.Excavator;
import de.dakror.quarry.structure.producer.Mine;
import de.dakror.quarry.structure.producer.OilWell;
import de.dakror.quarry.structure.storage.Barrel;
import de.dakror.quarry.structure.storage.Storage;
import de.dakror.quarry.structure.storage.Tank;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.QuarrySoundPlayer;
import de.dakror.quarry.util.SpriterDelegateBatch;
import de.dakror.quarry.util.StructureSoundSpatializer;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class Game extends GameScene {
    private enum LiningUpState {
        NoOther,
        LiningUp,
        NotLiningUp;
    }

    public class QuarryCameraControl extends EditorCameraControl {
        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            activeEnd = 0;

            return super.touchUp(screenX, screenY, pointer, button);
        }

        @Override
        public boolean mouseMoved(int screenX, int screenY) {
            if (Gdx.app.getType() == ApplicationType.Desktop) {
                viewport.unproject(tmp.set(screenX, screenY));
                hoverTileX = (int) (tmp.x / tileSize);
                hoverTileY = (int) (tmp.y / tileSize);

                return true;
            }
            return false;
        }

        public void deleteStructure(Structure<?> s) {
            if (s instanceof CopperCable) {
                layer.removeCable((CopperCable) s);
            } else {
                layer.removeStructure(s);
            }

            // remove attached hoppers
            int removedHoppers = layer.removeAttachedHoppers(s);

            if (Quarry.Q.sound.isPlaySound()) {
                destroySfx.play(Quarry.Q.sound.getSoundVolume(), (float) (Math.random() * 0.2 + 0.8), 0);
            }

            Items costs = s.getSchema().buildCosts;
            for (Amount e : costs.entries) {
                if (e.getAmount() > 1)
                    addResources(e.getItem(), (int) Math.ceil(e.getAmount()
                            * (hasScience(ScienceType.ConsiderateConstruction) ? Const.REFUND_CONSIDERATE_PERCENTAGE
                                    : Const.REFUND_PERCENTAGE)),
                            true);
            }

            // refund hoppers

            costs = Hopper.classSchema.buildCosts;
            for (Amount e : costs.entries) {
                if (e.getAmount() > 1)
                    addResources(e.getItem(), (int) Math.ceil(e.getAmount() * removedHoppers
                            * (hasScience(ScienceType.ConsiderateConstruction) ? Const.REFUND_CONSIDERATE_PERCENTAGE
                                    : Const.REFUND_PERCENTAGE)),
                            true);
            }
        }

        @Override
        public boolean handleTap(int x, int y, int tileX, int tileY) {
            activeEnd = 0;
            if (tileX < 0 || tileX >= layer.width || tileY < 0 || tileY >= layer.height)
                return true;

            System.out.println(tileX + ":" + tileY + " ("
                    + (layer.getIndex() * (layer.width * layer.height) + (tileX * layer.height + tileY)) + ")");

            if (FOGMODE) {
                layer.removeMeta(tileX, tileY, TileMeta.FOG_OF_WAR);
                FOGMODE = false;
            }

            if (cableDestroyMode) {
                CopperCable c = layer.getCable(tileX, tileY);
                if (c != null) {
                    if (activeStructure == c) {
                        deleteStructure(c);
                        //                        spatializedPlayer.play(c, cableSfx, (float) (Math.random() * 0.1 + 0.9), false);

                        resetActiveStructure();
                    } else if (!bulkCableMode && !bulkDestroyMode) {
                        activeStructure = c;
                    }
                }
                return true;
            }

            if ((bulkDestroyMode || bulkCableMode) && activeEnd == 0) {
                if ((isInEndMarkerHitbox(x, y, true) || isInEndMarkerHitbox(x, y, false)) && endB.x > -1) {
                    final Array<Structure<?>> toDelete = new Array<>();

                    int x1 = (int) Math.min(endA.x, endB.x);
                    int y1 = (int) Math.min(endA.y, endB.y);
                    int width = (int) Math.max(endA.x, endB.x) - x1 + 1;
                    int height = (int) Math.max(endA.y, endB.y) - y1 + 1;

                    for (Chunk c : layer.getChunks()) {
                        if (bulkDestroyMode) {
                            for (Structure<?> str : c.getStructures()) {
                                if (!str.getSchema().has(Flags.Indestructible)
                                        && !(str.x >= x1 + width
                                                || str.x + str.getWidth() <= x1
                                                || str.y + str.getHeight() <= y1
                                                || str.y >= y1 + height)) {
                                    toDelete.add(str);
                                }
                            }
                        } else {
                            for (CopperCable str : c.getCables().values()) {
                                if (!str.getSchema().has(Flags.Indestructible)
                                        && !(str.x >= x1 + width
                                                || str.x + str.getWidth() <= x1
                                                || str.y + str.getHeight() <= y1
                                                || str.y >= y1 + height)) {
                                    toDelete.add(str);
                                }
                            }
                        }
                    }

                    if (!toDelete.isEmpty()) {
                        ui.confirm.show(ui, Quarry.Q.i18n.get("confirm.destroy_structure_bulk"),
                                new Callback<Boolean>() {
                                    @Override
                                    public void call(Boolean e) {
                                        if (e instanceof Boolean && e == true) {
                                            for (Structure<?> s : toDelete) {
                                                deleteStructure(s);
                                            }

                                            resetActiveStructure();
                                        }
                                    }
                                });
                    }
                } else if (endA.x == -1) {
                    endA.set(tileX, tileY);
                } else {
                    endB.set(tileX, tileY);
                }

                return true;
            }

            if (copyMode && activeEnd == 0) {
                if ((isInEndMarkerHitbox(x, y, true) || isInEndMarkerHitbox(x, y, false)) && endB.x > -1
                        && ui.canAffordStructure) {
                    removeResources(ItemType.Blueprint, copyCost.get(ItemType.Blueprint), true);

                    copyStructures.clear();
                    copyCables.clear();
                    copyCost.clear();

                    for (Chunk c : layer.getChunks()) {
                        for (Structure<?> str : c.getStructures()) {
                            if (str instanceof ShaftDrillHead)
                                continue;

                            // exclude lifts from copy paste
                            if (str instanceof ItemLift)
                                continue;
                            if (str instanceof TubeShaft)
                                continue;
                            if (str instanceof CableShaft)
                                continue;
                            if (str instanceof HighPowerShaft)
                                continue;

                            if (!(str.x >= copyRegion[0] + copyRegion[2]
                                    || str.x + str.getWidth() <= copyRegion[0]
                                    || str.y + str.getHeight() <= copyRegion[1]
                                    || str.y >= copyRegion[1] + copyRegion[3])) {
                                Structure<?> clone = (Structure<?>) str.clone();
                                copyStructures.put(clone.x * layer.height + clone.y,
                                        new Pair<Structure<?>, CompoundTag>(clone, str.copy(copyRegion)));

                                for (Items.Amount t : str.getSchema().buildCosts.entries) {
                                    copyCost.put(t.getItem(), t.getAmount()
                                            + (copyCost.containsKey(t.getItem()) ? copyCost.get(t.getItem()) : 0));
                                }
                            }
                        }
                        for (CopperCable str : c.getCables().values()) {
                            if (!(str.x >= copyRegion[0] + copyRegion[2]
                                    || str.x + str.getWidth() <= copyRegion[0]
                                    || str.y + str.getHeight() <= copyRegion[1]
                                    || str.y >= copyRegion[1] + copyRegion[3])) {
                                Structure<?> clone = (Structure<?>) str.clone();
                                copyCables.put(clone.x * layer.height + clone.y,
                                        new Pair<Structure<?>, CompoundTag>(clone, str.copy(copyRegion)));

                                for (Items.Amount t : str.getSchema().buildCosts.entries) {
                                    copyCost.put(t.getItem(), t.getAmount()
                                            + (copyCost.containsKey(t.getItem()) ? copyCost.get(t.getItem()) : 0));
                                }
                            }
                        }
                    }

                    for (Pair<Structure<?>, CompoundTag> p : copyStructures.values()) {
                        p.getKey().paste(copyRegion, p.getVal());
                    }
                    for (Pair<Structure<?>, CompoundTag> p : copyCables.values()) {
                        p.getKey().paste(copyRegion, p.getVal());
                    }

                    resetActiveStructure();
                    // reshow copy table for new resources
                    pasteMode = true;
                    copyMode = false;
                    ui.updateCopyTableResources(true);
                    updateActiveElementPlaceable();
                } else if (endA.x == -1) {
                    endA.set(tileX, tileY);
                } else {
                    endB.set(tileX, tileY);
                    updateCopySelection();
                }

                return true;
            }

            Structure<?> s = layer.getStructure(tileX, tileY);
            if (s != null) {
                if (FLUIDMODE && s instanceof BrickChannel) {
                    s.acceptFluid(ItemType.MoltenCopper, 500, null);
                } else if (FLUIDMODE && s instanceof FluidTubeStructure) {
                    s.acceptFluid(ItemType.Water, 1000, null);
                }
                if (FILLMODE) {
                    if (s instanceof Storage) {
                        //((Storage) s).putBack(ItemType.CopperOre, 200);
                        for (ItemType t : ItemType.values) {
                            if (!t.categories.contains(ItemCategory.Fluid)
                                    && !t.categories.contains(ItemCategory.Abstract))
                                ((Storage) s).putBack(t, 100);
                        }
                    } else if (s instanceof Substation) {
                        ((Substation) s).acceptPower(100_000_000, NetworkStrength.PowerPole.maxPowerPerSecond);
                    } else if (s instanceof Tank) {
                        ((Tank) s).acceptFluid(ItemType.Water, 100_000_000, null);
                    }
                    FILLMODE = false;
                }
                if (structureDestroyMode) {
                    if (!s.getSchema().has(Flags.Indestructible)) {
                        if (activeStructure == s) {
                            final Structure<?> finalS = s;
                            Callback<Boolean> callback = new Callback<Boolean>() {

                                @Override
                                public void call(Boolean data) {
                                    if (data != null && data) {
                                        deleteStructure(finalS);
                                    }
                                    resetActiveStructure();
                                }
                            };

                            if (s.getSchema().has(Flags.ConfirmDestruction)) {
                                String text = Quarry.Q.i18n
                                        .get("confirm.destroy_structure." + s.getSchema().type.name().toLowerCase());
                                ui.confirm.show(Game.G.ui, Quarry.Q.i18n.get("confirm.destroy_structure")
                                        + (text.startsWith("???") ? "" : " " + text), callback);
                            } else if (layer.hasHoppersAttached(s)) {
                                ui.confirm.show(Game.G.ui, Quarry.Q.i18n.get("confirm.destroy_structure") + " "
                                        + Quarry.Q.i18n.get("confirm.destroy_structure.hoppers"), callback);
                            } else {
                                callback.call(true);
                            }
                        } else {
                            activeStructure = s;
                        }
                    }

                    return true;
                } else if (activeStructure == null && ui.tooltipCurrentStructure == null && s.getSchema().clickable
                        && !bulkDestroyMode && !bulkCableMode) {
                    ui.showStructureUI(s);
                }

                return true;
            } else {
                if (structureDestroyMode && (!bulkDestroyMode && !bulkCableMode)) {
                    resetActiveStructure();
                    return true;
                }
                ui.hideStructureUI();
            }

            if (activeStructure == null && ui.currentClickedStructure == null && !structureDestroyMode
                    && !cableDestroyMode && !(bulkDestroyMode || bulkCableMode)) {
                TileType t = layer.get(tileX, tileY);
                if (t == null || t == TileType.Air
                        || (activeTile != null && activeTile.x == tileX && activeTile.y == tileY)) {
                    ui.tileUI.hide();
                    resetActiveTile();
                } else {
                    ui.tileUI.show();
                    ui.tileUI.setText((layer.getMeta(tileX, tileY) & TileMeta.FOG_OF_WAR) == 0 ? t : null);
                    activeTile.set(tileX, tileY);
                }
                return true;
            } else if (tileX < 0 || tileX >= layer.width || tileY < 0 || tileY >= layer.height) {
                ui.hideStructureUI();
                ui.tileUI.hide();
                resetActiveTile();
                return false;
            } else {
                ui.tileUI.hide();
                resetActiveTile();

                return false;
            }
        }

        public QuarryCameraControl(Viewport viewport) {
            super(Const.TILE_SIZE, viewport);
        }

        public boolean isInEndMarkerHitbox(int x, int y, boolean isA) {
            Vector2 end = isA ? endA : endB;

            float ax = (end.x) * Const.TILE_SIZE;
            float ay = (end.y + 0.4f) * Const.TILE_SIZE;
            return x >= ax && x <= ax + Const.TILE_SIZE && y >= ay & y <= ay + Const.TILE_SIZE + 10;
        }

        @Override
        protected boolean isWithinActiveElement(int x, int y, int tileX, int tileY) {
            if (pasteMode) {
                return tileX >= copyRegion[0] && tileX < copyRegion[0] + copyRegion[2] &&
                        tileY >= copyRegion[1] && tileY < copyRegion[1] + copyRegion[3];
            }
            if (bulkDestroyMode || bulkCableMode || copyMode) {
                return isInEndMarkerHitbox(x, y, true) || isInEndMarkerHitbox(x, y, false);
            } else {
                if (activeStructure.getSchema().has(Flags.Draggable) && endB.x > -1) {
                    return isInEndMarkerHitbox(x, y, true) || isInEndMarkerHitbox(x, y, false);
                } else {
                    return tileX >= activeStructure.x && tileX < activeStructure.x + activeStructure.getWidth()
                            && tileY >= activeStructure.y && tileY < activeStructure.y + activeStructure.getHeight();
                }
            }
        }

        protected void updateCopySelection() {
            int x1 = (int) Math.min(endA.x, endB.x);
            int y1 = (int) Math.min(endA.y, endB.y);
            int width = (int) Math.max(endA.x, endB.x) - x1 + 1;
            int height = (int) Math.max(endA.y, endB.y) - y1 + 1;
            copyRegion[0] = x1;
            copyRegion[1] = y1;
            copyRegion[2] = width;
            copyRegion[3] = height;
            copyCost.put(ItemType.Blueprint, width * height);
            ui.updateCopyTableResources(false);
        }

        @Override
        protected void setParamRawPosition(Vector2 rawPosition, int x, int y, int tileX, int tileY) {
            if (pasteMode) {
                rawPosition.set(copyRegion[0] * Const.TILE_SIZE, copyRegion[1] * Const.TILE_SIZE);
            } else if (((bulkDestroyMode || bulkCableMode || copyMode)
                    || activeStructure.getSchema().has(Flags.Draggable)) && endB.x > -1) {
                rawPosition.set(tileX * Const.TILE_SIZE, tileY * Const.TILE_SIZE);

                // set which end pole is active right now
                // start with endB first because youre more likely to move that
                if (isInEndMarkerHitbox(x, y, false)) {
                    activeEnd = 2;
                } else if (isInEndMarkerHitbox(x, y, true)) {
                    activeEnd = 1;
                }
            } else if (activeStructure != null) {
                rawPosition.set(activeStructure.x * Const.TILE_SIZE, activeStructure.y * Const.TILE_SIZE);
            }
        }

        @Override
        protected boolean isActiveElementEnabled() {
            if (pasteMode)
                return true;
            if (bulkDestroyMode || bulkCableMode || copyMode)
                return endA.x > -1;

            if (activeStructure == null)
                return false;

            if (activeStructure.getSchema().has(Flags.Draggable)) {
                return endA.x > -1 && !structureDestroyMode;
            } else {
                return activeStructure.x > -1 && !structureDestroyMode;
            }
        }

        @Override
        public void clampCam(OrthographicCamera cam) {
            cam.position.x = MathUtils.clamp(cam.position.x, 0, layer.width * Const.TILE_SIZE);
            cam.position.y = MathUtils.clamp(cam.position.y, 0, layer.height * Const.TILE_SIZE);
        }

        @Override
        protected boolean clampZoom(float zoom) {
            maxZoom = Math.max(Math.max(Const.H, layer.height * Const.TILE_SIZE) / Const.H,
                    Math.max(Const.W, layer.width * Const.TILE_SIZE) / Const.W);
            return super.clampZoom(zoom);
        }

        @Override
        public boolean scrolled(int amount) {
            if (SMOOTH_CAMERA) {
                cameraZoomAcc += amount * 2;
                return true;
            } else {
                return super.scrolled(amount);
            }
        }

        protected void setCopyPosition(int x, int y) {
            for (Pair<Structure<?>, CompoundTag> p : copyStructures.values()) {
                Structure<?> s = p.getKey();
                s.x = s.x - copyRegion[0] + x;
                s.y = s.y - copyRegion[1] + y;
            }
            for (Pair<Structure<?>, CompoundTag> p : copyCables.values()) {
                Structure<?> s = p.getKey();
                s.x = s.x - copyRegion[0] + x;
                s.y = s.y - copyRegion[1] + y;
            }
            copyRegion[0] = x;
            copyRegion[1] = y;
            //            int i = 0;
            //            for (Structure<?> s : copyStructures) {
            //                s.paste(copyRegion, (CompoundTag) copyStructuresData.data.get(i++));
            //            }
            //            i = 0;
            //            for (Structure<?> s : copyCables) {
            //                s.paste(copyRegion, (CompoundTag) copyCablesData.data.get(i++));
            //            }
        }

        @Override
        protected void setActiveElementPosition(Vector2 rawPosition) {
            int x = (int) (rawPosition.x / Const.TILE_SIZE + 0.5f), y = (int) (rawPosition.y / Const.TILE_SIZE + 0.5f);

            x = MathUtils.clamp(x, 0, layer.width - 1);
            y = MathUtils.clamp(y, 0, layer.height - 1);

            if (bulkDestroyMode || bulkCableMode || copyMode) {
                if (activeEnd == 1) {
                    endA.set(x, y);
                } else {
                    endB.set(x, y);
                }

                if (copyMode && endB.x > -1) {
                    updateCopySelection();
                }
            } else if (pasteMode) {
                if (copyRegion[0] != x || copyRegion[1] != y) {
                    setCopyPosition(x, y);
                }
            } else {
                if (activeStructure.getSchema().has(Flags.Draggable)) {
                    if (activeEnd == 1) {
                        endA.set(x, y);

                        activeStructure.x = x;
                        activeStructure.y = y;
                        activeStructure.onPlacement(false);

                        if (endB.equals(endA) && endA.x > -1) {
                            endB.x = -1;
                            endB.y = -1;
                            activeStructureTrail.clear();
                            activeStructure.update(0, 1, Game.G.layer.dirtyBounds);
                        }
                    } else {
                        endB.set(x, y);

                        if (endB.equals(endA) && endA.x > -1) {
                            endB.x = -1;
                            endB.y = -1;
                            activeStructureTrail.clear();
                            activeStructure.update(0, 1, Game.G.layer.dirtyBounds);
                        }
                    }

                    //                    if (endB.x == endA.x || endB.y == endA.y) {
                    updateTrail();
                    //                    }
                    ui.updateTooltipResources();
                } else if (activeStructure.x != x || activeStructure.y != y) {
                    activeStructure.x = x;
                    activeStructure.y = y;
                    activeStructure.onPlacement(false);

                    smartChecksOnPlacement();
                }
            }
        }

        boolean placingTrail;

        public void updateTrail() {
            if (endA.x == -1)
                return;

            activeStructure.update(0, 1, Game.G.layer.dirtyBounds);

            if (placingTrail || endB.x == -1)
                return;

            activeStructureTrail.clear();
            activeStructurePath.clear();
            if (activeStructure instanceof CopperCable) {
                activeStructurePath.addAll(tilePathfinding.findPath(cableNetwork,
                        (int) (endB.x * layer.height + endB.y),
                        (int) (endA.x * layer.height + endA.y)));
            } else {
                activeStructurePath.addAll(tilePathfinding.findPath(tileNetwork,
                        (int) (endB.x * layer.height + endB.y),
                        (int) (endA.x * layer.height + endA.y)));
            }

            Structure<?> last = null;

            if (activeStructurePath.size() > 0) {
                for (int i : activeStructurePath) {
                    int x = i / layer.height;
                    int y = i % layer.height;

                    Structure<?> s = (Structure<?>) activeStructure.clone();

                    if (s instanceof IRotatable) {
                        Direction rotation = ((IRotatable) activeStructure).getDirection();
                        if (last != null) {
                            if (x == last.x) {
                                rotation = y > last.y ? Direction.South : Direction.North;
                            } else {
                                rotation = x > last.x ? Direction.West : Direction.East;
                            }
                        }
                        ((IRotatable) s).setRotation(rotation);
                        if (last != null && last.x == endB.x && last.y == endB.y) {
                            ((IRotatable) last).setRotation(rotation);
                        }

                        last = s;
                    }

                    s.x = x;
                    s.y = y;

                    activeStructureTrail.put(i, s);
                }
            }

            // check if end is next to only one "thing" and rotate it to there
            if (activeStructure instanceof Conveyor
                    && activeStructureTrail.containsKey((int) (endB.x * layer.height + endB.y))
                    && endB.x * layer.height + endB.y != endA.x * layer.height + endA.y) {
                Structure<?> target = null;
                Direction targetDirection = null;
                Structure<?> s = activeStructureTrail.get((int) endB.x * layer.height + (int) endB.y);
                o: for (Direction d : Direction.values) {
                    Structure<?> q = layer.getStructure((int) endB.x + d.dx, (int) endB.y + d.dy);
                    if (q != null && !activeStructureTrail.containsKey(q.getIndex())) {
                        if (q instanceof Conveyor) {
                            // more than one eligile, aborting
                            if (target != null) {
                                target = null;
                                break o;
                            }
                            // cant connect to the side of a electric conveyor core
                            if (q instanceof ElectricConveyorCore && ((Conveyor) q).getDirection() != d
                                    && ((Conveyor) q).getDirection() != d.inv()) {
                                target = null;
                                break o;
                            }

                            target = q;
                            targetDirection = d;
                        } else if (q.getDocks().length > 0) {
                            for (Dock dock : q.getDocks()) {
                                if (q.isNextToDock((int) endB.x, (int) endB.y, d, dock)) {
                                    // more than one eligile, aborting
                                    if (target != null) {
                                        target = null;
                                        break o;
                                    }
                                    target = q;
                                    targetDirection = d;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (target != null) {
                    ((IRotatable) s).setRotation(targetDirection);
                }
            }

            // call after full trail is built
            for (Structure<?> s : activeStructureTrail.values()) {
                s.onPlacement(false);
            }
        }

        protected void smartChecksOnPlacement() {
            if (activeStructure instanceof Hopper && !pasteMode) {
                Structure<?> target = null;
                Direction targetDirection = null;
                o: for (Direction d : Direction.values) {
                    Structure<?> q = layer.getStructure(activeStructure.x + d.dx, activeStructure.y + d.dy);
                    if (q != null) {
                        if (q instanceof Conveyor) {
                            // more than one eligile, aborting
                            if (target != null) {
                                target = null;
                                break o;
                            }
                            target = q;
                            targetDirection = d;
                        } else if (q.getDocks().length > 0) {
                            for (Dock dock : q.getDocks()) {
                                if (q.isNextToDock(activeStructure.x, activeStructure.y, d, dock)) {
                                    // more than one eligile, aborting
                                    if (target != null) {
                                        target = null;
                                        break o;
                                    }
                                    target = q;
                                    targetDirection = d;
                                    break;
                                }
                            }
                        }
                    }
                }

                if (target != null) {
                    ((IRotatable) activeStructure).setRotation(targetDirection);
                }
            }
        }

        protected boolean isStructurePlaceable(Structure<?> structure) {
            synchronized (highlightLock) {
                if (tutorialHighlight.size > 0) {
                    for (int i = 0; i < structure.getWidth(); i++) {
                        for (int j = 0; j < structure.getHeight(); j++) {
                            if (!tutorialHighlight.contains((i + structure.x) * layer.height + (j + structure.y))) {
                                return false;
                            }
                        }
                    }

                    if (structure instanceof Mine) {
                        if (structure.getUpDirection() != Direction.North)
                            return false;
                    }
                }
            }

            boolean fog = !layer.isNotInFogOfWar(structure, structure instanceof AirPurifier);

            if (structure instanceof CopperCable) {
                if (layer.getCable(structure.x, structure.y) != null
                        || layer.isPowerDockCollision(structure.x, structure.y))
                    return false;
                else
                    return !fog;
            }

            if (structure instanceof PowerPole) {
                if (layer.getCable(structure.x, structure.y) != null
                        || layer.isPowerDockCollision(structure.x, structure.y))
                    return false;
            }

            if ((structure instanceof SolarPanel || structure instanceof SolarPanelOutlet) && layerIndex != 0) {
                return false;
            }

            if (structure instanceof ItemLiftBelow
                    || structure instanceof CableShaftBelow
                    || structure instanceof TubeShaftBelow
                    || structure instanceof HighPowerShaftBelow) {
                if (layerIndex == 0
                        || getLayer(layerIndex - 1).isColliding(structure, true, true)
                        || !getLayer(layerIndex - 1).isNotInFogOfWar(structure, false))
                    return false;
            } else if (structure instanceof ItemLift
                    || structure instanceof CableShaft
                    || structure instanceof TubeShaft
                    || structure instanceof HighPowerShaft) {
                if (layers.size <= layerIndex + 1
                        || getLayer(layerIndex + 1).isColliding(structure, true, true)
                        || !getLayer(layerIndex + 1).isNotInFogOfWar(structure, false))
                    return false;
            }

            if (structure instanceof OilWell) {
                int count = 0;

                o: for (int i = 0; i < structure.getWidth(); i++) {
                    for (int j = 0; j < structure.getHeight(); j++) {
                        if (layer.get(structure.x + i, structure.y + j) == TileType.CrudeOil) {
                            count++;

                            if (count >= OilWell.MIN_OIL_TILES)
                                break o;
                        }
                    }
                }

                if (count < OilWell.MIN_OIL_TILES)
                    return false;
            }

            if (structure instanceof Excavator) {
                EnumMap<TileType, Integer> counts = new EnumMap<>(TileType.class);

                for (int i = 0; i < structure.getWidth(); i++) {
                    for (int j = 0; j < structure.getHeight(); j++) {
                        TileType tile = layer.get(structure.x + i, structure.y + j);
                        if (tile == TileType.CopperOre
                                || tile == TileType.IronOre
                                || tile == TileType.TinOre
                                || tile == TileType.CoalOre) {
                            counts.put(tile, (counts.containsKey(tile) ? counts.get(tile) : 0) + 1);
                        }
                    }
                }
                boolean any = false;
                for (Map.Entry<TileType, Integer> e : counts.entrySet()) {
                    if (e.getValue() >= Excavator.MIN_ORE_TILES) {
                        if (any)
                            return false;
                        else
                            any = true;
                    }
                }

                if (!any)
                    return false;
            }

            if (structure instanceof Hopper) {
                Hopper h = (Hopper) structure;
                if (!h.isValidRotation(h.getDirection())) {
                    return false;
                }
            }

            if (structure instanceof Mine) {
                if (((Mine) structure).getMineableItems() == 0)
                    return false;
            }

            boolean colliding = layer.isColliding(structure,
                    !(structure instanceof Conveyor),
                    !(structure instanceof FluidTubeStructure));

            if (structure instanceof ConveyorBridge && !fog && !pasteMode) {
                Structure<?> x = layer.getStructure(structure.x, structure.y);
                if (x != null && x.getSchema().type == StructureType.Conveyor) {
                    ((ConveyorBridge) structure).setRotation(((Conveyor) x).getDirection());
                    return true;
                }
            }

            if (!fog && structure.getSchema().has(Flags.Stackable)) {
                liningUpInput = LiningUpState.NoOther;
                liningUpOutput = LiningUpState.NoOther;
                liningUpStructureInput = null;
                liningUpStructureOutput = null;

                getStateOfLiningUpWithOtherStackable(structure, DockType.StackIn);
                getStateOfLiningUpWithOtherStackable(structure, DockType.StackOut);

                if (structure instanceof DistillationColumn) {
                    ((DistillationColumn) structure).setLevel(0);
                }

                if (liningUpInput == LiningUpState.NotLiningUp || liningUpOutput == LiningUpState.NotLiningUp) {
                    return false;
                }

                if (liningUpInput == LiningUpState.LiningUp || liningUpOutput == LiningUpState.LiningUp) {
                    // do own check for collisions
                    boolean collisionFree = isCollisionFreeStacking(structure);

                    if (!collisionFree)
                        return false;

                    if (structure instanceof DistillationColumn) {
                        if (liningUpInput == LiningUpState.LiningUp
                                && liningUpOutput == LiningUpState.LiningUp
                                && (((DistillationColumn) liningUpStructureInput).getLevels() > 1
                                        || ((DistillationColumn) liningUpStructureOutput).getLevels() > 1)) {

                            liningUpInput = LiningUpState.NoOther;
                            liningUpOutput = LiningUpState.NoOther;
                            liningUpStructureInput = null;
                            liningUpStructureOutput = null;

                            return false;
                        }

                        if (liningUpInput == LiningUpState.LiningUp) {
                            if (((DistillationColumn) liningUpStructureInput).getLevel() == 2) {
                                liningUpInput = LiningUpState.NoOther;
                                liningUpStructureInput = null;
                                return false;
                            } else {
                                ((DistillationColumn) structure)
                                        .setLevel(((DistillationColumn) liningUpStructureInput).getLevel() + 1);
                            }
                        } else if (liningUpOutput == LiningUpState.LiningUp) {
                            if (((DistillationColumn) liningUpStructureOutput).getLevels() == 3) {
                                liningUpOutput = LiningUpState.NoOther;
                                liningUpStructureOutput = null;
                                return false;
                            } else {
                                ((DistillationColumn) structure).setLevel(0);
                            }
                        } else {
                            ((DistillationColumn) structure).setLevel(0);
                        }
                    }

                    return true;
                }
            }

            if (fog || colliding) {
                return false;
            }

            return true;
        }

        @Override
        protected boolean checkActiveElementPlaceable() {
            if (!ui.canAffordStructure && !GOD_MODE)
                return false;

            if (pasteMode) {
                copyCollisions.clear();
                for (Pair<Structure<?>, CompoundTag> s : copyStructures.values()) {
                    if (!isStructurePlaceable(s.getKey())) {
                        copyCollisions.add(s.getKey());
                    }
                }
                for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                    if (!isStructurePlaceable(s.getKey())) {
                        copyCollisions.add(s.getKey());
                    }
                }
                if (!copyCollisions.isEmpty())
                    return false;

                return true;
            }

            if (activeStructure == null
                    || (activeStructure.getSchema().has(Flags.Draggable) && (endA.x == -1 && endB.x == -1)))
                return false;

            if (endB.x > -1) {
                if (activeStructurePath.isEmpty()) {
                    return false;
                }

                synchronized (highlightLock) {
                    if (tutorialHighlight.size > 0) {
                        for (int i : activeStructurePath) {
                            if (!tutorialHighlight.contains(i))
                                return false;
                        }
                    }
                }

                for (IntMap.Keys iter = activeStructureTrail.keys(); iter.hasNext;) {
                    if (!Game.this.trailStructureCanBePlaced(iter.next(), (int) (endA.x * layer.height + endA.y),
                            (int) (endB.x * layer.height + endB.y))) {
                        return false;
                    }
                }

                return true;
            }

            return isStructurePlaceable(activeStructure);
        }

        protected boolean isCollisionFreeStacking(Structure<?> structure) {
            // inside the structure (for other docks poking in)
            for (int i = 0; i < structure.getWidth(); i++) {
                for (int j = 0; j < structure.getHeight(); j++) {
                    if (structure.x + i < 0 || structure.x + i >= layer.width)
                        return false;
                    if (structure.y + j < 0 || structure.y + j >= layer.height)
                        return false;

                    // if overlap with other structure, hard fail
                    if (layer.isCollision(structure.x + i, structure.y + j, false, false)) {
                        return false;
                    }

                    // if overlap with tube, test for possible stackable
                    if (layer.isCollision(structure.x + i, structure.y + j, true, true)) {
                        // find own stack dock
                        for (Dock d : structure.getDocks()) {
                            if ((d.type == DockType.StackIn || d.type == DockType.StackOut)
                                    && d.x - d.dir.dx == i
                                    && d.y - d.dir.dy == j) {
                                // check all directions for tube origins
                                for (Direction dir : Direction.values) {
                                    // now walk into direction to find possible other structure
                                    // which has to be one of the lining up structures, or null
                                    Structure<?> s = layer.getStructure(
                                            structure.x + i + dir.dx,
                                            structure.y + j + dir.dy);
                                    if (s != null && !(s == liningUpStructureInput || s == liningUpStructureOutput)) {
                                        return false;
                                    }
                                }
                                // no not-lining up structure found, all good
                                return true;
                            }
                        }

                        // fall through, collision without possible stack dock
                        return false;
                    }

                }
            }

            // check own docks (outside collisions)
            for (Dock d : structure.getDocks()) {
                int sx = structure.x + d.x + d.dir.dx;
                int sy = structure.y + d.y + d.dir.dy;

                if (d.type == DockType.StackIn || d.type == DockType.StackOut) {
                    continue;
                }

                if (sx < 0 || sy < 0 || sx >= layer.width || sy >= layer.height) {
                    return false;
                }

                if (d.type == DockType.Power || d.type == DockType.BigPower) {
                    if ((layer.getFlags(sx, sy) & Layer.FLAG_POWER_DOCK_COLLISION) != 0) {
                        return false;
                    }
                } else if ((layer.getFlags(sx, sy) & Layer.FLAG_STRUCTURE_COLLISION) != 0) {
                    // structure <-> dock collision, check for other structure being liningup 
                    Structure<?> s = layer.getStructure(sx, sy);

                    return s == liningUpStructureInput || s == liningUpStructureOutput;
                }
            }

            return true;
        }

        protected void getStateOfLiningUpWithOtherStackable(Structure<?> structure, DockType myType) {
            DockType theirType = myType == DockType.StackIn ? DockType.StackOut : DockType.StackIn;

            Structure<?> other = null;

            boolean allDocks = true;

            loop: for (Dock d : structure.getDocks()) {
                if (d.type == myType) {
                    Structure<?> s = layer.getStructure(structure.x + d.x, structure.y + d.y);
                    if (s == null
                            || s.getSchema().type != structure.getSchema().type
                            || (other != null && s != other)) {
                        allDocks = false;
                        // do not break here so we can perhaps identify <other> later on
                    } else {
                        if (s.getUpDirection() != structure.getUpDirection()) {
                            other = s;
                            allDocks = false;
                            break;
                        }

                        for (Dock d1 : s.getDocks()) {
                            if (d1.type == theirType
                                    && d1.dir == d.dir.inv()
                                    && s.x + d1.x - d1.dir.dx == structure.x + d.x
                                    && s.y + d1.y - d1.dir.dy == structure.y + d.y) {
                                other = s;
                                continue loop;
                            }
                        }
                        allDocks = false;
                        break;
                    }
                }
            }

            if (myType == DockType.StackIn) {
                if (other == null)
                    liningUpInput = LiningUpState.NoOther;
                else
                    liningUpInput = allDocks ? LiningUpState.LiningUp : LiningUpState.NotLiningUp;
                liningUpStructureInput = other;
            } else {
                if (other == null)
                    liningUpOutput = LiningUpState.NoOther;
                else
                    liningUpOutput = allDocks ? LiningUpState.LiningUp : LiningUpState.NotLiningUp;
                liningUpStructureOutput = other;
            }
        }

        @Override
        protected void placeActiveElement() {
            if (endB.x > -1) {
                placingTrail = true;
                for (Structure<?> s : activeStructureTrail.values()) {
                    placeStructure(layer, s);
                }

                // check if pre-existing conveyor at endA should be rotated
                if (activeStructure instanceof Conveyor
                        && !activeStructureTrail.containsKey((int) (endA.x * layer.height + endA.y))
                        && activeStructurePath.size() > 0) {
                    Structure<?> s = layer.getStructure((int) endA.x, (int) endA.y);
                    if (s != null && s instanceof Conveyor) {
                        int first = activeStructurePath.get(activeStructurePath.size() - 1);
                        int dx = first / layer.height - (int) endA.x;
                        int dy = first % layer.height - (int) endA.y;

                        if (dx == -1)
                            ((IRotatable) s).setRotation(Direction.West);
                        else if (dx == 1)
                            ((IRotatable) s).setRotation(Direction.East);
                        else if (dy == -1)
                            ((IRotatable) s).setRotation(Direction.South);
                        else if (dy == 1)
                            ((IRotatable) s).setRotation(Direction.North);
                    }
                }

                // check if conveyor next to endA has a free end and could be made to fit us
                if (activeStructure instanceof Conveyor
                        && activeStructureTrail.containsKey((int) (endA.x * layer.height + endA.y))) {
                    for (Direction d : Direction.values) {
                        if (!activeStructureTrail
                                .containsKey((int) ((endA.x + d.dx) * layer.height + (endA.y + d.dy)))) {
                            Structure<?> q = layer.getStructure((int) endA.x + d.dx, (int) endA.y + d.dy);
                            if (q != null && q.getSchema().type == StructureType.Conveyor) {
                                if (((Conveyor) q).getStructureInDirection(((Conveyor) q).getDirection()) == null) {
                                    ((Conveyor) q).setRotation(d.inv());
                                    break;
                                }
                            }
                        }
                    }
                }

                activeStructureTrail.clear();

                endA.set(-1, 0);
                endB.set(-1, 0);
                activeStructure.x = -1;
                activeStructure.y = 0;

                ui.updateTooltipResources();
                placingTrail = false;
            } else {
                if (pasteMode) {
                    Array<Pair<Structure<?>, CompoundTag>> pairs = new Array<>();
                    for (Pair<Structure<?>, CompoundTag> p : copyStructures.values()) {
                        Structure<?> s = p.getKey();

                        /*if (s instanceof ItemLiftBelow) {
                            Structure<?> q = new ItemLift(s.x, s.y);
                            q.setUpDirection(s.getUpDirection());
                            placeStructure(layers.get(layerIndex - 1), q);
                            pairs.add(new Pair<Structure<?>, CompoundTag>(q, p.getVal()));
                        } else if (s instanceof CableShaftBelow) {
                            Structure<?> q = new CableShaft(s.x, s.y);
                            q.setUpDirection(s.getUpDirection());
                            placeStructure(layers.get(layerIndex - 1), q);
                            pairs.add(new Pair<Structure<?>, CompoundTag>(q, p.getVal()));
                        } else if (s instanceof TubeShaftBelow) {
                            Structure<?> q = new TubeShaft(s.x, s.y);
                            q.setUpDirection(s.getUpDirection());
                            placeStructure(layers.get(layerIndex - 1), q);
                            pairs.add(new Pair<Structure<?>, CompoundTag>(q, p.getVal()));
                        } else if (s instanceof HighPowerShaftBelow) {
                            Structure<?> q = new HighPowerShaft(s.x, s.y);
                            q.setUpDirection(s.getUpDirection());
                            placeStructure(layers.get(layerIndex - 1), q);
                            pairs.add(new Pair<Structure<?>, CompoundTag>(q, p.getVal()));
                        } else {*/
                        Structure<?> q = (Structure<?>) s.clone();
                        placeStructure(layer, q);
                        pairs.add(new Pair<Structure<?>, CompoundTag>(q, p.getVal()));
                        //}
                    }

                    for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                        Structure<?> q = (Structure<?>) s.getKey().clone();
                        placeStructure(layer, q);
                        pairs.add(new Pair<Structure<?>, CompoundTag>(q, s.getVal()));
                    }

                    for (Pair<Structure<?>, CompoundTag> s : pairs) {
                        s.getKey().paste(copyRegion, s.getVal());
                    }
                } else {
                    // check if conveyor next to endA has a free end and could be made to fit us
                    if (activeStructure instanceof Conveyor) {
                        for (Direction d : Direction.values) {
                            Structure<?> q = layer.getStructure(activeStructure.x + d.dx, activeStructure.y + d.dy);
                            if (q != null && q.getSchema().type == StructureType.Conveyor) {
                                if (((Conveyor) q).getStructureInDirection(((Conveyor) q).getDirection()) == null) {
                                    ((Conveyor) q).setRotation(d.inv());
                                    break;
                                }
                            }
                        }
                    }

                    // remove conveyor when building conveyor bridge on top
                    if (activeStructure instanceof ConveyorBridge) {
                        Structure<?> x = layer.getStructure(activeStructure.x, activeStructure.y);
                        if (x != null && x.getSchema().type == StructureType.Conveyor) {
                            layer.removeStructure(x);
                        }
                    }

                    placeStructure(layer, (Structure<?>) activeStructure.clone());

                    if (endA.x > -1) {
                        endA.set(-1, 0);
                        endB.set(-1, 0);
                        activeStructure.x = -1;
                        activeStructure.y = 0;
                    }
                }
            }
        }

        protected void placeStructure(Layer currentLayer, Structure<?> structure) {
            if (currentLayer.addStructure(structure)) {
                if (!GOD_MODE) {
                    Items costs = structure.getSchema().buildCosts;

                    for (Amount e : costs.entries) {
                        removeResources(e.getItem(), e.getAmount(), true);
                    }
                }

                synchronized (highlightLock) {
                    if (tutorialHighlight.size > 0) {
                        for (int i = 0; i < structure.getWidth(); i++) {
                            for (int j = 0; j < structure.getHeight(); j++) {
                                tutorialHighlight.remove((i + structure.x) * currentLayer.height + (j + structure.y));
                            }
                        }
                    }
                }

                if (!structure.getSchema().has(Flags.NoDustEffect)) {
                    // pfx
                    for (int i = 0; i < structure.getWidth(); i++) {
                        PooledEffect effect = dustPfxD.obtain();
                        effect.reset();
                        effect.setPosition(Const.TILE_SIZE * (structure.x + i), Const.TILE_SIZE * structure.y);
                        effect.setDuration(10);
                        layer.addParticleEffect(effect, true);

                        effect = dustPfxU.obtain();
                        effect.reset();
                        effect.setPosition(Const.TILE_SIZE * (structure.x + i),
                                Const.TILE_SIZE * (structure.y + structure.getHeight()));
                        effect.setDuration(10);
                        currentLayer.addParticleEffect(effect, true);
                    }

                    for (int i = 0; i < structure.getHeight(); i++) {
                        PooledEffect effect = dustPfxL.obtain();
                        effect.reset();
                        effect.setPosition(Const.TILE_SIZE * structure.x, Const.TILE_SIZE * (structure.y + i));
                        effect.setDuration(10);
                        layer.addParticleEffect(effect, true);

                        effect = dustPfxR.obtain();
                        effect.reset();
                        effect.setPosition(Const.TILE_SIZE * (structure.x + structure.getWidth()),
                                Const.TILE_SIZE * (structure.y + i));
                        effect.setDuration(10);
                        currentLayer.addParticleEffect(effect, true);
                    }
                }

                //                spatializedPlayer.play(structure, buildSfx, (float) (Math.random() * 0.4 + 0.6), false);
                if (Quarry.Q.sound.isPlaySound()) {
                    buildSfx.play(Quarry.Q.sound.getSoundVolume(), (float) (Math.random() * 0.4 + 0.6), 0);
                }
            }
        }

        @Override
        protected boolean handleInitialPlacement(int tileX, int tileY) {
            if (pasteMode) {
                int x = (int) Math
                        .ceil(MathUtils.clamp(tileX - copyRegion[2] / 2f + 0.5f, 0, layer.width - copyRegion[2]));
                int y = (int) Math
                        .ceil(MathUtils.clamp(tileY - copyRegion[3] / 2f + 0.5f, 0, layer.height - copyRegion[3]));

                if (copyRegion[0] != x || copyRegion[1] != y) {
                    setCopyPosition(x, y);
                    return true;
                }

            }
            if (activeStructure == null || structureDestroyMode || cableDestroyMode)
                return false;

            int x = (int) Math.ceil(MathUtils.clamp(tileX - activeStructure.getSchema().width / 2f + 0.5f, 0,
                    layer.width - activeStructure.getWidth()));
            int y = (int) Math.ceil(MathUtils.clamp(tileY - activeStructure.getSchema().height / 2f + 0.5f, 0,
                    layer.height - activeStructure.getHeight()));

            if (activeStructure.getSchema().has(Flags.Draggable)) {
                if (endA.x == -1) {
                    endA.set(x, y);

                    activeStructure.x = x;
                    activeStructure.y = y;
                    activeStructure.onPlacement(false);
                } else if (!(x == endA.x && y == endA.y)) {
                    endB.set(x, y);
                }

                if (endA.x > -1 && endB.x > -1) {
                    updateTrail();
                }
            } else if (activeStructure.x != x || activeStructure.y != y) {
                activeStructure.x = x;
                activeStructure.y = y;
                activeStructure.onPlacement(false);

                activeStructureTrail.clear();

                smartChecksOnPlacement();
            }
            return true;
        }
    }

    /**
     * Iterate through layer list starting at current layer and then at 0 skipping current layer
     *
     * @author Maximilian Stark | Dakror
     */
    private class LayerIterable implements Iterable<Layer> {
        private class LayerIterator implements Iterator<Layer> {
            boolean current = false;
            int index = 0;

            public void reset() {
                current = false;
                index = 0;
            }

            @Override
            public boolean hasNext() {
                return !current || index < layers.size - 1;
            }

            @Override
            public Layer next() {
                if (!current) {
                    current = true;
                    return layer;
                } else {
                    // skip duplicate current layer
                    if (index == layer.getIndex())
                        index++;
                    return layers.get(index++);
                }
            }
        }

        private LayerIterator iter = new LayerIterator();

        @Override
        public Iterator<Layer> iterator() {
            iter.reset();
            return iter;
        }

    }

    public static boolean GOD_MODE = false;
    public static boolean DRAW_DEBUG = false;
    public static boolean FLUIDMODE = false;
    public static boolean FILLMODE = false;
    public static boolean RECORDMODE = false;
    public static boolean SCREENSHOT = false;
    public static boolean FOGMODE = false;
    public static boolean SINGLE_FRAME = false;
    public static boolean UI_VISIBLE = true;
    public static boolean SMOOTH_CAMERA = false;

    private static final Pattern fileRegex = Pattern.compile("[^0-9a-zA-Z-_]");

    private static final HashMap<String, Texture> saveThumbnailCache = new HashMap<>();

    private static final Object layerLock = new Object();
    private static final Object resourceLock = new Object();
    private static final Object highlightLock = new Object();
    public static final Object renderThreadLock = new Object();

    static final Bounds tempBounds = new Bounds();

    public static Game G;
    Batch batch;
    BatchDelegate delegate;
    OrthographicCamera cam;
    OrthographicCamera fboCam;
    ShaderProgram colorShader;
    ShapeRenderer shaper;
    DepthSpriter spriter;
    SpriterDelegateBatch pfxBatch;

    public GameUi ui;
    public InputMultiplexer input;

    public final Array<Runnable> renderThreadTasks = new Array<>();

    public Viewport viewport;
    // Player stuff
    private Array<Layer> layers;
    private LayerIterable layerIter = new LayerIterable();
    FrameBuffer[] chunkFBOs; // array of fbo, one for each chunk per layer
    int layerIndex;
    public Layer layer;
    int deltaLayer;
    public final PowerGrid powerGrid = new PowerGrid();
    AStar<Integer> tilePathfinding;
    Network<Integer> tileNetwork = new AStar.Network<Integer>() {

        @Override
        public float getH(Integer start, Integer end) {
            int x1 = start / layer.height;
            int y1 = start % layer.height;

            int x2 = end / layer.height;
            int y2 = end % layer.height;

            // euclid distance
            //            return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
            // manhattan distance
            return Math.abs(x2 - x1) + Math.abs(y2 - y1);
        }

        @Override
        public float getEdgeLength(Integer start, Integer end) {
            return layer.get(end / layer.height, end % layer.height) != TileType.Stone ? 1.25f : 1;
        }

        @Override
        public void visitNeighbors(Integer node, Integer start, Integer end, Visitor<Integer> visitor) {
            int x1 = node / layer.height;
            int y1 = node % layer.height;

            for (Direction d : Direction.values) {
                int x2 = x1 + d.dx;
                int y2 = y1 + d.dy;

                if (x2 < 0 || y2 < 0 || x2 >= layer.width || y2 >= layer.height)
                    continue;

                int index = x2 * layer.height + y2;

                if (trailStructureCanBePlaced(index, start, end)) {
                    visitor.visit(index);
                }
            }
        }
    };
    Network<Integer> cableNetwork = new AStar.Network<Integer>() {

        @Override
        public float getH(Integer start, Integer end) {
            int x1 = start / layer.height;
            int y1 = start % layer.height;

            int x2 = end / layer.height;
            int y2 = end % layer.height;

            // euclid distance
            //            return (float) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow(y1 - y2, 2));
            // manhattan distance
            return Math.abs(x2 - x1) + Math.abs(y2 - y1);
        }

        @Override
        public float getEdgeLength(Integer start, Integer end) {
            return 1;
        }

        @Override
        public void visitNeighbors(Integer node, Integer start, Integer end, Visitor<Integer> visitor) {
            int x1 = node / layer.height;
            int y1 = node % layer.height;

            for (Direction d : Direction.values) {
                int x2 = x1 + d.dx;
                int y2 = y1 + d.dy;

                if (x2 < 0 || y2 < 0 || x2 >= layer.width || y2 >= layer.height)
                    continue;

                int index = x2 * layer.height + y2;

                if (trailStructureCanBePlaced(index, start, end)) {
                    visitor.visit(index);
                }
            }
        }
    };

    public EnumSet<ScienceType> sciences;
    public EnumSet<ScienceType> currentSciences;

    protected EnumMap<ItemType, Integer> resources;
    protected EnumSet<ItemType> seenResources;

    // fbo / saving
    FrameBuffer fbo;
    OrthographicCamera thumbCam;
    boolean saveMap;
    Callback<Void> saveCallback;
    final ByteArrayFileHandle bafh = new ByteArrayFileHandle();
    String saveName;
    long playTime;
    long lastTimerStart;

    FrameBuffer recordFbo;
    OrthographicCamera recordCam;
    long lastScreenshot;

    FrameBuffer screenshotFbo;
    OrthographicCamera screenshotCam;

    public String currentGameName;

    // Active structure for building
    // Is controlled (partly) by BuildRing
    public Structure<?> activeStructure;
    public final IntMap<Structure<?>> activeStructureTrail = new IntMap<>();
    public final List<Integer> activeStructurePath = new LinkedList<>();
    public boolean structureDestroyMode;
    public boolean cableDestroyMode;
    public boolean bulkDestroyMode;
    public boolean bulkCableMode;
    public boolean copyMode;
    public boolean pasteMode;
    public QuarryCameraControl camControl;
    protected TextureRegion caret;
    public final Vector2 endA = new Vector2(-1, 0);
    public final Vector2 endB = new Vector2(-1, 0);
    int activeEnd = 0;

    public IntMap<Pair<Structure<?>, CompoundTag>> copyStructures = new IntMap<>();
    public IntMap<Pair<Structure<?>, CompoundTag>> copyCables = new IntMap<>();
    List<Structure<?>> copyCollisions = new ArrayList<>();
    public int[] copyRegion = new int[4];
    EnumMap<ItemType, Integer> copyCost;

    final TextureRegion endMarker = Quarry.Q.atlas.findRegion("end_marker");
    final TextureRegion endMarkerGlow = Quarry.Q.atlas.findRegion("end_marker_glow");
    final TextureRegion endMarkerA = Quarry.Q.atlas.findRegion("end_marker_a");

    final Vector2 activeTile = new Vector2(-1, 0);

    int hoverTileX, hoverTileY;

    private int gameSpeed = 1;
    private boolean gamePaused = false;

    private boolean startNewGame;
    private boolean infinite;

    private LiningUpState liningUpInput = LiningUpState.NoOther, liningUpOutput = LiningUpState.NoOther;
    private Structure<?> liningUpStructureInput, liningUpStructureOutput;

    final Vector2 tmp = new Vector2();

    // Various data notifiers
    ChangeNotifier<Pair<ItemType, Integer>> resourceChangeNotifier = new ChangeNotifier<>();
    final Pair<ItemType, Integer> resourceChangePair = new Pair<>();
    // called for new layers
    public ChangeNotifier<Layer> layerChangeNotifier = new ChangeNotifier<>();

    // pfx
    public ParticleEffectPool dustPfxU, dustPfxD, dustPfxL, dustPfxR;
    public ParticleEffectPool firePfx;
    public ParticleEffect confettiPfx;

    IntSet tutorialHighlight = new IntSet();

    // sfx
    StructureSoundSpatializer spatializer;
    public QuarrySoundPlayer spatializedPlayer;
    Sound buildSfx, destroySfx, cableSfx;

    AmbientSound heavyAmb, emptyAmb, baseAmb;

    Music music;

    Thread loudnessCalculatorThread;

    long lastAutosave;
    boolean autosaving;

    // smooth camera
    Vector3 cameraVelocity = new Vector3();
    final Vector3 tmp3 = new Vector3();
    float cameraAcc = 400f;
    float cameraMaxSpeed = 600f;
    float cameraZoomAcc = 0;

    @Override
    public void init() {
        G = this;

        music = Quarry.Q.assets.get("music/Fading_into_the_Dream" + Const.MUSIC_FORMAT, Music.class);
        input = new InputMultiplexer(this);

        colorShader = new ShaderProgram(Gdx.files.internal("glsl/color.vs"), Gdx.files.internal("glsl/color.fs"));
        if (!colorShader.isCompiled())
            throw new IllegalArgumentException("Error compiling shader: " + colorShader.getLog());

        batch = new SpriteBatch(8191, colorShader);

        cam = new OrthographicCamera();
        shaper = new ShapeRenderer();
        shaper.setAutoShapeType(true);

        spriter = new DepthSpriter(Quarry.Q.atlas.getTextures().first(), 8191);
        delegate = new BatchDelegate(Quarry.Q.atlas.getTextures().first(), batch);
        pfxBatch = new SpriterDelegateBatch(spriter);

        fboCam = new OrthographicCamera(Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE);
        viewport = new FitViewport(Const.W, Const.H, cam);
        fboCam.position.set(0.5f * Const.CHUNK_FULL_SIZE, 0.5f * Const.CHUNK_FULL_SIZE, fboCam.near);
        fboCam.update();
        caret = Quarry.Q.atlas.findRegion("caret_down");

        currentSciences = EnumSet.noneOf(ScienceType.class);
        sciences = EnumSet.noneOf(ScienceType.class);
        resources = new EnumMap<>(ItemType.class);
        seenResources = EnumSet.noneOf(ItemType.class);
        copyCost = new EnumMap<>(ItemType.class);

        initParticles();

        ui = new GameUi(viewport);

        camControl = new QuarryCameraControl(viewport);
        //        cam.position.set(Const.W / 2, Const.H / 2, 0);
        input.addProcessor(camControl.input);

        chunkFBOs = new FrameBuffer[(Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE)
                * (Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE)];

        spatializer = new StructureSoundSpatializer();
        spatializer.setVerticalRange(8);
        spatializer.setHorizontalRange(8);

        spatializedPlayer = new QuarrySoundPlayer();
        spatializedPlayer.setSpatializer(spatializer);
        spatializedPlayer.setVolume(Quarry.Q.sound.getSoundVolume());
        spatializedPlayer.setFadeTime(.15f);

        buildSfx = Quarry.Q.assets.get("sfx/build" + Const.SFX_FORMAT, Sound.class);
        destroySfx = Quarry.Q.assets.get("sfx/destroy" + Const.SFX_FORMAT, Sound.class);
        cableSfx = Quarry.Q.assets.get("sfx/cable" + Const.SFX_FORMAT, Sound.class);

        emptyAmb = new AmbientSound(Quarry.Q.assets.get("sfx/ambience_empty" + Const.SFX_FORMAT, Sound.class), 0.25f);
        baseAmb = new AmbientSound(Quarry.Q.assets.get("sfx/ambience_base" + Const.SFX_FORMAT, Sound.class), 0.25f);
        heavyAmb = new AmbientSound(Quarry.Q.assets.get("sfx/ambience_heavy" + Const.SFX_FORMAT, Sound.class), 0.25f);

        tilePathfinding = new AStar<>();
        tilePathfinding.setMaxTime(100);

        ///// Pallet item generator
        //        int meta = 2;
        //        for (ItemType i : ItemType.values()) {
        //            if (i == ItemType.Nil || i.categories.contains(ItemCategory.Fluid) || Item.base(i) == ItemType.Pallet || i.name().startsWith("_")) continue;
        //            System.out.format("%sPallet(Pallet, %d, %s, 0),\n", i.name(), meta, i.name());
        //            meta++;
        //        }
    }

    protected void initParticles() {
        ParticleEffect fire = new ParticleEffect();
        fire.load(Gdx.files.internal("pfx/oven_fire.p"), Quarry.Q.atlas);
        firePfx = new ParticleEffectPool(fire, 16, 100);

        confettiPfx = new ParticleEffect();
        confettiPfx.load(Gdx.files.internal("pfx/confetti.p"), Quarry.Q.atlas);
        ParticleEmitter pe = new ParticleEmitter(confettiPfx.getEmitters().first());
        confettiPfx.getEmitters().clear();
        for (int r = 0; r <= 360; r += 45) {
            Color c = r == 360 ? Color.WHITE : ColorUtil.HSVtoRGB(r, 100, 100);
            ParticleEmitter p = new ParticleEmitter(pe);
            p.getTint().setColors(new float[] { c.r, c.g, c.b });
            confettiPfx.getEmitters().add(p);
        }

        ParticleEffect dust = new ParticleEffect();
        dust.load(Gdx.files.internal("pfx/dust.p"), Quarry.Q.atlas);
        dustPfxU = new ParticleEffectPool(dust, 16, 100);

        ParticleEffect dust1 = new ParticleEffect();
        dust1.load(Gdx.files.internal("pfx/dust.p"), Quarry.Q.atlas);
        dust1.getEmitters().get(0).getVelocity().setHigh(-dust1.getEmitters().get(0).getVelocity().getHighMin(),
                -dust1.getEmitters().get(0).getVelocity().getHighMax());
        dustPfxD = new ParticleEffectPool(dust1, 16, 100);

        ParticleEffect dust2 = new ParticleEffect();
        dust2.load(Gdx.files.internal("pfx/dust_h.p"), Quarry.Q.atlas);
        dustPfxL = new ParticleEffectPool(dust2, 16, 100);

        ParticleEffect dust3 = new ParticleEffect();
        dust3.load(Gdx.files.internal("pfx/dust_h.p"), Quarry.Q.atlas);
        dust3.getEmitters().get(0).getVelocity().setHigh(-dust3.getEmitters().get(0).getVelocity().getHighMin(),
                -dust3.getEmitters().get(0).getVelocity().getHighMax());
        dustPfxR = new ParticleEffectPool(dust3, 16, 100);

    }

    public void reset() {
        ui.tileUI.hide();
        ui.tutorial.reset();
        resetActiveTile();
        resetActiveStructure();
        ui.destroyButton.setChecked(false);
        ui.cableRemoveButton.setChecked(false);
        ui.hideBuildMenu(true);
        ui.hideScienceUI(true);
        ui.hideStructureUI();
        ui.hideTooltip();
        ui.hideCopyTable();
        tutorialHighlight.clear();

        powerGrid.clear();

        currentSciences.clear();
        sciences.clear();
        resources.clear();
        getSeenResources().clear();
        try {
            spatializedPlayer.stop();
        } catch (Exception e) {
        }
        heavyAmb.stop();
        emptyAmb.stop();
        baseAmb.stop();
        ui.pauseButton.setChecked(false);

        ui.destroyButton.setDisabled(false);
        ui.destroyButton.setVisible(true);
        ui.copyButton.setVisible(false);

        for (Actor c : ui.buildMenuStars)
            c.remove();
        ui.buildMenuStars.clear();
        ui.buildMenuSciences.clear();
    }

    public void newGame() {
        currentGameName = null;
        setPaused(false);
        reset();

        lastAutosave = System.currentTimeMillis();
        addScience(ScienceType.Start);

        infinite = false;
        Layer l0 = null;

        synchronized (layerLock) {
            if (layers != null) {
                for (Layer l : layers)
                    l.dispose();
                layers.clear();
            } else {
                layers = new Array<>();
            }

            l0 = new Layer(0, Const.DEFAULT_LAYER_SIZE, Const.DEFAULT_LAYER_SIZE, TileType.Stone, true, true);

            Storage st = new Storage(31, 31);
            st.setRefundStorage(true);
            l0.addStructure(st);
            layers.add(l0);

            st.addToInventory(ItemType.Wood, 40, null);
            st.addToInventory(ItemType.Scaffolding, 40, null);
            st.addToInventory(ItemType.Stone, 20, null);

        }

        layer = l0;
        layerIndex = 0;

        playTime = 0;

        if (Quarry.Q.prefs.getBoolean("tutorial", true)) {
            Generator.G.setSeed(5044591726400517120l);
            ui.tutorial.next();
            Quarry.Q.prefs.putBoolean("tutorial", false).flush();
        }

        Generator.G.generate(l0);

        cam.zoom = 1;
        cam.position.set((l0.width + 0.75f) * Const.TILE_SIZE / 2, l0.height * Const.TILE_SIZE / 2, 0);
        //        cam.position.set(4 * Const.TILE_SIZE, 6 * Const.TILE_SIZE, 0);

        layerChangeNotifier.notify(Type.RESET, layer);

        lastTimerStart = System.currentTimeMillis();
    }

    public void startNewGame() {
        startNewGame = true;
    }

    @Override
    public void show() {
        super.show();

        Quarry.Q.sound.playMusic(music, true);

        ui.menu.init();

        Gdx.input.setCatchBackKey(true);

        // initialize fbos
        int chunksH = (int) Math.ceil(Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE);

        for (int i = 0; i < Const.DEFAULT_LAYER_SIZE; i += Const.CHUNK_SIZE) {
            for (int j = 0; j < Const.DEFAULT_LAYER_SIZE; j += Const.CHUNK_SIZE) {
                chunkFBOs[i / Const.CHUNK_SIZE * chunksH + j / Const.CHUNK_SIZE] = new FrameBuffer(Format.RGB565,
                        Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE, false);
            }
        }
    }

    @Override
    public void hide() {
        super.hide();

        for (int i = 0; i < chunkFBOs.length; i++) {
            try {
                // apparently can be run on non-main thread. too lazy to introduce a proper main-thread looper
                chunkFBOs[i].dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
            chunkFBOs[i] = null;
        }
    }

    @Override
    public void update(double deltaTime) {
        if (startNewGame) {
            newGame();
            startNewGame = false;
        }

        if (layers == null)
            return;

        // handle camera control keys

        if (Quarry.Q.desktop && (ui.prompt.getStage() == null || !ui.prompt.isVisible())
                && !ui.menu.menuButton.isChecked()) {
            if (!SMOOTH_CAMERA) {
                if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
                    cam.position.x -= 700 * cam.zoom * deltaTime;
                }
                if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
                    cam.position.x += 700 * cam.zoom * deltaTime;
                }
                if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) {
                    cam.position.y += 700 * cam.zoom * deltaTime;
                }
                if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
                    cam.position.y -= 700 * cam.zoom * deltaTime;
                }
            } else {
                cam.position.add(tmp3.set(cameraVelocity.x, cameraVelocity.y, 0).scl((float) deltaTime));
                camControl.clampCam(cam);
                camControl.clampZoom(cam.zoom + cameraVelocity.z);

                if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
                    cameraVelocity.x = (float) Math.max(-cameraMaxSpeed,
                            cameraVelocity.x - cameraAcc * deltaTime * cam.zoom * (cameraVelocity.y > 0 ? 2 : 1));
                } else if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
                    cameraVelocity.x = (float) Math.min(cameraMaxSpeed,
                            cameraVelocity.x + cameraAcc * deltaTime * cam.zoom * (cameraVelocity.y < 0 ? 2 : 1));
                } else {
                    if (Math.abs(cameraVelocity.x) < 1f) {
                        cameraVelocity.x = 0;
                    } else {
                        cameraVelocity.x += Math.min(cameraAcc * deltaTime * cam.zoom, Math.abs(cameraVelocity.x))
                                * -Math.signum(cameraVelocity.x);
                    }
                }

                if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
                    cameraVelocity.y = (float) Math.max(-cameraMaxSpeed,
                            cameraVelocity.y - cameraAcc * deltaTime * cam.zoom * (cameraVelocity.y > 0 ? 2 : 1));
                } else if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP)) {
                    cameraVelocity.y = (float) Math.min(cameraMaxSpeed,
                            cameraVelocity.y + cameraAcc * deltaTime * cam.zoom * (cameraVelocity.y < 0 ? 2 : 1));
                } else {
                    if (Math.abs(cameraVelocity.y) < 1f) {
                        cameraVelocity.y = 0;
                    } else {
                        cameraVelocity.y += Math.min(cameraAcc * deltaTime * cam.zoom, Math.abs(cameraVelocity.y))
                                * -Math.signum(cameraVelocity.y);
                    }
                }

                if (Math.abs(cameraZoomAcc) < 0.00001f)
                    cameraZoomAcc = 0;
                else
                    cameraZoomAcc += Math.min(Math.abs(cameraZoomAcc), 20 * deltaTime) * -Math.signum(cameraZoomAcc);
                cameraVelocity.z += cameraZoomAcc * deltaTime * 0.001f;
                if (cameraZoomAcc == 0) {
                    cameraVelocity.z += Math.min(Math.abs(cameraVelocity.z), 0.02f * deltaTime)
                            * -Math.signum(cameraVelocity.z);
                }
            }
        }

        camControl.update();
        if (SINGLE_FRAME) {
            powerGrid.update(deltaTime, gameSpeed);

            synchronized (layerLock) {
                for (Layer l : layers) {
                    l.update(deltaTime, gameSpeed);
                    if (l.getIndex() != layerIndex) {
                        l.postUpdate();
                    }
                }
            }
            SINGLE_FRAME = false;
        } else {
            powerGrid.update(deltaTime, gamePaused ? 0 : gameSpeed);

            synchronized (layerLock) {
                for (Layer l : layers) {
                    l.update(deltaTime, gamePaused ? 0 : gameSpeed);
                    if (l.getIndex() != layerIndex) {
                        l.postUpdate();
                    }
                }
            }
        }

        ui.update(deltaTime);

        spatializer.setCenter(cam.position.x, cam.position.y, cam.zoom / 0.5f);
        spatializedPlayer.update((float) deltaTime);

        int tileX = (int) (cam.position.x / Const.TILE_SIZE);
        int tileY = (int) (cam.position.y / Const.TILE_SIZE);
        float loudness = layer.getLoudness(tileX, tileY);

        boolean sfx = Quarry.Q.sound.isPlaySound();

        emptyAmb.setVolume(!sfx ? 0
                : spatializedPlayer.getVolume() *
                        MathUtils.clamp((cam.zoom - camControl.minZoom) / (camControl.maxZoom - camControl.minZoom), 0,
                                1));
        baseAmb.setVolume(spatializedPlayer.getVolume() *
                MathUtils.clamp(
                        cam.zoom > 4 || gamePaused || !sfx ? 0 : Math.min(0.5f, loudness) * (1 - (cam.zoom / 4)), 0,
                        1));
        heavyAmb.setVolume(spatializedPlayer.getVolume() *
                MathUtils.clamp(cam.zoom > 2 || gamePaused || !sfx ? 0
                        : Math.max(0, loudness - 0.6f) * 4f * (1 - (cam.zoom / 2)), 0, 1));

        emptyAmb.update(deltaTime);
        baseAmb.update(deltaTime);
        heavyAmb.update(deltaTime);

        // auto save
        long delta = System.currentTimeMillis() - lastAutosave;
        if (!saveMap && !autosaving && delta >= Const.MIN_AUTOSAVE_INTERVAL /* && longer than config*/) {
            autosaving = true;
            saveMap = true;
        }

        //        System.out.println("FPS: " + (int) (1 / Gdx.graphics.getRawDeltaTime() * 10) / 10f);
        //        System.out.println("B: " + layer.getStructureCount());
        //        System.out.println("I: " + layer.getEntityCount());
    }

    private void takeScreenShot(final boolean record) {
        FrameBuffer fbo = record ? recordFbo : screenshotFbo;

        OrthographicCamera cam = record ? recordCam : screenshotCam;

        final int s = fbo.getWidth();

        fbo.begin();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        layer.draw(cam, fboCam, null, batch, spriter, shaper, pfxBatch);
        batch.end();

        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);

        Pixmap pixmap = null;
        try {
            pixmap = new Pixmap(s, s, Format.RGB888);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        final Pixmap fpix = pixmap;

        ByteBuffer pixels = pixmap.getPixels();
        Gdx.gl.glReadPixels(0, 0, s, s, GL20.GL_RGB, GL20.GL_UNSIGNED_BYTE, pixels);

        Quarry.Q.threadPool.execute(new Runnable() {
            @Override
            public void run() {
                // Flip the pixmap upside down
                ByteBuffer px = fpix.getPixels();
                int numBytes = s * s * 3;
                byte[] lines = new byte[numBytes];
                int numBytesPerLine = s * 3;
                for (int i = 0; i < s; i++) {
                    px.position((s - i - 1) * numBytesPerLine);
                    px.get(lines, i * numBytesPerLine, numBytesPerLine);
                }
                px.clear();
                px.put(lines);
                px.clear();

                try {
                    String filename = "TheQuarry/" + (record ? "record" : "screenshots") + "/"
                            + System.currentTimeMillis() + ".png";
                    PixmapIO.writePNG(Quarry.Q.file(filename, true), fpix);
                } catch (Exception e) {
                    ui.toast.show(Quarry.Q.i18n.get("toast.save_error"));
                }

                fpix.dispose();
            }
        });

        fbo.end();
    }

    @Override
    public void draw() {
        if (layer == null)
            return;

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        if (saveMap) {
            if (fbo == null) {
                if (Quarry.Q.desktop) {
                    fbo = new FrameBuffer(Format.RGB888, 160, 90, false);
                } else {
                    fbo = new FrameBuffer(Format.RGB888, 90, 160, false);
                }

                thumbCam = new OrthographicCamera(Const.W, Const.H);
            }

            thumbCam.position.set(cam.position);
            thumbCam.zoom = cam.zoom;

            thumbCam.update();

            fbo.begin();

            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            batch.setProjectionMatrix(thumbCam.combined);

            batch.begin();
            layer.draw(thumbCam, fboCam, viewport, batch, spriter, shaper, pfxBatch);
            batch.end();

            byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, fbo.getWidth(), fbo.getHeight(), true);
            Pixmap p = new Pixmap(fbo.getWidth(), fbo.getHeight(), Format.RGBA8888);
            p.getPixels().put(pixels).position(0);

            String save = this.saveName;
            if (autosaving)
                save = getAutosaveName(currentGameName);
            updateThumbnailCache(getFileName(save), p);

            PixmapIO.writeCIM(bafh, p);
            p.dispose();

            fbo.end();

            viewport.apply();

            if (autosaving) {
                Quarry.Q.threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        saveData(currentGameName, true, true);
                        lastAutosave = System.currentTimeMillis();
                        autosaving = false;
                    }
                });
            } else {
                Quarry.Q.threadPool.execute(new Runnable() {

                    @Override
                    public void run() {
                        saveData(saveName, false, true);
                    }
                });
            }
            saveMap = false;
        }

        if (RECORDMODE) {

            int s = layer.width * Const.TILE_SIZE;

            if (recordFbo == null) {
                recordFbo = new FrameBuffer(Format.RGB888, s, s, true);
                recordCam = new OrthographicCamera(s, s);
                recordCam.position.set(layer.width * Const.TILE_SIZE * 0.5f, layer.height * Const.TILE_SIZE * 0.5f, 0);
                recordCam.zoom = layer.width * Const.TILE_SIZE / (float) s;
                recordCam.update();
            }

            if (System.currentTimeMillis() - lastScreenshot >= 5000) {
                takeScreenShot(true);

                viewport.apply();
                lastScreenshot = System.currentTimeMillis();
            }
        }

        if (SCREENSHOT) {
            int s = layer.width * Const.TILE_SIZE;

            if (screenshotFbo == null) {
                screenshotFbo = new FrameBuffer(Format.RGB888, s, s, true);
                screenshotCam = new OrthographicCamera(s, s);
                screenshotCam.position.set(layer.width * Const.TILE_SIZE * 0.5f, layer.height * Const.TILE_SIZE * 0.5f,
                        0);
                screenshotCam.zoom = layer.width * Const.TILE_SIZE / (float) s;
                screenshotCam.update();
            }

            takeScreenShot(false);

            viewport.apply();

            SCREENSHOT = false;
        }

        cam.update();

        batch.setProjectionMatrix(cam.combined);

        batch.begin();

        synchronized (layerLock) {
            layer.draw(cam, fboCam, viewport, batch, spriter, shaper, pfxBatch);
        }

        batch.end();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        synchronized (highlightLock) {
            if (tutorialHighlight.size != 0) {
                shaper.setProjectionMatrix(cam.combined);
                shaper.begin(ShapeType.Filled);
                shaper.setColor(0, 1, 0, 0.25f);
                for (IntSetIterator iter = tutorialHighlight.iterator(); iter.hasNext;) {
                    int i = iter.next();
                    shaper.rect((i / layer.height) * Const.TILE_SIZE, (i % layer.height) * Const.TILE_SIZE,
                            Const.TILE_SIZE, Const.TILE_SIZE);
                }
                shaper.end();
            }
        }

        batch.begin();
        Gdx.gl.glEnable(GL20.GL_BLEND);

        if (activeStructure != null && (activeStructure.x > -1 || endA.x > -1)) {
            if (endB.x > -1) {
                for (Structure<?> s : activeStructureTrail.values()) {
                    s.draw(delegate);
                }

                if (camControl.elementPlaceable && ui.canAffordStructure) {
                    batch.setColor(0.1f, 0.8f, 0.1f, 1);
                    shaper.setColor(0.1f, 0.8f, 0.1f, 0.75f);
                } else {
                    batch.setColor(0.8f, 0.1f, 0.1f, 1);
                    shaper.setColor(0.8f, 0.1f, 0.1f, 0.75f);
                }

                batch.draw(endMarkerGlow, (endA.x + 0.5f) * Const.TILE_SIZE - endMarkerA.getRegionWidth() / 2,
                        (endA.y + 0.2f) * Const.TILE_SIZE);

                if (endB.x > -1) {
                    batch.draw(endMarkerGlow, (endB.x + 0.5f) * Const.TILE_SIZE - endMarker.getRegionWidth() / 2,
                            (endB.y + 0.2f) * Const.TILE_SIZE);

                    Gdx.gl.glEnable(GL20.GL_BLEND);
                    batch.end();
                    shaper.setProjectionMatrix(cam.combined);
                    shaper.begin(ShapeType.Filled);
                    Gdx.gl.glEnable(GL20.GL_BLEND);

                    shaper.rectLine((endA.x + 0.5f) * Const.TILE_SIZE, (endA.y + 0.5f) * Const.TILE_SIZE,
                            (endB.x + 0.5f) * Const.TILE_SIZE, (endB.y + 0.5f) * Const.TILE_SIZE, 6);

                    if (camControl.elementPlaceable && ui.canAffordStructure) {
                        shaper.setColor(0.2f, 1, 0.2f, 0.6f);
                    } else {
                        shaper.setColor(1, 0.2f, 0.2f, 0.6f);
                    }

                    shaper.rectLine((endA.x + 0.5f) * Const.TILE_SIZE, (endA.y + 0.5f) * Const.TILE_SIZE,
                            (endB.x + 0.5f) * Const.TILE_SIZE, (endB.y + 0.5f) * Const.TILE_SIZE, 2);

                    //                    shaper.rect((endA.x) * Const.TILE_SIZE,(endA.y+0.4f) * Const.TILE_SIZE,Const.TILE_SIZE,Const.TILE_SIZE + 10 );

                    shaper.end();
                    batch.begin();
                }
                batch.setColor(1, 1, 1, 1);

                float factorA = (activeEnd == 1 ? 1.35f : 1);
                float widthA = endMarkerA.getRegionWidth() * factorA;
                float heightA = endMarkerA.getRegionHeight() * factorA;

                // sprite batch does not support depth, so drawing order is important to fix z-fighting
                if (endA.y > endB.y) {
                    batch.draw(endMarkerA, (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                            (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);

                    if (endB.x > -1) {
                        float factorB = (activeEnd == 2 ? 1.35f : 1);
                        float widthB = endMarker.getRegionWidth() * factorB;
                        float heightB = endMarker.getRegionHeight() * factorB;

                        batch.draw(endMarker,
                                (endB.x + 0.5f) * Const.TILE_SIZE - widthB / 2,
                                (endB.y + 0.2f * 1 / factorB) * Const.TILE_SIZE, widthB, heightB);
                    }
                } else {
                    if (endB.x > -1) {
                        float factorB = (activeEnd == 2 ? 1.35f : 1);
                        float widthB = endMarker.getRegionWidth() * factorB;
                        float heightB = endMarker.getRegionHeight() * factorB;

                        batch.draw(endMarker, (endB.x + 0.5f) * Const.TILE_SIZE - widthB / 2,
                                (endB.y + 0.2f * 1 / factorB) * Const.TILE_SIZE, widthB, heightB);
                    }

                    batch.draw(endMarkerA, (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                            (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);
                }
            } else {
                if (structureDestroyMode || cableDestroyMode) {
                    batch.setColor(1, 0.5f, 0.5f, 1);
                } else {
                    if (camControl.elementPlaceable && ui.canAffordStructure) {
                        batch.setColor(0.75f, 1, 0.5f, 1);
                    } else {
                        batch.setColor(1, 0.5f, 0.5f, 1);
                    }
                }

                activeStructure.draw(delegate);

                if (endA.x > -1) {
                    float factorA = (activeEnd == 1 ? 1.35f : 1);
                    float widthA = endMarkerA.getRegionWidth() * factorA;
                    float heightA = endMarkerA.getRegionHeight() * factorA;
                    if (camControl.elementPlaceable && ui.canAffordStructure) {
                        batch.setColor(0.1f, 0.8f, 0.1f, 1);
                        shaper.setColor(0.1f, 0.8f, 0.1f, 0.75f);
                    } else {
                        batch.setColor(0.8f, 0.1f, 0.1f, 1);
                        shaper.setColor(0.8f, 0.1f, 0.1f, 0.75f);
                    }
                    batch.draw(endMarkerGlow, (endA.x + 0.5f) * Const.TILE_SIZE - endMarkerA.getRegionWidth() / 2,
                            (endA.y + 0.2f) * Const.TILE_SIZE);

                    batch.setColor(1, 1, 1, 1);
                    batch.draw(endMarkerA, (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                            (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);
                }

                batch.setColor(1, 1, 1, 1);
            }

            if (!structureDestroyMode) {
                drawStructureAssists(activeStructure, ui.currentRecipe);
            }
        }

        if ((bulkDestroyMode || bulkCableMode || copyMode) && endA.x > -1) {
            float factorA = (activeEnd == 1 ? 1.35f : 1);
            float widthA = endMarkerA.getRegionWidth() * factorA;
            float heightA = endMarkerA.getRegionHeight() * factorA;

            if (endB.x > -1) {
                if (copyMode) {
                    batch.setColor(0.1f, 0.1f, 0.8f, 0.5f);
                    shaper.setColor(0.1f, 0.1f, 0.8f, 0.3f);
                } else {
                    batch.setColor(0.8f, 0.1f, 0.1f, 0.5f);
                    shaper.setColor(0.8f, 0.1f, 0.1f, 0.3f);
                }

                Gdx.gl.glEnable(GL20.GL_BLEND);
                batch.end();
                shaper.setProjectionMatrix(cam.combined);
                shaper.begin(ShapeType.Filled);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shaper.rect(Math.min(endA.x, endB.x) * Const.TILE_SIZE, Math.min(endA.y, endB.y) * Const.TILE_SIZE,
                        (Math.max(endA.x, endB.x) - Math.min(endA.x, endB.x) + 1) * Const.TILE_SIZE,
                        (Math.max(endA.y, endB.y) - Math.min(endA.y, endB.y) + 1) * Const.TILE_SIZE);
                shaper.end();
                shaper.setColor(1, 1, 1, 1);
                batch.begin();

                batch.draw(endMarkerGlow, (endA.x + 0.5f) * Const.TILE_SIZE - endMarkerA.getRegionWidth() / 2,
                        (endA.y + 0.2f) * Const.TILE_SIZE);
                batch.draw(endMarkerGlow, (endB.x + 0.5f) * Const.TILE_SIZE - endMarker.getRegionWidth() / 2,
                        (endB.y + 0.2f) * Const.TILE_SIZE);

                batch.setColor(1, 1, 1, 1);

                if (endA.y > endB.y) {
                    batch.draw(endMarkerA,
                            (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                            (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);

                    if (endB.x > -1) {
                        float factorB = (activeEnd == 2 ? 1.35f : 1);
                        float widthB = endMarker.getRegionWidth() * factorB;
                        float heightB = endMarker.getRegionHeight() * factorB;

                        batch.draw(endMarker,
                                (endB.x + 0.5f) * Const.TILE_SIZE - widthB / 2,
                                (endB.y + 0.2f * 1 / factorB) * Const.TILE_SIZE, widthB, heightB);
                    }
                } else {
                    if (endB.x > -1) {
                        float factorB = (activeEnd == 2 ? 1.35f : 1);
                        float widthB = endMarker.getRegionWidth() * factorB;
                        float heightB = endMarker.getRegionHeight() * factorB;

                        batch.draw(endMarker,
                                (endB.x + 0.5f) * Const.TILE_SIZE - widthB / 2,
                                (endB.y + 0.2f * 1 / factorB) * Const.TILE_SIZE, widthB, heightB);
                    }

                    batch.draw(endMarkerA,
                            (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                            (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);
                }
            } else {
                if (copyMode) {
                    batch.setColor(0.1f, 0.1f, 0.8f, 0.5f);
                    shaper.setColor(0.1f, 0.1f, 0.8f, 0.3f);
                } else {
                    batch.setColor(0.8f, 0.1f, 0.1f, 0.5f);
                    shaper.setColor(0.8f, 0.1f, 0.1f, 0.3f);
                }

                Gdx.gl.glEnable(GL20.GL_BLEND);
                batch.end();
                shaper.setProjectionMatrix(cam.combined);
                shaper.begin(ShapeType.Filled);
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shaper.rect(endA.x * Const.TILE_SIZE, endA.y * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);
                shaper.end();
                shaper.setColor(1, 1, 1, 1);
                batch.begin();

                batch.draw(endMarkerGlow, (endA.x + 0.5f) * Const.TILE_SIZE - endMarkerA.getRegionWidth() / 2,
                        (endA.y + 0.2f) * Const.TILE_SIZE);
                batch.setColor(1, 1, 1, 1);
                batch.draw(endMarkerA, (endA.x + 0.5f) * Const.TILE_SIZE - widthA / 2,
                        (endA.y + 0.2f * 1 / factorA) * Const.TILE_SIZE, widthA, heightA);
            }
        }

        if (pasteMode) {
            batch.setColor(0.1f, 0.1f, 0.8f, 0.5f);
            shaper.setColor(0.1f, 0.1f, 0.8f, 0.3f);

            Gdx.gl.glEnable(GL20.GL_BLEND);
            batch.end();
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Filled);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.rect(copyRegion[0] * Const.TILE_SIZE, copyRegion[1] * Const.TILE_SIZE,
                    copyRegion[2] * Const.TILE_SIZE, copyRegion[3] * Const.TILE_SIZE);
            shaper.end();
            batch.begin();

            if (camControl.elementPlaceable && ui.canAffordStructure) {
                batch.setColor(0.75f, 1, 0.5f, 1);
                shaper.setColor(0.1f, 0.8f, 0.1f, 0.4f);

                for (Pair<Structure<?>, CompoundTag> s : copyStructures.values()) {
                    s.getKey().draw(delegate);
                }
                for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                    s.getKey().draw(delegate);
                }
            } else if (!ui.canAffordStructure) {
                batch.setColor(1, 0.5f, 0.5f, 1);
                shaper.setColor(0.8f, 0.1f, 0.1f, 0.4f);

                for (Pair<Structure<?>, CompoundTag> s : copyStructures.values()) {
                    s.getKey().draw(delegate);
                }
                for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                    s.getKey().draw(delegate);
                }
            } else {
                for (Pair<Structure<?>, CompoundTag> s : copyStructures.values()) {
                    if (copyCollisions.contains(s.getKey())) {
                        batch.setColor(1, 0.5f, 0.5f, 1);
                        shaper.setColor(0.8f, 0.1f, 0.1f, 0.4f);
                    } else {
                        batch.setColor(0.75f, 1, 0.5f, 1);
                        shaper.setColor(0.1f, 0.8f, 0.1f, 0.4f);
                    }
                    s.getKey().draw(delegate);
                }
                for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                    if (copyCollisions.contains(s.getKey())) {
                        batch.setColor(1, 0.5f, 0.5f, 1);
                        shaper.setColor(0.8f, 0.1f, 0.1f, 0.4f);
                    } else {
                        batch.setColor(0.75f, 1, 0.5f, 1);
                        shaper.setColor(0.1f, 0.8f, 0.1f, 0.4f);
                    }
                    s.getKey().draw(delegate);
                }
            }

            shaper.setColor(1, 1, 1, 1);
            batch.setColor(1, 1, 1, 1);
            batch.end();

            // draw grid
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Line);
            shaper.setColor(0, 0, 0, 0.25f);
            for (int i = 1; i < layer.width /* abusing width = height*/; i++) {
                shaper.line(i * Const.TILE_SIZE, 0 * Const.TILE_SIZE, i * Const.TILE_SIZE,
                        layer.height * Const.TILE_SIZE);
                shaper.line(0 * Const.TILE_SIZE, i * Const.TILE_SIZE, layer.width * Const.TILE_SIZE,
                        i * Const.TILE_SIZE);
            }

            shaper.end();
            batch.begin();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (ui.currentClickedStructure != null
                && ((ui.structureUIRecipes.isChecked() && !(ui.currentClickedStructure instanceof ShaftDrillHead)
                        || (!ui.structureUIRecipes.isVisible()
                                && !(ui.currentClickedStructure instanceof ProducerStructure))))
                && ui.currentClickedStructure.layer == layer) {
            drawStructureAssists(ui.currentClickedStructure, ui.currentRecipe);
        }

        batch.end();

        if (Gdx.app.getType() == ApplicationType.Desktop && hoverTileX > -1 && hoverTileX < layer.width
                && hoverTileY > -1 && hoverTileY < layer.height) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Line);
            shaper.setColor(0, 0, 0, 0.25f);

            shaper.rect(hoverTileX * Const.TILE_SIZE, hoverTileY * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);

            shaper.end();
        }

        if (activeStructure != null && (activeStructure.x > -1 || endA.x > -1)) {
            if (activeStructure.x > -1 && endA.x == -1) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                batch.begin();
                shaper.setProjectionMatrix(cam.combined);
                shaper.begin(ShapeType.Filled);
                activeStructure.drawFrame(delegate, shaper, null);
                shaper.end();
                batch.end();
            }

            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Line);
            shaper.setColor(0, 0, 0, 0.25f);

            for (int i = 1; i < layer.width /* abusing width = height*/; i++) {
                shaper.line(i * Const.TILE_SIZE, 0 * Const.TILE_SIZE, i * Const.TILE_SIZE,
                        layer.height * Const.TILE_SIZE);
                shaper.line(0 * Const.TILE_SIZE, i * Const.TILE_SIZE, layer.width * Const.TILE_SIZE,
                        i * Const.TILE_SIZE);
            }

            shaper.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        } else if (activeTile.x > -1) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Filled);
            shaper.setColor(Structure.selectionColor);
            // lu -> ru
            shaper.line(activeTile.x * Const.TILE_SIZE, activeTile.y * Const.TILE_SIZE,
                    (activeTile.x + 1) * Const.TILE_SIZE, activeTile.y * Const.TILE_SIZE);
            // lo -> ro
            shaper.line(activeTile.x * Const.TILE_SIZE, (activeTile.y + 1) * Const.TILE_SIZE,
                    (activeTile.x + 1) * Const.TILE_SIZE, (activeTile.y + 1) * Const.TILE_SIZE);

            // lu -> lo
            shaper.line(activeTile.x * Const.TILE_SIZE, activeTile.y * Const.TILE_SIZE, activeTile.x * Const.TILE_SIZE,
                    (activeTile.y + 1) * Const.TILE_SIZE);
            // ru -> ro
            shaper.line((activeTile.x + 1) * Const.TILE_SIZE, activeTile.y * Const.TILE_SIZE,
                    (activeTile.x + 1) * Const.TILE_SIZE, (activeTile.y + 1) * Const.TILE_SIZE);

            shaper.setColor(Structure.selectionColor.r, Structure.selectionColor.g, Structure.selectionColor.b, 0.25f);
            shaper.rect(activeTile.x * Const.TILE_SIZE, activeTile.y * Const.TILE_SIZE, Const.TILE_SIZE,
                    Const.TILE_SIZE);

            shaper.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (ui.tutorial.getStepNum() == 1) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Line);
            shaper.setColor(0, 0, 0, 0.25f);

            int len = 20;
            for (int i = 1; i < len; i++) {
                int y = (layer.width / 2 - len / 2 + i);
                if (y < 1 || y > layer.height - 1)
                    continue;

                shaper.line(Math.max(0, (layer.width / 2 - len / 2)) * Const.TILE_SIZE, y * Const.TILE_SIZE,
                        Math.min(layer.width, (layer.width / 2 + len / 2)) * Const.TILE_SIZE, y * Const.TILE_SIZE);
            }
            for (int i = 1; i < len; i++) {
                int x = Math.max(0, (layer.width / 2 - len / 2 + i));
                if (x < 1 || x > layer.width - 1)
                    continue;

                shaper.line(x * Const.TILE_SIZE, Math.max(0, (layer.width / 2 - len / 2)) * Const.TILE_SIZE,
                        x * Const.TILE_SIZE, Math.min(layer.height, (layer.width / 2 + len / 2)) * Const.TILE_SIZE);
            }

            shaper.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        if (ui.currentClickedStructure instanceof Substation) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shaper.begin(ShapeType.Filled);
            for (Edge e : ui.currentClickedStructure.getPowerNetwork().getMinimumSpanningTree()) {
                if (e.getA().layer != layer || e.getB().layer != G.layer)
                    continue;
                if (e.getNetworkStrength() == NetworkStrength.PowerPole) {
                    shaper.setColor(1, 0, 1, 1);
                } else {
                    shaper.setColor(1, 1, 0, 1);
                }

                shaper.rect(
                        (e.getA().getX() + e.getA().getWidth() / 2.0f) * Const.TILE_SIZE - 10,
                        (e.getA().getY() + e.getA().getHeight() / 2.0f) * Const.TILE_SIZE - 10, 20, 20);
                shaper.rect(
                        (e.getB().getX() + e.getB().getWidth() / 2.0f) * Const.TILE_SIZE - 10,
                        (e.getB().getY() + e.getB().getHeight() / 2.0f) * Const.TILE_SIZE - 10, 20, 20);
                shaper.rectLine(
                        (e.getA().getX() + e.getA().getWidth() / 2.0f) * Const.TILE_SIZE,
                        (e.getA().getY() + e.getA().getHeight() / 2.0f) * Const.TILE_SIZE,
                        (e.getB().getX() + e.getB().getWidth() / 2.0f) * Const.TILE_SIZE,
                        (e.getB().getY() + e.getB().getHeight() / 2.0f) * Const.TILE_SIZE, 4);
            }
            shaper.end();
        }

        if (DRAW_DEBUG) {
            shaper.setProjectionMatrix(cam.combined);
            shaper.begin(ShapeType.Line);
            shaper.setColor(Color.GRAY);
            for (int i = 0; i < layer.width; i++) {
                for (int j = 0; j < layer.height; j++) {
                    shaper.line(i * Const.TILE_SIZE, j * Const.TILE_SIZE, (i + 1) * Const.TILE_SIZE,
                            j * Const.TILE_SIZE);
                    shaper.line(i * Const.TILE_SIZE, j * Const.TILE_SIZE, i * Const.TILE_SIZE,
                            (j + 1) * Const.TILE_SIZE);
                }
            }

            shaper.end();

            Gdx.gl.glEnable(GL20.GL_BLEND);

            shaper.setColor(Color.GREEN);
            shaper.begin(ShapeType.Filled);
            for (int i = 0; i < layer.width; i += Const.CHUNK_SIZE) {
                for (int j = 0; j < layer.height; j += Const.CHUNK_SIZE) {
                    if (j > 0)
                        shaper.rectLine(i * Const.TILE_SIZE, j * Const.TILE_SIZE,
                                (i + Const.CHUNK_SIZE) * Const.TILE_SIZE, j * Const.TILE_SIZE, 10);
                    if (i > 0)
                        shaper.rectLine(i * Const.TILE_SIZE, j * Const.TILE_SIZE, i * Const.TILE_SIZE,
                                (j + Const.CHUNK_SIZE) * Const.TILE_SIZE, 10);
                }
            }
            //                        shaper.rect(i * Const.TILE_SIZE, j * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);
            for (int i = 0; i < layer.width; i++) {
                for (int j = 0; j < layer.height; j++) {
                    //                    shaper.setColor(1, 0, 1, 0.2f);
                    //                    if (layer.isItemNotification(i, j))
                    //                        shaper.rect(i * Const.TILE_SIZE, j * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);

                    shaper.setColor(1, 0, 0, 1);
                    byte flag = layer.getFlags(i, j);
                    if ((flag & Layer.FLAG_STRUCTURE_COLLISION) != 0) {
                        shaper.rect(i * Const.TILE_SIZE, j * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2);
                    }
                    shaper.setColor(0, 0, 1, 1);
                    if ((flag & Layer.FLAG_TUBE_COLLISION) != 0) {
                        shaper.rect((i + 0.5f) * Const.TILE_SIZE, j * Const.TILE_SIZE, Const.TILE_SIZE / 2,
                                Const.TILE_SIZE / 2);
                    }
                    shaper.setColor(0, 1, 1, 1);
                    if ((flag & Layer.FLAG_POWER_DOCK_COLLISION) != 0) {
                        shaper.rect((i + 0.25f) * Const.TILE_SIZE, (j + 0.25f) * Const.TILE_SIZE, Const.TILE_SIZE / 2,
                                Const.TILE_SIZE / 2);
                    }
                    shaper.setColor(0, 1, 0, 1);
                    if ((flag & Layer.FLAG_ITEM_DOCK_COLLISION) != 0) {
                        shaper.rect(i * Const.TILE_SIZE, (j + 0.5f) * Const.TILE_SIZE, Const.TILE_SIZE / 2,
                                Const.TILE_SIZE / 2);
                    }
                    shaper.setColor(1, 1, 1, 1);
                    if ((flag & Layer.FLAG_FLUID_DOCK_COLLISION) != 0) {
                        shaper.rect((i + 0.5f) * Const.TILE_SIZE, (j + 0.5f) * Const.TILE_SIZE, Const.TILE_SIZE / 2,
                                Const.TILE_SIZE / 2);
                    }
                }
            }

            List<Integer> open = tilePathfinding.getOpenList();
            for (int i : open) {
                shaper.setColor(0, 0, 1, 0.3f);
                int x = i / layer.height;
                int y = i % layer.height;
                shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);
            }
            List<Integer> closed = tilePathfinding.getClosedList();
            shaper.setColor(0, 1, 0, 0.3f);
            for (int i : closed) {
                shaper.setColor(1, 0, 0, 0.3f);
                int x = i / layer.height;
                int y = i % layer.height;
                shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);
            }

            for (PowerNetwork n : powerGrid.getNetworks()) {
                for (Edge e : n.getMinimumSpanningTree()) {
                    if (e.getA().layer != layer || e.getB().layer != layer)
                        continue;
                    if (e.getNetworkStrength() == NetworkStrength.PowerPole) {
                        shaper.setColor(1, 0, 1, 1);
                    } else {
                        shaper.setColor(1, 1, 0, 1);
                    }

                    int off = Const.TILE_SIZE / 2;
                    shaper.circle(e.getA().getX() * Const.TILE_SIZE + off, e.getA().getY() * Const.TILE_SIZE + off, 12);
                    shaper.circle(e.getB().getX() * Const.TILE_SIZE + off, e.getB().getY() * Const.TILE_SIZE + off, 12);
                    shaper.rectLine(e.getA().getX() * Const.TILE_SIZE + off, e.getA().getY() * Const.TILE_SIZE + off,
                            e.getB().getX() * Const.TILE_SIZE + off, e.getB().getY() * Const.TILE_SIZE + off, 4);
                }

            }

            shaper.end();

            /*shaper.setColor(0, 0, 1, 0.2f);
            shaper.rect(layer.lastBounds.getX() * Const.TILE_SIZE, layer.lastBounds.getY() * Const.TILE_SIZE,
                layer.lastBounds.getWidth() * Const.TILE_SIZE, layer.lastBounds.getHeight() * Const.TILE_SIZE);*/

        }
        /*
        shaper.setProjectionMatrix(cam.combined);
        shaper.begin(ShapeType.Filled);
        
        float[] loudness = layer.getLoudness();
        
        if (loudness != null) {
        Gdx.gl.glEnable(GL20.GL_BLEND);
        
        //        float max = 0;
        //        for (float i : l) {
        //            max = Math.max(max, i);
        //        }
        //            System.out.println(max);
        
        for (int i = 0; i < loudness.length; i++) {
        shaper.setColor(loudness[i], 0, 0, 0.6f);
        shaper.rect((i / (layer.height)) * Const.TILE_SIZE, (i % (layer.height)) * Const.TILE_SIZE,
            Const.TILE_SIZE, Const.TILE_SIZE);
        }
        }
        shaper.end();*/

        ui.draw();

        if (deltaLayer != 0) {
            synchronized (layerLock) {
                int l = layerIndex + deltaLayer;
                if (l >= 0 && l < layers.size) {
                    layerIndex += deltaLayer;
                    layer = layers.get(layerIndex);
                    for (Chunk c : layer.getChunks())
                        c.dirty = true;

                    if (!layer.pendingBounds.isEmpty()) {
                        layer.dirtyBounds.add(layer.pendingBounds);
                        layer.pendingBounds.clear();
                    }

                    if (activeStructure != null) {
                        camControl.updateTrail();
                        camControl.updateActiveElementPlaceable();
                    }
                    layerChangeNotifier.notify(Type.CHANGE, layer);
                }

                deltaLayer = 0;
            }
        }

        synchronized (renderThreadLock) {
            if (renderThreadTasks.size > 0)
                renderThreadTasks.removeIndex(0).run();
        }
    }

    public void drawStructureAssists(Structure<?> structure, Recipe activeRecipe) {
        Dock[] docks = structure.getDocks();
        if (structure instanceof ProducerStructure && activeRecipe != null) {
            Recipe r = activeRecipe;
            Items in = r.getInput();
            Items out = r.getOutput();

            int i = 0, o = 0;
            for (Dock d : docks) {
                if (d.type == DockType.Power || d.type == DockType.BigPower)
                    continue;

                Amount a = null;
                if (in != null && (d.type == DockType.ItemIn || d.type == DockType.FluidIn) && in.entries.length > i) {
                    a = in.entries[i++];
                } else if (out != null && (d.type == DockType.ItemOut || d.type == DockType.FluidOut)
                        && out.entries.length > o) {
                    a = out.entries[o++];
                }
                if (a != null) {
                    batch.draw(a.getCat() != null ? a.getCat().icon : a.getItem().icon,
                            (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                            (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24,
                            24);

                    batch.draw(caret, (structure.x + d.x + d.dir.dx) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                            (structure.y + d.y + d.dir.dy) * Const.TILE_SIZE + (Const.TILE_SIZE - 10) / 2f, 12, 5f, 24,
                            10,
                            1, 1, -90 * (d.dir.ordinal()
                                    + (d.type == DockType.FluidOut || d.type == DockType.ItemOut ? 2 : 0)));
                }
            }

            // draw in-/outputs for other sub turbines
            if (structure instanceof GasTurbine) {
                GasTurbine t = (GasTurbine) structure;
                if (t.isLevel0()) {
                    Direction dir = t.getForwardDirection();
                    for (int j = 1; j < t.getLevel(); j++) {
                        // input lubricant
                        Dock d = t.getDocks()[1];

                        batch.draw(ItemType.Lubricant.icon,
                                (structure.x + d.x + d.dir.dx * 1.5f + j * structure.getWidth() * dir.dx)
                                        * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                (structure.y + d.y + d.dir.dy * 1.5f + j * structure.getHeight() * dir.dy)
                                        * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                24, 24);

                        batch.draw(caret,
                                (structure.x + d.x + d.dir.dx + j * structure.getWidth() * dir.dx) * Const.TILE_SIZE
                                        + (Const.TILE_SIZE - 24) / 2f,
                                (structure.y + d.y + d.dir.dy + j * structure.getHeight() * dir.dy) * Const.TILE_SIZE
                                        + (Const.TILE_SIZE - 10) / 2f,
                                12, 5f, 24, 10,
                                1, 1, -90 * (d.dir.ordinal()
                                        + (d.type == DockType.FluidOut || d.type == DockType.ItemOut ? 2 : 0)));

                        if (r.getOutput() != null) {
                            // input water
                            d = t.getDocks()[2];

                            batch.draw(ItemType.Water.icon,
                                    (structure.x + d.x + d.dir.dx * 1.5f + j * structure.getWidth() * dir.dx)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                    (structure.y + d.y + d.dir.dy * 1.5f + j * structure.getHeight() * dir.dy)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                    24, 24);

                            batch.draw(caret,
                                    (structure.x + d.x + d.dir.dx + j * structure.getWidth() * dir.dx) * Const.TILE_SIZE
                                            + (Const.TILE_SIZE - 24) / 2f,
                                    (structure.y + d.y + d.dir.dy + j * structure.getHeight() * dir.dy)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 10) / 2f,
                                    12, 5f, 24, 10,
                                    1, 1, -90 * (d.dir.ordinal()
                                            + (d.type == DockType.FluidOut || d.type == DockType.ItemOut ? 2 : 0)));

                            //output steam

                            d = t.getDocks()[4];

                            batch.draw(ItemType.PressurizedSteam.icon,
                                    (structure.x + d.x + d.dir.dx * 1.5f + j * structure.getWidth() * dir.dx)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                    (structure.y + d.y + d.dir.dy * 1.5f + j * structure.getHeight() * dir.dy)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                                    24, 24);

                            batch.draw(caret,
                                    (structure.x + d.x + d.dir.dx + j * structure.getWidth() * dir.dx) * Const.TILE_SIZE
                                            + (Const.TILE_SIZE - 24) / 2f,
                                    (structure.y + d.y + d.dir.dy + j * structure.getHeight() * dir.dy)
                                            * Const.TILE_SIZE + (Const.TILE_SIZE - 10) / 2f,
                                    12, 5f, 24, 10,
                                    1, 1, -90 * (d.dir.ordinal()
                                            + (d.type == DockType.FluidOut || d.type == DockType.ItemOut ? 2 : 0)));
                        }
                    }
                }
            }
        } else {
            if (structure instanceof Boiler) {
                Dock d = structure.getDocks()[0];
                batch.draw(ItemType.Steam.icon,
                        (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                        (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24, 24);
                d = structure.getDocks()[1];
                batch.draw(ItemType.Water.icon,
                        (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                        (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24, 24);
                d = structure.getDocks()[2];
                batch.draw(ItemCategory.CoalFuel.icon,
                        (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                        (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24, 24);
            } else if (structure instanceof Refinery) {
                Dock d = structure.getDocks()[0];
                batch.draw(ItemType.RefinedOil.icon,
                        (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                        (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24, 24);
                d = structure.getDocks()[1];
                batch.draw(ItemType.CrudeOil.icon,
                        (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f,
                        (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f, 24, 24);

                for (int i = 2; i < 10; i++) {
                    d = structure.getDocks()[i];
                    batch.draw(((TextureRegionDrawable) DistillationColumn.classSchema.icon).getRegion(),
                            (structure.x + d.x + d.dir.dx * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 40) / 2f,
                            (structure.y + d.y + d.dir.dy * 1.5f) * Const.TILE_SIZE + (Const.TILE_SIZE - 40) / 2f, 40,
                            40);
                }
            }
        }

        // draw carets

        for (Dock d : docks) {
            if (d.type == DockType.Power || d.type == DockType.BigPower)
                continue;

            float x = (structure.x + d.x + d.dir.dx) * Const.TILE_SIZE + (Const.TILE_SIZE - 24) / 2f;
            float y = (structure.y + d.y + d.dir.dy) * Const.TILE_SIZE + (Const.TILE_SIZE - 10) / 2f;

            float rot = -90 * (d.dir.ordinal()
                    + (d.type == DockType.FluidOut || d.type == DockType.ItemOut || structure instanceof Filter ? 2
                            : 0));

            if (activeStructure != null) {
                if (d.type == DockType.StackIn) {
                    x -= d.dir.dx * (15 + Const.TILE_SIZE * 1.5f);
                    y -= d.dir.dy * (15 + Const.TILE_SIZE * 1.5f);
                    batch.setColor(1, 1, 1, 0.8f);
                    if (liningUpInput != LiningUpState.NoOther) {
                        x += d.dir.dx * Math.sin(Gdx.graphics.getFrameId() / 15f) * 4;
                        y += d.dir.dy * Math.sin(Gdx.graphics.getFrameId() / 15f) * 4;
                    }
                    if (liningUpInput == LiningUpState.NotLiningUp) {
                        batch.setColor(1, 0, 0, 0.8f);
                    }
                } else if (d.type == DockType.StackOut) {
                    rot += 180;
                    x -= d.dir.dx * (15 + Const.TILE_SIZE * 1.5f);
                    y -= d.dir.dy * (15 + Const.TILE_SIZE * 1.5f);
                    batch.setColor(1, 1, 1, 0.8f);
                    if (liningUpOutput != LiningUpState.NoOther) {
                        x += d.dir.dx * Math.sin(Gdx.graphics.getFrameId() / 15f) * 4;
                        y += d.dir.dy * Math.sin(Gdx.graphics.getFrameId() / 15f) * 4;
                    }
                    if (liningUpOutput == LiningUpState.NotLiningUp) {
                        batch.setColor(1, 0, 0, 0.8f);
                    }
                }
            } else {
                if (d.type == DockType.StackIn || d.type == DockType.StackOut)
                    continue;
            }

            batch.draw(caret, x, y, 12, 5f, 24, 10, 1, 1, rot);
            batch.setColor(Color.WHITE);
        }
    }

    public boolean trailStructureCanBePlaced(int i, int start, int target) {
        int x = i / layer.height;
        int y = i % layer.height;

        if (x < 0 || y < 0 || x >= layer.width || y >= layer.height) {
            return false;
        }

        if (layer.isInFogOfWar(x, y))
            return false;

        boolean end = i == start || i == target;
        if (activeStructure instanceof CopperCable) {
            if ((layer.getCable(x, y) != null && !end) || layer.isPowerDockCollision(x, y)) {
                return false;
            }
        } else if (!end) {
            if (layer.isCollision(x, y, !(activeStructure instanceof Conveyor),
                    !(activeStructure instanceof FluidTubeStructure))) {
                return false;
            }
        } else {
            Structure<?> trailS = activeStructureTrail.get(i);
            Structure<?> s = layer.getStructure(i / layer.height, i % layer.height);
            if (trailS != null && s != null) {
                if (s.getSchema().type != trailS.getSchema().type) {
                    return false;
                } else {
                    activeStructureTrail.remove(i);
                    activeStructurePath.remove((Integer) i);
                }
            }
        }

        return true;
    }

    //////////////////////////////////////////////////////

    public synchronized void addResources(ItemType item, int amount, boolean addToAStorage) {
        if (amount <= 0)
            return;

        synchronized (resourceLock) {
            int remaining = amount;

            if (addToAStorage) {
                synchronized (layerLock) {
                    o: for (Layer layer : layerIter) {
                        for (StorageStructure b : layer.storages) {
                            if (!b.isRefundStorage())
                                continue;

                            // skip empty barrels
                            if (b.getSchema().type == StructureType.Barrel && b.isEmpty())
                                continue;

                            remaining = b.addToInventoryWithRest(item, remaining);
                            if (remaining == 0)
                                break o;
                        }
                    }

                    if (remaining > 0) {
                        ui.toast.show(Quarry.Q.i18n.get("toast.refund_full"));
                        // Delete items forever
                        amount -= remaining;
                    }

                }
            }

            Integer val = resources.get(item);
            if (val == null)
                val = 0;
            val += amount;
            resources.put(item, val);
            resourceChangeNotifier.notify(Type.ADD, resourceChangePair.set(item, amount));
            getSeenResources().add(item);

            ui.updateResources(true);
            if (activeStructure != null)
                camControl.updateActiveElementPlaceable();
        }
    }

    public synchronized boolean removeResources(ItemType item, int amount, boolean removeFromAStorage) {
        if (amount <= 0)
            return true;

        synchronized (resourceLock) {
            Integer val = resources.get(item);
            if (val == null)
                return false;
            val -= amount;
            if (val < 0)
                val = 0;
            resources.put(item, val);
            resourceChangeNotifier.notify(Type.REMOVE, resourceChangePair.set(item, amount));
            getSeenResources().add(item);

            int remaining = amount;

            if (removeFromAStorage) {
                synchronized (layerLock) {
                    for (Layer layer : layers) {
                        for (Chunk c : layer.getChunks()) {
                            if (c != null && c.isInit()) {
                                for (Structure<?> s : c.getStructures()) {
                                    if (s instanceof StorageStructure) {
                                        remaining = ((StorageStructure) s).removeFromInventoryWithRest(item, remaining);
                                        if (remaining == 0)
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ui.updateResources(true);
            if (activeStructure != null)
                camControl.updateActiveElementPlaceable();

            if (remaining > 0) {
                return false;
            }

            return true;
        }
    }

    public synchronized int getResource(ItemType item) {
        synchronized (resourceLock) {
            Integer val = resources.get(item);
            if (val == null)
                return 0;
            return val;
        }
    }

    public synchronized void addSeenResource(ItemType item) {
        synchronized (resourceLock) {
            if (getSeenResources().add(item))
                ui.updateResources(true);
        }
    }

    public synchronized boolean hasSeenResource(ItemType item) {
        synchronized (resourceLock) {
            return getSeenResources().contains(item);
        }
    }

    public EnumSet<ItemType> getSeenResources() {
        return seenResources;
    }

    public synchronized Set<Map.Entry<ItemType, Integer>> getAllResources() {
        return resources.entrySet();
    }

    public void recalcResources() {

        // recalc resources
        EnumMap<ItemType, Integer> res = new EnumMap<>(ItemType.class);
        synchronized (layerLock) {
            for (Layer l : layers) {
                for (Chunk c : l.getChunks()) {
                    if (c != null) {
                        for (Structure<?> s : c.getStructures()) {
                            if (s instanceof Storage) {
                                CInventory ci = (CInventory) s.getComponents()[0];
                                for (EnumMap.Entry<ItemType, Integer> e : ci.getAll()) {
                                    Integer v = res.get(e.getKey());
                                    v = v == null ? 0 : v;
                                    res.put(e.getKey(), v + e.getValue());
                                }
                            }
                            if (s instanceof Barrel) {
                                CSingleInventory ci = (CSingleInventory) s.getComponents()[0];

                                if (ci.getItem() != null) {
                                    Integer v = res.get(ci.getItem());
                                    v = v == null ? 0 : v;
                                    res.put(ci.getItem(), v + ci.getCount());
                                }
                            }
                        }
                    }
                }
            }
        }
        synchronized (resourceLock) {
            resources.clear();
            resources.putAll(res);
            seenResources.addAll(res.keySet());
            ui.updateResources(true);
        }
    }

    //////////////////////////////////////////////////////

    public synchronized boolean hasSciences(Collection<ScienceType> sciencesRequired) {
        if (GOD_MODE)
            return true;
        return sciences.containsAll(sciencesRequired);
    }

    public synchronized boolean hasSciences(ScienceType... sciencesRequired) {
        if (GOD_MODE)
            return true;
        for (ScienceType s : sciencesRequired)
            if (!sciences.contains(s))
                return false;
        return true;
    }

    public synchronized boolean hasScience(ScienceType science) {
        return sciences.contains(science) || GOD_MODE;
    }

    public synchronized boolean hasCurrentScience(ScienceType science) {
        return currentSciences.contains(science);
    }

    public synchronized void addScience(ScienceType science) {
        currentSciences.remove(science);
        sciences.add(science);
        ui.onScienceChange();

    }

    public synchronized void removeCurrentScience(ScienceType science) {
        currentSciences.remove(science);
    }

    public synchronized void addCurrentScience(ScienceType science) {
        currentSciences.add(science);
    }

    //////////////////////////////////////////////////////

    public void save(Callback<Void> callback) {
        save(currentGameName, true, callback);
    }

    public String getAutosaveName(String saveName) {
        if (saveName == null || saveName.length() == 0) {
            return "Autosave";
        } else if (saveName.endsWith(" Autosave")) {
            return saveName;
        } else {
            return saveName + " Autosave";
        }
    }

    public boolean saveExists(String name) {
        return Quarry.Q.file("TheQuarry/saves/" + getFileName(name) + ".qsf", false).exists();
    }

    public void save(String name, boolean thumbnail, final Callback<Void> callback) {
        this.saveName = name;
        if (thumbnail) {
            saveMap = true;
            saveCallback = callback;
        } else {
            Quarry.Q.threadPool.execute(new Runnable() {
                @Override
                public void run() {
                    saveData(saveName, false, false);
                    if (callback != null)
                        callback.call(null);
                }
            });
        }
    }

    public static String getFileName(String saveName) {
        if (saveName == null) {
            saveName = Quarry.Q.i18n.get("ui.unnamed_save");
        }

        return fileRegex.matcher(saveName).replaceAll("_").toLowerCase();
    }

    protected void saveData(String save, boolean isAutosave, boolean saveMap) {
        Delta.r();
        if (save == null && isAutosave) {
            save = Quarry.Q.i18n.get("ui.unnamed_save");
        }

        try {
            // Write meta
            Builder metaBuilder = new Builder("Meta");

            playTime = getPlayTime();
            lastTimerStart = System.currentTimeMillis();

            metaBuilder
                    .Byte("version", Const.QSF_VERSION)
                    .Int("build", Quarry.Q.versionNumber)
                    .Byte("full", (byte) (Quarry.Q.fullVersion ? 1 : 0))
                    .Long("playTime", playTime)
                    .Long("seed", Generator.G.getSeed())
                    .String("name", save);

            if (saveMap)
                metaBuilder.ByteArray("thumbnail", bafh.getBytes());

            CompoundTag meta = metaBuilder.Get();

            String saveName = save;
            if (isAutosave)
                saveName = getAutosaveName(save);
            String file = getFileName(saveName);

            // backup old file
            FileHandle fh = Quarry.Q.file("TheQuarry/saves/" + file + ".qmf", true);
            if (fh.exists()) {
                fh.copyTo(Quarry.Q.file("TheQuarry/saves/" + file + "-old.qmf", true));
            }

            NBT.write(fh.write(false), meta, CompressionType.Fast);
            meta.free();

            // Write data
            Builder builder = new Builder("Save");
            builder
                    .Byte("version", Const.QSF_VERSION)
                    .Int("build", Quarry.Q.versionNumber)
                    .Byte("full", (byte) (Quarry.Q.fullVersion ? 1 : 0))
                    .Long("playTime", playTime)
                    .String("name", save)

                    .Short("layer", (short) layerIndex)
                    .Byte("infinite", (byte) (infinite ? 1 : 0))
                    .Long("seed", Generator.G.getSeed())
                    .LongArray("rng", Generator.G.getState())
                    .List("Map", TagType.Compound);

            synchronized (layerLock) {
                for (Layer l : layers)
                    l.save(builder);
            }

            builder
                    .End()
                    .Compound("Resources");
            synchronized (resourceLock) {
                Array<Short> seen = new Array<>();

                for (ItemType seenResource : getSeenResources())
                    seen.add(seenResource.value);

                // god dammit
                short[] s = new short[seen.size];
                int i = 0;
                for (Short q : seen)
                    s[i++] = q;

                Util.NBTwriteInventory(builder, resources);
                builder.ShortArray("Seen", s);
            }
            builder.End();

            if (sciences.size() > 0) {
                byte[] science = new byte[sciences.size()];
                int i = 0;
                for (ScienceType s : sciences)
                    science[i++] = s.id;
                builder.ByteArray("Sciences", science);
            }

            builder.Compound("camera")
                    .Float("x", cam.position.x)
                    .Float("y", cam.position.y)
                    .Float("z", cam.position.z)
                    .Float("zoom", cam.zoom)
                    .End();

            CompoundTag data = builder.Get();

            FileHandle fh1 = Quarry.Q.file("TheQuarry/saves/" + file + ".qsf", true);
            if (fh1.exists()) {
                fh1.copyTo(Quarry.Q.file("TheQuarry/saves/" + file + "-old.qsf", true));
            }

            NBT.write(fh1.write(false), data, CompressionType.Fast);

            //            // This is debug only and its super slow
            if (Quarry.Q.desktop && Quarry.Q.version.equals("debug"))
                Quarry.Q.file("TheQuarry/saves/" + file + ".txt", true).writeString(data.toString(), false);

            saveThumbnailCache.remove(file);

            data.free();

            ui.toast.show(Quarry.Q.i18n.get("toast.game_saved"));
            if (saveCallback != null) {
                saveCallback.call(null);
                saveCallback = null;
            }
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            ui.toast.show(Quarry.Q.i18n.get("toast.save_error"));
        }
    }

    public Texture getThumbnail(String filename) {
        return saveThumbnailCache.get(filename);
    }

    protected void updateThumbnailCache(String filename, Pixmap p) {
        Texture t = saveThumbnailCache.get(filename);
        if (t != null)
            t.dispose();
        saveThumbnailCache.put(filename, new Texture(p));
    }

    public CompoundTag loadMetaData(String filename) throws IOException {
        if (!Quarry.Q.file("TheQuarry/saves/" + filename + ".qmf", false).exists()) {
            // meta file does not exist, create a stub from qsf
            FileHandle qsf = Quarry.Q.file("TheQuarry/saves/" + filename + ".qsf", false);
            if (!qsf.exists()) {
                throw new FileNotFoundException();
            } else {
                CompoundTag data = null;
                try {
                    data = NBT.read(
                            new BufferedInputStream(
                                    Quarry.Q.file("TheQuarry/saves/" + filename + ".qsf", false).read()),
                            CompressionType.Fast);
                } catch (Exception e) {
                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                }
                Builder metaBuilder = new Builder("Meta");

                metaBuilder
                        .Byte("version", Const.QSF_VERSION)
                        .Int("build", data != null ? data.Int("build", 0) : 0)
                        .Byte("full", (byte) (Quarry.Q.fullVersion ? 1 : 0))
                        .Long("playTime", data != null ? data.Long("build", 0) : 0)
                        .Long("seed", data != null ? data.Long("seed", 0) : 0)
                        .String("name", data != null ? data.String("name", filename) : filename);

                CompoundTag meta = metaBuilder.Get();

                NBT.write(Quarry.Q.file("TheQuarry/saves/" + filename + ".qmf", true).write(false), meta,
                        CompressionType.Fast);

                return meta;
            }
        } else {
            CompoundTag data = NBT.read(
                    new BufferedInputStream(Quarry.Q.file("TheQuarry/saves/" + filename + ".qmf", false).read()),
                    CompressionType.Fast);

            if (!saveThumbnailCache.containsKey(filename) && data.has("thumbnail")) {
                try {
                    ByteArrayFileHandle bafh = new ByteArrayFileHandle(data.ByteArray("thumbnail"));
                    Pixmap p = PixmapIO.readCIM(bafh);
                    updateThumbnailCache(filename, p);
                    p.dispose();
                } catch (NBTException e) {
                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                }
            }
            return data;
        }
    }

    public CompoundTag loadSaveData(String filename) throws IOException {
        if (!Quarry.Q.file("TheQuarry/saves/" + filename + ".qsf", false).exists()) {
            throw new FileNotFoundException();
        }

        long t = System.currentTimeMillis();
        CompoundTag data = NBT.read(
                new BufferedInputStream(Quarry.Q.file("TheQuarry/saves/" + filename + ".qsf", false).read()),
                CompressionType.Fast);
        System.out.println("NBT loading took " + (System.currentTimeMillis() - t));
        return data;
    }

    public void load(final String filename, final Callback<Object> callback) {
        try {
            reset();

            System.out.println("Loading " + filename);

            final CompoundTag meta = loadMetaData(filename);
            if (meta == null) {
                callback.call(false);
                return;
            }

            final int build = meta.Int("build", 0);
            boolean full = meta.Byte("full", (byte) 0) == 1;
            if (build > Quarry.Q.versionNumber && Quarry.Q.isVersion()) {
                if (Quarry.Q.getScene() == this) {
                    ui.alert.show(ui, Quarry.Q.i18n.get("alert.newer_version"), new Callback<Void>() {
                        @Override
                        public void call(Void data) {
                            callback.call(false);
                        }
                    });
                } else {
                    ((MainMenu) Quarry.Q.getScene()).alert.show(((MainMenu) Quarry.Q.getScene()),
                            Quarry.Q.i18n.get("alert.newer_version"), new Callback<Void>() {
                                @Override
                                public void call(Void data) {
                                    callback.call(false);
                                }
                            });
                }
            } else if (full && !Quarry.Q.fullVersion) {
                if (Quarry.Q.getScene() == this) {
                    ui.alert.show(ui, Quarry.Q.i18n.get("alert.full_version_save"), new Callback<Void>() {

                        @Override
                        public void call(Void data) {
                            callback.call(false);
                        }
                    });
                } else {
                    ((MainMenu) Quarry.Q.getScene()).alert.show(((MainMenu) Quarry.Q.getScene()),
                            Quarry.Q.i18n.get("alert.full_version_save"), new Callback<Void>() {
                                @Override
                                public void call(Void data) {
                                    callback.call(false);
                                }
                            });
                }
            } else if (!full && Quarry.Q.fullVersion) {
                if (Quarry.Q.getScene() == this) {
                    ui.confirm.show(ui, Quarry.Q.i18n.get("confirm.demo_version_save"), new Callback<Boolean>() {
                        @Override
                        public void call(Boolean data) {
                            if (data == null)
                                callback.call(null);
                            else if (data == true)
                                loadData(meta, filename, callback, build);
                            else
                                callback.call(false);
                        }
                    });
                } else {
                    ((MainMenu) Quarry.Q.getScene()).confirm.show(((MainMenu) Quarry.Q.getScene()),
                            Quarry.Q.i18n.get("confirm.demo_version_save"), new Callback<Boolean>() {
                                @Override
                                public void call(Boolean data) {
                                    if (data == null)
                                        callback.call(null);
                                    else if (data == true)
                                        loadData(meta, filename, callback, build);
                                    else
                                        callback.call(false);
                                }
                            });
                }
            } else {
                loadData(meta, filename, callback, build);
            }
        } catch (Exception e) {
            callback.call(e);
        }
    }

    protected void loadData(CompoundTag meta, String filename, Callback<Object> callback, int build) {
        try {
            CompoundTag data = loadSaveData(filename);
            if (data == null) {
                callback.call(false);
                return;
            }

            long t0 = System.currentTimeMillis();
            if (build < Quarry.Q.versionNumber) {
                LoadingCompat.instance.upgrade(data, build);
            }

            currentGameName = meta.String("name");

            playTime = meta.Long("playTime", 0);
            lastTimerStart = System.currentTimeMillis();

            infinite = data.Byte("infinite", (byte) 0) == 1;

            Generator.G.setSeed(data.Long("seed", 0));
            if (data.has("rng"))
                Generator.G.setState(data.LongArray("rng"));

            try {
                layerIndex = data.Short("layer");
            } catch (NBTException e) {
                layerIndex = data.Byte("layer");
                if (layerIndex < 0)
                    layerIndex = 0;
            }
            ListTag map = data.List("Map", TagType.Compound);

            final Array<Layer> myLayers = new Array<>();

            int index = 0;
            for (Tag c : map.data) {
                Layer l = Layer.load(index++, (CompoundTag) c);
                myLayers.add(l);
            }

            Layer active = myLayers.get(layerIndex);

            final int fbuild = build;

            CompoundTag camera = data.Compound("camera");
            if (camera != null) {
                cam.position.set(camera.Float("x", active.width * Const.TILE_SIZE / 2),
                        camera.Float("y", active.height * Const.TILE_SIZE / 2), camera.Float("z", 0));
                cam.zoom = camera.Float("zoom", 0.5f);
                cam.update();
            }

            CompoundTag res = data.Compound("Resources");

            Util.NBTreadInventory(res, resources);

            getSeenResources().clear();
            short[] seen = res.ShortArray("Seen");
            for (short s : seen) {
                ItemType t = Item.get(s);
                if (t != null) {
                    getSeenResources().add(t);
                }
            }

            byte[] science = data.ByteArray("Sciences", null);
            if (science != null) {
                sciences.clear();
                for (byte s : science)
                    sciences.add(Science.sciences[s & 0xff]);
            }

            // for older versions. root science must be included
            sciences.add(ScienceType.Start);
            ui.buildMenuSciences.clear();
            ui.onScienceChange();

            data.free();

            ui.updateResources(true);

            // re-validate all layers
            for (Layer l : myLayers) {
                l.dirtyBounds.set(0, 0, l.width, l.height, Integer.MAX_VALUE);
            }

            System.out.println("Game loading took " + (System.currentTimeMillis() - t0));

            Gdx.app.postRunnable(new Runnable() {
                @Override
                public void run() {
                    synchronized (layerLock) {
                        if (layers == null) {
                            layers = new Array<>();
                        }
                        if (layers.size > 0) {
                            for (Layer l : layers)
                                l.dispose();
                            layers.clear();
                        }
                        layers.addAll(myLayers);
                        for (Layer l : layers)
                            l.postAllLayersLoad(true);
                        for (Layer l : layers)
                            l.postAllLayersLoad(false);

                        powerGrid.clearHighPowerCache();

                        layer = layers.get(layerIndex);
                        lastAutosave = System.currentTimeMillis();

                        System.gc();
                    }
                    ui.updateResources(true);
                    layerChangeNotifier.notify(Type.BULK_ADD, null);

                    // fix for barrel bug, in <v76 and in <v80
                    // fix for refund bug in <115
                    if (fbuild < 115) {
                        recalcResources();
                    }
                }
            });

            callback.call(true);
        } catch (Exception e) {
            callback.call(e);
        }
    }

    //////////////////////////////////////////////////////

    public void exportBlueprint() {
        NBT.Builder b = new NBT.Builder("Blueprint")
                .Int("build", Quarry.Q.versionNumber)
                .IntArray("region", Game.G.copyRegion)
                .List("structures", TagType.Compound);
        for (Pair<Structure<?>, CompoundTag> p : Game.G.copyStructures.values()) {
            b.add(p.getVal());
        }
        b.End().List("cables", TagType.Compound);
        for (Pair<Structure<?>, CompoundTag> p : Game.G.copyCables.values()) {
            b.add(p.getVal());
        }
        b.End();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            NBT.write(baos, b.Get(), CompressionType.Small);
            Quarry.Q.pi.message(Const.MSG_PASTE, new String(Base64Coder.encode(baos.toByteArray())));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void importBlueprint() {
        Object payload = Quarry.Q.pi.message(Const.MSG_COPY, null);
        if (payload == null) {
            ui.toast.show(Quarry.Q.i18n.get("toast.clipboard_empty"));
            return;
        }

        CompoundTag tag = null;

        try {
            byte[] data = Base64Coder.decode(payload.toString().trim());
            ByteArrayInputStream bais = new ByteArrayInputStream(data);
            tag = NBT.read(bais, CompressionType.Small);
        } catch (Exception e) {
            e.printStackTrace();
            ui.toast.show(Quarry.Q.i18n.get("toast.clipboard_invalid"));
            return;
        }

        try {
            int build = tag.Int("build");
            if (build > Quarry.Q.versionNumber) {
                ui.alert.show(ui, Quarry.Q.i18n.get("alert.blueprint_newer_version"), null);
                return;
            }

            copyRegion = tag.IntArray("region");
            Vector3 worldPos = cam.unproject(new Vector3(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2, 0));
            copyRegion[0] = MathUtils.clamp((int) (worldPos.x / Const.TILE_SIZE - copyRegion[2] / 2.0f), 0,
                    layer.width - copyRegion[2]);
            copyRegion[1] = MathUtils.clamp((int) (worldPos.y / Const.TILE_SIZE - copyRegion[3] / 2.0f), 0,
                    layer.height - copyRegion[3]);

            copyStructures.clear();
            copyCables.clear();
            copyCost.clear();
            for (Tag t : tag.List("structures", TagType.Compound).data) {
                Structure<?> s = Structure.loadPaste(copyRegion, (CompoundTag) t);
                if (!hasSciences(s.getSchema().sciencesRequired) && !GOD_MODE) {
                    ui.alert.show(ui, Quarry.Q.i18n.get("alert.blueprint_insufficient_science"), null);
                    return;
                }
                copyStructures.put(s.x * layer.height + s.y, new Pair<Structure<?>, CompoundTag>(s, (CompoundTag) t));

                for (Items.Amount a : s.getSchema().buildCosts.entries) {
                    copyCost.put(a.getItem(),
                            a.getAmount() + (copyCost.containsKey(a.getItem()) ? copyCost.get(a.getItem()) : 0));
                }
            }
            for (Tag t : tag.List("cables", TagType.Compound).data) {
                Structure<?> s = Structure.loadPaste(copyRegion, (CompoundTag) t);
                if (!hasSciences(s.getSchema().sciencesRequired) && !GOD_MODE) {
                    ui.alert.show(ui, Quarry.Q.i18n.get("alert.blueprint_insufficient_science"), null);
                    return;
                }
                copyCables.put(s.x * layer.height + s.y, new Pair<Structure<?>, CompoundTag>(s, (CompoundTag) t));

                for (Items.Amount a : s.getSchema().buildCosts.entries) {
                    copyCost.put(a.getItem(),
                            a.getAmount() + (copyCost.containsKey(a.getItem()) ? copyCost.get(a.getItem()) : 0));
                }
            }

            // paste data after loading all structures
            for (Pair<Structure<?>, CompoundTag> s : copyStructures.values()) {
                s.getKey().paste(copyRegion, s.getVal());
            }
            for (Pair<Structure<?>, CompoundTag> s : copyCables.values()) {
                s.getKey().paste(copyRegion, s.getVal());
            }
        } catch (Exception e) {
            e.printStackTrace();
            ui.toast.show(Quarry.Q.i18n.get("toast.clipboard_invalid"));
            return;
        }

        ui.hideTooltip();
        ui.hideBuildMenu();
        ui.hideScienceUI();
        resetActiveStructure();
        ui.copyButton.setChecked(true);
        copyMode = false;
        pasteMode = true;
        ui.updateCopyTableResources(true);
        camControl.updateActiveElementPlaceable();
    }

    //////////////////////////////////////////////////////

    public void resetActiveStructure() {
        activeStructure = null;
        endA.x = -1;
        endB.x = -1;
        activeStructureTrail.clear();
    }

    public void resetActiveTile() {
        activeTile.x = -1;
    }

    @Override
    public void dispose() {
        for (FrameBuffer fbo : chunkFBOs)
            if (fbo != null)
                fbo.dispose();

        batch.dispose();
        synchronized (layerLock) {
            if (layers.size > 0) {
                for (Layer l : layers)
                    l.dispose();
            }
        }

        if (fbo != null)
            fbo.dispose();
        for (Texture t : saveThumbnailCache.values())
            t.dispose();

        colorShader.dispose();
        music.dispose();

        spriter.dispose();
        shaper.dispose();
    }

    @Override
    public void resize(int width, int height) {
        //        viewport.setWorldSize(width>height?Const.H:Const.W, width>height?Const.W:Const.H);
        viewport.update(width, height);
        ui.resize(width, height);
        //        applyViewport = true;
    }

    @Override
    public boolean keyDown(int keycode) {
        // debug keybinds
        if (Quarry.Q.version.equals("debug")) {
            switch (keycode) {
                case Keys.F:
                    FLUIDMODE = !FLUIDMODE;
                    break;
                case Keys.H:
                    SINGLE_FRAME = true;
                    break;
                case Keys.R:
                    layer.dirtyBounds.set(0, 0, layer.width, layer.height, Integer.MAX_VALUE);
                    break;
                case Keys.D:
                    DRAW_DEBUG = !DRAW_DEBUG;
                    ui.toggleDebug();
                    break;
                case Keys.G:
                    GOD_MODE = !GOD_MODE;
                    ui.onScienceChange();
                    break;
                case Keys.T:
                    for (Chunk c : layer.getChunks())
                        if (c != null)
                            c.dirty = true;
                    break;
                case Keys.L:
                    addLayer();
                    break;
                case Keys.X:
                    FILLMODE = true;
                    break;
                case Keys.C:
                    RECORDMODE = true;
                    break;
                case Keys.V:
                    SCREENSHOT = true;
                    break;
                case Keys.K:
                    recalcResources();
                    break;
                case Keys.P:
                    increaseSpeed();
                    break;
                case Keys.O:
                    resetSpeed();
                    break;
                case Keys.U:
                    FOGMODE = true;
                    break;
                case Keys.W:
                    for (int i = 0; i < layer.width; i++)
                        for (int j = 0; j < layer.height; j++)
                            layer.removeMeta(i, j, TileMeta.FOG_OF_WAR);
                    break;
            }
        }

        switch (keycode) {
            case Keys.F1:
                UI_VISIBLE = !UI_VISIBLE;
                break;
            case Keys.F4:
                SMOOTH_CAMERA = !SMOOTH_CAMERA;
                break;
            /*
            case Keys.BACK:
                ui.confirm.show(ui, Quarry.Q.i18n.get("confirm.save_game"), new Callback<Boolean>() {
                    @Override
                    public void call(Boolean x) {
                        if (x instanceof Boolean) {
                            if (x) {
                                ui.menu.save(new Callback<Void>() {
                                    @Override
                                    public void call(Void o) {
                                        Gdx.app.exit();
                                    }
                                });
                            } else {
                                Gdx.app.exit();
                            }
                        }
                    }
                });
                return true;
                */
            case Keys.SPACE:
                setPaused(!isPaused());
                ui.pauseButton.setChecked(isPaused());
                return true;
            case Keys.ESCAPE:
                ui.menu.menuButton.setChecked(!ui.menu.menuButton.isChecked());
                return true;
            case Keys.B:
                ui.destroyButton.setChecked(false);
                if (!ui.buildButton.isChecked()) {
                    ui.showBuildMenu();
                    ui.buildButton.setChecked(true);
                } else {
                    ui.hideBuildMenu();
                    ui.buildButton.setChecked(false);
                }
                return true;
            case Keys.R:
                ui.rotateActiveStructure();
                return true;
            case Keys.F:
                ui.flipActiveStructure();
                return true;
            case Keys.DEL:
            case Keys.FORWARD_DEL:
                resetActiveStructure();
                ui.destroyButton.setChecked(!ui.destroyButton.isChecked());
                return true;
            case Keys.C:
                if (Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Keys.CONTROL_RIGHT)) {
                    if (ui.copyButton.isVisible())
                        ui.copyButton.setChecked(true);
                    return true;
                } else {
                    resetActiveStructure();
                    if (Game.G.hasScience(ScienceType.Electricity) || Game.GOD_MODE) {
                        ui.cableRemoveButton.setChecked(!ui.destroyButton.isChecked());
                        ui.destroyButton.setChecked(!ui.destroyButton.isChecked());
                        return true;
                    }
                }
            case Keys.Z:
                if (ui.currentClickedStructure instanceof ProducerStructure) {
                    // toggle sleep
                    ui.structureUIButtons[0].setChecked(!ui.structureUIButtons[0].isChecked());
                    return true;
                }
        }

        return false;
    }

    @Override
    public InputProcessor getInput() {
        return input;
    }

    public float getZoom() {
        return cam.zoom;
    }

    public OrthographicCamera getCamera() {
        return cam;
    }

    public void highlightArea(int... tiles) {
        synchronized (highlightLock) {
            tutorialHighlight.clear();
            tutorialHighlight.addAll(tiles);
        }
    }

    public IntSet getTutorialHighlight() {
        return tutorialHighlight;
    }

    public long getPlayTime() {
        return playTime + (System.currentTimeMillis() - lastTimerStart);
    }

    @Override
    public void pause() {
        super.pause();
        playTime = getPlayTime();
    }

    @Override
    public void resume() {
        super.resume();
        lastTimerStart = System.currentTimeMillis();
    }

    //////////////////////////////////////////////////////

    public boolean isInfinite() {
        return infinite;
    }

    public void setInfinite(boolean infinite) {
        this.infinite = infinite;
    }

    public boolean isPaused() {
        return gamePaused;
    }

    public void setPaused(boolean value) {
        gamePaused = value;
    }

    public void play() {
        gamePaused = false;
        gameSpeed = 1;
    }

    public void resetSpeed() {
        gameSpeed = 1;
    }

    public void increaseSpeed() {
        gameSpeed = Math.min(gameSpeed * 2, 100);
    }

    public int getItemCount() {
        int sum = 0;
        synchronized (layerLock) {
            for (Layer l : layers) {
                sum += l.getItemCount();
            }
        }

        return sum;
    }

    public int getStructureCount() {
        int sum = 0;
        synchronized (layerLock) {
            for (Layer l : layers) {
                sum += l.getStructureCount();
            }
        }

        return sum;
    }

    public boolean changeLayer(int delta) {
        if (layerIndex == 0 && delta < 0)
            return false;
        if (layerIndex + delta >= layers.size)
            return false;
        deltaLayer = delta;
        return true;
    }

    public int getLayerCount() {
        return layers.size;
    }

    public Layer getLayer(int index) {
        synchronized (layerLock) {
            if (layers == null || index < 0 || index >= layers.size)
                return null;
            return layers.get(index);
        }
    }

    public Layer addLayer() {
        synchronized (layerLock) {
            Layer l = new Layer(layers.size, Const.DEFAULT_LAYER_SIZE, Const.DEFAULT_LAYER_SIZE, TileType.Stone, true,
                    false);
            Generator.G.generate(l);
            layers.add(l);
            layerChangeNotifier.notify(Type.ADD, l);
            return l;
        }
    }

    public void stopSfx() {
        synchronized (layerLock) {
            layer.stopSfx();
        }
    }

}
