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

import java.util.HashSet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class ElectricConveyorCore extends Conveyor {
    public static final ConveyorSchema classSchema = new ConveyorSchema(0, StructureType.ElectricConveyorCore, true, 1, 1,
            "electric_conveyor_core",
            new Items(ItemType.SteelPlate, 1, ItemType.CopperWire, 4, ItemType.Rotor, 1),
            null, new Dock(0, 0, Direction.South, DockType.Power))
                    .flags(Flags.NoDustEffect)
                    .sciences(ScienceType.Electricity)
                    .loudness(0.5f);

    public static final float POWER_CAPACITY = 1000;
    public static final float POWER_USE = 5;
    public static final double POWER_USE_GROW = 1.275f;
    public static final int SPEED_BOOST = 5;

    Table ui;
    Container<Table> container;

    double powerLevel;
    double powerUse;
    boolean noPower;
    final WindowedMean powerLevelMean = new WindowedMean(60);
    protected int framesPassedWithPower;

    final HashSet<Integer> tmp = new HashSet<>();
    Array<ElectricConveyor> connectedConveyors = new Array<>();
    int[] conveyorsIndex;

    public ElectricConveyorCore(int x, int y) {
        super(x, y, classSchema);
        dir = Direction.East;
    }

    public ElectricConveyorCore(int x, int y, Direction direction) {
        super(x, y, classSchema);
        dir = direction;
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        if (!fromLoading && layer != null) {
            resetAdjacentConveyors();
            tmp.clear();
            discoverAdjacentConveyors(this, tmp, 0);
        }
    }

    private void resetAdjacentConveyors() {
        for (ElectricConveyor p : connectedConveyors) {
            p.core = null;
            layer.dirtyBounds.add(p, de.dakror.quarry.util.Bounds.Flags.CONVEYOR);
        }
        connectedConveyors.clear();
    }

    private void discoverAdjacentConveyors(Structure<?> of, HashSet<Integer> history, int depth) {
        if (((of instanceof ElectricConveyor && ((ElectricConveyor) of).core == null) || of == this)
                && !history.contains(of.getX() * layer.height + of.getY())) {
            history.add(of.getX() * layer.height + of.getY());
            if (of instanceof ElectricConveyor) {
                ElectricConveyor e = (ElectricConveyor) of;
                connectedConveyors.add(e);
                e.core = this;
            }

            //  only forward connection
            if (((Conveyor) of).structures[3] != null) {
                discoverAdjacentConveyors(((Conveyor) of).structures[3], history, depth + 1);
            }
        }

        powerUse = POWER_USE * Math.pow(POWER_USE_GROW, connectedConveyors.size);

        if (clicked) {
            updateUI();
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<>();
        }
        if (ui == null) {
            ui = GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "");
            container.setActor(ui);
        }

        ((Label) ui.getChildren().get(1)).setText(GameUi.formatPowerAmount(powerUse * 60) + "/s");
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(container).grow();
    }

    @Override
    public void postLoad() {
        super.postLoad();

        for (int i : conveyorsIndex) {
            Structure<?> p = layer.getStructure(i);
            if (p instanceof ElectricConveyor) {
                connectedConveyors.add((ElectricConveyor) p);
                ((ElectricConveyor) p).core = this;
            }
        }

        powerUse = POWER_USE * Math.pow(POWER_USE_GROW, connectedConveyors.size);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetAdjacentConveyors();
    }

    @Override
    public double getPowerCapacity() {
        return POWER_CAPACITY;
    }

    @Override
    public int getReceiverPriority() {
        return 50;
    }

    @Override
    public double getPowerLevel() {
        return powerLevel;
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
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (gameSpeed == 0) {
            powerLevelMean.addValue(powerLevelMean.getLatest());
        } else {
            powerLevelMean.addValue((float) powerLevel);
        }

        if (gameSpeed > 0) {
            noPower = powerLevel < powerUse / 2;
            if (!noPower) {
                powerLevel = Math.max(0, powerLevel - powerUse * 60 * deltaTime * gameSpeed);
                if (framesPassedWithPower == 0) {
                    // just regained power
                    notifyNeighbors(true);
                }
                framesPassedWithPower++;
                if (framesPassedWithPower > 10) framesPassedWithPower = 10;
            } else {
                framesPassedWithPower = 0;
            }
        } else noPower = false;

        if (dirtyBounds.hasFlag(de.dakror.quarry.util.Bounds.Flags.CONVEYOR)) {
            resetAdjacentConveyors();
            tmp.clear();
            discoverAdjacentConveyors(this, tmp, 0);
        }

        super.update(deltaTime, gameSpeed * SPEED_BOOST, dirtyBounds);
    }

    @Override
    public void postUpdate(Bounds dirtyBounds) {
        super.postUpdate(dirtyBounds);

        if (dirtyBounds.hasFlag(de.dakror.quarry.util.Bounds.Flags.CONVEYOR)) {
            resetAdjacentConveyors();
            tmp.clear();
            discoverAdjacentConveyors(this, tmp, 0);
        }
    }

    @Override
    public void updateItems(double deltaTime, int gameSpeed, boolean chain) {
        super.updateItems(deltaTime, noPower ? 0 : ElectricConveyorCore.SPEED_BOOST * gameSpeed, chain);
    }

    @Override
    public boolean isItemSlotFree(int slot) {
        return noPower ? false : super.isItemSlotFree(slot);
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        drawDocks(spriter);
        super.draw(spriter);
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

        if (clicked) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(Color.PURPLE);
            shaper.rect(
                    (x + getWidth() * 0.5f) * Const.TILE_SIZE - 10,
                    (y + getHeight() * 0.5f) * Const.TILE_SIZE - 10, 20, 20);
            Structure<?> q = this;
            for (Structure<?> p : connectedConveyors) {
                shaper.rectLine(
                        (q.x + q.getWidth() * 0.5f) * Const.TILE_SIZE,
                        (q.y + q.getHeight() * 0.5f) * Const.TILE_SIZE,
                        (p.x + p.getWidth() * 0.5f) * Const.TILE_SIZE,
                        (p.y + p.getHeight() * 0.5f) * Const.TILE_SIZE, 3);
                shaper.rect(
                        (p.x + p.getWidth() * 0.5f) * Const.TILE_SIZE - 6,
                        (p.y + p.getHeight() * 0.5f) * Const.TILE_SIZE - 6, 12, 12);
                q = p;
            }
        }
    }

    @Override
    public void setRotation(Direction direction) {
        super.setRotation(direction);
        setUpDirection(direction.prev()); // by default we're facing east so we go one dir back to get north
    }

    @Override
    public Object clone() {
        ElectricConveyorCore core = new ElectricConveyorCore(x, y, dir);
        core.setUpDirection(upDirection);
        return core;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        int[] arr = new int[connectedConveyors.size];
        for (int i = 0; i < arr.length; i++)
            arr[i] = connectedConveyors.get(i).x * layer.height + connectedConveyors.get(i).y;
        b
                .IntArray("conveyors", arr)
                .Double("power", powerLevel);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);

        conveyorsIndex = tag.IntArray("conveyors", new int[] {});
        powerLevel = tag.Double("power");
    }

}
