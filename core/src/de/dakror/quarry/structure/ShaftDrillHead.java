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

package de.dakror.quarry.structure;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class ShaftDrillHead extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.ShaftDrillHead, true, ShaftDrill.classSchema.width, ShaftDrill.classSchema.height, "", new Items(), null)
            .flags(Flags.Indestructible, Flags.TextureAlwaysUpright);

    static final TextureRegion core = Quarry.Q.atlas.findRegion("structure_shaft_drill_head");
    static final TextureRegion corner = Quarry.Q.atlas.findRegion("structure_shaft_drill_head_outer");

    ShaftDrill drill;

    // Resolved in postLoad
    int drillIndex, drillLayer;

    float rotation = 0;

    public ShaftDrillHead(int x, int y) {
        super(x, y, classSchema);
    }

    public ShaftDrillHead setDrill(ShaftDrill drill) {
        this.drill = drill;
        return this;
    }

    public ShaftDrill getDrill() {
        return drill;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        if (drill.getActiveRecipe() != null && !drill.isSleeping()) {
            rotation = (rotation - 0.125f * gameSpeed) % 360;
        }

        spriter.add(core, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, rotation);
        spriter.add(core, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, rotation + 90);
        spriter.add(core, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, rotation + 180);
        spriter.add(core, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, rotation - 90);

        if (drill.isClicked()) {
            drawHighlighting(shaper);
        }
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        spriter.add(corner, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, 0);
        spriter.add(corner, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, 90);
        spriter.add(corner, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, 180);
        spriter.add(corner, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_STRUCTURES, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, Const.TILE_SIZE * 2, 1, 1, -90);
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Int("drill", drill.x * drill.layer.height + drill.y)
                .Short("drillLayer", (short) drill.layer.getIndex());
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        drillIndex = tag.Int("drill", -1);
        try {
            drillLayer = tag.Short("drillLayer");
        } catch (NBTException e) {
            drillLayer = tag.Byte("drillLayer", (byte) 0);
        }
    }

    @Override
    public void postLoad() {
        Layer l = Game.G.getLayer(drillLayer);
        if (l == null) return;
        drill = (ShaftDrill) l.getStructure(drillIndex);
    }
}
