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

package de.dakror.quarry.structure;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.game.Tile;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class ShaftDrill extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.ShaftDrill, 4, 4,
            "drill",
            new Items(ItemType.SteelIngot, 15, ItemType.SteelPlate, 30, ItemType.StoneBrick, 50),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(300f, "drill").input(new Amount(ItemType.Water, 75000), new Amount(ItemType.SteelIngot, 180), new Amount(ItemType.ConcretePowder, 120)));
                }
            }, new Sfx("shaftdrill" + Const.SFX_FORMAT),
            false,
            new Dock(3, 0, Direction.East, DockType.FluidIn, new DockFilter(ItemType.Water)),
            new Dock(2, 0, Direction.South, DockType.ItemIn, new DockFilter(ItemType.SteelIngot)), new Dock(3, 0, Direction.South, DockType.ItemIn, new DockFilter(ItemType.ConcretePowder)))
                    .flags(Flags.ConfirmDestruction)
                    .sciences(ScienceType.MineExpansion);

    static TextureRegion jammedIcon = Quarry.Q.atlas.findRegion("icon_jammed");

    int depth;

    public ShaftDrill(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (int i = layer.getIndex() + 1; i < Game.G.getLayerCount(); i++) {
            Layer l = Game.G.getLayer(i);
            Structure<?> s = l.getStructure(x, y);
            if (s instanceof ShaftDrillHead) {
                l.removeStructure(s);
            } else break;
        }
    }

    private boolean isJammed() {
        if (layer == null) return false;

        if (layer.getIndex() == Game.G.getLayerCount() - 1) {
            return false;
        } else {
            for (int ind = 1; ind <= depth + 1; ind++) {
                Layer l = Game.G.getLayer(layer.getIndex() + ind);
                if (l == null) break;

                for (int i = 0; i < getWidth(); i++) {
                    for (int j = 0; j < getHeight(); j++) {
                        Structure<?> s = l.getAny(x + i, y + i, true);
                        if (s != null && !(s instanceof ShaftDrillHead)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (isJammed()) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(jammedIcon, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 3,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
        }
    }

    @Override
    protected boolean additionalWorkBlockingCondition() {
        return isJammed();
    }

    @Override
    protected void doProductionStep() {
        Layer l = null;
        if (layer.getIndex() + depth == Game.G.getLayerCount() - 1) {
            l = Game.G.addLayer();
        } else {
            l = Game.G.getLayer(layer.getIndex() + depth + 1);
        }

        int outer = 2;

        for (int i = -outer; i < getWidth() + outer; i++) {
            for (int j = -outer; j < getHeight() + outer; j++) {
                l.removeMeta(x + i, y + j, Tile.TileMeta.FOG_OF_WAR);
            }
        }

        if (l.addStructure(new ShaftDrillHead(x, y).setDrill(this))) {
            depth++;
        }
    }

    @Override
    protected void playSfx() {
        if (sfxPlaying || getSchema().sfx == null || !Quarry.Q.sound.isPlaySound()) return;

        if (sfxId != -1) {
            Game.G.spatializedPlayer.resume(sfxId);
            sfxPlaying = true;
        } else {
            sfxId = Game.G.spatializedPlayer.play(this, getSchema().sfx.sound, getSchema().sfx.pitch, true, true);
            sfxPlaying = sfxId != -1;
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Int("depth", depth);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        depth = tag.Int("depth", 0);
    }
}
