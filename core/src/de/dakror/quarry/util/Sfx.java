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

package de.dakror.quarry.util;

import net.spookygames.gdx.sfx.SfxSound;

public class Sfx {
    public SfxSound sound;

    public final String sfx;
    public final float pitch;

    public Sfx(String sfx) {
        this(sfx, 1);
    }

    public Sfx(String sfx, float pitch) {
        this.sfx = sfx;
        this.pitch = pitch;
    }
}
