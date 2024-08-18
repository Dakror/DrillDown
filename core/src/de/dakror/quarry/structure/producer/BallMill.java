/*******************************************************************************
 * Copyright 2017 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure.producer;

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class BallMill extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.BallMill, 4,
            1,
            "ballmill",
            new Items(ItemType.Scaffolding, 10, ItemType.IronPlate, 10, ItemType.IronIngot, 6),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(10f, "sand"/*, 80f*/)
                            .input(new Amount(ItemType.StoneGravel, 1))
                            .output(new Amount(ItemType.Sand, 2)));
                    add(new Recipe(15f, "ore"/*, 100f*/)
                            .input(new Amount(ItemType._Gravel, 1))
                            .output(new ParameterizedAmount(ItemType._Dust, 3, 0)));
                    add(new Recipe(8f, "stonedust"/*, 80f*/)
                            .input(new Amount(ItemType.Sand, 1))
                            .output(new Amount(ItemType.StoneDust, 2)));
                    add(new Recipe(12f, "coal")
                            .input(new Amount(ItemType.CoalOre, 1))
                            .output(new Amount(ItemType.CoalDust, 3)));
                }
            },
            new Sfx("ballmill" + Const.SFX_FORMAT),
            true, /*new Dock(1, 0, Direction.North, DockType.PowerIn),*/
            new Dock(0, 0, Direction.West, DockType.ItemOut), new Dock(3, 0, Direction.North, DockType.ItemIn, new DockFilter(ItemType._Gravel, ItemType.CoalOre, ItemType.Sand)))
                    .sciences(/*ScienceType.Electricity, */ScienceType.OreProcessing);

    public BallMill(int x, int y) {
        super(x, y, classSchema);
    }
}
