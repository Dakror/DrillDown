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

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.CopyRegionHelper;

/**
 * @author Maximilian Stark | Dakror
 */
public class PowerPoleGhost extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.PowerPoleGhost, false, 1, 1, "powerpoleghost", new Items(), null)
            .flags(Flags.Indestructible);

    PowerPole source;
    Structure<?> target;
    int sourceIndex, targetIndex;

    int dockIndex = -1, absDockIndex;

    public PowerPoleGhost(int x, int y) {
        super(x, y, classSchema);
    }

    public PowerPoleGhost(int x, int y, PowerPole source, Structure<?> target) {
        super(x, y, classSchema);
        this.source = source;
        this.target = target;
    }

    @Override
    public void postLoad() {
        super.postLoad();

        try {
            source = (PowerPole) layer.getStructure(sourceIndex);
            target = layer.getStructure(targetIndex);
        } catch (Exception e) {
            layer.removeStructure(this);
        }
    }

    public int getDockIndex() {
        if (dockIndex == -1) {
            for (int i = 0, j = 0; i < target.getDocks().length; i++) {
                Dock d = target.getDocks()[i];
                if (d.type == DockType.BigPower && target.isNextToDock(x, y, d.dir.inv(), d)) {
                    dockIndex = j;
                    absDockIndex = i;
                    break;
                }
                if (d.type == DockType.Power || d.type == DockType.BigPower) j++;
            }
        }

        return dockIndex;
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        // determine rotation
        getDockIndex();
        Direction dir = target.getDocks()[absDockIndex].dir;

        spriter.add(getSchema().tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.Z_CABLES,
                (getWidth() / 2f) * Const.TILE_SIZE, (getHeight() / 2f) * Const.TILE_SIZE,
                getWidth() * Const.TILE_SIZE, getHeight() * Const.TILE_SIZE, 1, 1, dir.rot - 90);
    }

    public PowerPole getPowerPole() {
        return source;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);

        b.Int("src", source.x * layer.height + source.y);
        b.Int("tgt", target.x * layer.height + target.y);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);

        sourceIndex = tag.Int("src");
        targetIndex = tag.Int("tgt");
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Int("src", CopyRegionHelper.indexGlobalToRelative(copyRegion, source.x * layer.height + source.y));
        b.Int("tgt", CopyRegionHelper.indexGlobalToRelative(copyRegion, target.x * layer.height + target.y));
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);

        Structure<?> s = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("src", 0));
        if (s instanceof PowerPole) source = (PowerPole) s;
        else source = null;
        target = CopyRegionHelper.getStructure(this, pasteRegion, tag.Int("tgt", 0));
    }
}
