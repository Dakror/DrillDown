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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.Callback;
import de.dakror.common.libgdx.ui.PfxActor;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.MainMenu;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class EndOfGame {
    Window endWindow;
    Image endOverlay;
    PfxActor endPfx;

    public EndOfGame(Stage stage) {
        Util.lml("endofgame");

        endOverlay = Util.id("overlay");
        endOverlay.setName("endOverlay");
        stage.addActor(endOverlay);
        endOverlay.setPosition(0, 0);
        endOverlay.setHeight(endOverlay.getHeight() + Quarry.Q.safeInsets[1] + Quarry.Q.safeInsets[3]); // overwrite clamping

        endPfx = new PfxActor(Game.G.confettiPfx);
        endPfx.setName("endPfx");
        stage.addActor(endPfx);

        endWindow = Util.id("window");
        endWindow.setVisible(false);
        endWindow.setPosition((Const.UI_W - endWindow.getWidth()) / 2, (Const.UI_H - endWindow.getHeight()) / 2);
        endWindow.getTitleTable().pad(80, 0, 20, 0);

        final Button leave = Util.id("leave");
        leave.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                Game.G.ui.menu.save(new Callback<Void>() {
                    @Override
                    public void call(Void e) {
                        // TODO ugly, polish, also remove toast from game ui
                        Quarry.Q.addScene(MainMenu.M);
                        Quarry.Q.dropScene(Game.G);
                    }
                });
            }
        });

        final Button stay = Util.id("stay");
        stay.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                hide();
                Game.G.setInfinite(true);
                Game.G.ui.menu.save(new Callback<Void>() {
                    @Override
                    public void call(Void e) {}
                });
            }
        });

        endWindow.setName("endWindow");
        stage.addActor(endWindow);
    }

    public void show() {
        Game.G.setPaused(true);
        Game.G.ui.pauseButton.setChecked(true);

        Game.G.confettiPfx.reset();
        Game.G.confettiPfx.setPosition(Const.UI_W / 2, Const.UI_H / 2);

        endWindow.addAction(sequence(alpha(0), visible(true), parallel(fadeIn(0.2f), sequence(delay(0.1f), Actions.run(new Runnable() {
            @Override
            public void run() {
                endPfx.start();
            }
        })))));
        endOverlay.addAction(sequence(alpha(0), visible(true), alpha(0.4f, 0.15f, Interpolation.fade)));
    }

    public void hide() {
        endWindow.addAction(sequence(fadeOut(0.2f), visible(false)));
        endOverlay.addAction(sequence(fadeOut(0.2f), visible(false)));
    }

}
