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
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class AnchorPortal extends Substation {
    public static final SubstationSchema classSchema = new SubstationSchema(0, StructureType.AnchorPortal, 7, 2, 200_000, 0,
            "anchorportal",
            new Items(ItemType.SteelCable, 100, ItemType.Brick, 80, ItemType.TinPlate, 350, ItemType.Battery, 30), new Sfx("anchorportal" + Const.SFX_FORMAT),
            0,
            new Dock(1, 1, Direction.North, DockType.BigPower),
            new Dock(3, 1, Direction.North, DockType.BigPower),
            new Dock(5, 1, Direction.North, DockType.BigPower),
            new Dock(0, 0, Direction.South, DockType.Power),
            new Dock(1, 0, Direction.South, DockType.Power),
            new Dock(2, 0, Direction.South, DockType.Power),
            new Dock(3, 0, Direction.South, DockType.Power),
            new Dock(4, 0, Direction.South, DockType.Power),
            new Dock(5, 0, Direction.South, DockType.Power), new Dock(6, 0, Direction.South, DockType.Power))
                    .sciences(ScienceType.HighPower);

    public AnchorPortal(int x, int y) {
        super(x, y, classSchema);
    }
}
