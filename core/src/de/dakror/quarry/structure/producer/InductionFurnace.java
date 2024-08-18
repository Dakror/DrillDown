package de.dakror.quarry.structure.producer;

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

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemCategory;
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
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class InductionFurnace extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(1, StructureType.InductionFurnace, 5,
            5, "inductionfurnace",
            new Items(ItemType.MachineFrame, 8, ItemType.CopperWire, 150, ItemType.SteelIngot, 3, ItemType.CopperTube, 40, ItemType.Brick, 50, ItemType.CarbonBlock, 20),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(2.0f, "ore", 500)
                            .input(new Amount(ItemType._Ore, 1), new Amount(ItemType.Water, 3000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 2, 0), new Amount(ItemType.Steam, 15000)));

                    add(new Recipe(2.0f, "gravel", 750)
                            .input(new Amount(ItemType._Gravel, 4), new Amount(ItemType.Water, 7000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 4, 0), new Amount(ItemType.Steam, 35000)));

                    add(new Recipe(2.0f, "dust", 1000)
                            .input(new Amount(ItemType._Dust, 12), new Amount(ItemType.Water, 14000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 6, 0), new Amount(ItemType.Steam, 70000)));

                    add(new Recipe(3.0f, "steel", 2000)
                            .input(new Amount(ItemType.IronIngot, 12), new Amount(ItemType.Water, 18000))
                            .output(new Amount(ItemType.SteelIngot, 6), new Amount(ItemType.Steam, 90000)));

                    add(new Recipe(3.0f, "titanium", 4000)
                            .input(new Amount(ItemType.TitaniumDust, 12), new Amount(ItemType.Water, 25000))
                            .output(new Amount(ItemType.TitaniumIngot, 6), new Amount(ItemType.Steam, 125000)));
                }

                @Override
                public void validateRecipes(Schema schema) {
                    super.validateRecipes(schema);
                    fluidInputs.set(0, 100_000); // increase water tank size
                }
            },
            new Sfx("node" + Const.SFX_FORMAT, 0.7f),
            true,
            new Dock(2, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 0, Direction.West, DockType.FluidOut),
            new Dock(2, 4, Direction.North, DockType.ItemIn, new DockFilter(ItemCategory.RawOre, ItemType.IronIngot, ItemType.TitaniumDust).exclude(ItemType.CoalOre)),
            new Dock(0, 4, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Water)),
            new Dock(0, 2, Direction.West, DockType.Power))
                    .sciences(ScienceType.HighTech)
                    .flags(Flags.TextureAlwaysUpright, Flags.MirroredTextureHorizontal);

    public static final ProducerSchema classSchemaV110 = new ProducerSchema(0, StructureType.InductionFurnace, 5,
            5, "inductionfurnace",
            new Items(ItemType.MachineFrame, 8, ItemType.CopperWire, 150, ItemType.SteelIngot, 3, ItemType.CopperTube, 40, ItemType.Brick, 50, ItemType.CarbonBlock, 20),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(2.0f, "ore", 500)
                            .input(new Amount(ItemType._Ore, 1), new Amount(ItemType.Water, 3000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 1, 0)));

                    add(new Recipe(2.0f, "gravel", 750)
                            .input(new Amount(ItemType._Gravel, 4), new Amount(ItemType.Water, 7000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 3, 0)));

                    add(new Recipe(2.0f, "dust", 1000)
                            .input(new Amount(ItemType._Dust, 12), new Amount(ItemType.Water, 14000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 5, 0)));
                }

                @Override
                public void validateRecipes(de.dakror.quarry.structure.base.Schema schema) {
                    super.validateRecipes(schema);
                    fluidInputs.set(0, 50_000); // increase water tank size
                }
            },
            new Sfx("node" + Const.SFX_FORMAT, 0.7f),
            true,
            new Dock(2, 0, Direction.South, DockType.ItemOut),
            new Dock(2, 4, Direction.North, DockType.ItemIn, new DockFilter(ItemCategory.RawOre).exclude(ItemType.CoalOre, ItemType.TitaniumDust)),
            new Dock(0, 4, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Water)),
            new Dock(0, 2, Direction.West, DockType.Power))
                    .sciences(ScienceType.HighTech)
                    .flags(Flags.TextureAlwaysUpright, Flags.MirroredTextureHorizontal);

    public InductionFurnace(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchemaV110, classSchema));
        ((CInventory) outputInventories[0]).setPumpOutSpeed(0);
        if (outputInventories.length == 2)
            ((CTank) outputInventories[1]).setPumpOutDelay(0).setMaxOutput(10000);
    }

    public InductionFurnace(int x, int y) {
        super(x, y, classSchema);
        ((CInventory) outputInventories[0]).setPumpOutSpeed(0);
        ((CTank) outputInventories[1]).setPumpOutDelay(0).setMaxOutput(10000);
    }
}
