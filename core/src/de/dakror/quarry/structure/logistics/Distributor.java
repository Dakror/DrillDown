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

package de.dakror.quarry.structure.logistics;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.RouterStructure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Distributor extends RouterStructure {
    public static final RouterSchema classSchema = new RouterSchema(0, StructureType.Distributor, false,
            "distributor", new Items(ItemType.StoneBrick, 2, ItemType.TinIngot, 2), null)
                    .sciences(ScienceType.Routers);

    int index;

    static TextureRegion cursor = Quarry.Q.atlas.findRegion("structure_distributor_cursor");
    static TextureRegion base = Quarry.Q.atlas.findRegion("structure_distributor_lower");

    int oldIndex;
    float visual;

    boolean anyTube;

    public Distributor(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        if (getDocks().length > 0)
            drawDocks(spriter);

        spriter.add(base, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, getWidth() * Const.TILE_SIZE, getHeight() * Const.TILE_SIZE);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        int w = 36, h = 16, px = 8, py = 8;

        int to = -index * 90;
        if (to == 0 && oldIndex != index) to = -360;

        float val = Interpolation.linear.apply(-oldIndex * 90, to, visual);

        spriter.add(cursor, (x + 0.5f) * Const.TILE_SIZE - px, (y + 0.5f) * Const.TILE_SIZE - py, Const.Z_STRUCTURES + 0.1f, px, py, w, h, 1, 1, val + 90);

        super.drawFrame(spriter, shaper, pfxBatch);
    }

    @Override
    public void postLoad() {
        super.postLoad();
        for (int i = 0; i < 4; i++) {
            if (tubes[index] != null) {
                anyTube = true;
                break;
            }
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (layer != null && dirtyBounds.touches(this)) {
            anyTube = false;
            for (int i = 0; i < 4; i++) {
                if (tubes[index] == null) {
                    visual = 0;
                    oldIndex = index;
                    index = (index + 1) % 4;
                } else {
                    anyTube = true;
                }
            }
        }

        if (visual < 1) {
            visual += deltaTime * gameSpeed * 4f;
        } else {
            visual = 1;
        }
    }

    @Override
    protected boolean dispatchItem() {
        if (!anyTube) return false;

        oldIndex = index;
        visual = 0;

        boolean success = layer.addItemEntity(currentItem, this, Direction.values[index], currentSource);

        for (int i = 0; i < 4; i++) {
            int ind = (index + i + 1) % 4;
            if (isOutputOpen(ind)) {
                index = ind;
                break;
            }
        }

        return success;
    }

    protected boolean isOutputOpen(int i) {
        Direction d = Direction.values[i];
        return tubes[i] != null && tubes[i].isItemSlotFree(Layer.getStartingSlot(d));
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Int("index", index)
                .Int("old", oldIndex);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        index = tag.Int("index", 0);
        oldIndex = tag.Int("old", 0);
    }
}
