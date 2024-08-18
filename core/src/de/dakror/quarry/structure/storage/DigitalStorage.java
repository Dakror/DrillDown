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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;

import de.dakror.common.BiCallback;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class DigitalStorage extends Storage {
    public static final Schema classSchema = new Schema(0, StructureType.DigitalStorage, true, 3,
            3,
            "digitalstorage",
            new Items(ItemType.MachineFrame, 20, ItemType.AdvancedMachineFrame, 5, ItemType.TinPlate, 80, ItemType.Glass, 80, ItemType.CopperWire, 100),
            new Sfx("digitalstorage" + Const.SFX_FORMAT),
            new Dock(0, 1, Direction.West, DockType.ItemIn),
            new Dock(2, 1, Direction.East, DockType.ItemOut), new Dock(1, 2, Direction.North, DockType.Power))
                    .components(new CInventory(20000).setPumpOutSpeed(0))
                    .flags(Flags.ConfirmDestruction)
                    // @Refactor copy paste from parent (Storage) maybe abstract away duplicate code
                    .button(new ButtonDef("icon_pump_out", "button.pump", ButtonType.TempRadio, new BiCallback<Boolean, Structure<?>>() {

                        @Override
                        public void call(Boolean on, Structure<?> data) {
                            DigitalStorage st = (DigitalStorage) data;
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
                            DigitalStorage st = (DigitalStorage) data;
                            TooltipManager.getInstance().enabled = !on;
                            st.refundStorage = on;
                        }
                    }))
                    .sciences(ScienceType.BetterStorage, ScienceType.HighTech);

    static final TextureRegion fillStatus = Quarry.Q.atlas.findRegion("structure_digitalstorage_fill_status");

    public static final float POWER_PER_ITEM = 0.16175f; // ugly number, but results in nice 200k at 20k items
    public static final float POWER_BASE = 100;
    public static final float POWER_CAPACITY = 50000;

    double powerLevel;
    double powerUse;
    boolean noPower;
    final WindowedMean powerLevelMean = new WindowedMean(60);
    protected int framesPassedWithPower;

    public DigitalStorage(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public int getReceiverPriority() {
        return 100;
    }

    @Override
    public double getPowerLevel() {
        return powerLevel;
    }

    @Override
    public double getPowerCapacity() {
        return POWER_CAPACITY;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return !noPower && super.canAccept(item, x, y, dir);
    }

    @Override
    public boolean addToInventory(ItemType item, int amount, Structure<?> source) {
        return !noPower && super.addToInventory(item, amount, source);
    }

    @Override
    public int addToInventoryWithRest(ItemType item, int amount) {
        return noPower ? amount : super.addToInventoryWithRest(item, amount);
    }

    @Override
    public int removeFromInventoryWithRest(ItemType item, int amount) {
        return noPower ? amount : super.removeFromInventoryWithRest(item, amount);
    }

    @Override
    public double acceptPower(double amount, double networkStrength) {
        // limit to inflow of network
        double realAmount = Math.min(amount, networkStrength - powerReceivedThisTick);

        if (realAmount <= 0) return amount;

        double old = powerLevel;
        double add = Math.min(POWER_CAPACITY, powerLevel + realAmount);

        powerReceivedThisTick += add - old;

        powerLevel = add;

        return amount - (add - old);
    }

    @Override
    public boolean getButtonState(int buttonIndex) {
        if (buttonIndex == 1)
            return refundStorage;
        return false;
    }

    @Override
    public void postLoad() {
        super.postLoad();
        CInventory inv = (CInventory) components[0];
        powerUse = inv.getCount() * POWER_PER_ITEM + POWER_BASE;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (gameSpeed == 0) {
            powerLevelMean.addValue(powerLevelMean.getLatest());
        } else {
            powerLevelMean.addValue((float) powerLevel);
        }

        CInventory inv = (CInventory) components[0];
        powerUse = inv.getCount() * POWER_PER_ITEM + POWER_BASE;

        if (gameSpeed > 0) {
            noPower = powerLevel < powerUse / 2;
            if (!noPower) {
                playSfx();
                powerLevel = Math.max(0, powerLevel - powerUse * 60 * deltaTime * gameSpeed);
                if (framesPassedWithPower == 0) {
                    // regained power
                    setItemNotifications();
                }
                framesPassedWithPower++;
                if (framesPassedWithPower > 10) framesPassedWithPower = 10;
                pumping = outputs.size > 0;
            } else {
                pauseSfx();
                framesPassedWithPower = 0;
                pumping = false;
            }
        } else noPower = false;

        super.update(deltaTime, gameSpeed, dirtyBounds);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (framesPassedWithPower < 10) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(ProducerStructure.nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
        }

        float scale = Math.min(1, powerLevelMean.getMean() / POWER_CAPACITY);
        if (scale > 0)
            spriter.add(x * Const.TILE_SIZE + 1, y * Const.TILE_SIZE + 1, Const.Z_STATES,
                    4, (getHeight() * Const.TILE_SIZE - 2) * scale,
                    Substation.powermeterTex.getU(), Substation.powermeterTex.getV2(), Substation.powermeterTex.getU2(),
                    Substation.powermeterTex.getV2() + (Substation.powermeterTex.getV() - Substation.powermeterTex.getV2()) * scale);
    }

    @Override
    protected void drawFillStatus(SpriteRenderer spriter) {
        float rat = ((CInventory) components[0]).getFillRatio();
        if (rat > 0) {

            float width = 76;
            float height = 170;
            float originX = width / 2f;
            float originY = height / 2f;

            spriter.add(x * Const.TILE_SIZE + (getWidth() * Const.TILE_SIZE - width) / 2f,
                    y * Const.TILE_SIZE + (getHeight() * Const.TILE_SIZE - height) / 2f, -.5f,
                    originX, originY,
                    width, height * rat,
                    1, 1, upDirection.rot - Direction.North.rot,
                    fillStatus.getRegionX(),
                    (1 - rat) * fillStatus.getRegionHeight() + fillStatus.getRegionY(),
                    fillStatus.getRegionWidth(),
                    rat * fillStatus.getRegionHeight(),
                    false, false);
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Double("power", powerLevel);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        powerLevel = tag.Double("power");
    }
}
