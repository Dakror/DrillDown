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

package de.dakror.quarry.ui;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.scenes.GameUi;

/**
 * @author Maximilian Stark | Dakror
 */
public class ResourceAmountLabel extends IconLabel {
    private TextTooltip tooltip;

    private ItemType item;
    private ItemCategory cat;
    private float amount;

    public ResourceAmountLabel(int iconSize, Skin skin, String icon, float amount) {
        this(iconSize, skin, skin.getDrawable(icon), amount);
    }

    public ResourceAmountLabel(int iconSize, Skin skin, Drawable icon, float amount, Object... userObject) {
        this(iconSize, skin, icon, amount, "default-font", userObject);
    }

    public ResourceAmountLabel(int iconSize, Skin skin, Drawable icon, float amount, String font, Object... userObject) {
        super(iconSize, skin, icon, Float.toString(amount), font, userObject);
        setAmount(amount);
    }

    public ResourceAmountLabel(int iconSize, Skin skin, ItemType item, float amount, Object... userObject) {
        this(iconSize, skin, item.drawable, amount, item, userObject);
        setTouchable(Touchable.enabled);
        this.item = item;
        tooltip = new TextTooltip("", skin);
        addListener(tooltip);
    }

    public ResourceAmountLabel(int iconSize, Skin skin, ItemCategory cat, float amount,
            Object... userObject) {
        this(iconSize, skin, cat.drawable, amount, userObject);
        setTouchable(Touchable.enabled);
        this.cat = cat;
        tooltip = new TextTooltip("", skin);
        addListener(tooltip);
    }

    public ItemType getItem() {
        return item;
    }

    public ItemCategory getCat() {
        return cat;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
        String text = null;

        if (item != null && item.categories.contains(ItemCategory.Fluid) || cat == ItemCategory.Fluid) {
            text = GameUi.formatResourceAmount(amount / 1000f) + "L";
        } else {
            text = GameUi.formatResourceAmount((int) amount);
        }

        setText(text);
        String name = "";
        if (item != null) name = " " + item.title;
        else if (cat != null) name = " " + cat.title;
        if (tooltip != null) tooltip.getActor().setText(text + name);
    }
}
