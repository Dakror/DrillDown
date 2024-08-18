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

package de.dakror.quarry.structure;

import java.util.EnumMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import de.dakror.common.BiCallback;
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
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science;
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
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;
import net.spookygames.gdx.sfx.SfxSound;

/**
 * @author Maximilian Stark | Dakror
 */
public class ScienceLab extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.ScienceLab, true, 2, 2,
            "sciencelab", new Items(ItemType.StoneBrick, 10, ItemType.Scaffolding, 10), null, new Dock(1, 0, Direction.East, DockType.ItemIn))
                    .button(new ButtonDef("icon_science", "button.science", ButtonType.TempRadio, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean u, Structure<?> v) {
                            if (u != null) {
                                if (u == true) {
                                    if ((((ScienceLab) v).activeScience == null || ((ScienceLab) v).waitingForInputs) && ((CInventory) ((ScienceLab) v).components[0]).isEmpty())
                                        Game.G.ui.showScienceUI();
                                } else {
                                    Game.G.ui.hideScienceUI();
                                }
                            }
                        }
                    }))
                    .components(new CInventory(Science.getMaxBuyCosts()))
                    .flags(Flags.TextureAlwaysUpright);

    ScienceType activeScience;
    boolean waitingForInputs;
    float workingTime;

    // ui stuff
    Table ui;
    Container<Table> container;
    EnumMap<ItemType, Table> cells;
    HorizontalGroup items;
    static Label waitingLabel;

    static SfxSound sfxSound;

    static {
        sfxSound = Quarry.Q.assets.get("sfx/science" + Const.SFX_FORMAT);
    }

    public ScienceLab(int x, int y) {
        super(x, y, classSchema);
    }

    protected synchronized boolean checkItemAddition(ItemType item, boolean add) {
        if (activeScience == null || !waitingForInputs) return false;

        CInventory inv = (CInventory) components[0];
        for (Amount a : activeScience.costs.entries) {
            if (a.getItem() != null) {
                if ((a.getItem() == item || a.getItem() == Item.base(item))) {
                    if (inv.get(a.getItem()) < a.getAmount()) {
                        if (add) {
                            inv.add(item, 1);
                            updateUIAmount(item);

                            boolean hasAll = true;
                            for (Amount e : activeScience.costs.entries) {
                                if (inv.get(e.getItem()) < e.getAmount()) {
                                    hasAll = false;
                                    break;
                                }
                            }

                            if (hasAll) {
                                waitingForInputs = false;
                                inv.clear();
                                updateUI();
                            }
                        }
                        return true;
                    } else return false;
                }
            }
        }

        return false;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (!isNextToDock(x, y, dir, getDocks()[0]) || item.categories.contains(ItemCategory.Fluid)) return false;

        return checkItemAddition(item, false);
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        return checkItemAddition(item, true);
    }

    public boolean setActiveScience(ScienceType science) {
        if (activeScience != null && !waitingForInputs) return false;

        if (activeScience != null)
            Game.G.removeCurrentScience(activeScience);

        Game.G.addCurrentScience(science);
        activeScience = science;
        workingTime = science.workingTime;
        waitingForInputs = true;
        updateUI();
        if (ui != null) {
            items.clear();
            cells.clear();
            for (Amount a : science.costs.entries) {
                updateUIAmount(a.getItem());
            }
        }
        return true;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);
        if (activeScience != null && !waitingForInputs) {
            workingTime -= deltaTime * gameSpeed;
            if (workingTime <= 0) {
                Quarry.Q.sound.play(sfxSound);

                Game.G.removeCurrentScience(activeScience);
                Game.G.addScience(activeScience);
                activeScience = null;
                workingTime = 0;
                updateUI();
            }
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (activeScience != null && !waitingForInputs) {
            shaper.setColor(0, 0.5f, 0, 1);
            float progress = 1 - workingTime / activeScience.workingTime;
            shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * Const.TILE_SIZE, 8);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (activeScience != null)
            Game.G.removeCurrentScience(activeScience);
    }

    protected void updateUI() {
        if (ui == null) return;

        if (activeScience != null) {
            container.setActor(ui);
            ((TextureRegionDrawable) ((Image) ui.getChildren().get(0)).getDrawable()).setRegion(activeScience.icon);
            ((Label) ui.getChildren().get(1)).setText(activeScience.title);
            waitingLabel.setVisible(waitingForInputs);
        } else {
            container.setActor(null);
        }
    }

    protected void updateUIAmount(ItemType type) {
        if (ui == null) return;

        if (activeScience == null) {
            cells.clear();
            items.clear();
            return;
        }
        CInventory inv = (CInventory) components[0];
        int amount = activeScience.costs.getAmount(type) - inv.get(type);
        Table t = cells.get(type);
        if (amount <= 0) {
            if (t != null) {
                items.removeActor(t);
                cells.remove(type);
                items.invalidateHierarchy();
            }
        } else if (t == null) {
            Table q = GameUi.createResourceTable(32, Quarry.Q.skin, type, amount + "", type);
            q.getCells().get(1).width(61).height(32).padRight(15);
            items.addActor(q);
            items.invalidateHierarchy();
            cells.put(type, q);
        } else {
            ((Label) t.getChildren().get(1)).setText(amount + "");
            ((TextTooltip) t.getListeners().get(0)).getActor().setText(amount + " " + type.title);
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);
        if (ui == null) {
            ui = new Table();
            ui.defaults().top();
            ui.add(new Image(new TextureRegionDrawable())).size(50);
            ui.add(new Label("", Quarry.Q.skin)).padTop(5).left().growX();

            ui.row();
            if (waitingLabel == null) {
                waitingLabel = new Label(Quarry.Q.i18n.get("structure.UI.waiting_for_inputs"), Quarry.Q.skin, "small-font", Color.WHITE);
                waitingLabel.setAlignment(Align.center);
            }
            ui.add(waitingLabel).colspan(2).spaceTop(10);
            ui.row();

            items = new HorizontalGroup();
            items.wrap().left().rowAlign(Align.left).top();

            ui.add(items).spaceTop(10).colspan(2).grow();

            cells = new EnumMap<>(ItemType.class);

            container = new Container<>();
            container.fill();
        }
        updateUI();

        if (activeScience != null && waitingForInputs) {
            for (Amount a : activeScience.costs.entries) {
                updateUIAmount(a.getItem());
            }
        }

        content.add(container).grow();
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        if (activeScience != null) {
            b
                    .Byte("science", activeScience.id)
                    .Float("workingTime", workingTime)
                    .Byte("waiting", (byte) (waitingForInputs ? 1 : 0));
        }
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        int science = tag.Byte("science", (byte) 0) & 0xff;
        if (science != 0) {
            try {
                activeScience = Science.sciences[science];
                if (activeScience != null) {
                    workingTime = tag.Float("workingTime");
                    waitingForInputs = tag.Byte("waiting") == 1;
                    Game.G.addCurrentScience(activeScience);
                }
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }
}
