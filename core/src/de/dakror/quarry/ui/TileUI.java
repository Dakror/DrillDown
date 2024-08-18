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

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.common.libgdx.lml.LmlTag;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */

@LmlTag(tagName = "tileui")
public class TileUI extends Table implements CustomTag {
    Label label;

    final String dust = Quarry.Q.i18n.get("tile.dust");

    public TileUI(Skin skin, String style) {
        super(skin);
        setBackground(skin.getDrawable("panel_boltsBlue"));
    }

    @Override
    public void postInit() {
        label = Util.id("label");

        setPosition((Const.UI_W - getWidth()) / 2, 130);
        setVisible(false);
    }

    public void setText(TileType t) {
        label.setText(t == null ? dust : t.name);
    }

    public void show() {
        setVisible(true);
    }

    public void hide() {
        Game.G.resetActiveTile();
        setVisible(false);
    }
}
