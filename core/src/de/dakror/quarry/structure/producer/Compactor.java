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
public class Compactor extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0,
            StructureType.Compactor, 3, 2,
            "compactor",
            new Items(ItemType.StoneBrick, 25, ItemType.SteelPlate, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(10f, "coalchunk")
                            .input(new Amount(ItemType.CoalDust, 8))
                            .output(new Amount(ItemType.CoalChunk, 1)));
                    add(new Recipe(10f, "carbonblock")
                            .input(new Amount(ItemType.CoalChunk, 4))
                            .output(new Amount(ItemType.CarbonBlock, 1)));
                    add(new Recipe(490f, "quartz")
                            .input(new Amount(ItemType.QuartzDust, 120))
                            .output(new Amount(ItemType.SyntheticQuartz, 1))
                            .science(ScienceType.MineralExtraction));
                }
            }, new Sfx("compactor" + Const.SFX_FORMAT),
            true,
            new Dock(1, 0, Direction.South, DockType.ItemOut), new Dock(1, 1, Direction.North, DockType.ItemIn, new DockFilter(ItemType.CoalDust, ItemType.CoalChunk, ItemType.QuartzDust)))
                    .sciences(ScienceType.OreProcessing);

    public Compactor(int x, int y) {
        super(x, y, classSchema);
    }
}
