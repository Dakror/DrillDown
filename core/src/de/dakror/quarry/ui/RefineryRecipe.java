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

import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.Refinery;

/**
 * @author Maximilian Stark | Dakror
 */
public class RefineryRecipe extends Table {
    public RefineryRecipe(Skin skin) {
        setBackground(skin.getDrawable("panel_metal"));
        pad(20, 14, 20, 14);
        Table left = new Table();

        left.defaults();

        left.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.CrudeOil.drawable,
                GameUi.formatResourceAmount(Refinery.recipe.crudeInput / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(Refinery.recipe.crudeInput / 1000f) + "L " + ItemType.CrudeOil.title)).expandX().center();

        left.row();
        Table t = new Table();

        Image arrow = new Image(skin.getDrawable("symb_rightarrow"));
        arrow.setOrigin(12, 7);
        arrow.setRotation(-90);
        t.add(arrow).size(24, 15).padRight(10);

        t.add(new IconLabel(25, skin, skin.getDrawable("icon_time"), GameUi.formatResourceAmount(Refinery.recipe.crudeTime) + "s", "small-font"));

        left.add(t);

        left.row();

        Table outputs = new Table();
        outputs.defaults().left();

        outputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.IntermediateOilToColumn.drawable,
                GameUi.formatResourceAmount(Refinery.recipe.crudeOutput / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(Refinery.recipe.crudeOutput / 1000f) + "L " + ItemType.IntermediateOilToColumn.title));
        left.add(outputs).center();

        add(left).grow();

        add(GameUi.createResourceTable(25, skin, skin.getDrawable("icon_power"), GameUi.formatPowerAmount(Refinery.recipe.power * 60) + "/s", "small-font")).pad(0, 5, 0, 5);

        Table right = new Table();

        right.defaults();

        right.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.IntermediateOilToRefinery.drawable,
                GameUi.formatResourceAmount(Refinery.recipe.refInput / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(Refinery.recipe.refInput / 1000f) + "L " + ItemType.IntermediateOilToRefinery.title)).expandX().center();

        right.row();
        t = new Table();

        arrow = new Image(skin.getDrawable("symb_rightarrow"));
        arrow.setOrigin(12, 7);
        arrow.setRotation(-90);
        t.add(arrow).size(24, 15).padRight(10);

        t.add(new IconLabel(25, skin, skin.getDrawable("icon_time"), GameUi.formatResourceAmount(Refinery.recipe.refTime) + "s", "small-font"));

        right.add(t);

        right.row();

        outputs = new Table();
        outputs.defaults().left();

        outputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.RefinedOil.drawable,
                GameUi.formatResourceAmount(Refinery.recipe.refOutput / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(Refinery.recipe.refOutput / 1000f) + "L " + ItemType.RefinedOil.title));
        right.add(outputs).center();

        add(right).grow();
    }
}
