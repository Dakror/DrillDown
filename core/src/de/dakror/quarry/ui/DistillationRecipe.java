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
import de.dakror.quarry.structure.DistillationColumn;

/**
 * @author Maximilian Stark | Dakror
 */
public class DistillationRecipe extends Table {
    public DistillationRecipe(Skin skin) {
        setBackground(skin.getDrawable("panel_metal"));
        pad(20, 14, 20, 14);

        Table inputs = new Table();
        inputs.defaults().left();

        inputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.IntermediateOilToColumn.drawable,
                GameUi.formatResourceAmount(DistillationColumn.recipe.inputOil / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(DistillationColumn.recipe.inputOil / 1000f) + "L " + ItemType.IntermediateOilToColumn.title)).expandX().center();
        add(inputs).center();

        row();
        Table t = new Table();

        t.add(GameUi.createResourceTable(25, skin, skin.getDrawable("icon_power"), GameUi.formatPowerAmount(DistillationColumn.recipe.power * 60) + "/s", "small-font"));

        Image arrow = new Image(skin.getDrawable("symb_rightarrow"));
        arrow.setOrigin(12, 7);
        arrow.setRotation(-90);
        t.add(arrow).size(24, 15).pad(10);

        t.add(new IconLabel(25, skin, skin.getDrawable("icon_time"), GameUi.formatResourceAmount(DistillationColumn.recipe.workingTime) + "s", "small-font"));

        add(t);

        row();

        Table outputs = new Table();
        outputs.defaults().left().space(10);

        outputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.IntermediateOilToRefinery.drawable,
                GameUi.formatResourceAmount(DistillationColumn.recipe.outputOil / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(DistillationColumn.recipe.outputOil / 1000f) + "L " + ItemType.IntermediateOilToRefinery.title));

        outputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.Lubricant.drawable,
                GameUi.formatResourceAmount(DistillationColumn.recipe.outputLubricant / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(DistillationColumn.recipe.outputLubricant / 1000f) + "L " + ItemType.Lubricant.title));

        outputs.add(new IconLabel(GameUi.TOOLTIP_ICON_SIZE, skin, ItemType.NaturalGas.drawable,
                GameUi.formatResourceAmount(DistillationColumn.recipe.outputGas / 1000f) + "L")
                        .tooltip(skin, GameUi.formatResourceAmount(DistillationColumn.recipe.outputGas / 1000f) + "L " + ItemType.NaturalGas.title));

        add(outputs).center().grow();
    }
}
