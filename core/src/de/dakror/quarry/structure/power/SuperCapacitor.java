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

package de.dakror.quarry.structure.power;

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class SuperCapacitor extends Substation {
    public static final SubstationSchema classSchema = new SubstationSchema(1, StructureType.SuperCapacitor, 3, 3, 1_000_000_000, 200,
            "supercapacitor",
            new Items(ItemType.MachineFrame, 50, ItemType.AdvancedMachineFrame, 10, ItemType.SteelCable, 200, ItemType.Battery, 60), new Sfx("node" + Const.SFX_FORMAT, 0.2f),
            0,
            new Dock(1, 2, Direction.North, DockType.BigPower),
            new Dock(1, 0, Direction.South, DockType.BigPower))
                    .sciences(ScienceType.HighPower, ScienceType.BetterStorage)
                    .flags(Flags.TextureAlwaysUpright, Flags.MirroredTextureHorizontal);

    public SuperCapacitor(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public int getDonorPriority() {
        return 20;
    }

    @Override
    public int getReceiverPriority() {
        return 20;
    }
}
