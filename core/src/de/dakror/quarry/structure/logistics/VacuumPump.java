/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure.logistics;

import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemCategory;
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
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class VacuumPump extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.VacuumPump, 2, 1,
            "vacuumpump",
            new Items(ItemType.SteelPlate, 20, ItemType.Dynamo, 1, ItemType.SteelTube, 10),
            new RecipeList() {

                @Override
                protected void init() {
                    add(new Recipe(0.1f, "pump", 10)
                            .input(new Amount(ItemCategory.Fluid, 1000))
                            .output(new SameAmount(0, 1000)));
                }
            },
            new Sfx("centrifuge" + Const.SFX_FORMAT, 0.5f),
            false,
            new Dock(0, 0, Direction.West, DockType.FluidIn, new DockFilter(ItemCategory.Fluid)),
            new Dock(1, 0, Direction.East, DockType.FluidOut), new Dock(0, 0, Direction.South, DockType.Power))
                    .sciences(ScienceType.Electricity, ScienceType.Routers);

    public VacuumPump(int x, int y) {
        super(x, y, classSchema);

        ((CTank) outputInventories[0]).setPumpOutDelay(0).setMaxOutput(1000);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        // compat
        if (tag.has("dir")) {
            setUpDirection(Direction.values[tag.Byte("dir", (byte) 0)]);
        }
    }
}
