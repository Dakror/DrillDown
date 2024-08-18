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

package de.dakror.quarry.structure.base.component;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.util.Bounds;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class Component {
    protected Structure<?> structure;

    public abstract void init();

    public void onPlacement() {}

    public abstract void update(double deltaTime, int gameSpeed, Bounds dirtyBounds);

    public void postLoad() {}

    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper) {}

    public abstract void saveData(Builder b);

    public abstract void loadData(CompoundTag tag);

    @Override
    public abstract Component clone();

    public void setStructure(Structure<?> structure) {
        this.structure = structure;
    }
}
