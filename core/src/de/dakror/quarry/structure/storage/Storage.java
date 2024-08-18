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

package de.dakror.quarry.structure.storage;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.BiCallback;
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
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StorageStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ElectricConveyorCore;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Storage extends StorageStructure {
    static final Drawable lightBg = Quarry.Q.skin.getDrawable("light-bg");
    static final Drawable greenBg = Quarry.Q.skin.getDrawable("green-bg");
    static final TextureRegion fillStatus = Quarry.Q.atlas.findRegion("structure_storage_fill_status");

    public static final Schema classSchema = new Schema(0, StructureType.Storage, true, 3,
            2,
            "storage",
            new Items(ItemType.Stone, 16, ItemType.StoneBrick, 2, ItemType.Scaffolding, 8), null, new Dock(0, 0, Direction.South, DockType.ItemIn), new Dock(2, 0, Direction.South, DockType.ItemOut))
                    .components(new CInventory(200))
                    .flags(Flags.ConfirmDestruction)
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.TempRadio, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            Storage st = (Storage) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.outputSelectMode = on;
                            for (Actor a : st.ui.getChildren()) {
                                Table t = (Table) a;
                                if (!st.outputs.contains((ItemType) a.getUserObject(), true)) {
                                    Drawable bg = null;
                                    if (on)
                                        bg = lightBg;
                                    t.setBackground(bg);
                                }
                                t.invalidateHierarchy();
                            }
                        }
                    }))
                    .button(new ButtonDef("icon_destroy", "button.refund", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            Storage st = (Storage) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.refundStorage = on;
                        }
                    }));

    protected HorizontalGroup ui;
    protected ScrollPane sp;
    protected Label fl;
    protected EnumMap<ItemType, Table> cells;
    protected Array<ItemType> outputs;

    protected boolean outputSelectMode;
    protected boolean pumping;
    protected boolean isTubeAtOutput;
    protected float pumpOutDelay;
    protected int pumpOutItemIndex;

    public Storage(int x, int y) {
        this(x, y, classSchema);
    }

    protected Storage(int x, int y, Schema schema) {
        super(x, y, schema);
        outputs = new Array<>();

        cells = new EnumMap<>(ItemType.class);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        drawFillStatus(spriter);
    }

    protected void drawFillStatus(SpriteRenderer spriter) {
        float rat = ((CInventory) components[0]).getFillRatio();
        if (rat > 0) {

            float width = 42;
            float height = 28;
            float originX = width / 2f;
            float originY = height / 2f;

            spriter.add(x * Const.TILE_SIZE + (getWidth() * Const.TILE_SIZE - width) / 2f,
                    y * Const.TILE_SIZE + (getHeight() * Const.TILE_SIZE - height) / 2f, -.5f,
                    originX, originY,
                    width * rat, height,
                    1, 1, upDirection.rot - Direction.North.rot,
                    fillStatus.getRegionX(),
                    fillStatus.getRegionY(),
                    (int) (rat * fillStatus.getRegionWidth()),
                    fillStatus.getRegionHeight(),
                    false, false);
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        Dock d = getDocks()[1];

        if (dirtyBounds.touches(this)) {
            Structure<?> s = layer.getStructure(x + d.x + d.dir.dx, y + d.y + d.dir.dy);
            isTubeAtOutput = s instanceof Conveyor;
            if (s instanceof ElectricConveyorCore && ((ElectricConveyorCore) s).getDirection().isPerpendicular(d.dir))
                isTubeAtOutput = false;
        }

        if (pumping && !outputSelectMode && gameSpeed > 0) {
            pumpOutDelay -= deltaTime * gameSpeed;
            if (pumpOutDelay <= 0) {
                pumpOutDelay = ((CInventory) components[0]).getPumpOutSpeed();
                if (!isTubeAtOutput)
                    return;
                pumpOutItems();
            }
        }
    }

    // TODO Not really stable code for pumping out alternatingly.
    // Maybe revise
    protected void pumpOutItems() {
        CInventory inv = (CInventory) components[0];

        for (int i = 0; i < outputs.size; i++) {
            int index = (pumpOutItemIndex + i) % outputs.size;
            ItemType t = outputs.get(index);
            if (inv.remove(t, 1)) {
                if (layer.addItemEntity(t, this, getDocks()[1], this)) {
                    if (ui != null)
                        setUIAmount(t, inv.get(t));
                    onItemCountChanged(t);
                    Game.G.removeResources(t, 1, false);
                    setItemNotifications();
                    pumpOutItemIndex += i + 1;
                    return;
                } else {
                    // revert
                    inv.addUnsafe(t, 1);
                }
            }
        }

        //        pumpOutItemIndex = 0;
        if (ui != null)
            ui.getChildren().sort(new Comparator<Actor>() {
                @Override
                public int compare(Actor a, Actor b) {
                    return Integer.compare(((ItemType) a.getUserObject()).value & 0xff, ((ItemType) b.getUserObject()).value & 0xff);
                }
            });
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        CInventory inventory = ((CInventory) components[0]);
        if (!inventory.hasSpace())
            return false;
        if (item.categories.contains(ItemCategory.Fluid))
            return false;

        Dock d = getDocks()[0];
        if (isNextToDock(x, y, dir, d) && (d.filter == null || (inventory.hasSpace() && d.filter.accepts(item)))) {
            return true;
        }

        return false;
    }

    public boolean hasItemType(ItemType item) {
        CInventory inventory = ((CInventory) components[0]);
        return inventory.get(item) > 0;
    }

    protected void setUIAmount(final ItemType type, int amount) {
        Table t = cells.get(type);
        if (amount <= 0 && !outputs.contains(type, true)) {
            if (t != null) {
                ui.removeActor(t);

                // TODO: memory leak, gets created again and again if the item goes out and comes back and so on.
                cells.remove(type);
            }
            return;
        } else if (t == null) {
            t = GameUi.createResourceTable(32, Quarry.Q.skin, type, amount + "", type);
            t.setTouchable(Touchable.enabled);
            t.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (outputSelectMode) {
                        setOutput(type, !outputs.contains(type, true));
                    }
                }
            });
            // TODO: figure out a way to add the new resource at the right
            // location (within the itemtypes)
            ((Label) t.getCells().get(1).width(92).height(48).getActor()).setAlignment(Align.left);

            ui.addActor(t);
            ui.invalidateHierarchy();
            cells.put(type, t);

            if (outputs.contains(type, true))
                t.setBackground(greenBg);
        } else {
            ((Label) t.getChildren().get(1)).setText(amount + "");
            ((TextTooltip) t.getListeners().get(0)).getActor().setText(amount + " " + type.title);
        }
    }

    protected void setOutput(ItemType type, boolean output) {
        Table t = cells.get(type);
        if (output) {
            outputs.add(type);
        } else {
            outputs.removeValue(type, true);
        }

        pumping = outputs.size > 0;

        if (t != null) {
            Drawable d = lightBg;
            if (output)
                d = greenBg;
            t.setBackground(d);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Muhahaha
        for (Entry<ItemType, Integer> e : ((CInventory) components[0]).getAll()) {
            Game.G.removeResources(e.getKey(), e.getValue(), false);
        }
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 1)
            return refundStorage;
        return false;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Byte("output", isTubeAtOutput ? (byte) 1 : (byte) 0);
        if (pumping) {
            short[] outs = new short[outputs.size];
            for (int i = 0; i < outs.length; i++)
                outs[i] = outputs.get(i).value;

            b.ShortArray("pumping", outs);
        }
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        isTubeAtOutput = tag.Byte("output", (byte) 0) == 1;

        short[] outs = tag.ShortArray("pumping", null);
        if (outs != null) {
            for (short s : outs) {
                ItemType t = Item.get(s);

                if (t != null)
                    setOutput(t, true);
            }
        }
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("output", isTubeAtOutput ? (byte) 1 : (byte) 0);
        if (pumping) {
            short[] outs = new short[outputs.size];
            for (int i = 0; i < outs.length; i++)
                outs[i] = outputs.get(i).value;

            b.ShortArray("pumping", outs);
        }
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        isTubeAtOutput = tag.Byte("output", (byte) 0) == 1;

        short[] outs = tag.ShortArray("pumping", null);
        if (outs != null) {
            for (short s : outs) {
                ItemType t = Item.get(s);

                if (t != null)
                    setOutput(t, true);
            }
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        if (ui == null) {
            ui = new HorizontalGroup();
            ui.wrap().rowAlign(Align.left);

            sp = new ScrollPane(ui, Quarry.Q.skin, "container");
            sp.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    super.enter(event, x, y, pointer, fromActor);
                    event.getStage().setScrollFocus(event.getTarget());
                }

                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    super.exit(event, x, y, pointer, toActor);
                    event.getStage().setScrollFocus(toActor);
                }
            });
            sp.setScrollbarsVisible(true);
            sp.setScrollbarsOnTop(true);
            sp.setFadeScrollBars(true);
            sp.setScrollingDisabled(true, false);

            fl = new Label("", Quarry.Q.skin);
            fl.setAlignment(Align.center);
        }

        sp.setScrollX(0);
        sp.updateVisualScroll();
        sp.invalidateHierarchy();
        sp.validate();

        CInventory inv = (CInventory) components[0];
        fl.setText(inv.getCount() + " / " + inv.getSize());

        for (Map.Entry<ItemType, Table> e : cells.entrySet()) {
            setUIAmount(e.getKey(), ((CInventory) components[0]).get(e.getKey()));
        }
        for (Map.Entry<ItemType, Integer> e : ((CInventory) components[0]).getAll()) {
            setUIAmount(e.getKey(), e.getValue());
        }
        for (ItemType i : outputs) {
            setUIAmount(i, ((CInventory) components[0]).get(i));
        }

        ui.getChildren().sort(new Comparator<Actor>() {
            @Override
            public int compare(Actor a, Actor b) {
                return Integer.compare(((ItemType) a.getUserObject()).value & 0xff, ((ItemType) b.getUserObject()).value & 0xff);
            }
        });

        content.add(sp).growX().top().left().expand();
        content.row();
        content.add(fl).growX();

        content.getStage().setScrollFocus(sp);
    }

    @Override
    protected void onItemCountChanged(ItemType item) {
        if (ui != null) {
            CInventory inv = (CInventory) components[0];
            setUIAmount(item, inv.get(item));
            fl.setText(inv.getCount() + " / " + inv.getSize());

            ui.getChildren().sort(new Comparator<Actor>() {
                @Override
                public int compare(Actor a, Actor b) {
                    return Integer.compare(((ItemType) a.getUserObject()).value & 0xff, ((ItemType) b.getUserObject()).value & 0xff);
                }
            });
        }
    }
}
