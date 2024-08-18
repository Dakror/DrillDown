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
public class ArcWelder extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.ArcWelder, 3,
            3, "arcwelder",
            new Items(ItemType.StoneBrick, 90, ItemType.BronzePlate, 100, ItemType.SteelPlate, 100, ItemType.SteelWire, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(8f, "machineframe", 500)
                            .input(new Amount(ItemType.SteelPlate, 6), new Amount(ItemType.CopperWire, 40))
                            .output(new Amount(ItemType.MachineFrame, 1)));
                    add(new Recipe(12f, "barrel", 800)
                            .input(new Amount(ItemType.TinPlate, 25), new Amount(ItemType.IronPlate, 5))
                            .output(new Amount(ItemType.EmptyBarrel, 1))
                            .science(ScienceType.AdvancedTransport));
                    add(new Recipe(15f, "tank", 1400)
                            .input(new Amount(ItemType.SteelPlate, 10), new Amount(ItemType.CopperPlate, 10))
                            .output(new Amount(ItemType.EmptyTank, 1))
                            .science(ScienceType.AdvancedTransport));
                    add(new Recipe(25f, "platestack", 1500)
                            .input(new Amount(ItemType.SteelPlate, 2), new Amount(ItemType.TitaniumPlate, 1))
                            .output(new Amount(ItemType.PlateStack, 1))
                            .science(ScienceType.HighTech, ScienceType.MineralExtraction));
                    add(new Recipe(40f, "advancedmachineframe", 4000)
                            .input(new Amount(ItemType.HardenedSteelPlate, 6), new Amount(ItemType.SteelWire, 40))
                            .output(new Amount(ItemType.AdvancedMachineFrame, 1))
                            .science(ScienceType.HighTech, ScienceType.MineralExtraction));
                }
            }, new Sfx("arcwelder" + Const.SFX_FORMAT),
            false,
            new Dock(1, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 1, Direction.West, DockType.Power),
            new Dock(0, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.SteelPlate, ItemType.TinPlate, ItemType.HardenedSteelPlate)),
            new Dock(2, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.CopperWire, ItemType.CopperPlate, ItemType.SteelWire, ItemType.TitaniumPlate, ItemType.IronPlate)))
                    .sciences(ScienceType.Electricity);

    public ArcWelder(int x, int y) {
        super(x, y, classSchema);
    }
}
