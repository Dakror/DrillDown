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
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Mixer extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(1, StructureType.Mixer, 2, 2,
            "mixer",
            new Items(ItemType.Brick, 10, ItemType.IronIngot, 2, ItemType.SteelPlate, 4),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(3f, "cement")
                            .input(new Amount(ItemType.Sand, 3), new Amount(ItemType.Clay, 4))
                            .output(new Amount(ItemType.Cement, 2)));
                    add(new Recipe(5f, "concretepowder")
                            .input(new Amount(ItemType.Cement, 2), new Amount(ItemType.StoneDust, 5))
                            .output(new Amount(ItemType.ConcretePowder, 2)));
                    add(new Recipe(6f, "bronze")
                            .input(new Amount(ItemType.CopperDust, 12), new Amount(ItemType.TinDust, 4))
                            .output(new Amount(ItemType.BronzeDust, 4)));
                    add(new Recipe(15f, "cellulose")
                            .input(new Amount(ItemType.WoodChips, 8), new Amount(ItemType.WaterBarrel, 1))
                            .output(new Amount(ItemType.Cellulose, 1), new Amount(ItemType.EmptyBarrel, 1))
                            .science(ScienceType.Blueprints));
                    add(new Recipe(8f, "gunpowder")
                            .input(new Amount(ItemType.SulfurDust, 4), new Amount(ItemType.CoalDust, 8))
                            .output(new Amount(ItemType.Gunpowder, 6))
                            .science(ScienceType.MineralExtraction, ScienceType.HighTech));
                }
            }, new Sfx("mixer" + Const.SFX_FORMAT),
            true,
            new Dock(0, 0, Direction.South, DockType.ItemOut),
            new Dock(1, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType.CopperDust, ItemType.Cement, ItemType.Sand, ItemType.WoodChips, ItemType.SulfurDust)),
            new Dock(1, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType.TinDust, ItemType.Clay, ItemType.StoneDust, ItemType.WaterBarrel, ItemType.CoalDust, ItemType.Gunpowder)))
                    .sciences(ScienceType.OreProcessing)
                    .flags(Flags.TextureAlwaysUpright);

    public static final ProducerSchema classSchemaV103 = new ProducerSchema(0, StructureType.Mixer, 2, 2,
            "mixer",
            new Items(ItemType.Brick, 10, ItemType.IronIngot, 2, ItemType.SteelPlate, 4),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(3f, "cement")
                            .input(new Amount(ItemType.Sand, 3), new Amount(ItemType.Clay, 4))
                            .output(new Amount(ItemType.Cement, 2)));
                    add(new Recipe(5f, "concretepowder")
                            .input(new Amount(ItemType.Cement, 2), new Amount(ItemType.StoneDust, 5))
                            .output(new Amount(ItemType.ConcretePowder, 2)));
                    add(new Recipe(6f, "bronze")
                            .input(new Amount(ItemType.CopperDust, 12), new Amount(ItemType.TinDust, 4))
                            .output(new Amount(ItemType.BronzeDust, 4)));
                }
            }, new Sfx("mixer" + Const.SFX_FORMAT),
            true,
            new Dock(0, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType.CopperDust, ItemType.Cement, ItemType.Sand)), new Dock(1, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType.TinDust, ItemType.Clay, ItemType.StoneDust)))
                    .sciences(ScienceType.OreProcessing)

                    .flags(Flags.TextureAlwaysUpright);

    public Mixer(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchema, classSchemaV103));
    }

    public Mixer(int x, int y) {
        super(x, y, classSchema);
    }
}
