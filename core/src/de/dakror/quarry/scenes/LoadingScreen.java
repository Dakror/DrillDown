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
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.SkinLoader.SkinParameter;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.viewport.FitViewport;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.ui.Scene;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import net.spookygames.gdx.sfx.SfxMusic;
import net.spookygames.gdx.sfx.SfxMusicLoader;
import net.spookygames.gdx.sfx.SfxSound;
import net.spookygames.gdx.sfx.SfxSoundLoader;

/**
 * @author Maximilian Stark | Dakror
 */
public class LoadingScreen extends Scene {
    long l;

    Label label;

    TextureRegion progress;
    TextureRegion bg;

    float prog;
    float visualProg;
    float oldVisualProg;
    float interp;

    boolean finishingUp = false;

    @Override
    public void init() {
        FileHandleResolver resolver = new InternalFileHandleResolver();
        AssetManager assets = new AssetManager(resolver);
        Quarry.Q.assets = assets;
        assets.setLoader(SfxMusic.class, new SfxMusicLoader(resolver));
        assets.setLoader(SfxSound.class, new SfxSoundLoader(resolver));

        assets.load("tex.atlas", TextureAtlas.class);

        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("Roboto-Medium.ttf"));
        ObjectMap<String, Object> fontMap = new ObjectMap<String, Object>();
        fontMap.put("small-font", createFont(generator, 24));
        fontMap.put("default-font", createFont(generator, 32));
        fontMap.put("big-font", createFont(generator, 48));
        SkinParameter param = new SkinParameter("tex.atlas", fontMap);

        assets.load("skin.json", Skin.class, param);

        assets.finishLoading();

        Quarry.Q.atlas = assets.get("tex.atlas");


        Quarry.Q.skin = assets.get("skin.json");
        Quarry.Q.font = Quarry.Q.skin.getFont("default-font");

        Quarry.Q.font.getData().markupEnabled = true;
        Quarry.Q.font.setFixedWidthGlyphs("0123456789-+");
        Quarry.Q.skin.getFont("small-font").setFixedWidthGlyphs("0123456789");

        progress = Quarry.Q.atlas.findRegion("structure_conveyor_we");
        bg = Quarry.Q.atlas.findRegion("button");
        Quarry.Q.mouseTex = Quarry.Q.atlas.findRegion("mouse");

        ///////////////////////////

        stage = new Stage(new FitViewport(Const.UI_W, Const.UI_H));
        stage.setActionsRequestRendering(false);
        label = new Label(Quarry.Q.i18n.get("loading.sounds"), Quarry.Q.skin);
        label.setAlignment(Align.center);
        Table t = new Table();
        t.setBackground(Quarry.Q.skin.getTiledDrawable("tile_stone"));
        t.add(label).grow();
        t.setSize(Const.UI_W, Const.UI_H);
        stage.addActor(t);

        ///////////////////////////

        assets.load("sfx/airpurifier" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/anchorportal" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/arcwelder" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/assembler" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/ballmill" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/bender" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/boiler" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/booster" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/carpenter" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/centrifuge" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/charcoalmound" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/column" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/compactor" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/condenser" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/crucible" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/devicefabricator" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/digitalstorage" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/drawer" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/excavator" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/furnace" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/injection" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/kiln" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/mason" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/mine" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/mixer" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/node" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/oilwell" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/polarizer" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/polymerizer" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/pump1" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/refinery" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/rockcrusher" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/rollingmachine" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/sawmill" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/science" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/shaftdrill" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/stacker" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/turbine" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/waterwheel" + Const.SFX_FORMAT, SfxSound.class);
        assets.load("sfx/woodcutter" + Const.SFX_FORMAT, SfxSound.class);

        assets.load("sfx/click3" + Const.SFX_FORMAT, Sound.class);
        assets.load("sfx/build" + Const.SFX_FORMAT, Sound.class);
        assets.load("sfx/destroy" + Const.SFX_FORMAT, Sound.class);
        assets.load("sfx/cable" + Const.SFX_FORMAT, Sound.class);

        assets.load("sfx/ambience_empty" + Const.SFX_FORMAT, Sound.class);
        assets.load("sfx/ambience_base" + Const.SFX_FORMAT, Sound.class);
        assets.load("sfx/ambience_heavy" + Const.SFX_FORMAT, Sound.class);

        assets.load("music/Fading_into_the_Dream" + Const.MUSIC_FORMAT, Music.class);
        assets.load("music/Impact Prelude" + Const.MUSIC_FORMAT, Music.class);

        l = System.currentTimeMillis();
    }

    protected BitmapFont createFont(FreeTypeFontGenerator gen, float dp) {
        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.magFilter = TextureFilter.Linear;
        param.minFilter = TextureFilter.Linear;
        if (Quarry.Q.desktop) {
            param.size = (int) (dp * (float) Quarry.Q.pi.message(Const.MSG_DPI, null));
        } else {
            param.size = (int) (dp * (float) Quarry.Q.pi.message(Const.MSG_DPI, null));
        }

        BitmapFont font = gen.generateFont(param);
        font.getData().markupEnabled = true;
        return font;
    }

    @Override
    public void update(double deltaTime) {
        super.update(deltaTime);

        float p = Quarry.Q.assets.getProgress();

        if (p != prog) {
            oldVisualProg = visualProg;
            interp = 0;
            prog = p;
        }

        if (visualProg != prog) {
            if (interp >= 1) {
                visualProg = prog;
                interp = 0;
            } else {
                visualProg = oldVisualProg + (prog - oldVisualProg) * interp;
                interp += deltaTime / 0.1f;
            }
        }

        if (Quarry.Q.assets.getProgress() > 0.9f) {
            label.setText(Quarry.Q.i18n.get("loading.buildings"));
        }

        try {
            if (!finishingUp && Quarry.Q.assets.update() && visualProg == 1 && !fadeOut) {
                System.out.println("Asset loading took " + (System.currentTimeMillis() - l) + " ms");
                finishingUp = true;
                Quarry.Q.loadingFinished();
            }
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    public void draw() {
        Gdx.gl.glClearColor(127 / 256f, 127 / 256f, 127 / 256f, 1);
        super.draw();

        stage.getBatch().begin();

        stage.getBatch().setColor(0, 0, 0, 0.5f);

        int x = (int) ((Const.UI_W - 600) / 2);
        stage.getBatch().draw(bg, x, Const.UI_H / 4, 600, 40);
        stage.getBatch().setColor(1, 1, 1, 1);

        float p = 600 * visualProg;
        for (int i = 0; i < (int) (p) / 64; i++)
            stage.getBatch().draw(progress, x + i * 64, Const.UI_H / 4 - 12);

        float len = (int) (p / 64) * 64;
        stage.getBatch().draw(progress.getTexture(), x + len, Const.UI_H / 4 - 12, p - len, 64, progress.getU(), progress.getV2(), (progress.getU2() - progress.getU()) * (p - len) / 64 + progress.getU(), progress.getV());
        stage.getBatch().end();
    }
}
