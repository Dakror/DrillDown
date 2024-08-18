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

package de.dakror.quarry.structure.base;

import com.badlogic.gdx.math.MathUtils;

/**
 * @author Maximilian Stark | Dakror
 */
public enum Direction {
    // Fixed order!! Do not change!!
    North(0, 1), East(1, 0), South(0, -1), West(-1, 0);

    public static final Direction[] values = values();

    public final int dx, dy;

    public final int rot;

    private Direction inv, next, prev;

    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;

        rot = (int) (-MathUtils.radDeg * MathUtils.atan2(-dy, dx));
    }

    public Direction inv() {
        if (inv != null)
            return inv;
        inv = values[(ordinal() + 2) % 4];
        return inv;
    }

    public Direction next() {
        if (next != null)
            return next;
        next = values[(ordinal() + 1) % 4];
        return next;
    }

    public Direction prev() {
        if (prev != null)
            return prev;
        prev = values[(ordinal() + 3) % 4];
        return prev;
    }

    public boolean isPerpendicular(Direction o) {
        return (ordinal() % 2) != (o.ordinal() % 2);
    }
}
