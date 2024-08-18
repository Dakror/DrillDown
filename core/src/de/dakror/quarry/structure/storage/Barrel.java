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

package de.dakror.quarry.structure.storage;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TooltipManager;
import com.badlogic.gdx.utils.Align;

import de.dakror.common.BiCallback;
import de.dakror.common.Callback;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
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
import de.dakror.quarry.structure.base.component.CSingleInventory;
import de.dakror.quarry.ui.ResourceAmountLabel;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Barrel extends StorageStructure {
    public static Schema classSchema = new Schema(0, StructureType.Barrel, true, 1,
            1,
            "barrel",
            new Items(ItemType.Scaffolding, 5, ItemType.WoodPlank, 24),
            null,
            new Dock(0, 0, Direction.South, DockType.ItemOut), new Dock(0, 0, Direction.North, DockType.ItemIn))
                    .components(new CSingleInventory(350, 0, false))
                    .flags(Flags.ConfirmDestruction)
                    .sciences(ScienceType.BetterStorage)
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            ((CSingleInventory) ((Barrel) data).components[0]).setOutputEnabled(on);
                        }
                    }))
                    .button(new ButtonDef("icon_destroy", "button.refund", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            Barrel st = (Barrel) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.refundStorage = on;
                        }
                    }));

    ResourceAmountLabel res;
    Table ui;
    Label fl;
    ItemType uiType;

    public Barrel(int x, int y) {
        super(x, y, classSchema);

        ((CSingleInventory) components[0]).setPumpOutCallback(new Callback<ItemType>() {
            @Override
            public void call(ItemType i) {
                Game.G.removeResources(i, 1, false);
                onItemCountChanged(i);
            }
        });
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 0)
            return ((CSingleInventory) components[0]).isOutputEnabled();
        if (buttonIndex == 1)
            return refundStorage;
        return false;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        CSingleInventory inv = ((CSingleInventory) components[0]);
        if (isNextToDock(x, y, dir, getDocks()[1])) {
            return inv.canAccept(item);
        }
        return false;
    }

    @Override
    public boolean putBack(ItemType item, int amount) {
        CSingleInventory inv = ((CSingleInventory) components[0]);
        if (inv.getItem() == null || inv.getItem() == item) return super.putBack(item, amount);
        else return false;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (clicked) {
            updateUI();
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        // draw item icon
        CSingleInventory inv = ((CSingleInventory) components[0]);
        if (!inv.isEmpty()) {
            if (inv.getItem().stackable != null)
                spriter.add(inv.getItem().stackable.icon, this.x * Const.TILE_SIZE + (Const.TILE_SIZE - 32) / 2 + 5,
                        this.y * Const.TILE_SIZE + (Const.TILE_SIZE - 32) / 2 + 5, Const.Z_STRUCTURES + 0.1f, 22, 22);
            spriter.add(inv.getItem().icon, this.x * Const.TILE_SIZE + (Const.TILE_SIZE - 32) / 2,
                    this.y * Const.TILE_SIZE + (Const.TILE_SIZE - 32) / 2, Const.Z_STRUCTURES + 0.1f, 32, 32);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Muhahaha
        CSingleInventory inv = (CSingleInventory) components[0];
        if (!inv.isEmpty()) {
            Game.G.removeResources(inv.getItem(), inv.getCount(), false);
        }
    }

    private void updateUI() {
        if (ui == null) {
            ui = new Table();
            fl = new Label("", Quarry.Q.skin);
            fl.setAlignment(Align.center);
        }

        CSingleInventory ci = (CSingleInventory) components[0];

        if (res == null || uiType != ci.getItem()) {
            uiType = ci.getItem();
            ui.clear();
            if (uiType != null) {
                res = new ResourceAmountLabel(32, Quarry.Q.skin, uiType, ci.getCount());
                ui.add(res).grow();
            } else {
                res = null;
                ui.add().grow();
            }

            ui.row();
            ui.add(fl);
        } else {
            res.setAmount(ci.getCount());
        }

        fl.setText(ci.getCount() + " / " + ci.getSize());
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(ui).grow();
    }

    @Override
    protected void onItemCountChanged(ItemType item) {
        updateUI();
        setItemNotifications();
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("pumping", ((CSingleInventory) components[0]).isOutputEnabled() ? (byte) 1 : (byte) 0);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        ((CSingleInventory) components[0]).setOutputEnabled(tag.Byte("pumping", (byte) 0) == 1);
    }
}
