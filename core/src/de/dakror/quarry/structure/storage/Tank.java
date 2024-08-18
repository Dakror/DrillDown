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

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;

import de.dakror.common.BiCallback;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Tank extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.Tank, true, 4,
            2,
            "tank",
            new Items(ItemType.Scaffolding, 5, ItemType.CopperPlate, 40, ItemType.CopperTube, 2), null, new Dock(0, 0, Direction.West, DockType.FluidIn), new Dock(3, 0, Direction.East, DockType.FluidOut))
                    .components(new CTank(20000, 1, false).setPumpOutDelay(0).setMaxOutput(50))
                    .flags(Flags.ConfirmDestruction)
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.StateToggle, new BiCallback<Boolean, Structure<?>>() {
                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            ((CTank) ((Tank) data).components[0]).setOutputEnabled(on);
                        }
                    }))
                    .sciences(ScienceType.WaterUsage);

    Table ui;
    Container<Table> container;
    ItemType uiType;

    public Tank(int x, int y) {
        super(x, y, classSchema);
    }

    protected Tank(int x, int y, Schema schema) {
        super(x, y, schema);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (!((IStorage) components[0]).hasSpace()) {
            drawFullState(spriter);
        }
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 0)
            return ((CTank) components[0]).isOutputEnabled();
        return false;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        CTank tank = ((CTank) components[0]);
        if (isNextToDock(x, y, dir, getDocks()[0])) {
            return tank.canAccept(item);
        }

        return false;
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (item.categories.contains(ItemCategory.IntermediateOil)) // volatile
            item = ItemType.CrudeOil;

        return ((CTank) components[0]).addWithRest(item, amount);
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (clicked) {
            updateUI();
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<>();
        }
        if (ui == null || uiType != ((CTank) components[0]).getFluid()) {
            uiType = ((CTank) components[0]).getFluid();
            if (uiType != null) {
                ui = GameUi.createResourceTable(32, Quarry.Q.skin, uiType, GameUi.formatResourceAmount(((CTank) components[0]).getCount() / 1000f, true) + "L");
                container.setActor(ui);
            } else {
                ui = null;
                container.setActor(null);
            }
        } else {
            ((Label) ui.getChildren().get(1)).setText(GameUi.formatResourceAmount(((CTank) components[0]).getCount() / 1000f, true) + "L");
            ((TextTooltip) ui.getListeners().get(0)).getActor().setText(GameUi.formatResourceAmount(((CTank) components[0]).getCount() / 1000f, true) + "L " + uiType.title);

        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(container).grow();
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("pumping", ((CTank) components[0]).isOutputEnabled() ? (byte) 1 : (byte) 0);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        ((CTank) components[0]).setOutputEnabled(tag.Byte("pumping", (byte) 0) == 1);
    }
}
