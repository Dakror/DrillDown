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

import java.util.Locale;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.libgdx.ui.CameraAction;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.producer.Mine;

/**
 * @author Maximilian Stark | Dakror
 */
public class Tutorial {
    public static class Step {
        float x, y, width, height;
        boolean modal;
        boolean tappable;

        public Step(float x, float y, float width, float height, boolean tappable, boolean modal) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.tappable = tappable;
            this.modal = modal;
        }

        public void onShow(Window w) {}

        public boolean isDone() {
            return false;
        }
    }

    Array<Step> steps;
    int stepNum;
    Window win;
    Label l;
    public Image indicator;
    IndicatorAction indicatorAction;

    class IndicatorAction extends Action {
        float y;
        float counter = 0;

        @Override
        public boolean act(float delta) {
            counter += delta;
            if (counter >= 2 * MathUtils.PI) counter -= 2 * MathUtils.PI;
            getActor().setY(y + MathUtils.sin(counter * 5) * 3);
            getActor().toFront();
            return false;
        }
    }

    public Tutorial(Stage stage, Skin skin) {
        win = new Window("", skin);
        win.setResizable(false);
        win.setMovable(false);
        win.setVisible(false);
        win.pad(32);
        win.setSize(65, 65);
        win.setKeepWithinStage(false);
        win.setPosition(Const.UI_W / 2, Const.UI_H / 2);
        win.setTouchable(Touchable.enabled);
        win.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Step step = getStep();
                if (step == null) return;

                if (step.tappable && event.getStageX() >= step.x && event.getStageX() <= step.x + step.width
                        && event.getStageY() >= step.y && event.getStageY() <= step.y + step.height) {
                    Quarry.Q.sound.play(Quarry.Q.clickSfx);
                    next();
                }
            }
        });
        l = new Label("", skin);
        l.setWrap(true);
        l.setAlignment(Align.topLeft);
        l.getStyle().font.getData().markupEnabled = true;
        win.add(l).grow().top();
        win.setName("tutorial");
        stage.addActor(win);

        indicator = new Image(skin.getDrawable("caret_down"));
        indicator.addAction(indicatorAction = new IndicatorAction());
        indicator.setSize(24, 10);
        indicator.setVisible(false);
        indicator.setName("tutorial_indicator");
        stage.addActor(indicator);

        steps = new Array<>();

        reset();
        initSteps();
    }

    public void update() {
        Step step = getStep();
        if (step != null && !step.tappable && step.isDone()) next();
    }

    public Step getStep() {
        if (stepNum < 0 || stepNum >= steps.size) return null;
        return steps.get(stepNum);
    }

    public int getStepNum() {
        return stepNum;
    }

    public void reset() {
        stepNum = -1;
        if (win.getStage() != null) {
            win.addAction(sequence(fadeOut(0.15f), visible(false)));
            indicator.clearActions();
            indicator.setVisible(false);
        }
    }

    protected void initSteps() {
        // GOOD SEED 
        // 5044591726400517120

        int[][] heights = { { 400, 480 }, { 330, 330 }, { 250, 330 }, { 230, 300 }, { 370, 370 }, { 380, 400 }, {},
                { 300, 270 }, { 430, 520 }, { 330, 330 }, { 300, 300 }, { 440, 480 }, { 440, 440 }, { 440, 440 } };

        int l = Quarry.Q.i18n.getLocale().getLanguage().equals(new Locale("de").getLanguage()) ? 1 : 0;

        steps.add(new Step((Const.UI_W - 600) / 2, (Const.UI_H - heights[0][l]) / 2, 600, heights[0][l], true, true) {
            @Override
            public void onShow(Window w) {
                super.onShow(w);

                Game.G.ui.destroyButton.setDisabled(true);
                Game.G.ui.destroyButton.setVisible(false);
            }
        });
        steps.add(new Step(0, 0, Const.UI_W, heights[1][l], true, true));
        steps.add(new Step(0, 0, Const.UI_W, heights[2][l], false, false) {
            @Override
            public boolean isDone() {
                return Game.G.getZoom() >= ((Const.DEFAULT_LAYER_SIZE - 2) * Const.TILE_SIZE) / Const.W;
            }
        });
        steps.add(new Step(0, 0, Const.UI_W, heights[3][l], false, false) {
            @Override
            public boolean isDone() {
                return true;
            }
        });
        //        steps.add(new Step(0, 0, Const.UI_W, heights[3][l], true, true));
        steps.add(new Step(0, Const.UI_H - heights[4][l], Const.UI_W - 150, heights[4][l], true, true));
        steps.add(new Step(250, Const.UI_H - heights[5][l], Const.UI_W - 250, heights[5][l], true, true));
        steps.add(new Step(0, 0, 0, 0, false, true) {
            CameraAction action;

            @Override
            public void onShow(Window w) {
                action = new CameraAction(0.5f, new Vector3((Game.G.layer.width + 0.75f) * Const.TILE_SIZE / 2, (Game.G.layer.height - 7) * Const.TILE_SIZE / 2, 0), 0.75f, Game.G.getCamera());
                win.addAction(action);
            }

            @Override
            public boolean isDone() {
                return action.isDone();
            }
        });
        steps.add(new Step(0, 450, Const.UI_W, heights[7][l], false, false) {
            @Override
            public boolean isDone() {
                return Game.G.ui.tooltipCurrentStructure instanceof Mine;
            }
        });
        steps.add(new Step(0, 450, Const.UI_W, heights[8][l], false, false) {
            @Override
            public boolean isDone() {
                return Game.G.activeStructure instanceof Mine;
            }
        });
        steps.add(new Step(0, 120, Const.UI_W, heights[9][l], false, false) {
            CameraAction action;

            @Override
            public void onShow(Window w) {
                action = new CameraAction(0.5f, new Vector3((Game.G.layer.width + 0.75f) * Const.TILE_SIZE / 2, (Game.G.layer.height - 14) * Const.TILE_SIZE / 2, 0), 0.75f, Game.G.getCamera());
                win.addAction(action);
                Game.G.highlightArea(2009, 2010, 2073, 2074);
            }

            @Override
            public boolean isDone() {
                return action.isDone() && Game.G.getTutorialHighlight().size == 0;
            }
        });
        steps.add(new Step(0, 120, Const.UI_W, heights[10][l], false, false) {
            @Override
            public void onShow(Window w) {
                Game.G.resetActiveStructure();
                Game.G.ui.hideTooltip();
            }

            @Override
            public boolean isDone() {
                return Game.G.activeStructure != null && Game.G.activeStructure.getSchema().type == StructureType.Conveyor;
            }
        });
        steps.add(new Step(0, 120, Const.UI_W, heights[11][l], false, false) {
            @Override
            public void onShow(Window w) {
                Game.G.highlightArea(2011, 2012, 2013, 2014);
            }

            @Override
            public boolean isDone() {
                return Game.G.getTutorialHighlight().size == 0;
            }
        });
        steps.add(new Step(0, Const.UI_H - heights[12][l], Const.UI_W, heights[12][l], false, false) {
            boolean hasBamboozled = false;

            @Override
            public void onShow(Window w) {
                synchronized (Game.renderThreadLock) {
                    Game.G.renderThreadTasks.add(new Runnable() {
                        @Override
                        public void run() {
                            Conveyor c = (Conveyor) Game.G.layer.getStructure(2012);
                            c.setRotation(Direction.East);
                            hasBamboozled = true;
                        }
                    });
                }
            }

            @Override
            public boolean isDone() {
                if (!hasBamboozled) return false;

                for (int i = 2011; i <= 2014; i++) {
                    if (Game.G.layer.getStructure(i) == null
                            || ((Conveyor) Game.G.layer.getStructure(i)).getDirection() != Direction.North) {
                        return false;
                    }
                }

                return true;
            }
        });
        steps.add(new Step(0, Const.UI_H - heights[13][l], Const.UI_W - 150, heights[13][l], true, true) {
            @Override
            public void onShow(Window w) {
                super.onShow(w);
                Game.G.ui.destroyButton.setDisabled(false);
                Game.G.ui.destroyButton.setVisible(true);
            }
        });
    }

    public void next() {
        Step old = getStep();
        win.clearActions();
        stepNum++;

        indicator.clearActions();
        indicator.setVisible(false);

        if (stepNum >= steps.size) {
            reset();
            return;
        }

        float durationB = 0.4f;

        float durationA = 0.2f;

        Interpolation interpA = Interpolation.pow3In;
        Interpolation interpB = Interpolation.pow3Out;

        Step step = getStep();
        step.onShow(win);

        if (Quarry.Q.desktop) {
            step.width = Math.min(720, step.width);
            step.x = (Const.UI_W - step.width) / 2f;
        }

        win.setWidth(step.width);
        l.setWidth(step.width - 64);

        String text = Quarry.Q.i18n.get("tutorial.step" + stepNum);

        if (!Quarry.Q.fullVersion) {
            String demoText = Quarry.Q.i18n.get("tutorial.step" + stepNum + "_demo");
            if (demoText != null && !demoText.startsWith("???")) text = demoText;
        } else if (Quarry.Q.desktop) {
            String desktopText = Quarry.Q.i18n.get("tutorial.step" + stepNum + "_desktop");
            if (desktopText != null && !desktopText.startsWith("???")) text = desktopText;
        }

        l.setText(text.replace("\r", ""));
        win.invalidateHierarchy();
        win.validate();
        float oh = step.height;
        step.height = win.getPrefHeight();
        if (step.y == Const.UI_H - oh) {
            step.y -= step.height - oh;
        }

        // clamp to insets
        if (step.x < Quarry.Q.safeInsets[0]) step.x = Quarry.Q.safeInsets[0];
        if (step.y < Quarry.Q.safeInsets[3]) step.y = Quarry.Q.safeInsets[3];
        if (step.y + step.height >= Const.UI_H - Quarry.Q.safeInsets[1]) step.y = Const.UI_H - Quarry.Q.safeInsets[1] - step.height;
        if (step.x + step.width >= Const.UI_W - Quarry.Q.safeInsets[2]) step.x = Const.UI_W - Quarry.Q.safeInsets[2] - step.width;

        if (stepNum == 0) {
            win.addAction(sequence(alpha(0), moveTo(step.x, step.y),
                    sizeTo(step.width, step.height), visible(true), fadeIn(0.15f)));
        } else if (step.width > 0) {
            win.addAction(sequence(parallel(alpha(0.4f, durationA), sizeTo(100, 100, durationA, interpA),
                    moveTo(win.getX() + old.width / 2 - 50, win.getY() + old.height / 2 - 50, durationA, interpA)),
                    parallel(fadeIn(durationB), moveTo(step.x, step.y, durationB, interpB),
                            sizeTo(step.width, step.height, durationB, interpB))));
        } else {
            win.addAction(alpha(0));
        }

        // Disabled modality because it breaks the UI because of missing initialization propagation 
        //        win.setModal(step.modal);

        if (step.tappable) {
            indicatorAction.y = step.y + 30;
            indicator.addAction(indicatorAction);
            indicator.addAction(sequence(delay(1), alpha(0), visible(true),
                    moveTo(step.x + step.width - 50, indicatorAction.y), fadeIn(0.1f)));
            indicator.toFront();
        }

    }
}
