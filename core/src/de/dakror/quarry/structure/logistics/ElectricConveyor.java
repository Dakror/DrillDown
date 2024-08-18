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

package de.dakror.quarry.structure.logistics;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class ElectricConveyor extends Conveyor {
    public static final ConveyorSchema classSchema = new ConveyorSchema(0, StructureType.ElectricConveyor, true, 1, 1,
            "electric_conveyor", new Items(ItemType.SteelPlate, 1, ItemType.CopperWire, 4), null)
                    .flags(Flags.Draggable, Flags.NoDustEffect)
                    .sciences(ScienceType.Electricity)
                    .loudness(0.5f);

    public ElectricConveyorCore core;

    public ElectricConveyor(int x, int y) {
        super(x, y, classSchema);
        dir = Direction.East;
    }

    public ElectricConveyor(int x, int y, Direction direction) {
        super(x, y, classSchema);
        dir = direction;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (layer != null && (core == null || core.noPower)) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(ProducerStructure.nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
        }
    }

    @Override
    public void updateItems(double deltaTime, int gameSpeed, boolean chain) {
        super.updateItems(deltaTime, (core == null || core.noPower) ? 0 : ElectricConveyorCore.SPEED_BOOST * gameSpeed, chain);
    }

    @Override
    public boolean isItemSlotFree(int slot) {
        return (core == null || core.noPower) ? false : super.isItemSlotFree(slot);
    }

    @Override
    public Object clone() {
        return new ElectricConveyor(x, y, dir);
    }
}
