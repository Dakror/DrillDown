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
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Lumberjack extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Lumberjack, 2, 2,
            "lumberjack",
            new Items(ItemType.IronIngot, 3, ItemType.Stone, 8),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(30f, "wood").output(new Amount(ItemType.Wood, 1)));
                }
            }, new Sfx("woodcutter" + Const.SFX_FORMAT),
            true, new Dock(0, 1, Direction.West, DockType.ItemOut))
                    .flags(Flags.TextureAlwaysUpright);

    public Lumberjack(int x, int y) {
        super(x, y, classSchema);
    }
}
