/*******************************************************************************
 * Copyright 2021 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.util;

import de.dakror.common.libgdx.Pair;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.power.CopperCable;

/**
 * @author Maximilian Stark | Dakror
 */
public class CopyRegionHelper {
    public static int indexGlobalToRelative(int[] copyRegion, int index) {
        int x = index / Game.G.layer.height;
        int y = index % Game.G.layer.height;

        return (x - copyRegion[0]) * Game.G.layer.height + (y - copyRegion[1]);
    }

    public static int indexRelativeToGlobal(int[] copyRegion, int index) {
        int x = index / Game.G.layer.height;
        int y = index % Game.G.layer.height;

        return (x + copyRegion[0]) * Game.G.layer.height + (y + copyRegion[1]);
    }

    public static Structure<?> getStructure(Structure<?> context, int[] copyRegion, int index) {
        Pair<Structure<?>, CompoundTag> t = null;
        Structure<?> s = null;
        int ind = ((index / Game.G.layer.height) + copyRegion[0]) * Game.G.layer.height + ((index % Game.G.layer.height) + copyRegion[1]);
        if (context.layer == null) {
            t = Game.G.copyStructures.get(ind);
            if (t != null) s = t.getKey();
        } else if (s == null) s = context.layer.getStructure(ind);
        return s;
    }

    public static CopperCable getCable(Structure<?> context, int[] copyRegion, int index) {
        Pair<Structure<?>, CompoundTag> t = null;
        Structure<?> s = null;
        int ind = ((index / Game.G.layer.height) + copyRegion[0]) * Game.G.layer.height + ((index % Game.G.layer.height) + copyRegion[1]);
        if (context.layer == null) {
            t = Game.G.copyCables.get(ind);
            if (t != null) s = t.getKey();
        } else if (s == null) s = context.layer.getCable(ind);
        return (CopperCable) s;
    }

    public static Structure<?> getAny(Structure<?> context, int[] copyRegion, int index, boolean structuresFirst) {
        if (structuresFirst) {
            Structure<?> s = getStructure(context, copyRegion, index);
            if (s != null) return s;
            return getCable(context, copyRegion, index);
        } else {
            CopperCable c = getCable(context, copyRegion, index);
            if (c != null) return c;
            return getStructure(context, copyRegion, index);
        }
    }

}
