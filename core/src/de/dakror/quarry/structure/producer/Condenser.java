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
public class Condenser extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Condenser, 3, 1,
            "condenser",
            new Items(ItemType.CopperTube, 8, ItemType.SteelPlate, 10, ItemType.Scaffolding, 50, ItemType.Glass, 25),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(2f, "water")
                            .input(new Amount(ItemType.Steam, 2000))
                            .output(new Amount(ItemType.Water, 250)));
                }
            },
            new Sfx("condenser" + Const.SFX_FORMAT),
            true,
            new Dock(2, 0, Direction.East, DockType.FluidOut), new Dock(0, 0, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Steam)))
                    .sciences(ScienceType.WaterUsage);

    public Condenser(int x, int y) {
        super(x, y, classSchema);
    }
}
