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

package de.dakror.quarry.ui;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.Response;
import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
@LmlTag(tagName = "prompt")
public class Prompt extends Window implements CustomTag {
    Label text;
    TextField input;
    Response<String, Boolean> callback;
    Ui ui;

    public Prompt(Skin skin, String style) {
        super("", skin, style);
        setName("prompt");
    }

    @Override
    public void postInit() {
        text = Util.id("label");
        getTitleTable().padTop(50);
        input = Util.id("input");
        input.setOnlyFontChars(true);
        Util.id("ok").addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (input.getText().trim().length() == 0)
                    return;

                boolean hide = true;
                if (callback != null)
                    hide = callback.call(input.getText());

                if (hide) {
                    input.getOnscreenKeyboard().show(false);
                    callback = null;
                    ui.hide(Prompt.this);
                }
            }
        });
        Util.id("cancel").addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                boolean hide = true;
                if (callback != null)
                    hide = callback.call(null);
                if (hide) {
                    callback = null;
                    input.getOnscreenKeyboard().show(false);
                    ui.hide(Prompt.this);
                }
            }
        });
    }

    public void show(Ui ui, String title, String value, Response<String, Boolean> callback) {
        // only one at a time
        if (getStage() != null)
            return;

        this.ui = ui;
        this.callback = callback;
        text.setText(title);
        input.setText(value);
        pack();
        ui.getStage().setKeyboardFocus(input);
        input.getOnscreenKeyboard().show(true);
        setPosition((Const.UI_W - getWidth()) / 2, (Const.UI_H - getHeight()) / 3 * 2);
        ui.show(this);
    }
}
