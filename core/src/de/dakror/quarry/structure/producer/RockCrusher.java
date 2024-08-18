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
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class RockCrusher extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.RockCrusher, 2,
            2,
            "crusher",
            new Items(ItemType.Stone, 15, ItemType.SteelIngot, 15, ItemType.Scaffolding, 5),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(8f, "ore")
                            .input(new Amount(ItemType._Ore, 1))
                            .output(new ParameterizedAmount(ItemType._Gravel, 2, 0)));
                    add(new Recipe(8f, "stone")
                            .input(new Amount(ItemType.Stone, 1))
                            .output(new Amount(ItemType.StoneGravel, 2)));
                    add(new Recipe(3f, "wood")
                            .input(new Amount(ItemType.Wood, 1))
                            .output(new Amount(ItemType.WoodChips, 20))
                            .science(ScienceType.Blueprints));
                }
            }, new Sfx("rockcrusher" + Const.SFX_FORMAT),
            true,
            new Dock(1, 0, Direction.South, DockType.ItemOut), new Dock(0, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType._Ore, ItemType.Stone, ItemType.Wood).exclude(ItemType.CoalOre)))
                    .sciences(ScienceType.OreProcessing)
                    .flags(Flags.TextureAlwaysUpright);

    public RockCrusher(int x, int y) {
        super(x, y, classSchema);
    }
}
