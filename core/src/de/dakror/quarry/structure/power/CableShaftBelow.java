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

import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class CableShaftBelow extends CableShaft {
    public static final SubstationSchema classSchema = new SubstationSchema(0, StructureType.CableShaftBelow, 1, 1, 200_000,
            80, "cableshaftbelow",
            CableShaft.classSchema.buildCosts, null,
            1, new Dock(0, 0, Direction.West, DockType.Power))
                    .sciences(ScienceType.MineExpansion, ScienceType.Routers, ScienceType.Electricity);

    public CableShaftBelow(int x, int y) {
        super(x, y, classSchema, -1);
    }
}
