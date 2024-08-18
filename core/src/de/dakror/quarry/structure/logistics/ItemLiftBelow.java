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
import de.dakror.quarry.structure.base.component.CInventory;

/**
 * @author Maximilian Stark | Dakror
 */
public class ItemLiftBelow extends ItemLift {
    public static final Schema classSchema = new Schema(0, StructureType.ItemLiftBelow, true, 1, 1,
            "itemliftout",
            ItemLift.classSchema.buildCosts /*same costs to get the stuff when destroying*/,
            null, new Dock(0, 0, Direction.East, DockType.ItemOut), new Dock(0, 0, Direction.West, DockType.ItemIn))
                    .components(new CInventory(1, 0));

    public ItemLiftBelow(int x, int y) {
        super(x, y, false, classSchema);
    }
}
