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

package de.dakror.quarry.ui;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TransformDrawable;

/**
 * @author Maximilian Stark | Dakror
 */
public class PalletDrawable extends BaseDrawable implements TransformDrawable {
    TransformDrawable base;
    TransformDrawable stackable;

    final float s = 0.58f;

    public PalletDrawable(TransformDrawable base, TransformDrawable stackable) {
        this.base = base;
        this.stackable = stackable;
        setMinWidth(1);
        setMinHeight(1);
    }

    @Override
    public void draw(Batch batch, float x, float y, float width, float height) {
        base.draw(batch, x, y, width, height);
        stackable.draw(batch, x + (width - width * s) / 2, y + (height - height * s) / 2, width * s, height * s);
    }

    @Override
    public void draw(Batch batch, float x, float y, float originX, float originY, float width, float height, float scaleX,
            float scaleY, float rotation) {
        base.draw(batch, x, y, originX, originY, width, height, scaleX, scaleY, rotation);
        stackable.draw(batch, x + (width - width * s) / 2, y + (height - height * s) / 2, originX, originY, width * s, height * s, scaleX, scaleY, rotation);
    }
}
