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

package de.dakror.quarry.structure.base;

import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class OvenStructure extends ProducerStructure {
    protected PooledEffect pfx;

    SpriterDelegateBatch pfxBatch;

    protected OvenStructure(int x, int y, ProducerSchema schema) {
        super(x, y, schema);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        pfx = Game.G.firePfx.obtain();
        pfx.reset();
        initPfx();
    }

    protected abstract void initPfx();

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (pfxBatch != null && pfx != null) {
            pfx.update((float) (deltaTime * gameSpeed));
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        this.pfxBatch = pfxBatch;

        if (sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction()) || noPower)
            return;

        if (pfxBatch != null && pfx != null) {
            pfxBatch.setNextZ(Const.Z_STATES);
            pfx.draw(pfxBatch);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (pfx != null) {
            pfx.free();
        }
    }
}
