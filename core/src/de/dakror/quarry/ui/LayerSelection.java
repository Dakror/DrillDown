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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.SnapshotArray;

import de.dakror.common.libgdx.ChangeNotifier.Event;
import de.dakror.common.libgdx.ChangeNotifier.Listener;
import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */

@LmlTag(tagName = "layerselection")
public class LayerSelection extends Table implements CustomTag, Listener<Layer> {
    Label label;
    ImageButton up, up10, down, down10;
    VerticalGroup minerals;

    public LayerSelection(Skin skin, String style) {
        super(skin);
    }

    @Override
    public void postInit() {
        label = Util.id("label");
        up = Util.id("up");
        up10 = Util.id("up10");
        down = Util.id("down");
        down10 = Util.id("down10");

        minerals = Util.id("minerals");
        // init minerals
        SnapshotArray<Actor> mins = minerals.getChildren();
        mins.get(0).setUserObject(ItemType.Dirt);
        mins.get(1).setUserObject(ItemType.Clay);
        mins.get(2).setUserObject(ItemType.CoalOre);
        mins.get(3).setUserObject(ItemType.IronOre);
        mins.get(4).setUserObject(ItemType.CopperOre);
        mins.get(5).setUserObject(ItemType.TinOre);
        mins.get(6).setUserObject(ItemType.CrudeOil);
        for (Actor a : mins)
            a.addListener(new TextTooltip(((ItemType) a.getUserObject()).title, getSkin()));

        up.setDisabled(true);

        Image img = up.getImage();
        up.getImageCell().size(48);
        img.setOrigin(24, 24);
        img.setRotation(90);
        up.getImage().invalidateHierarchy();

        up10.setDisabled(true);

        img = up10.getImage();
        up10.getImageCell().size(48);
        img.setOrigin(24, 24);
        img.setRotation(90);
        up10.getImage().invalidateHierarchy();

        img = down.getImage();
        down.getImageCell().size(48);
        img.setOrigin(24, 24);
        img.setRotation(-90);
        down.getImage().invalidateHierarchy();

        img = down10.getImage();
        down10.getImageCell().size(48);
        img.setOrigin(24, 24);
        img.setRotation(-90);
        down10.getImage().invalidateHierarchy();

        up.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.changeLayer(-1);
            }
        });
        up10.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.changeLayer(-10);
            }
        });
        down.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.changeLayer(1);
            }
        });
        down10.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.changeLayer(10);
            }
        });

        setVisible(false);
        setPosition(Const.UI_W - getWidth() + 40, (Const.UI_H - getHeight()) / 2);
    }

    public void onScienceChange() {
        minerals.getChildren().get(5).setVisible(Game.G.hasScience(ScienceType.MineExpansion));
        minerals.getChildren().get(6).setVisible(Game.G.hasScience(ScienceType.OilProcessing));
    }

    @Override
    public void onChangeEvent(Event<Layer> event) {
        setVisible(Game.G.getLayerCount() > 1);
        label.setText((0 - Game.G.layer.getIndex()) + "");
        up.setDisabled(Game.G.layer.getIndex() == 0);
        up10.setDisabled(Game.G.layer.getIndex() - 10 < 0);
        down10.setDisabled(Game.G.layer.getIndex() + 10 > Game.G.getLayerCount() - 1);
        down.setDisabled(Game.G.layer.getIndex() == Game.G.getLayerCount() - 1);

        Game.G.camControl.updateActiveElementPlaceable();

        // update minerals
        for (Actor a : minerals.getChildren()) {
            Image i = (Image) a;
            ItemType t = (ItemType) a.getUserObject();
            if (Game.G.layer.minerals.contains(t)) {
                i.setDrawable(t.drawable);
            } else {
                i.setDrawable(ItemType.Nil.drawable);
            }
        }

    }
}
