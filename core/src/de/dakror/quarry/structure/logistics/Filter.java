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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.Callback;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.RouterStructure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class Filter extends RouterStructure {
    public static final RouterSchema classSchema = new RouterSchema(0, StructureType.Filter, true,
            "filter", new Items(ItemType.Stone, 2, ItemType.IronIngot, 2), null)
                    .sciences(ScienceType.Routers);

    protected static Group ui;

    ItemType[] filters;

    public Filter(int x, int y) {
        super(x, y, classSchema);
        filters = new ItemType[4];
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (super.canAccept(item, x, y, dir)) {
            for (int i = 0; i < 4; i++) {
                if (tubes[i] == null) continue;
                if (filters[i] == null || filters[i] == item) return true;
            }
        }

        return false;
    }

    @Override
    protected boolean dispatchItem() {
        boolean anyNull = false;
        boolean anyTube = false;

        if (currentSource == null)
            currentSource = this;

        for (int i = 0; i < 4; i++) {
            anyTube = true;

            if (filters[i] == null) anyNull = true;

            if (filters[i] == currentItem) {
                // filter on this item is the source direction, so we abort this until the filter is resolved
                if (currentSourceDirection == Direction.values[i]) {
                    return false;
                }
                return layer.addItemEntity(currentItem, this, Direction.values[i], currentSource);
            }

        }

        if (!anyTube || !anyNull) return false;

        // go through unfiltered directions to find one that isnt backed up
        for (int i = 0; i < 4; i++) {
            if (tubes[i] == null) continue;
            if (filters[i] == null && layer.addItemEntity(currentItem, this, Direction.values[i], currentSource)) {
                return true;
            }
        }
        return false;
    }

    private void updateUI() {
        int i = 0;
        for (Actor a : ui.getChildren()) {
            if (a instanceof ImageButton) {
                ImageButton ib = (ImageButton) a;
                ItemType t = ((Filter) ui.getUserObject()).filters[i];
                ib.setChecked(false);
                ImageButtonStyle ibs = new ImageButtonStyle(ib.getStyle());
                ibs.imageUp = t == null ? null : t.drawable;
                ib.setStyle(ibs);
                ib.getImage().invalidateHierarchy();
                i++;
            }
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        if (ui == null) {
            ui = Util.lml("structure-filter");
            ui.setUserObject(this);
            int i = 2;
            int j = 0;
            for (Actor a : ui.getChildren()) {
                if (a instanceof Image) {
                    ((Image) a).setOrigin(a.getWidth() / 2, a.getHeight() / 2);
                    ((Image) a).setScale(1.5f);
                    ((Image) a).setRotation(90 * i);
                    i--;
                } else {
                    final ImageButton ib = (ImageButton) a;
                    ib.getImageCell().size(36).center().expand();

                    final int me = j;
                    j++;
                    final Callback<ItemType> callback = new Callback<ItemType>() {
                        @Override
                        public void call(ItemType data) {
                            ((Filter) ui.getUserObject()).filters[me] = data;
                            updateUI();
                            setItemNotifications();
                        }
                    };
                    a.addListener(new ClickListener() {
                        @Override
                        public void clicked(InputEvent event, float x, float y) {
                            if (!ib.isChecked()) Game.G.ui.itemSelection.hide();
                            else {
                                for (Actor c : ui.getChildren()) {
                                    if (c instanceof ImageButton && c != ib) ((ImageButton) c).setChecked(false);
                                }
                                Game.G.ui.itemSelection.show(callback, true);
                            }
                        }
                    });
                }
            }
        }

        ui.setUserObject(this);
        updateUI();

        content.add(ui).center().grow();
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        short[] s = tag.ShortArray("filters", null);
        if (s != null) {
            for (int i = 0; i < 4; i++) {
                filters[i] = s[i] == 0 ? null : Item.get(s[i]);
            }
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        short[] arr = new short[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = filters[i] == null ? 0 : filters[i].value;
        }

        b.ShortArray("filters", arr);
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        short[] arr = new short[4];
        for (int i = 0; i < 4; i++) {
            arr[i] = filters[i] == null ? 0 : filters[i].value;
        }

        b.ShortArray("filters", arr);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        short[] s = tag.ShortArray("filters", null);
        if (s != null) {
            for (int i = 0; i < 4; i++) {
                filters[i] = s[i] == 0 ? null : Item.get(s[i]);
            }
        }
    }
}
