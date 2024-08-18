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

package de.dakror.quarry.structure.storage;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import de.dakror.common.BiCallback;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;

/**
 * @author Maximilian Stark | Dakror
 */
public class Warehouse extends Storage {
    public static final Schema classSchema = new Schema(0, StructureType.Warehouse, true, 10,
            4,
            "warehouse",
            new Items(ItemType.StoneBrick, 50, ItemType.WoodPlank, 100, ItemType.Scaffolding, 50),
            null,
            new Dock(0, 0, Direction.South, DockType.ItemIn), new Dock(9, 0, Direction.South, DockType.ItemOut))
                    .components(new CInventory(5000).setPumpOutSpeed(Const.DEFAULT_PUMP_OUT_DELAY / 2f))
                    .flags(Flags.ConfirmDestruction, Flags.MirroredTextureHorizontal, Flags.MirroredTextureVertical)
                    // @Refactor copy paste from parent (Storage) maybe abstract away duplicate code
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.TempRadio, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            Warehouse st = (Warehouse) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.outputSelectMode = on;
                            for (Actor a : st.ui.getChildren()) {
                                Table t = (Table) a;
                                if (!st.outputs.contains((ItemType) a.getUserObject(), true)) {
                                    Drawable bg = null;
                                    if (on)
                                        bg = lightBg;
                                    t.setBackground(bg);
                                }
                                t.invalidateHierarchy();
                            }
                        }
                    }))
                    .button(new ButtonDef("icon_destroy", "button.refund", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            Warehouse st = (Warehouse) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.refundStorage = on;
                        }
                    }))
                    .sciences(ScienceType.BetterStorage);

    static final TextureRegion fillStatus = Quarry.Q.atlas.findRegion("structure_warehouse_fill_status");

    public Warehouse(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 1)
            return refundStorage;
        return false;
    }

    @Override
    protected void drawFillStatus(SpriteRenderer spriter) {
        float rat = ((CInventory) components[0]).getFillRatio();
        if (rat > 0) {

            float width = 232;
            float height = 28;
            float originX = width / 2f;
            float originY = height / 2f;

            spriter.add(x * Const.TILE_SIZE + (getWidth() * Const.TILE_SIZE - width) / 2f,
                    y * Const.TILE_SIZE + (getHeight() * Const.TILE_SIZE - height) / 2f, -.5f,
                    originX, originY,
                    width * rat, height,
                    1, 1, upDirection.rot - Direction.North.rot,
                    fillStatus.getRegionX(),
                    fillStatus.getRegionY(),
                    (int) (rat * fillStatus.getRegionWidth()),
                    fillStatus.getRegionHeight(),
                    false, false);
        }
    }
}
