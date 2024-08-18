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
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class BarrelDrainer extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.BarrelDrainer, 2,
            2, "barreldrainer",
            new Items(ItemType.SteelTube, 10, ItemType.CopperTube, 10, ItemType.BronzeIngot, 30, ItemType.MachineFrame, 5, ItemType.Dynamo, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(1f, "drain", 250)
                            .input(new Amount(ItemType.WaterBarrel, 1))
                            .output(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.Water, 8000)));
                    add(new Recipe(1f, "drain", 250)
                            .input(new Amount(ItemType.CrudeOilBarrel, 1))
                            .output(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.CrudeOil, 8000)));
                    add(new Recipe(1f, "drain", 250)
                            .input(new Amount(ItemType.RefinedOilBarrel, 1))
                            .output(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.RefinedOil, 8000)));
                    add(new Recipe(1f, "drain", 250)
                            .input(new Amount(ItemType.LubricantBarrel, 1))
                            .output(new Amount(ItemType.EmptyBarrel, 1), new Amount(ItemType.Lubricant, 8000)));
                    add(new Recipe(1f, "tank", 550)
                            .input(new Amount(ItemType.GasTank, 1))
                            .output(new Amount(ItemType.EmptyTank, 1), new Amount(ItemType.NaturalGas, 12000)));
                    add(new Recipe(1f, "tank", 550)
                            .input(new Amount(ItemType.PressurizedSteamTank, 1))
                            .output(new Amount(ItemType.EmptyTank, 1), new Amount(ItemType.PressurizedSteam, 12000)));
                }
            }, new Sfx("compactor" + Const.SFX_FORMAT, 1.5f),
            true,
            new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType.WaterBarrel, ItemType.CrudeOilBarrel, ItemType.RefinedOilBarrel,
                    ItemType.LubricantBarrel, ItemType.GasTank, ItemType.PressurizedSteamTank)),
            new Dock(1, 0, Direction.East, DockType.ItemOut),
            new Dock(1, 0, Direction.South, DockType.FluidOut), new Dock(0, 1, Direction.North, DockType.Power))
                    .sciences(ScienceType.AdvancedTransport);

    public BarrelDrainer(int x, int y) {
        super(x, y, classSchema);
        ((CTank) outputInventories[1]).setPumpOutDelay(0).setMaxOutput(1000);
    }
}
