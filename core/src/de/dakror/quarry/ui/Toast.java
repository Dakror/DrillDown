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

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;

/**
 * @author Maximilian Stark | Dakror
 */
@LmlTag(tagName = "toast")
public class Toast extends Window implements CustomTag {
    public Toast(Skin skin, String style) {
        super("", skin, style);

        getTitleTable().padTop(80);
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setVisible(false);
            }
        });
    }

    @Override
    public void postInit() {}

    public void show(String text) {
        if (isVisible()) {
            clearActions();
        }

        toFront();
        getTitleLabel().setText(text);
        setSize(500, 80);
        setPosition((getStage().getWidth() - getWidth()) / 2, 140);
        addAction(sequence(fadeIn(0.25f, Interpolation.fade), delay(1.5f), fadeOut(0.5f, Interpolation.fade), visible(false)));
        setColor(0.1f, 0.1f, 0.1f, 0);
        setVisible(true);
    }

}
