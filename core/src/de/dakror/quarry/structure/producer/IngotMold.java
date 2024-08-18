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

import com.badlogic.gdx.Gdx;

import de.dakror.common.Callback;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class IngotMold extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.IngotMold, 2, 2,
            "ingotmold",
            new Items(ItemType.Stone, 30),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(30f, "ingot")
                            .input(new Amount(ItemType._MoltenMetal, 1000))
                            .output(new ParameterizedAmount(ItemType._Ingot, 1, 0)));
                }
            }, null,
            true,
            new Dock(1, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 1, Direction.North, DockType.FluidIn,
                    new DockFilter(ItemType._MoltenMetal).exclude(ItemType.MoltenSilicon)))
            .flags(Flags.TextureAlwaysUpright);

    public IngotMold(int x, int y) {
        super(x, y, classSchema);
    }
}
