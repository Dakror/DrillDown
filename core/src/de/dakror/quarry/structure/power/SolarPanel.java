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

package de.dakror.quarry.structure.power;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;

import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class SolarPanel extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.SolarPanel, true, 3,
            3, "solarpanel", new Items(ItemType.SiliconWafer, 60, ItemType.Glass, 50, ItemType.CopperWire, 30), null)
                    .loudness(0.1f)
                    .flags(Flags.MirroredTextureHorizontal)
                    .sciences(ScienceType.SolarPower);

    SolarPanelOutlet outlet;

    public SolarPanel(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (outlet == null && layer != null) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(ProducerStructure.nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
        } else if (clicked) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(Color.PURPLE);
            shaper.rect(
                    (x + getWidth() * 0.5f) * Const.TILE_SIZE - 6,
                    (y + getHeight() * 0.5f) * Const.TILE_SIZE - 6, 12, 12);
            shaper.rectLine(
                    (x + getWidth() * 0.5f) * Const.TILE_SIZE,
                    (y + getHeight() * 0.5f) * Const.TILE_SIZE,
                    (outlet.x + outlet.getWidth() * 0.5f) * Const.TILE_SIZE,
                    (outlet.y + outlet.getHeight() * 0.5f) * Const.TILE_SIZE, 3);
            shaper.rect(
                    (outlet.x + outlet.getWidth() * 0.5f) * Const.TILE_SIZE - 6,
                    (outlet.y + outlet.getHeight() * 0.5f) * Const.TILE_SIZE - 6, 12, 12);
        }
    }

    @Override
    public int getDonorPriority() {
        return 1;
    }
}
