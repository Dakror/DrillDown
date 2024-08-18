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

package de.dakror.quarry;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.I18NBundle;
import com.github.czyzby.lml.parser.LmlParser;
import com.github.czyzby.lml.parser.impl.DefaultLmlSyntax;
import com.github.czyzby.lml.util.Lml;
import com.github.czyzby.lml.util.LmlParserBuilder;

import de.dakror.common.libgdx.GameBase;
import de.dakror.common.libgdx.I18NBundleDelegate;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.audio.SoundManager;
import de.dakror.common.libgdx.ui.Scene;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.LoadingScreen;
import de.dakror.quarry.scenes.MainMenu;
import de.dakror.gen.CustomTagRegistrator;

/**
 * @author Maximilian Stark | Dakror
 */
public class Quarry extends GameBase implements PlatformInterface {

    public static Quarry Q;

    public BitmapFont font;
    public Skin skin;
    public LmlParser lml;
    public TextureAtlas atlas;
    public I18NBundleDelegate i18n;
    public Preferences prefs;
    public int[] safeInsets;

    public FrameBuffer[] chunkFBOs;

    public AssetManager assets;

    public SoundManager sound;

    public final boolean fullVersion;
    public final int versionNumber;
    public final String version;

    public boolean newAndroid;

    private int filePermissionState;

    public Sound clickSfx;

    public ExecutorService threadPool = Executors.newSingleThreadExecutor();//newCachedThreadPool();

    public Quarry(PlatformInterface pi, boolean fullVersion, int versionNumber, String version, boolean desktop,
            boolean newAndroid, WindowMode mode) {
        super(mode, desktop, pi);
        this.fullVersion = fullVersion;
        this.versionNumber = versionNumber;
        this.version = version;
        this.newAndroid = newAndroid;
    }

    public boolean isVersion() {
        return versionNumber > 1 && !version.equals("debug");
    }

    @Override
    public void create() {
        super.create();

        safeInsets = (int[]) pi.message(Const.MSG_PADDING, null);

        if (desktop) {
            float ratio = Math.max(1920f / Gdx.graphics.getWidth(), 1080f / Gdx.graphics.getHeight());
            Const.W = Gdx.graphics.getWidth() * ratio;
            Const.H = Gdx.graphics.getHeight() * ratio;

            Const.UI_H = Const.H * 1.5f;
            Const.UI_W = Const.W * 1.5f;
        } else {
            float ratio = Math.max(1080f / Gdx.graphics.getWidth(), 1920f / Gdx.graphics.getHeight());
            Const.W = Gdx.graphics.getWidth() * ratio;
            Const.H = Gdx.graphics.getHeight() * ratio;

            Const.UI_H = Const.H * 0.75f;
            Const.UI_W = Const.W * 0.75f;
        }

        if (!isVersion()) {
            Gdx.app.setLogLevel(Application.LOG_DEBUG);
        }

        if (Gdx.app.getType() == ApplicationType.iOS) {
            Const.SFX_FORMAT = ".wav";
            Const.MUSIC_FORMAT = ".mp3";
        }

        Q = this;

        prefs = Gdx.app.getPreferences("TheQuarry");

        i18n = new I18NBundleDelegate(I18NBundle.createBundle(Gdx.files.internal("i18n/TheQuarry"),
                prefs.getBoolean("german", false) ? Locale.GERMAN : Locale.ENGLISH));

        // backwards compat
        try {
            prefs.getFloat("sound", 1f);
        } catch (Exception e) {
            prefs.putFloat("sound", 1f).flush();
        }

        try {
            prefs.getFloat("music", 1f);
        } catch (Exception e) {
            prefs.putFloat("music", 1f).flush();
        }
        sound = new SoundManager(prefs.getFloat("sound", 1f), prefs.getFloat("music", 1f), 2.0f);
        sound.setPlaySound(sound.getSoundVolume() > 0);
        sound.setPlayMusic(sound.getMusicVolume() > 0);

        Scene s = new LoadingScreen();
        s.init();
        addScene(s);
    }

    @Override
    public void pause() {
        // wait for saving to complete until pause is allowed
        try {
            threadPool.shutdown();
            threadPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        threadPool = Executors.newSingleThreadExecutor();
        super.pause();
        sound.pauseMusic();
    }

    @Override
    public void resume() {
        super.resume();
        sound.resumeMusic();
    }

    @Override
    public void update() {
        super.update();
        sound.update(updateRate);
    }

    public void loadingFinished() {
        clickSfx = assets.get("sfx/click3" + Const.SFX_FORMAT, Sound.class);

        long t = System.currentTimeMillis();

        LmlParserBuilder b = Lml.parser(skin)
                .i18nBundle(i18n.getBundle())
                .syntax(new DefaultLmlSyntax() {
                    @Override
                    public char getIdSeparatorMarker() {
                        return '~';
                    }
                });

        CustomTagRegistrator.addCustomTags(b);
        lml = b.build();

        lml.setStrict(false);

        lml.getData().addArgument("UI_W", Const.UI_W);
        lml.getData().addArgument("UI_H", Const.UI_H);
        lml.getData().addArgument("BUILD_RING_ITEM_SIZE", Const.BUILD_RING_ITEM_SIZE);

        System.out.println("LML took " + (System.currentTimeMillis() - t) + " ms");

        t = System.currentTimeMillis();

        Game g = new Game();
        g.init();
        Quarry.Q.pi.message(-1230, this);

        System.out.println("Game init took " + (System.currentTimeMillis() - t) + " ms");

        // init fbos
        chunkFBOs = new FrameBuffer[(Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE)
                * (Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE)];

        int chunksH = (int) Math.ceil(Const.DEFAULT_LAYER_SIZE / Const.CHUNK_SIZE);
        for (int i = 0; i < Const.DEFAULT_LAYER_SIZE; i += Const.CHUNK_SIZE) {
            for (int j = 0; j < Const.DEFAULT_LAYER_SIZE; j += Const.CHUNK_SIZE) {
                chunkFBOs[i / Const.CHUNK_SIZE * chunksH + j / Const.CHUNK_SIZE] = new FrameBuffer(Format.RGB888,
                        Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE, false);
            }
        }

        Scene s = new MainMenu();
        s.init();
        addSceneBelow(s);

        Scene q = dropScene();
        q.dispose();
    }

    public FileHandle[] listFiles() {
        FileHandle[] local = Gdx.files.local("TheQuarry/saves/").list(".qsf");
        FileHandle[] external = Gdx.files.external("TheQuarry/saves/").list(".qsf");

        FileHandle[] all = new FileHandle[local.length + external.length];
        System.arraycopy(local, 0, all, 0, local.length);
        System.arraycopy(external, 0, all, local.length, external.length);

        return all;
    }

    public FileHandle file(String text, boolean write) {
        if (hasFilePerm()) {
            FileHandle local = Gdx.files.local(text);

            // if folder creation fails, go internal
            FileHandle fh = Gdx.files.external("TheQuarry/saves");
            fh.mkdirs();
            if (!fh.exists()) {
                return local;
            }

            FileHandle external = Gdx.files.external(text);

            if (write) {
                return external;
            } else if (local.exists()) {
                return local.lastModified() > external.lastModified() ? local : external;
            } else {
                return external;
            }
        } else
            return Gdx.files.local(text);
    }

    private boolean hasFilePerm() {
        if (newAndroid)
            return true;

        filePermissionState = -1;
        if ((Boolean) pi.message(Const.MSG_FILE_PERMISSION, null) == false) {
            return false;
        } else {
            filePermissionState = 1;
        }

        return filePermissionState == 1;
    }

    @Override
    public Object message(int messageCode, Object payload) {
        switch (messageCode) {
            case Const.MSG_FILE_PERMISSION: {
                filePermissionState = (Boolean) payload ? 1 : 0;
                break;
            }
        }

        return null;
    }
}
