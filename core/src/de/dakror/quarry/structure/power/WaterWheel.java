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

package de.dakror.quarry.structure.power;

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.GeneratorStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class WaterWheel extends GeneratorStructure {
    public static final GeneratorSchema classSchema = new GeneratorSchema(0, StructureType.WaterWheel, 2, 2,
            "waterwheel",
            new Items(ItemType.Wood, 20, ItemType.WoodPlank, 10, ItemType.Scaffolding, 8, ItemType.SteelIngot, 2, ItemType.Dynamo, 1),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new GeneratorRecipe("water", 20 * 60)
                            .input(new Amount(ItemType.Water, 500))
                            .output(new Amount(ItemType.Water, 350)));
                }
            },
            new Sfx("waterwheel" + Const.SFX_FORMAT),
            new Dock(0, 1, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Water)),
            new Dock(0, 1, Direction.North, DockType.Power), new Dock(1, 0, Direction.East, DockType.FluidOut))
                    .sciences(ScienceType.WaterUsage, ScienceType.Electricity)
                    .flags(Flags.TextureAlwaysUpright);

    public WaterWheel(int x, int y) {
        super(x, y, classSchema);
    }
}
