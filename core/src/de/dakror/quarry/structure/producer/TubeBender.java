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
public class TubeBender extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.TubeBender, 2,
            3, "tubebender",
            new Items(ItemType.IronIngot, 20, ItemType.SteelIngot, 20, ItemType.Brick, 40),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(12f, "cu_tube")
                            .input(new Amount(ItemType.CopperPlate, 1))
                            .output(new Amount(ItemType.CopperTube, 1)));
                    add(new Recipe(20f, "steel_tube")
                            .input(new Amount(ItemType.SteelPlate, 2))
                            .output(new Amount(ItemType.SteelTube, 1)));
                }
            }, new Sfx("bender" + Const.SFX_FORMAT),
            false,
            new Dock(1, 0, Direction.South, DockType.ItemOut), new Dock(0, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.CopperPlate, ItemType.SteelPlate)))
                    .sciences(ScienceType.Metalworking);

    public TubeBender(int x, int y) {
        super(x, y, classSchema);
    }
}
