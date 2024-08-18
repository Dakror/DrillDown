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
public class InjectionMolder extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(2, StructureType.InjectionMolder, 3, 3,
            "injectionmolder",
            new Items(ItemType.StoneBrick, 120, ItemType.MachineFrame, 40, ItemType.AdvancedMachineFrame, 2, ItemType.CopperIngot, 150, ItemType.SteelIngot, 100, ItemType.Rotor, 16),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(5f, "sleeve", 500)
                            .input(new Amount(ItemType.PlasticBeads, 30), new Amount(ItemType.SteelWire, 4))
                            .output(new Amount(ItemType.SteelCable, 1)));
                    add(new Recipe(34f, "casing", 800)
                            .input(new Amount(ItemType.PlasticBeads, 170), new Amount(ItemType.BronzeIngot, 1))
                            .output(new Amount(ItemType.PlasticCasing, 1)));
                }
            }, new Sfx("injection" + Const.SFX_FORMAT),
            true,
            new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType.PlasticBeads)),
            new Dock(1, 0, Direction.South, DockType.ItemIn, new DockFilter(ItemType.SteelWire, ItemType.BronzeIngot)),
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(1, 2, Direction.North, DockType.Power))
                    .sciences(ScienceType.PlasticMolding);

    public static final ProducerSchema classSchemaV104 = new ProducerSchema(1, StructureType.InjectionMolder, 3, 3,
            "injectionmolder",
            new Items(ItemType.StoneBrick, 120, ItemType.MachineFrame, 40, ItemType.AdvancedMachineFrame, 2, ItemType.CopperIngot, 150, ItemType.SteelIngot, 100, ItemType.Rotor, 16),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(5f, "sleeve", 500)
                            .input(new Amount(ItemType.PlasticBeads, 30), new Amount(ItemType.SteelWire, 4))
                            .output(new Amount(ItemType.SteelCable, 1)));
                    add(new Recipe(34f, "casing", 800)
                            .input(new Amount(ItemType.PlasticBeads, 170))
                            .output(new Amount(ItemType.PlasticCasing, 1)));
                }
            }, new Sfx("injection" + Const.SFX_FORMAT),
            true,
            new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType.PlasticBeads)),
            new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType.SteelWire)),
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(1, 2, Direction.North, DockType.Power))
                    .sciences(ScienceType.PlasticMolding);

    public static final ProducerSchema classSchemaV103 = new ProducerSchema(0, StructureType.InjectionMolder, 3, 3,
            "injectionmolder",
            new Items(ItemType.StoneBrick, 120, ItemType.MachineFrame, 40, ItemType.AdvancedMachineFrame, 2, ItemType.CopperIngot, 150, ItemType.SteelIngot, 100, ItemType.Rotor, 16),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(34f, "casing", 800)
                            .input(new Amount(ItemType.PlasticBeads, 170))
                            .output(new Amount(ItemType.PlasticCasing, 1)));
                }
            }, new Sfx("injection" + Const.SFX_FORMAT),
            true,
            new Dock(0, 1, Direction.West, DockType.ItemIn, new DockFilter(ItemType.PlasticBeads)),
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(1, 2, Direction.North, DockType.Power))
                    .sciences(ScienceType.PlasticMolding);

    public InjectionMolder(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchema, classSchemaV104, classSchemaV103));
    }

    public InjectionMolder(int x, int y) {
        super(x, y, classSchema);
    }
}
