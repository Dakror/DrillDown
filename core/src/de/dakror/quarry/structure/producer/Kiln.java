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
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.OvenStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Kiln extends OvenStructure {
    public static ProducerSchema classSchema = new ProducerSchema(0, StructureType.Kiln, 1, 3, "kiln", new Items(ItemType.Stone, 10, ItemType.Dirt, 10), new RecipeList() {
        @Override
        protected void init() {
            add(new Recipe(20f, "brick")
                    .input(new Amount(ItemCategory.CoalFuel, 2), new Amount(ItemType.Clay, 8))
                    .output(new Amount(ItemType.Brick, 3)));
            add(new Recipe(32f, "glass")
                    .input(new Amount(ItemCategory.CoalFuel, 6), new Amount(ItemType.Sand, 7))
                    .output(new Amount(ItemType.Glass, 1))
                    .science(ScienceType.OreProcessing));
        }
    },
            new Sfx("kiln" + Const.SFX_FORMAT),
            true,
            new Dock(0, 0, Direction.East, DockType.ItemOut),
            new Dock(0, 0, Direction.West, DockType.ItemIn, new Dock.DockFilter(ItemCategory.CoalFuel)), new Dock(0, 2, Direction.North, DockType.ItemIn, new Dock.DockFilter(ItemType.Clay, ItemType.Sand)));

    public Kiln(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    protected void initPfx() {
        pfx.setPosition((x + 0.5f) * Const.TILE_SIZE, (y + 0.4f) * Const.TILE_SIZE);
    }
}
