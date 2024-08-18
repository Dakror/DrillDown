/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
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

package com.badlogic.gdx.scenes.scene2d.ui.modified;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Value;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;

/** A tooltip that shows a label.
 * @author Nathan Sweet */
public class TextTooltip extends Tooltip<Label> {
    public TextTooltip(String text, Skin skin) {
        this(text, TooltipManager.getInstance(), skin.get(TextTooltipStyle.class));
    }

    public TextTooltip(String text, Skin skin, String styleName) {
        this(text, TooltipManager.getInstance(), skin.get(styleName, TextTooltipStyle.class));
    }

    public TextTooltip(String text, TextTooltipStyle style) {
        this(text, TooltipManager.getInstance(), style);
    }

    public TextTooltip(String text, TooltipManager manager, Skin skin) {
        this(text, manager, skin.get(TextTooltipStyle.class));
    }

    public TextTooltip(String text, TooltipManager manager, Skin skin, String styleName) {
        this(text, manager, skin.get(styleName, TextTooltipStyle.class));
    }

    public TextTooltip(String text, final TooltipManager manager, TextTooltipStyle style) {
        super(null, manager);

        Label label = new Label(text, style.label);

        // TODO: make this in a class of my own
        // currently not possible thanks to non-protected fields of Tooltip
        container.center().height(48);
        container.setActor(label);
        label.setAlignment(Align.center);
        container.width(new Value() {
            @Override
            public float get(Actor context) {
                return ((Label) context).getPrefWidth() + 40;
            }
        });

        setStyle(style);
    }

    public void setStyle(TextTooltipStyle style) {
        if (style == null) throw new NullPointerException("style cannot be null");
        if (!(style instanceof TextTooltipStyle)) throw new IllegalArgumentException("style must be a TextTooltipStyle.");
        container.getActor().setStyle(style.label);
        container.setBackground(style.background);
        container.maxWidth(style.wrapWidth);
    }

    /** The style for a text tooltip, see {@link TextTooltip}.
     * @author Nathan Sweet */
    static public class TextTooltipStyle {
        public LabelStyle label;
        /** Optional. */
        public Drawable background;
        /** Optional, 0 means don't wrap. */
        public float wrapWidth;

        public TextTooltipStyle() {}

        public TextTooltipStyle(LabelStyle label, Drawable background) {
            this.label = label;
            this.background = background;
        }

        public TextTooltipStyle(TextTooltipStyle style) {
            this.label = new LabelStyle(style.label);
            background = style.background;
        }
    }
}
