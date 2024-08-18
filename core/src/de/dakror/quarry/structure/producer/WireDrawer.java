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
public class WireDrawer extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0,
            StructureType.WireDrawer, 1, 7,
            "wiredrawer",
            new Items(ItemType.StoneBrick, 7, ItemType.Scaffolding, 5, ItemType.SteelIngot, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(60f, "wire").input(new Amount(ItemType.SteelIngot, 1)).output(new Amount(ItemType.SteelWire, 8)));
                    add(new Recipe(60f, "wire").input(new Amount(ItemType.CopperIngot, 1)).output(new Amount(ItemType.CopperWire, 8)));
                    add(new Recipe(60f, "wire").input(new Amount(ItemType.TinIngot, 1)).output(new Amount(ItemType.TinWire, 8)));
                    add(new Recipe(60f, "wire").input(new Amount(ItemType.GoldIngot, 1)).output(new Amount(ItemType.GoldWire, 8)));
                }
            }, new Sfx("drawer" + Const.SFX_FORMAT),
            true,
            new Dock(0, 0, Direction.South, DockType.ItemOut), new Dock(0, 6, Direction.North, DockType.ItemIn, new DockFilter(ItemType.SteelIngot, ItemType.CopperIngot, ItemType.TinIngot, ItemType.GoldIngot)))
                    .sciences(ScienceType.Metalworking);

    public WireDrawer(int x, int y) {
        super(x, y, classSchema);
    }
}
