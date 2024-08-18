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

package de.dakror.quarry.structure.producer;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.game.Tile.TileMeta;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class AirPurifier extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.AirPurifier, 3, 3,
            "airpurifier",
            new Items(ItemType.StoneBrick, 8, ItemType.SteelPlate, 25, ItemType.Scaffolding, 30),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(10f, "purify").output(new Amount(ItemType.StoneDust, 1)));
                }
            }, new Sfx("airpurifier" + Const.SFX_FORMAT),
            true, new Dock(0, 0, Direction.West, DockType.ItemOut))
                    .sciences(ScienceType.MineExpansion)
                    .flags(Flags.TextureAlwaysUpright);

    private static final float halfWidth = (int) (Const.CHUNK_SIZE * 0.75f) + 0.5f;

    int currentHalfWidth;

    public AirPurifier(int x, int y) {
        super(x, y, classSchema);
        currentHalfWidth = getWidth() / 2 + 1;
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        if (layer != null && layer.getIndex() == 0)
            sleeping = true;

        if (layer != null) {
            unveilArea(getWidth() / 2 + 1);

            // find current radius (first tile in the area which is in fog)
            for (int i = getWidth() / 2; i < halfWidth; i++) {
                for (int j = -i / 2; j < i / 2; j++) {
                    // left edge, right edge, top edge, bottom edge
                    if (layer.isInFogOfWar(x + getWidth() / 2 - i, y + getHeight() / 2 + j)
                            || layer.isInFogOfWar(x + getWidth() / 2 + i, y + getHeight() / 2 + j)
                            || layer.isInFogOfWar(x + getWidth() / 2 + j, y + getHeight() / 2 - i)
                            || layer.isInFogOfWar(x + getWidth() / 2 + j, y + getHeight() / 2 + i)) {
                        currentHalfWidth = i - 1;
                        return;
                    }
                }
            }
        }
    }

    protected boolean unveilArea(int radius) {
        boolean any = false;

        int cx = x + getWidth() / 2;
        int cy = y + getHeight() / 2;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                if (!any && (layer.getMeta(i + cx, j + cy) & TileMeta.FOG_OF_WAR) != 0)
                    any = true;

                layer.removeMeta(i + cx, j + cy, TileMeta.FOG_OF_WAR);
            }
        }

        return any;
    }

    @Override
    protected void doProductionStep() {
        if (currentHalfWidth < halfWidth) {
            currentHalfWidth++;

            if (unveilArea(currentHalfWidth)) {
                super.doProductionStep();
            }
        } else {
            sleeping = true;
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (Game.G.activeStructure == this || clicked) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(1, 0.498f, 0.314f, 0.2f);
            shaper.rect((x + getWidth() / 2f - halfWidth) * Const.TILE_SIZE, (y + getHeight() / 2f - halfWidth) * Const.TILE_SIZE, halfWidth * 2 * Const.TILE_SIZE, halfWidth * 2 * Const.TILE_SIZE);
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Byte("radius", (byte) currentHalfWidth);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        currentHalfWidth = tag.Byte("radius", (byte) 0);
    }
}
