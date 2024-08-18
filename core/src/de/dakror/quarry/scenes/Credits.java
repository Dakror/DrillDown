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

package de.dakror.quarry.scenes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;

import de.dakror.common.libgdx.ui.Scene;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;

/**
 * @author Maximilian Stark | Dakror
 */
public class Credits extends Scene {
    TextureRegion px;

    ScrollPane sp;

    public static final Credits instance = new Credits();

    private Credits() {
        init();
    }

    @Override
    public void init() {
        px = Quarry.Q.atlas.findRegion("pixel");
        stage = new Stage(new FitViewport(Const.UI_W, Const.UI_H));
        stage.setActionsRequestRendering(false);

        Table table = new Table();
        table.setFillParent(true);
        table.row().grow();
        Label l = new Label(Gdx.files.internal("CREDITS.txt").readString(), Quarry.Q.skin);
        l.setWrap(true);
        l.setAlignment(Align.topLeft);

        Table wrap = new Table();
        wrap.row().padLeft(35).grow();
        wrap.add(l);

        sp = new ScrollPane(wrap);
        sp.setScrollBarPositions(true, false);
        sp.setVariableSizeKnobs(false);
        sp.setScrollingDisabled(true, false);
        sp.setSmoothScrolling(false);

        table.add(sp).left().top().pad(20).grow();
        stage.addActor(table);

        stage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (alpha == 1) fadeOut = true;
            }
        });
        stage.addListener(new InputListener() {
            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if (keycode == Keys.BACK) {
                    if (alpha == 1) fadeOut = true;
                    return true;
                }

                return false;
            }
        });

    }

    @Override
    public void show() {
        super.show();
        sp.setScrollY(0);
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);

        if (alpha == 0 && fadeOut) {
            Quarry.Q.dropScene();
        }
    }

    @Override
    public void draw() {
        stage.getBatch().begin();
        stage.getBatch().setColor(0, 0, 0, 0.5f * alpha);
        stage.getBatch().draw(px, 0, 0, stage.getWidth(), stage.getHeight());
        stage.getBatch().setColor(1, 1, 1, 1);
        stage.getBatch().end();
        super.draw();
    }
}
