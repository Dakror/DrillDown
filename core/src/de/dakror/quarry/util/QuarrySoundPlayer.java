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

import de.dakror.quarry.structure.base.Structure;
import net.spookygames.gdx.sfx.SfxSound;
import net.spookygames.gdx.sfx.spatial.FadingSpatializedSoundPlayer;

/**
 * @author Maximilian Stark | Dakror
 */
public class QuarrySoundPlayer extends FadingSpatializedSoundPlayer<Structure<?>> {
    @Override
    public long play(Structure<?> position, SfxSound sound, float pitch, boolean looping) {
        if (((StructureSoundSpatializer) getSpatializer()).canPlayAt(position))
            return super.play(position, sound, pitch, looping);
        else return -1;
    }

    @Override
    public long play(Structure<?> position, SfxSound sound, float pitch, boolean looping, boolean fadeIn) {
        if (((StructureSoundSpatializer) getSpatializer()).canPlayAt(position))
            return super.play(position, sound, pitch, looping, fadeIn);
        else return -1;
    }
}
