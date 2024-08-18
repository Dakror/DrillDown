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

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.Comparator;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

import de.dakror.common.Callback;
import de.dakror.common.Response;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.CompressionType;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.render.DepthSpriter;
import de.dakror.common.libgdx.ui.Scene;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Generator;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.LoadingCompat;
import de.dakror.quarry.ui.Alert;
import de.dakror.quarry.ui.Confirm;
import de.dakror.quarry.ui.Prompt;
import de.dakror.quarry.ui.SeedPrompt;
import de.dakror.quarry.ui.Toast;
import de.dakror.quarry.ui.Ui;
import de.dakror.quarry.ui.VolumeButton;
import de.dakror.quarry.util.SpriterDelegateBatch;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class MainMenu extends Scene implements Ui {
    public static MainMenu M;
    ScrollPane saves;
    VerticalGroup savesList;
    TextButton tutorial;

    Image overlay, modalOverlay;

    Toast toast;
    Confirm confirm;

    Array<Layer> layers;

    boolean inited;
    boolean newGame;

    SpriteBatch batch;
    FitViewport viewport;
    OrthographicCamera cam, fboCam;

    Music music;

    public Alert alert;
    public Prompt prompt;
    public SeedPrompt seedPrompt;

    int uResolution, uChannel, uDirection;

    // rendering
    DepthSpriter spriter;
    ShapeRenderer shaper;
    SpriterDelegateBatch pfxBatch;

    @Override
    public void init() {
        M = this;
        stage = new Stage(new FitViewport(Const.UI_W, Const.UI_H));
        stage.setActionsRequestRendering(false);

        music = Quarry.Q.assets.get("music/Impact Prelude" + Const.MUSIC_FORMAT, Music.class);

        shaper = new ShapeRenderer();
        shaper.setAutoShapeType(true);

        spriter = new DepthSpriter(Quarry.Q.atlas.getTextures().first(), 8191);
        pfxBatch = new SpriterDelegateBatch(spriter);

        initUI();
    }

    @Override
    public void show(Window w) {
        stage.addActor(w);
        w.addAction(sequence(alpha(0), fadeIn(0.15f, Interpolation.fade)));
    }

    @Override
    public void hide(Window w) {
        w.addAction(sequence(fadeOut(0.15f, Interpolation.fade), Actions.removeActor()));
    }

    @Override
    public void show() {
        super.show();
        tutorial.setVisible(!Quarry.Q.prefs.getBoolean("tutorial", true));
        Quarry.Q.sound.playMusic(music, true);
    }

    @Override
    public void dispose() {
        super.dispose();
        layers.get(0).dispose();
        music.dispose();
        spriter.dispose();
        shaper.dispose();
    }

    protected void initUI() {
        stage.getActors().clear();

        Table t = Util.lml("main-menu");
        t.setSize(Const.UI_W, Const.UI_H);
        stage.addActor(t);

        confirm = Util.lml("confirm");
        toast = Util.lml("toast");
        alert = Util.lml("alert");
        prompt = Util.lml("prompt");
        seedPrompt = Util.lml("seed-prompt");
        stage.addActor(toast);

        final ImageButton de = Util.id("lang");
        de.setChecked(Quarry.Q.i18n.getLocale().getLanguage().equals("en"));
        de.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                alert.show(MainMenu.this, Quarry.Q.i18n.get("alert.language_change"), new Callback<Void>() {
                    @Override
                    public void call(Void data) {}
                });
                Quarry.Q.prefs.putBoolean("german", !de.isChecked()).flush();
            }
        });

        ((VolumeButton) Util.id("sfx")).initListener();
        ((VolumeButton) Util.id("music")).initListener();

        Util.id("new_game").addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ((TextButton) actor).setChecked(false);
                seedPrompt.show(MainMenu.this, Quarry.Q.i18n.get("prompt.seed"), Long.toString((long) (Math.random() * Long.MAX_VALUE)), new Response<String, Boolean>() {
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
                            fadeOut = true;
                            newGame = true;
                        }
                        return true;
                    }
                });
            }
        });
        Util.id("new_game").addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });

        Util.id("credits").addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.addScene(Credits.instance);
                ((Button) actor).setChecked(false);
            }
        });
        Util.id("credits").addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });

        tutorial = Util.id("tutorial");
        tutorial.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.prefs.putBoolean("tutorial", true).flush();
                fadeOut = true;
                newGame = true;
                ((TextButton) actor).setChecked(false);
            }
        });
        tutorial.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });

        final TextButton loadGame = Util.id("load_game");
        loadGame.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        loadGame.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, final Actor actor) {
                if (!((TextButton) actor).isChecked()) {
                    overlay.addAction(sequence(alpha(0, 0.25f, Interpolation.fade), visible(false)));
                    saves.addAction(sequence(moveTo(saves.getX(), -saves.getHeight(), .25f, Interpolation.fade), visible(false)));
                } else {
                    overlay.addAction(sequence(visible(true), alpha(0.4f, 0.25f, Interpolation.fade)));
                    saves.addAction(sequence(visible(true), moveTo(saves.getX(), -16, .25f, Interpolation.fade)));
                    savesList.clear();

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
                        final ImageButton del = new ImageButton(Quarry.Q.skin, "delete");
                        del.addListener(new ClickListener() {
                            @Override
                            public void clicked(InputEvent event, float x, float y) {
                                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                                confirm.show(MainMenu.this, Quarry.Q.i18n.format("confirm.delete_save", n),
                                        new Callback<Boolean>() {
                                            @Override
                                            public void call(Boolean data) {
                                                if (data != null && data) {
                                                    if (Quarry.Q.file("TheQuarry/saves/" + n + ".qsf", false).delete()) {
                                                        toast.show(Quarry.Q.i18n.get("toast.save_deleted"));
                                                        savesList.removeActor(entry);
                                                    } else {
                                                        toast.show(Quarry.Q.i18n.get("toast.save_not_deleted"));
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
                                toast.show(Quarry.Q.i18n.get("toast.loading_game"));
                                Quarry.Q.threadPool.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        Game.G.load(n, new Callback<Object>() {
                                            @Override
                                            public void call(Object data) {
                                                modalOverlay.addAction(sequence(alpha(0, 0.25f, Interpolation.fade), visible(false)));
                                                if (data instanceof Boolean && (Boolean) data == true) {
                                                    Game.G.ui.pauseButton.setChecked(true);
                                                    Game.G.setPaused(true);

                                                    ((TextButton) actor).setChecked(false);
                                                    fadeOut = true;
                                                    newGame = false;
                                                } else if (data instanceof Boolean && (Boolean) data == false) {
                                                    toast.show(Quarry.Q.i18n.get("toast.game_not_loaded"));
                                                } else if (data instanceof Exception) {
                                                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, data);
                                                    toast.show(Quarry.Q.i18n.get("toast.game_not_loaded"));
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
                                i = new Image(Quarry.Q.skin.getDrawable("thumb"));
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
                            details.add(new Label(data.String("name", Quarry.Q.i18n.get("menu.no_save_name")), Quarry.Q.skin));
                            details.row();

                            long playtime = data.Long("playTime", 0) / 1000;
                            Label l2 = new Label(String.format(Quarry.Q.i18n.getLocale(), "%s\n%s - %d:%02d:%02dh %s\nSeed: %d", n, GameUi.dateFormat.format(fh.lastModified()),
                                    playtime / 3600, (playtime % 3600) / 60, playtime % 60, Quarry.Q.i18n.get("ui.played"), data.Long("seed", 0)), Quarry.Q.skin);
                            l2.setColor(Color.LIGHT_GRAY);
                            l2.setFontScale(0.75f);
                            details.add(l2).padTop(-5);
                            entry.add(details).expandX().left().top().space(10);

                            entry.add(del).right().spaceRight(20);
                            data.free();

                            GameUi.sep(entry).colspan(99).fillX();
                            savesList.addActor(entry);
                        } catch (Exception e) {
                            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                        }

                    }
                    saves.setScrollY(0);
                    saves.toFront();

                }

            }
        });

        final TextButton selectExternal = Util.id("select_external");
        selectExternal.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        selectExternal.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.pi.message(Const.MSG_SELECT_ROOT, null);
                ((TextButton) event.getListenerActor()).setChecked(false);
            }
        });
        selectExternal.setVisible(Quarry.Q.newAndroid);

        if (Gdx.app.getType() == ApplicationType.iOS) {
            Util.id("quit_game").setVisible(false);
        } else {
            Util.id("quit_game").addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    System.exit(0); // kill app
                }
            });
            Util.id("quit_game").addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Quarry.Q.sound.play(Quarry.Q.clickSfx);
                }
            });
        }
        Util.lml("saves");

        overlay = Util.id("overlay");
        overlay.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                loadGame.setChecked(false);
            }
        });
        stage.addActor(overlay);

        saves = Util.id("saves");
        ScrollPaneStyle sps = new ScrollPaneStyle(saves.getStyle());
        sps.background = GameUi.getDrawable(Quarry.Q.skin, "panel_metalDark", 24, 24, 24, 24);
        saves.setStyle(sps);

        savesList = Util.id("saves_cont");
        savesList.grow();
        savesList.setRound(false);

        saves.setVisible(true);
        saves.setPosition(-16, -saves.getHeight());

        stage.addActor(saves);

        modalOverlay = Util.id("modal_overlay");
        stage.addActor(modalOverlay);

        cam = new OrthographicCamera(Const.W, Const.H);
        fboCam = new OrthographicCamera(Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE);
        fboCam.position.set(0.5f * Const.CHUNK_FULL_SIZE, 0.5f * Const.CHUNK_FULL_SIZE, fboCam.near);
        fboCam.update();

        Label l = new Label(Quarry.Q.version, Quarry.Q.skin);
        l.setFontScale(0.75f);
        l.setWidth(Const.UI_W);
        l.setAlignment(Align.center);
        l.setX(0);
        l.setY(5);
        stage.addActor(l);

        batch = new SpriteBatch();
        try {
            CompoundTag meta = NBT.read(new BufferedInputStream(Gdx.files.internal("main-menu.qmf").read()), CompressionType.Fast);
            CompoundTag data = NBT.read(new BufferedInputStream(Gdx.files.internal("main-menu.qsf").read()), CompressionType.Fast);
            int build = meta.Int("build", 0);
            LoadingCompat.instance.upgrade(data, build);

            layers = new Array<>();
            layers.add(Layer.load(0, (CompoundTag) data.List("Map", TagType.Compound).data.first()));
            layers.get(0).postAllLayersLoad(true);
            layers.get(0).postAllLayersLoad(false);
            layers.get(0).fake = true;
            data.free();
            meta.free();

            cam.position.set(layers.get(0).width * Const.TILE_SIZE * 0.5f, layers.get(0).height * Const.TILE_SIZE * 0.5f, 0);
            cam.zoom = (layers.get(0).height * 0.7f * Const.TILE_SIZE) / Math.max(Const.W, Const.H);
            cam.update();

        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    public void draw() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
        batch.begin();
        layers.get(0).draw(cam, fboCam, stage.getViewport(), batch, spriter, shaper, pfxBatch);
        batch.end();

        if (alpha == 1) stage.draw();
        else if (alpha != 0) {
            Camera camera = stage.getViewport().getCamera();
            camera.update();

            if (!stage.getRoot().isVisible()) return;

            Batch batch = stage.getBatch();
            if (batch != null) {
                batch.setProjectionMatrix(camera.combined);
                batch.begin();
                stage.getRoot().draw(batch, alpha);

                batch.end();
            }

            // if alpha != 1 we can't draw debug, thx libgdx private methods
        }
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);

        layers.get(0).update(deltaTime, 1);

        if (fadeOut && !Quarry.Q.hasScene(Game.G)) {
            Quarry.Q.addSceneBelow(Game.G);
            if (newGame) Game.G.newGame();
        }

        if (fadeOut && alpha == 0) {
            Quarry.Q.dropScene(this);
        }
    }

    @Override
    public Stage getStage() {
        return stage;
    }
}
