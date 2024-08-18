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

package de.dakror.quarry.structure.storage;

import de.dakror.common.BiCallback;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;

/**
 * @author Maximilian Stark | Dakror
 */
public class Silo extends Tank {
    public static final Schema classSchema = new Schema(0, StructureType.Silo, true, 2,
            7,
            "silo",
            new Items(ItemType.SteelPlate, 250, ItemType.SteelIngot, 10, ItemType.SteelTube, 8), null, new Dock(0, 0, Direction.West, DockType.FluidIn), new Dock(1, 0, Direction.East, DockType.FluidOut))
                    .components(new CTank(8000000, 1, false).setPumpOutDelay(0).setMaxOutput(1000))
                    .flags(Flags.ConfirmDestruction)
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            ((CTank) ((Silo) data).components[0]).setOutputEnabled(on);
                        }
                    }))
                    .sciences(ScienceType.BetterStorage, ScienceType.WaterUsage);

    public Silo(int x, int y) {
        super(x, y, classSchema);
    }
}
