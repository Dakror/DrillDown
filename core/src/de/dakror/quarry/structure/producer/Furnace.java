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
import de.dakror.quarry.structure.base.OvenStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Furnace extends OvenStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Furnace, 2,
            2, "smelter",
            new Items(ItemType.Stone, 15, ItemType.Brick, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(7.5f, "ore")
                            .input(new Amount(ItemType._Ore, 1), new Amount(ItemCategory.CoalFuel, 1))
                            .output(new ParameterizedAmount(ItemType._MoltenMetal, 1000, 0)));

                    add(new Recipe(7.5f, "gravel")
                            .input(new Amount(ItemType._Gravel, 1), new Amount(ItemCategory.CoalFuel, 1))
                            .output(new ParameterizedAmount(ItemType._MoltenMetal, 750, 0))
                            .science(ScienceType.OreProcessing));

                    add(new Recipe(7.5f, "dust")
                            .input(new Amount(ItemType._Dust, 1), new Amount(ItemCategory.CoalFuel, 1))
                            .output(new ParameterizedAmount(ItemType._MoltenMetal, 400, 0))
                            .science(ScienceType.OreProcessing));
                }
            },
            new Sfx("furnace" + Const.SFX_FORMAT),
            false,
            new Dock(1, 0, Direction.South, DockType.FluidOut),
            new Dock(0, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemCategory.RawOre).exclude(ItemType.CoalOre, ItemType.TitaniumDust)),
            new Dock(1, 1, Direction.East, DockType.ItemIn, new DockFilter(ItemCategory.CoalFuel)))
                    .flags(Flags.TextureAlwaysUpright);

    public Furnace(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    protected void initPfx() {
        pfx.scaleEffect(1.5f);
        pfx.setPosition((x + 1) * Const.TILE_SIZE, (y + 0.75f) * Const.TILE_SIZE);
    }
}
