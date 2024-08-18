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

import java.util.Arrays;
import java.util.Comparator;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import de.dakror.common.Callback;
import de.dakror.common.Response;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Generator;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.scenes.MainMenu;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class Menu {

    // Menu bar
    public ImageButton menuButton;
    Image menuOverlay, modalOverlay;
    ScrollPane menuSaves;
    VerticalGroup menuSavesContainer;
    Table menu;
    TextButton newGame, save, saveAs, load, quit, uiMode, export, import_;
    VolumeButton sound, music;
    public Label fps;

    public Menu(final Stage stage, final Skin skin) {
        Util.lml("menu");

        fps = Util.id("fps");

        menuOverlay = Util.id("overlay");
        menuOverlay.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                menuButton.setChecked(false);
            }
        });
        menuOverlay.setName("menuOverlay");
        stage.addActor(menuOverlay);
        menuOverlay.setPosition(0, 0);
        menuOverlay.setHeight(menuOverlay.getHeight() + Quarry.Q.safeInsets[1] + Quarry.Q.safeInsets[3]); // overwrite clamping

        menuSavesContainer = Util.id("saves_cont");
        menuSavesContainer.grow();
        menuSavesContainer.setRound(false);
        menuSaves = Util.id("saves");
        ScrollPaneStyle sps = new ScrollPaneStyle(menuSaves.getStyle());
        sps.background = GameUi.getDrawable(skin, "panel_metalDark", 24, 24, 24, 24);
        menuSaves.setStyle(sps);
        menuSaves.setPosition(-menuSavesContainer.getWidth(), -20);
        menuSaves.setName("menuSaves");
        stage.addActor(menuSaves);

        menuButton = Util.id("button");
        menuButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                boolean checked = ((ImageButton) actor).isChecked();

                float amount = menu.getWidth() * (checked ? 1 : -1);

                menuButton.addAction(moveBy(amount, 0, 0.15f, Interpolation.fade));

                if (checked) {
                    Quarry.Q.sound.play(Quarry.Q.clickSfx);
                    menuOverlay.addAction(sequence(visible(true), alpha(0.4f, 0.15f, Interpolation.fade)));
                    menu.addAction(sequence(visible(true), moveBy(amount, 0, 0.15f, Interpolation.fade)));
                } else {
                    menu.addAction(sequence(moveBy(amount, 0, 0.15f, Interpolation.fade), visible(false)));
                    menuOverlay.addAction(sequence(alpha(0, 0.15f, Interpolation.fade), visible(false)));
                    save.setChecked(false);
                    saveAs.setChecked(false);
                    load.setChecked(false);
                }

                menu.toFront();

            }
        });
        menuButton.setPosition(12, Const.UI_H - menuButton.getHeight() - 12);
        menuButton.setName("menuButton");
        stage.addActor(menuButton);

        menu = Util.id("menu");
        menu.addListener(new ActorGestureListener() {
            @Override
            public void fling(InputEvent event, float velocityX, float velocityY, int button) {
                if (Math.abs(velocityX / 1000) > 1 && Math.abs(velocityY / 1000) < 0.4f) {
                    menuButton.setChecked(false);
                }
            }
        });
        menu.setPosition(-menu.getWidth() - 32 + Quarry.Q.safeInsets[0], -32 + Quarry.Q.safeInsets[3]);
        menu.setHeight(menu.getHeight() - Quarry.Q.safeInsets[1] - Quarry.Q.safeInsets[3]);
        menu.setVisible(false);
        menu.setName("menu");
        stage.addActor(menu);

        newGame = Util.id("new_game");
        newGame.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                final Callback<Void> cb = new Callback<Void>() {
                    @Override
                    public void call(Void data1) {
                        Game.G.ui.seedPrompt.show(Game.G.ui, Quarry.Q.i18n.get("prompt.seed"),
                                Long.toString((long) (Math.random() * Long.MAX_VALUE)),
                                new Response<String, Boolean>() {
                                    @Override
                                    public Boolean call(String data) {
                                        if (data != null) {
                                            long seed = 0;
                                            try {
                                                seed = Long.parseLong(data.trim());
                                            } catch (NumberFormatException e) {
                                                seed = Util.hash(data);
                                            }
                                            Generator.G.setSeed(seed);

                                            Game.G.startNewGame();
                                            menuButton.setChecked(false);
                                            newGame.setChecked(false);
                                        }
                                        return true;
                                    }
                                });
                    }
                };
                Game.G.ui.confirm.show(Game.G.ui, Quarry.Q.i18n.get("confirm.save_game"), new Callback<Boolean>() {
                    @Override
                    public void call(Boolean data) {
                        if (data != null) {
                            if (data == true) {
                                save(cb);
                            } else {
                                cb.call(null);
                            }
                        }
                        newGame.setChecked(false);
                    }
                });
            }
        });

        save = Util.id("save_game");
        save.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                save(null);
            }
        });

        saveAs = Util.id("save_as");
        saveAs.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);

                final boolean p = Game.G.isPaused();
                Game.G.ui.pauseButton.setChecked(true);
                Game.G.setPaused(true);

                save.setChecked(false);
                load.setChecked(false);
                saveAs(new Callback<String>() {
                    @Override
                    public void call(String data) {
                        if (!p) {
                            Game.G.ui.pauseButton.setChecked(false);
                            Game.G.setPaused(false);
                        }
                    }
                });
            }
        });

        load = Util.id("load_game");
        load.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                save.setChecked(false);
                saveAs.setChecked(false);
                if (!load.isChecked()) {
                    menuSaves.addAction(
                            sequence(moveTo(-menuSaves.getWidth(), menuSaves.getY(), .15f, Interpolation.fade),
                                    visible(false)));
                } else {
                    Quarry.Q.sound.play(Quarry.Q.clickSfx);

                    menuSavesContainer.clear();

                    FileHandle[] list = Quarry.Q.listFiles();
                    Arrays.sort(list, new Comparator<FileHandle>() {
                        @Override
                        public int compare(FileHandle o1, FileHandle o2) {
                            long y = o1.lastModified();
                            long x = o2.lastModified();
                            return (x < y) ? -1 : ((x == y) ? 0 : 1);
                        }
                    });

                    for (FileHandle fh : list) {
                        final String n = fh.nameWithoutExtension();

                        final Table entry = new Table();
                        final ImageButton del = new ImageButton(skin, "delete");
                        del.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                                Game.G.ui.confirm.show(Game.G.ui, Quarry.Q.i18n.format("confirm.delete_save", n),
                                        new Callback<Boolean>() {
                                            @Override
                                            public void call(Boolean data) {
                                                if (data != null && data) {
                                                    if (Quarry.Q.file("TheQuarry/saves/" + n + ".qsf", false)
                                                            .delete()) {
                                                        Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.save_deleted"));
                                                        menuSavesContainer.removeActor(entry);
                                                    } else {
                                                        Game.G.ui.toast
                                                                .show(Quarry.Q.i18n.get("toast.save_not_deleted"));
                                                    }
                                                }
                                            }
                                        });
                            }
                        });

                        entry.setTouchable(Touchable.enabled);
                        entry.addListener(new ClickListener() {
                            @Override
                            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                                super.touchDown(event, x, y, pointer, button);
                                if (event.getTarget().isDescendantOf(del))
                                    return false;
                                entry.setBackground(GameUi.trButton);
                                return true;
                            }

                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                Quarry.Q.sound.play(Quarry.Q.clickSfx);

                                modalOverlay.toFront();
                                modalOverlay.addAction(sequence(visible(true), alpha(0.4f, 0.25f, Interpolation.fade)));
                                Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.loading_game"));
                                Game.G.ui.pauseButton.setChecked(true);
                                Game.G.setPaused(true);
                                Quarry.Q.threadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Game.G.load(n, new Callback<Object>() {
                                            @Override
                                            public void call(Object data) {
                                                modalOverlay.addAction(
                                                        sequence(alpha(0, 0.25f, Interpolation.fade), visible(false)));
                                                if (data instanceof Boolean && (Boolean) data == true) {
                                                    Game.G.ui.pauseButton.setChecked(true);
                                                    Game.G.setPaused(true);

                                                    stage.unfocusAll();
                                                    Game.G.resetActiveStructure();
                                                    Game.G.ui.hideTooltip();
                                                    Game.G.ui.destroyButton.setChecked(false);
                                                    Game.G.ui.hideBuildMenu();

                                                    load.setChecked(false);
                                                    menuButton.setChecked(false);
                                                    Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.game_loaded"));
                                                } else if (data instanceof Boolean && (Boolean) data == false) {
                                                    Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.game_not_loaded"));
                                                } else if (data instanceof Exception) {
                                                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, data);
                                                    Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.game_not_loaded"));
                                                }
                                            }
                                        });
                                    }
                                });
                            }

                            @Override
                            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                                super.touchUp(event, x, y, pointer, button);
                                if (event.getTarget().isDescendantOf(del))
                                    return;
                                entry.setBackground((Drawable) null);
                            }

                        });
                        try {
                            CompoundTag data = Game.G.loadMetaData(n);
                            if (data == null)
                                continue;
                            Texture t = Game.G.getThumbnail(n);
                            Image i = null;
                            if (t == null) {
                                i = new Image(skin.getDrawable("thumb"));
                            } else {
                                i = new Image(t);
                            }
                            if (Quarry.Q.desktop) {
                                entry.add(i).size(178, 100);
                            } else {
                                entry.add(i).size(56.25f, 100);
                            }
                            Table details = new Table();
                            details.defaults().left().top();
                            details.add(new Label(data.String("name", Quarry.Q.i18n.get("menu.no_save_name")), skin));
                            details.row();

                            long playtime = data.Long("playTime", 0) / 1000;
                            Label l2 = new Label(String.format("%s\n%s - %d:%02d:%02dh %s\nSeed: %d", n,
                                    GameUi.dateFormat.format(fh.lastModified()),
                                    playtime / 3600, (playtime % 3600) / 60, playtime % 60,
                                    Quarry.Q.i18n.get("ui.played"), data.Long("seed", 0)), skin);
                            l2.setColor(Color.LIGHT_GRAY);
                            l2.setFontScale(0.75f);
                            details.add(l2).padTop(-5);
                            entry.add(details).expandX().left().top().space(10);

                            entry.add(del).right().spaceRight(20);
                            data.free();
                        } catch (Exception e) {
                            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                        }
                        GameUi.sep(entry).colspan(99).fillX();
                        menuSavesContainer.addActor(entry);

                    }
                    menuSaves.setScrollY(0);
                    menuSaves.toFront();
                    // menuSavesContainer.removeActor(menuSavesContainer.getChildren().peek());
                    menuSaves.addAction(
                            sequence(visible(true), moveTo(-20, menuSaves.getY(), .15f, Interpolation.fade)));
                }

            }
        });

        final TextButton menu = Util.id("main_menu");
        menu.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                menu.setChecked(false);
                Game.G.ui.confirm.show(Game.G.ui, Quarry.Q.i18n.get("confirm.save_game"), new Callback<Boolean>() {
                    @Override
                    public void call(Boolean data) {
                        if (data != null) {
                            if (data == true) {
                                save(new Callback<Void>() {
                                    @Override
                                    public void call(Void data1) {
                                        menuButton.setChecked(false);
                                        Game.G.reset();
                                        Quarry.Q.addScene(MainMenu.M);
                                        Quarry.Q.dropScene(Game.G);
                                    }
                                });
                            } else {
                                menuButton.setChecked(false);
                                Game.G.reset();
                                Quarry.Q.addScene(MainMenu.M);
                                Quarry.Q.dropScene(Game.G);
                            }
                        }
                    }
                });
            }
        });

        final ImageButton de = Util.id("lang");
        de.setChecked(Quarry.Q.i18n.getLocale().getLanguage().equals("en"));
        de.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.ui.alert.show(Game.G.ui, Quarry.Q.i18n.get("alert.language_change"), new Callback<Void>() {
                    @Override
                    public void call(Void data) {
                    }
                });
                Quarry.Q.prefs.putBoolean("german", !de.isChecked()).flush();
            }
        });

        sound = Util.id("sfx");
        sound.initListener();

        music = Util.id("music");
        music.initListener();

        export = Util.id("export");
        export.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ((TextButton) event.getListenerActor()).setChecked(false);
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (Game.G.pasteMode) {
                    Game.G.exportBlueprint();
                    Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.selection_copied"));
                } else {
                    Game.G.ui.toast.show(Quarry.Q.i18n.get("toast.no_selection"));
                }
            }
        });
        export.setVisible(false);

        import_ = Util.id("import");
        import_.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ((TextButton) event.getListenerActor()).setChecked(false);
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                menuButton.setChecked(false);
                Game.G.importBlueprint();
            }
        });
        import_.setVisible(false);

        TextButton recording = Util.id("recording");
        recording.setVisible(true);
        recording.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.RECORDMODE = !Game.RECORDMODE;
            }
        });

        modalOverlay = Util.id("modal_overlay");
        stage.addActor(modalOverlay);
        modalOverlay.setPosition(0, 0);
        modalOverlay.setHeight(modalOverlay.getHeight() + Quarry.Q.safeInsets[1] + Quarry.Q.safeInsets[3]); // overwrite clamping
    }

    public void init() {
        sound.setChecked(!Quarry.Q.sound.isPlaySound());
        music.setChecked(!Quarry.Q.sound.isPlayMusic());
    }

    public void onScienceChange() {
        export.setVisible(Game.G.hasScience(ScienceType.Blueprints));
        import_.setVisible(Game.G.hasScience(ScienceType.Blueprints));
    }

    public void save(final Callback<Void> o) {
        final boolean p = Game.G.isPaused();
        Game.G.setPaused(true);
        Game.G.ui.pauseButton.setChecked(true);
        load.setChecked(false);
        saveAs.setChecked(false);
        if (Game.G.currentGameName == null) {
            saveAs(new Callback<String>() {
                @Override
                public void call(String data) {
                    Game.G.currentGameName = data;
                    save.setChecked(false);
                    if (!p) {
                        Game.G.setPaused(false);
                        Game.G.ui.pauseButton.setChecked(false);
                    }
                    if (o != null)
                        o.call(null);
                }
            });
        } else {
            Game.G.save(o);
            save.setChecked(false);
            if (!p) {
                Game.G.ui.pauseButton.setChecked(false);
                Game.G.setPaused(false);
            }
        }
    }

    public void saveAs(final Callback<String> o) {
        Game.G.ui.prompt.show(Game.G.ui, Quarry.Q.i18n.get("prompt.save_name"), null, new Response<String, Boolean>() {
            @Override
            public Boolean call(final String data) {
                if (data != null) {
                    if (Game.G.saveExists(data)) {
                        Game.G.ui.confirm.show(Game.G.ui, Quarry.Q.i18n.get("confirm.save_exists"),
                                new Callback<Boolean>() {
                                    @Override
                                    public void call(Boolean d) {
                                        if (d != null && d) {
                                            save.setChecked(false);
                                            saveAs.setChecked(false);
                                            Game.G.save(data, true, new Callback<Void>() {
                                                @Override
                                                public void call(Void x) {
                                                    o.call(data);
                                                }
                                            });
                                            Game.G.ui.hide(Game.G.ui.prompt);
                                        }
                                    }
                                });
                        return false;
                    } else {
                        save.setChecked(false);
                        saveAs.setChecked(false);
                        Game.G.save(data, true, new Callback<Void>() {
                            @Override
                            public void call(Void x) {
                                o.call(data);
                            }
                        });
                    }
                }
                save.setChecked(false);
                saveAs.setChecked(false);
                return true;
            }
        });
    }
}
