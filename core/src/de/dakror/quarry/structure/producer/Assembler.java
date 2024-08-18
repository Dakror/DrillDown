package de.dakror.quarry.structure.producer;

import de.dakror.quarry.Const;

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
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Assembler extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(3, StructureType.Assembler, 6, 6,
            "assembler", new Items(ItemType.StoneBrick, 35, ItemType.SteelIngot, 50, ItemType.WoodPlank, 85, ItemType.Scaffolding, 60, ItemType.CopperIngot, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(28f, "dynamo")
                            .input(
                                    new Amount(ItemType.CopperWire, 40),
                                    new Amount(ItemType.Magnet, 3),
                                    new Amount(ItemType.SteelIngot, 4),
                                    new Amount(ItemType.CarbonBlock, 2))
                            .output(new Amount(ItemType.Dynamo, 1)));

                    add(new Recipe(25f, "rotor", 30)
                            .input(
                                    new Amount(ItemType.BronzePlate, 20),
                                    new Amount(ItemType.IronPlate, 40),
                                    new Amount(ItemType.SteelIngot, 25),
                                    new Amount(ItemType.SteelPlate, 30))
                            .output(new Amount(ItemType.Rotor, 1)));

                    add(new Recipe(20f, "pallet", 500)
                            .input(
                                    new Amount(ItemType.WoodPlank, 36),
                                    new Amount(ItemType.SteelWire, 12))
                            .output(new Amount(ItemType.Pallet, 1))
                            .science(ScienceType.AdvancedTransport));

                    add(new Recipe(8f, "blueprint", 100)
                            .input(
                                    new Amount(ItemType.Paper, 4),
                                    new Amount(ItemType.IronPlate, 2))
                            .output(new Amount(ItemType.Blueprint, 1))
                            .science(ScienceType.Blueprints));

                    add(new Recipe(5f, "dynamite", 400)
                            .input(
                                    new Amount(ItemType.Paper, 8),
                                    new Amount(ItemType.Gunpowder, 4),
                                    new Amount(ItemType.Sand, 14),
                                    new Amount(ItemType.Clay, 3))
                            .output(new Amount(ItemType.Dynamite, 1))
                            .science(ScienceType.MineralExtraction, ScienceType.Blueprints));

                    add(new Recipe(13f, "battery", 200)
                            .input(
                                    new Amount(ItemType.BronzePlate, 8),
                                    new Amount(ItemType.IronPlate, 8),
                                    new Amount(ItemType.SulfurDust, 24),
                                    new Amount(ItemType.PlasticCasing, 1))
                            .output(new Amount(ItemType.Battery, 1))
                            .science(ScienceType.MineralExtraction, ScienceType.OilProcessing));

                    add(new Recipe(45f, "turbine", 1200)
                            .input(
                                    new Amount(ItemType.Rotor, 4),
                                    new Amount(ItemType.Dynamo, 8),
                                    new Amount(ItemType.SteelTube, 25),
                                    new Amount(ItemType.SteelPlate, 50),
                                    new Amount(ItemType.Lubricant, 10_000))
                            .output(new Amount(ItemType.Turbine, 1))
                            .science(ScienceType.OilProcessing));
                }
            }, new Sfx("assembler" + Const.SFX_FORMAT),
            false,
            new Dock(5, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 5, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CopperWire, ItemType.BronzePlate, ItemType.Rotor, ItemType.Paper, ItemType.WoodPlank)),
            new Dock(5, 5, Direction.East, DockType.ItemIn, new DockFilter(ItemType.Magnet, ItemType.IronPlate, ItemType.Dynamo, ItemType.IronPlate, ItemType.Gunpowder, ItemType.SteelWire)),
            new Dock(5, 3, Direction.East, DockType.ItemIn, new DockFilter(ItemType.SteelIngot, ItemType.SulfurDust, ItemType.SteelTube, ItemType.Sand)),
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CarbonBlock, ItemType.PlasticCasing, ItemType.SteelPlate, ItemType.Clay)),
            new Dock(1, 5, Direction.North, DockType.Power), new Dock(4, 5, Direction.North, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.Electricity)
                    .flags(Flags.TextureAlwaysUpright);

    public static final ProducerSchema classSchemaV113 = new ProducerSchema(2, StructureType.Assembler, 6, 6,
            "assembler", new Items(ItemType.StoneBrick, 35, ItemType.SteelIngot, 50, ItemType.WoodPlank, 85, ItemType.Scaffolding, 60, ItemType.CopperIngot, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(28f, "dynamo")
                            .input(
                                    new Amount(ItemType.CopperWire, 40),
                                    new Amount(ItemType.Magnet, 3),
                                    new Amount(ItemType.SteelIngot, 4),
                                    new Amount(ItemType.CarbonBlock, 2))
                            .output(new Amount(ItemType.Dynamo, 1)));

                    add(new Recipe(25f, "rotor", 30)
                            .input(
                                    new Amount(ItemType.BronzePlate, 20),
                                    new Amount(ItemType.IronPlate, 40),
                                    new Amount(ItemType.SteelIngot, 25),
                                    new Amount(ItemType.SteelPlate, 30))
                            .output(new Amount(ItemType.Rotor, 1)));

                    add(new Recipe(8f, "blueprint", 100)
                            .input(
                                    new Amount(ItemType.Paper, 4),
                                    new Amount(ItemType.IronPlate, 2))
                            .output(new Amount(ItemType.Blueprint, 1))
                            .science(ScienceType.Blueprints));

                    add(new Recipe(5f, "dynamite", 400)
                            .input(
                                    new Amount(ItemType.Paper, 8),
                                    new Amount(ItemType.Gunpowder, 4),
                                    new Amount(ItemType.Sand, 14),
                                    new Amount(ItemType.Clay, 3))
                            .output(new Amount(ItemType.Dynamite, 1))
                            .science(ScienceType.MineralExtraction, ScienceType.Blueprints));

                    add(new Recipe(13f, "battery", 200)
                            .input(
                                    new Amount(ItemType.BronzePlate, 8),
                                    new Amount(ItemType.IronPlate, 8),
                                    new Amount(ItemType.SulfurDust, 24),
                                    new Amount(ItemType.PlasticCasing, 1))
                            .output(new Amount(ItemType.Battery, 1))
                            .science(ScienceType.MineralExtraction, ScienceType.OilProcessing));

                    add(new Recipe(45f, "turbine", 1200)
                            .input(
                                    new Amount(ItemType.Rotor, 4),
                                    new Amount(ItemType.Dynamo, 8),
                                    new Amount(ItemType.SteelTube, 25),
                                    new Amount(ItemType.SteelPlate, 50),
                                    new Amount(ItemType.Lubricant, 10_000))
                            .output(new Amount(ItemType.Turbine, 1))
                            .science(ScienceType.OilProcessing));

                    add(new Recipe(20f, "pallet", 500)
                            .input(
                                    new Amount(ItemType.WoodPlank, 36),
                                    new Amount(ItemType.SteelWire, 12))
                            .output(new Amount(ItemType.Pallet, 1))
                            .science(ScienceType.AdvancedTransport));
                }
            }, new Sfx("assembler" + Const.SFX_FORMAT),
            false,
            new Dock(5, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 5, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CopperWire, ItemType.BronzePlate, ItemType.Rotor, ItemType.Paper, ItemType.WoodPlank)),
            new Dock(5, 5, Direction.East, DockType.ItemIn, new DockFilter(ItemType.Magnet, ItemType.IronPlate, ItemType.Dynamo, ItemType.IronPlate, ItemType.Gunpowder, ItemType.SteelWire)),
            new Dock(5, 3, Direction.East, DockType.ItemIn, new DockFilter(ItemType.SteelIngot, ItemType.SulfurDust, ItemType.SteelTube, ItemType.Sand)),
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CarbonBlock, ItemType.PlasticCasing, ItemType.SteelPlate, ItemType.Clay)),
            new Dock(1, 5, Direction.North, DockType.Power), new Dock(4, 5, Direction.North, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.Electricity)
                    .flags(Flags.TextureAlwaysUpright);

    public static final ProducerSchema classSchemaV106 = new ProducerSchema(1, StructureType.Assembler, 6, 6,
            "assembler", new Items(ItemType.StoneBrick, 35, ItemType.SteelIngot, 50, ItemType.WoodPlank, 85, ItemType.Scaffolding, 60, ItemType.CopperIngot, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(28f, "dynamo")
                            .input(
                                    new Amount(ItemType.CopperWire, 40),
                                    new Amount(ItemType.Magnet, 3),
                                    new Amount(ItemType.SteelIngot, 4),
                                    new Amount(ItemType.CarbonBlock, 2))
                            .output(new Amount(ItemType.Dynamo, 1)));

                    add(new Recipe(25f, "rotor", 30)
                            .input(
                                    new Amount(ItemType.BronzePlate, 20),
                                    new Amount(ItemType.IronPlate, 40),
                                    new Amount(ItemType.SteelIngot, 25),
                                    new Amount(ItemType.SteelPlate, 30))
                            .output(new Amount(ItemType.Rotor, 1)));

                    add(new Recipe(8f, "blueprint", 100)
                            .input(
                                    new Amount(ItemType.Paper, 4),
                                    new Amount(ItemType.IronPlate, 2))
                            .output(new Amount(ItemType.Blueprint, 1))
                            .science(ScienceType.Blueprints));

                    add(new Recipe(13f, "battery", 200)
                            .input(
                                    new Amount(ItemType.BronzePlate, 8),
                                    new Amount(ItemType.IronPlate, 8),
                                    new Amount(ItemType.SulfurDust, 24),
                                    new Amount(ItemType.PlasticCasing, 1))
                            .output(new Amount(ItemType.Battery, 1))
                            .science(ScienceType.MineralExtraction, ScienceType.OilProcessing));

                    add(new Recipe(45f, "turbine", 1200)
                            .input(
                                    new Amount(ItemType.Rotor, 4),
                                    new Amount(ItemType.Dynamo, 8),
                                    new Amount(ItemType.SteelTube, 25),
                                    new Amount(ItemType.SteelPlate, 50),
                                    new Amount(ItemType.Lubricant, 10_000))
                            .output(new Amount(ItemType.Turbine, 1))
                            .science(ScienceType.OilProcessing));
                }
            }, new Sfx("assembler" + Const.SFX_FORMAT),
            false,
            new Dock(5, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 5, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CopperWire, ItemType.BronzePlate, ItemType.Rotor, ItemType.Paper)),
            new Dock(5, 5, Direction.East, DockType.ItemIn, new DockFilter(ItemType.Magnet, ItemType.IronPlate, ItemType.Dynamo, ItemType.IronPlate)),
            new Dock(5, 3, Direction.East, DockType.ItemIn, new DockFilter(ItemType.SteelIngot, ItemType.SulfurDust, ItemType.SteelTube)),
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CarbonBlock, ItemType.PlasticCasing, ItemType.SteelPlate)),
            new Dock(1, 5, Direction.North, DockType.Power), new Dock(4, 5, Direction.North, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.Electricity)
                    .flags(Flags.TextureAlwaysUpright);

    public static final ProducerSchema classSchemaV103 = new ProducerSchema(0, StructureType.Assembler, 6, 6,
            "assembler", new Items(ItemType.StoneBrick, 35, ItemType.SteelIngot, 50, ItemType.WoodPlank, 85, ItemType.Scaffolding, 60, ItemType.CopperIngot, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(28f, "dynamo")
                            .input(
                                    new Amount(ItemType.CopperWire, 40),
                                    new Amount(ItemType.Magnet, 3),
                                    new Amount(ItemType.SteelIngot, 4),
                                    new Amount(ItemType.CarbonBlock, 2))
                            .output(new Amount(ItemType.Dynamo, 1)));

                    add(new Recipe(25f, "rotor", 30)
                            .input(
                                    new Amount(ItemType.BronzePlate, 20),
                                    new Amount(ItemType.IronPlate, 40),
                                    new Amount(ItemType.SteelIngot, 25),
                                    new Amount(ItemType.SteelPlate, 30))
                            .output(new Amount(ItemType.Rotor, 1)));

                    add(new Recipe(13f, "battery", 200)
                            .input(
                                    new Amount(ItemType.BronzePlate, 8),
                                    new Amount(ItemType.IronPlate, 8),
                                    new Amount(ItemType.SulfurDust, 24),
                                    new Amount(ItemType.PlasticCasing, 1))
                            .output(new Amount(ItemType.Battery, 1)));

                    add(new Recipe(45f, "turbine", 1200)
                            .input(
                                    new Amount(ItemType.Rotor, 4),
                                    new Amount(ItemType.Dynamo, 8),
                                    new Amount(ItemType.SteelTube, 25),
                                    new Amount(ItemType.SteelPlate, 50),
                                    new Amount(ItemType.Lubricant, 10_000))
                            .output(new Amount(ItemType.Turbine, 1)));
                }
            }, new Sfx("assembler" + Const.SFX_FORMAT),
            false,
            new Dock(5, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 5, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CopperWire, ItemType.BronzePlate, ItemType.Rotor)),
            new Dock(5, 5, Direction.East, DockType.ItemIn, new DockFilter(ItemType.Magnet, ItemType.IronPlate, ItemType.Dynamo)),
            new Dock(5, 3, Direction.East, DockType.ItemIn, new DockFilter(ItemType.SteelIngot, ItemType.SulfurDust, ItemType.SteelTube)),
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.CarbonBlock, ItemType.PlasticCasing, ItemType.SteelPlate)),
            new Dock(1, 5, Direction.North, DockType.Power), new Dock(4, 5, Direction.North, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.Electricity)
                    .flags(Flags.TextureAlwaysUpright);

    public Assembler(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchemaV113, classSchemaV103, classSchemaV106, classSchema));
    }

    public Assembler(int x, int y) {
        super(x, y, classSchema);
    }
}
