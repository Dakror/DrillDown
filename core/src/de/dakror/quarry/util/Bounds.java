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

package de.dakror.quarry.util;

import de.dakror.quarry.structure.DistillationColumn;
import de.dakror.quarry.structure.base.FluidTubeStructure;
import de.dakror.quarry.structure.base.GeneratorStructure;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ItemLift;
import de.dakror.quarry.structure.logistics.ItemLiftBelow;
import de.dakror.quarry.structure.logistics.TubeShaft;
import de.dakror.quarry.structure.logistics.TubeShaftBelow;
import de.dakror.quarry.structure.power.CableShaft;
import de.dakror.quarry.structure.power.CableShaftBelow;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.structure.power.GasTurbine;
import de.dakror.quarry.structure.power.PowerPole;
import de.dakror.quarry.structure.storage.Barrel;
import de.dakror.quarry.structure.storage.Storage;

/**
 * @author Maximilian Stark | Dakror
 */
public class Bounds {
    protected int x, y, width, height;
    protected int flags;

    public static class Flags {
        public static final int CONVEYOR = 1 << 0;
        public static final int FLUIDTUBE = 1 << 1;
        public static final int CABLE = 1 << 2;
        public static final int PRODUCER = 1 << 3;
        public static final int STORAGE = 1 << 4;
        public static final int GENERATOR = 1 << 5;
        public static final int POWERNODE = 1 << 6;
        public static final int SHAFT = 1 << 7;
        public static final int CONSTRUCTION = 1 << 8;
        public static final int DESTRUCTION = 1 << 9;
        public static final int DISTILLATIONCOLUMN = 1 << 10;
        public static final int GASTURBINE = 1 << 11;
    }

    public void clear() {
        width = 0;
        height = 0;
        flags = 0;
    }

    public void add(Structure<?> s, int flags) {
        this.flags |= flags;

        if (s instanceof Conveyor)
            this.flags |= Flags.CONVEYOR;
        if (s instanceof FluidTubeStructure)
            this.flags |= Flags.FLUIDTUBE;
        if (s instanceof CopperCable || s instanceof PowerPole)
            this.flags |= Flags.CABLE;
        if (s instanceof ProducerStructure)
            this.flags |= Flags.PRODUCER;
        if (s instanceof Storage || s instanceof Barrel)
            this.flags |= Flags.STORAGE;
        if (s instanceof GeneratorStructure)
            this.flags |= Flags.GENERATOR;
        if (s instanceof DistillationColumn)
            this.flags |= Flags.DISTILLATIONCOLUMN;
        if (s instanceof GasTurbine)
            this.flags |= Flags.GASTURBINE;
        if (s.getSchema().powerDocks > 0)
            this.flags |= Flags.POWERNODE;
        if (s instanceof CableShaft || s instanceof CableShaftBelow
                || s instanceof ItemLift || s instanceof ItemLiftBelow
                || s instanceof TubeShaft || s instanceof TubeShaftBelow)
            this.flags |= Flags.SHAFT;

        if (width == 0) {
            x = s.x;
            y = s.y;
            width = s.getWidth();
            height = s.getHeight();
            return;
        }
        int minX = Math.min(x, s.x);
        int maxX = Math.max(x + width, s.x + s.getWidth());
        x = minX;
        width = maxX - minX;

        int minY = Math.min(y, s.y);
        int maxY = Math.max(y + height, s.y + s.getHeight());
        y = minY;
        height = maxY - minY;

    }

    public void set(Bounds o) {
        x = o.x;
        y = o.y;
        width = o.width;
        height = o.height;
        flags = o.flags;
    }

    public void set(int x, int y, int width, int height, int flags) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.flags = flags;
    }

    public void add(Bounds o) {
        flags |= o.flags;
        width = Math.max(x + width, o.x + o.width) - Math.min(x, o.x);
        height = Math.max(y + height, o.y + o.height) - Math.min(y, o.y);
        x = Math.min(x, o.x);
        y = Math.min(y, o.y);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // TODO: make faster?
    public boolean touches(Structure<?> structure) {
        if (width == 0) return false;

        // SAT
        return (Math.max(x + width, structure.x + structure.getWidth()) - Math.min(x, structure.x)) - (width + structure.getWidth()) <= 0
                && (Math.max(y + height, structure.y + structure.getHeight()) - Math.min(y, structure.y)) - (height + structure.getHeight()) <= 0;
    }

    // TODO: make faster?
    public boolean touches(int x, int y, int width, int height) {
        if (this.width == 0) return false;

        // SAT
        return (Math.max(this.x + this.width, x + width) - Math.min(this.x, x)) - (this.width + width) <= 0
                && (Math.max(this.y + this.height, y + height) - Math.min(this.y, y)) - (this.height + height) <= 0;
    }

    public boolean intersects(int x, int y, int width, int height) {
        if (this.width == 0) return false;

        return !(x > this.x + this.width
                || x + width < this.x
                || y + height < this.y
                || y > this.y + this.height);
    }

    public boolean isEmpty() {
        return width == 0 && flags == 0;
    }

    @Override
    public String toString() {
        return "[" + x + ":" + y + ", " + width + "x" + height + ", " + Integer.toBinaryString(flags) + "]";
    }

    public boolean hasFlag(int flags) {
        return !isEmpty() && (this.flags & flags) == flags;
    }

    public boolean hasAnyFlag(int flags) {
        return !isEmpty() && (this.flags & flags) != 0;
    }
}
