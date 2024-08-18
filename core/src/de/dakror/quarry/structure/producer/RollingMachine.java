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
public class RollingMachine extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.RollingMachine, 3,
            3,
            "rollingmachine",
            new Items(ItemType.Scaffolding, 10, ItemType.StoneBrick, 10, ItemType.SteelIngot, 20),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(20f, "plate")
                            .input(new Amount(ItemType._Ingot, 1))
                            .output(new ParameterizedAmount(ItemType._Plate, 1, 0)));
                    add(new Recipe(4f, "paper")
                            .input(new Amount(ItemType.Cellulose, 1))
                            .output(new Amount(ItemType.Paper, 1))
                            .science(ScienceType.Blueprints));
                    add(new Recipe(40f, "platestack")
                            .input(new Amount(ItemType.PlateStack, 1))
                            .output(new Amount(ItemType.HardenedSteelPlate, 1))
                            .science(ScienceType.HighTech, ScienceType.MineralExtraction));
                }
            }, new Sfx("rollingmachine" + Const.SFX_FORMAT),
            true,
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType._Ingot, ItemType.PlateStack, ItemType.Cellulose)))
                    .sciences(ScienceType.Metalworking);

    public RollingMachine(int x, int y) {
        super(x, y, classSchema);
    }
}
