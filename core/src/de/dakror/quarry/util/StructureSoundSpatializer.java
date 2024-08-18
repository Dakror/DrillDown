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

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import de.dakror.quarry.Const;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import net.spookygames.gdx.sfx.spatial.SpatializedSound;
import net.spookygames.gdx.sfx.spatial.Spatializer;

/**
 * @author Maximilian Stark | Dakror
 */
public class StructureSoundSpatializer implements Spatializer<Structure<?>> {

    private float horizontalRange;
    private float verticalRange;
    private final Vector3 center = new Vector3();

    public float getHorizontalRange() {
        return horizontalRange;
    }

    public void setHorizontalRange(float horizontalRange) {
        if (this.horizontalRange == horizontalRange)
            return;
        this.horizontalRange = horizontalRange;
    }

    public float getVerticalRange() {
        return verticalRange;
    }

    public void setVerticalRange(float verticalRange) {
        if (this.verticalRange == verticalRange)
            return;
        this.verticalRange = verticalRange;
    }

    public Vector3 getCenter() {
        return center;
    }

    public void setCenter(float x, float y, float z) {
        if (this.center.epsilonEquals(x, y, z, 0.01f))
            return;
        this.center.set(x, y, z);
    }

    public void setCenter(Vector3 center) {
        if (this.center.epsilonEquals(center, 0.01f))
            return;
        this.center.set(center);
    }

    public boolean canPlayAt(Structure<?> position) {
        if (Game.G.layer.getIndex() != position.layer.getIndex() && position.getSchema().type != StructureType.ShaftDrill) {
            return false;
        }

        float x = position.x + position.getWidth() / 2f;
        float y = position.y + position.getHeight() / 2f;

        float centerX = center.x / Const.TILE_SIZE;
        float centerY = center.y / Const.TILE_SIZE;

        // Horizontal attenuation
        float range2 = horizontalRange * horizontalRange;
        float dst2 = Vector2.dst2(x, y, centerX, centerY);

        float hRatio = 1f - MathUtils.clamp(dst2 / range2, 0f, 1f);

        // Vertical attenuation
        float centerZ = center.z;

        float vRatio = 1f - MathUtils.clamp(centerZ / verticalRange, 0f, 1f);
        vRatio = vRatio * vRatio * vRatio;

        // Result
        return hRatio * vRatio > 0;

    }

    @Override
    public void spatialize(SpatializedSound<Structure<?>> instance, float nominalVolume) {
        Structure<?> position = instance.getPosition();
        float x = position.x + position.getWidth() / 2f;
        float y = position.y + position.getHeight() / 2f;

        float centerX = center.x / Const.TILE_SIZE;
        float centerY = center.y / Const.TILE_SIZE;

        // Horizontal attenuation
        float range2 = horizontalRange * horizontalRange;
        float dst2 = Vector2.dst2(x, y, centerX, centerY);

        float hRatio = 1f - MathUtils.clamp(dst2 / range2, 0f, 1f);

        // Vertical attenuation
        float centerZ = center.z;

        float vRatio = 1f - MathUtils.clamp(centerZ / verticalRange, 0f, 1f);
        vRatio = vRatio * vRatio * vRatio;

        // Panning
        float panning = (x - centerX) / horizontalRange;

        // Result
        float volume = nominalVolume * hRatio * vRatio;

        try {
            if (volume == 0 || (Game.G.layer.getIndex() != position.layer.getIndex() && position.getSchema().type != StructureType.ShaftDrill)) {
                position.stopSfx();
            } else {
                instance.setPan(MathUtils.clamp(panning, -1f, 1f), MathUtils.clamp(volume, 0f, 1f));
            }
        } catch (NullPointerException e) {
            position.stopSfx();
        }
    }
}
