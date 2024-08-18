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
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.Callback;
import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
@LmlTag(tagName = "upgrade")
public class Upgrade extends Window implements CustomTag {
    Label text;
    Callback<Boolean> callback;

    Ui ui;

    public Upgrade(Skin skin, String style) {
        super("", skin, style);
        setName("upgrade");
    }

    @Override
    public void postInit() {
        text = Util.id("label");

        Util.id("yes").addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (callback != null)
                    callback.call(true);
                callback = null;

                ui.hide(Upgrade.this);
            }
        });
        Util.id("no").addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (callback != null)
                    callback.call(false);
                callback = null;

                ui.hide(Upgrade.this);
            }
        });

    }

    public void show(Ui ui, String title, Callback<Boolean> callback) {
        // only one at a time
        if (getStage() != null)
            return;
        this.ui = ui;
        this.callback = callback;
        text.setText(title);
        pack();
        setPosition((Const.UI_W - getWidth()) / 2, (Const.UI_H - getHeight()) / 3 * 2);
        ui.show(this);
    }
}
