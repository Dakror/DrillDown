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
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class FillingMachine extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.FillingMachine, 4,
            4, "fillingmachine",
            new Items(ItemType.MachineFrame, 10, ItemType.SteelIngot, 20, ItemType.Brick, 90, ItemType.CopperTube, 20, ItemType.Dynamo, 5),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(3f, "barrel", 167)
                            .input(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.Water, 8000))
                            .output(new Amount(ItemType.WaterBarrel, 1)));
                    add(new Recipe(3f, "barrel", 167)
                            .input(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.CrudeOil, 8000))
                            .output(new Amount(ItemType.CrudeOilBarrel, 1)));
                    add(new Recipe(3f, "barrel", 167)
                            .input(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.RefinedOil, 8000))
                            .output(new Amount(ItemType.RefinedOilBarrel, 1)));
                    add(new Recipe(3f, "barrel", 167)
                            .input(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.Lubricant, 8000))
                            .output(new Amount(ItemType.LubricantBarrel, 1)));
                    add(new Recipe(3f, "tank", 334)
                            .input(new Amount(ItemType.EmptyTank, 1), new Amount(ItemType.NaturalGas, 12000))
                            .output(new Amount(ItemType.GasTank, 1)));
                    add(new Recipe(4f, "tank", 500)
                            .input(new Amount(ItemType.EmptyTank, 1), new Amount(ItemType.Steam, 50000))
                            .output(new Amount(ItemType.PressurizedSteamTank, 1)));
                    add(new Recipe(3f, "tank", 334)
                            .input(new Amount(ItemType.EmptyTank, 1), new Amount(ItemType.PressurizedSteam, 12000))
                            .output(new Amount(ItemType.PressurizedSteamTank, 1)));
                }

                @Override
                public void validateRecipes(Schema schema) {
                    super.validateRecipes(schema);
                    fluidInputs.set(0, 60_000); // increase tank size
                }
            }, new Sfx("compactor" + Const.SFX_FORMAT, 0.7f),
            true,
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.EmptyBarrel, ItemType.EmptyTank)),
            new Dock(3, 3, Direction.East, DockType.FluidIn, new DockFilter(ItemType.Water, ItemType.CrudeOil, ItemType.RefinedOil, ItemType.Lubricant,
                    ItemType.Steam, ItemType.NaturalGas, ItemType.PressurizedSteam)),
            new Dock(3, 0, Direction.East, DockType.ItemOut), new Dock(0, 0, Direction.West, DockType.Power))
                    .sciences(ScienceType.AdvancedTransport);

    public FillingMachine(int x, int y) {
        super(x, y, classSchema);
    }
}
