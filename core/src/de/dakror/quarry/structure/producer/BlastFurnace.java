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
public class BlastFurnace extends OvenStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.BlastFurnace, 4,
            4, "smelter",
            new Items(ItemType.StoneBrick, 30, ItemType.Brick, 10, ItemType.IronIngot, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(16f, "steel")
                            .input(new Amount(ItemType.IronIngot, 3), new Amount(ItemCategory.CoalFuel, 9))
                            .output(new Amount(ItemType.MoltenSteel, 2200)));
                    add(new Recipe(23f, "silicon")
                            .input(new Amount(ItemType.SiliconDust, 10), new Amount(ItemCategory.CoalFuel, 24))
                            .output(new Amount(ItemType.MoltenSilicon, 400))
                            .science(ScienceType.MineralExtraction));
                    add(new Recipe(40f, "titanium")
                            .input(new Amount(ItemType.TitaniumDust, 1), new Amount(ItemCategory.CoalFuel, 40))
                            .output(new Amount(ItemType.MoltenTitanium, 400))
                            .science(ScienceType.MineralExtraction));
                }
            },
            new Sfx("furnace" + Const.SFX_FORMAT, 0.5f),
            true,
            new Dock(2, 0, Direction.South, DockType.FluidOut),
            new Dock(2, 3, Direction.North, DockType.ItemIn, new DockFilter(ItemType.IronIngot, ItemType.SiliconDust, ItemType.TitaniumDust)), new Dock(3, 2, Direction.East, DockType.ItemIn, new DockFilter(ItemCategory.CoalFuel)))
                    .sciences(ScienceType.SteelProduction)
                    .flags(Flags.TextureAlwaysUpright);

    public BlastFurnace(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    protected void initPfx() {
        pfx.scaleEffect(3.5f);
        pfx.setPosition((x + 2) * Const.TILE_SIZE, (y + 1.5f) * Const.TILE_SIZE);
    }
}
