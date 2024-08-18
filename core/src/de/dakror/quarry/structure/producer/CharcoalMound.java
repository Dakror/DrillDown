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
public class CharcoalMound extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(1, StructureType.CharcoalMound, 3, 3,
            "charcoalmound",
            new Items(ItemType.Dirt, 40, ItemType.Wood, 25),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(240f, "charcoal")
                            .input(new Amount(ItemType.Wood, 5), new Amount(ItemType.Dirt, 15))
                            .output(new Amount(ItemType.Charcoal, 24)));
                }
            },
            new Sfx("charcoalmound" + Const.SFX_FORMAT),
            true,
            new Dock(2, 1, Direction.East, DockType.ItemOut),
            new Dock(0, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.Wood)), new Dock(0, 0, Direction.South, DockType.ItemIn, new DockFilter(ItemType.Dirt)))
                    .sciences(ScienceType.CharcoalProduction);

    public static final ProducerSchema classSchemaV103 = new ProducerSchema(0, StructureType.CharcoalMound, 3, 3,
            "charcoalmound",
            new Items(ItemType.Dirt, 40, ItemType.Wood, 25),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(240f, "charcoal")
                            .input(new Amount(ItemType.Wood, 5))
                            .output(new Amount(ItemType.Charcoal, 24)));
                }
            },
            new Sfx("charcoalmound" + Const.SFX_FORMAT),
            true,
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(0, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.Wood)))
                    .sciences(ScienceType.CharcoalProduction);

    public CharcoalMound(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchema, classSchemaV103));
    }

    public CharcoalMound(int x, int y) {
        super(x, y, classSchema);
    }
}
