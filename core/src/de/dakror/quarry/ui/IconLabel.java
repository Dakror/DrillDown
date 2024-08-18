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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

/**
 * @author Maximilian Stark | Dakror
 */
public class IconLabel extends Table {
    private Image icon;
    private Label label;

    private TextTooltip textTooltip;

    public IconLabel(int iconSize, Skin skin, String icon, String text) {
        this(iconSize, skin, skin.getDrawable(icon), text);
    }

    public IconLabel(int iconSize, Skin skin, Drawable icon, String text, Object... userObject) {
        this(iconSize, skin, icon, text, "default-font", userObject);
    }

    public IconLabel(int iconSize, Skin skin, Drawable icon, String text, String font, Object... userObject) {
        if (userObject.length > 0)
            setUserObject(userObject[0]);
        this.icon = new Image(icon);

        add(this.icon).size(iconSize);
        label = new Label(text, skin, font, Color.WHITE);
        add(label).height(iconSize + 4).padLeft(5).right();
    }

    public IconLabel tooltip(Skin skin) {
        return tooltip(skin, "");
    }

    public IconLabel tooltip(Skin skin, String text) {
        textTooltip = new TextTooltip(text, skin);
        setTouchable(Touchable.enabled);
        addListener(textTooltip);
        return this;
    }

    public void setText(String text) {
        label.setText(text);
    }

    public void setTooltipText(String text) {
        textTooltip.getActor().setText(text);
    }

    public void setIcon(Drawable icon) {
        this.icon.setDrawable(icon);
    }
}
