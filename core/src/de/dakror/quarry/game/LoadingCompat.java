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

package de.dakror.quarry.game;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.ByteArrayTag;
import de.dakror.common.libgdx.io.NBT.ByteTag;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.FloatTag;
import de.dakror.common.libgdx.io.NBT.IntTag;
import de.dakror.common.libgdx.io.NBT.ListTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class LoadingCompat {
    public static final LoadingCompat instance = new LoadingCompat();

    private LoadingCompat() {}

    public void upgrade(CompoundTag tag, int build) {
        for (int i = build + 1; i <= Quarry.Q.versionNumber; i++) {
            try {
                getClass().getMethod("version_" + i, CompoundTag.class).invoke(this, tag);
            } catch (NoSuchMethodException e) {
                continue;
            } catch (Exception e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }

    public void version_2(CompoundTag data) throws NBTException {
        // Items moved from layer data to conveyor data
        for (Tag t : data.List("Map", TagType.Compound).data) {
            ListTag chunks = ((CompoundTag) t).List("Chunks", TagType.Compound);
            for (Tag t1 : ((CompoundTag) t).List("Items", TagType.Compound).data) {
                CompoundTag i = (CompoundTag) t1;
                for (Tag t2 : chunks.data) {
                    for (Tag t3 : ((CompoundTag) t2).List("Structures", TagType.Compound).data) {
                        CompoundTag str = (CompoundTag) t3;

                        if (str.Byte("type") == StructureType.Conveyor.id
                                || str.Byte("type") == StructureType.ConveyorBridge.id
                                || str.Byte("type") == StructureType.Hopper.id) {

                            if (!str.has("Items")) {
                                str.add(new ListTag("Items", TagType.Compound));
                            }

                            if (str.Int("x") == i.Int("x") && str.Int("y") == i.Int("y")) {
                                str.List("Items", TagType.Compound).add(i);
                            }
                        }
                    }
                }
            }
        }
    }

    public void version_16(CompoundTag data) throws NBTException {
        // procuder structures have additional byte array for input categories
        for (Tag t : data.List("Map", TagType.Compound).data) {
            ListTag chunks = ((CompoundTag) t).List("Chunks", TagType.Compound);
            for (Tag t1 : chunks.data) {
                ListTag structs = ((CompoundTag) t1).List("Structures", TagType.Compound);
                for (Tag t2 : structs.data) {
                    CompoundTag struct = (CompoundTag) t2;
                    // add empty activeCats
                    if (struct.has("activeTypes")) {
                        struct.add(new ByteArrayTag("activeCats", new byte[struct.ShortArray("activeTypes").length]));
                    }
                }
            }
        }
    }

    public void version_20(CompoundTag data) throws NBTException {
        // refinery, producerstructure, powernode all switched from int power to float power
        for (Tag tag : data.query("#Structures int,#power")) {
            tag.parent.remove(tag);
            tag.parent.add(new FloatTag("power", ((IntTag) tag).data));

        }
    }

    public void version_25(CompoundTag data) throws NBTException {
        for (Tag tag : data.query("#Structures>compound")) {
            CompoundTag t = (CompoundTag) tag;
            if (Structure.types[t.Byte("type") & 0xff] == StructureType.CableShaft) {
                t.parent.remove(t);
            }
        }
    }

    public void version_109(CompoundTag data) throws NBTException {
        // enable all outputs to ensure they are re-validated
        for (Tag tag : data.query("#Structures byte,#output")) {
            ((ByteTag) tag).data = 1;
        }
    }
}
