/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
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
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.scenes.Game;

/**
 * @author Maximilian Stark | Dakror
 */
@LmlTag(tagName = "volumebutton")
public class VolumeButton extends ImageButton implements CustomTag {

    int state;
    String style;

    public VolumeButton(Skin skin, String style) {
        super(skin, style);
        this.style = style;
    }

    private void setImage() {
        getStyle().imageUp = getSkin().getDrawable("symb_" + style + "_" + state);
    }

    public void initListener() {
        state = (int) (Quarry.Q.prefs.getFloat(style, 1.0f) * 3);
        setImage();
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                state = (state + 1) % 4;
                if (style.equals("sound")) {
                    if (state > 0) {
                        Quarry.Q.sound.setSoundVolume(state / 3f);
                    }
                    Quarry.Q.sound.setPlaySound(state > 0);
                    if (Quarry.Q.getScene() instanceof Game) {
                        if (state == 0) Game.G.stopSfx();
                        Game.G.spatializedPlayer.setVolume(state / 3f);
                    }
                } else {
                    if (state > 0) Quarry.Q.sound.setMusicVolume(state / 3f);
                    Quarry.Q.sound.setPlayMusic(state > 0);
                }
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Quarry.Q.prefs.putFloat(style, state / 3f).flush();
                setImage();
            }
        });
    }

    @Override
    public void postInit() {}
}
