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

package de.dakror.quarry.structure.power;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

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
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.power.Substation.SubstationSchema;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Substation extends Structure<SubstationSchema> {
    public static class SubstationSchema extends Schema {
        public final int capacity, leakage;

        public SubstationSchema(int version, StructureType type, int width, int height, int capacity, int leakage, String tex, Items buildCosts, Sfx sfx, int numFakeDocks, Dock... docks) {
            super(version, type, true, width, height, tex, buildCosts, sfx, docks);
            this.capacity = capacity;
            this.leakage = leakage;
            this.powerDocks += numFakeDocks;
        }
    }

    public static final TextureRegion powermeterTex = Quarry.Q.atlas.findRegion("power_meter");

    public static final SubstationSchema classSchema = new SubstationSchema(0, StructureType.Substation, 1, 1, 200_000, 20,
            "power_node",
            new Items(ItemType.Brick, 8, ItemType.BronzePlate, 10, ItemType.CopperWire, 48), new Sfx("node" + Const.SFX_FORMAT),
            0,
            new Dock(0, 0, Direction.North, DockType.Power),
            new Dock(0, 0, Direction.East, DockType.Power),
            new Dock(0, 0, Direction.South, DockType.Power), new Dock(0, 0, Direction.West, DockType.Power))
                    .sciences(ScienceType.Electricity, ScienceType.Routers)
                    .flags(Flags.NotRotatable);

    double powerLevel;
    private final WindowedMean powerLevelMean = new WindowedMean(60);
    private final WindowedMean powerInMean = new WindowedMean(60);
    private final WindowedMean powerOutMean = new WindowedMean(60);

    float powerInTick, powerOutTick;

    Table ui;
    Container<Table> container;

    float timePassed;

    public Substation(int x, int y) {
        this(x, y, classSchema);
    }

    protected Substation(int x, int y, SubstationSchema schema) {
        super(x, y, schema);
    }

    @Override
    public void postLoad() {
        super.postLoad();
    }

    @Override
    public double acceptPower(double amount, double networkStrength) {
        // limit to inflow of network
        double realAmount = Math.min(amount, networkStrength - powerReceivedThisTick);

        if (realAmount <= 0) return amount;

        double old = powerLevel;
        double add = Math.min(getSchema().capacity, powerLevel + realAmount);

        powerReceivedThisTick += add - old;

        powerInTick += add - old;

        powerLevel = add;
        return amount - (add - old);
    }

    @Override
    public void refundPower(double power) {
        powerLevel += power;

        powerOutTick -= power;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (powerLevel > 0) {
            float scale = Math.min(1, powerLevelMean.getMean() / getSchema().capacity);
            if (scale > 0)
                spriter.add(x * Const.TILE_SIZE + 1, y * Const.TILE_SIZE + 1, Const.Z_STATES,
                        4, (getHeight() * Const.TILE_SIZE - 2) * scale,
                        Substation.powermeterTex.getU(), Substation.powermeterTex.getV2(), Substation.powermeterTex.getU2(),
                        Substation.powermeterTex.getV2() + (Substation.powermeterTex.getV() - Substation.powermeterTex.getV2()) * scale);
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (gameSpeed == 0) {
            if (powerLevelMean.getValueCount() == 0) {
                powerLevelMean.addValue((float) powerLevel);
                powerInMean.addValue(0);
                powerOutMean.addValue(0);
            } else {
                powerLevelMean.addValue(powerLevelMean.getLatest());
                powerInMean.addValue(powerInMean.getLatest());
                powerOutMean.addValue(powerOutMean.getLatest());
            }
            pauseSfx();
            return;
        } else {
            powerLevelMean.addValue((float) powerLevel);
            powerInMean.addValue(powerInTick);
            powerOutMean.addValue(powerOutTick);
            powerInTick = 0;
            powerOutTick = 0;
        }

        if (powerLevel > 0) {
            playSfx();

            double old = powerLevel;
            powerLevel = Math.max(0.0, powerLevel - getSchema().leakage * deltaTime * gameSpeed);

            powerOutTick += old - powerLevel;

            powerNetwork.addOutTick(old - powerLevel);
        } else {
            pauseSfx();
        }

        if (timePassed >= 1f) {
            if (clicked) {
                updateUI();
            }

            timePassed = 0;
        } else {
            timePassed += deltaTime;
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

        try {
            powerLevel = tag.Double("power");
        } catch (NBTException e) {
            powerLevel = tag.Float("power", 0);
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<Table>().fill();
        }
        if (ui == null) {
            ui = new Table();
            ui.defaults().left().expandX();
            ui.row();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "Net-IN: "));
            ui.add(new Label("", Quarry.Q.skin)).right();
            ui.row();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "IN: "));
            ui.add(new Label("", Quarry.Q.skin)).right();
            ui.row();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "STR: "));
            ui.add(new Label("", Quarry.Q.skin)).right();
            ui.row();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "OUT: "));
            ui.add(new Label("", Quarry.Q.skin)).right();
            ui.row();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "Net-OUT: "));
            ui.add(new Label("", Quarry.Q.skin)).right();
            container.setActor(ui);
        }

        ((Label) ui.getChildren().get(1)).setText(GameUi.formatPowerAmount(powerNetwork.getMeanInPerSecond()) + "/s");
        ((Label) ui.getChildren().get(3)).setText(GameUi.formatPowerAmount((powerInMean.getMean() / powerNetwork.getTimeTickMean())) + "/s");
        ((Label) ui.getChildren().get(5)).setText(GameUi.formatPowerAmount(powerLevel));
        ((Label) ui.getChildren().get(7)).setText(GameUi.formatPowerAmount((powerOutMean.getMean() / powerNetwork.getTimeTickMean())) + "/s");
        ((Label) ui.getChildren().get(9)).setText(GameUi.formatPowerAmount(powerNetwork.getMeanOutPerSecond()) + "/s");
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();
        content.add(container).grow();
    }

    @Override
    public int getDonorPriority() {
        return 10;
    }

    @Override
    public int getReceiverPriority() {
        return 10;
    }

    @Override
    public double getPowerLevel() {
        return powerLevel;
    }

    @Override
    public double getPowerCapacity() {
        return getSchema().capacity;
    }

    @Override
    public double requestPower(double power, double networkStrength) {
        double deducted = Math.min(power, powerLevel);
        this.powerLevel -= deducted;

        powerOutTick += deducted;

        return deducted;
    }
}
