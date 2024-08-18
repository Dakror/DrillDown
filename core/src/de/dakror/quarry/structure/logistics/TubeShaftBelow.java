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

package de.dakror.quarry.structure.logistics;

import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;

/**
 * @author Maximilian Stark | Dakror
 */
public class TubeShaftBelow extends TubeShaft {
    public static final Schema classSchema = new Schema(0, StructureType.TubeShaftBelow, true, 1, 1,
            "tubeshaftout",
            TubeShaft.classSchema.buildCosts /*same costs to get the stuff when destroying*/, null, new Dock(0, 0, Direction.East, DockType.FluidOut), new Dock(0, 0, Direction.West, DockType.FluidIn))
                    .components(new CTank(20_000, 0).setPumpOutDelay(0).setMaxOutput(1000));

    public TubeShaftBelow(int x, int y) {
        super(x, y, classSchema, -1);
    }
}
