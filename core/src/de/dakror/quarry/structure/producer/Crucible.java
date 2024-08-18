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
public class Crucible extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Crucible, 3, 3,
            "crucible",
            new Items(ItemType.Brick, 100, ItemType.SteelIngot, 75, ItemType.BronzePlate, 80),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(90f, "wafer")
                            .input(new Amount(ItemType.MoltenSilicon, 6000))
                            .output(new Amount(ItemType.SiliconWafer, 70)));
                }
            }, new Sfx("crucible" + Const.SFX_FORMAT),
            false,
            new Dock(1, 0, Direction.South, DockType.ItemOut), new Dock(1, 2, Direction.North, DockType.FluidIn, new DockFilter(ItemType.MoltenSilicon)))
                    .sciences(ScienceType.WaferGrowth);

    public Crucible(int x, int y) {
        super(x, y, classSchema);
    }

}
