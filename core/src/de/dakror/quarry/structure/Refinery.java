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

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.FluidTubeStructure;
import de.dakror.quarry.structure.base.PausableSchema;
import de.dakror.quarry.structure.base.PausableStructure;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.logistics.Valve;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Refinery extends PausableStructure<PausableSchema> {
    public static class RefineryRecipe {
        public final float crudeTime = 2, refTime = 5;
        public final int crudeInput = 10000, refInput = 8000 * 3;
        public final int crudeOutput = 7500, refOutput = 6000 * 3;
        public final float power = 600;
    }

    public static final PausableSchema classSchema = new PausableSchema(0, StructureType.Refinery, true, 9, 9,
            "refinery",
            new Items(ItemType.MachineFrame, 15, ItemType.SteelTube, 60, ItemType.CopperTube, 100, ItemType.CopperPlate, 150, ItemType.StoneBrick, 250, ItemType.SteelWire, 250),
            new Sfx("refinery" + Const.SFX_FORMAT),
            new Dock(8, 4, Direction.East, DockType.FluidOut),

            new Dock(0, 4, Direction.West, DockType.FluidIn, new DockFilter(ItemType.CrudeOil)),
            new Dock(0, 1, Direction.West, DockType.FluidIn, new DockFilter(ItemType.IntermediateOilToRefinery)),

            new Dock(1, 0, Direction.South, DockType.FluidOut),
            new Dock(1, 8, Direction.North, DockType.FluidIn, new DockFilter(ItemType.IntermediateOilToRefinery)),

            new Dock(0, 7, Direction.West, DockType.FluidOut),
            new Dock(8, 7, Direction.East, DockType.FluidIn, new DockFilter(ItemType.IntermediateOilToRefinery)),

            new Dock(7, 8, Direction.North, DockType.FluidOut),
            new Dock(7, 0, Direction.South, DockType.FluidIn, new DockFilter(ItemType.IntermediateOilToRefinery)),

            new Dock(8, 1, Direction.East, DockType.FluidOut), new Dock(4, 8, Direction.North, DockType.Power))
                    .components(new CTank(50000) /* input */, new CTank(100000) /*processing*/, new CTank(100000, 0).setPumpOutDelay(0)/*output*/)
                    .sciences(ScienceType.OilProcessing)
                    .flags(Flags.MirroredTextureHorizontal, Flags.TextureAlwaysUpright);

    public static final RefineryRecipe recipe = new RefineryRecipe();

    // columns
    Structure<?>[] outputs;
    int enabledOutputs;

    CTank t0, t1, t2;

    private static final float CRUDE_OIL_PROCESSING_RATIO = recipe.crudeOutput / (float) recipe.crudeInput;
    private static final float OIL_PROCESSING_RATIO = recipe.refOutput / (float) recipe.refInput;

    float workingTime0;
    float workingTime1;

    double powerLevel;
    private final WindowedMean powerLevelMean = new WindowedMean(60);
    boolean noPower;
    protected int framesPassedWithPower;

    Table ui;
    Container<Table> container;

    public Refinery(int x, int y) {
        super(x, y, classSchema);

        outputs = new Structure[4];
        t0 = (CTank) components[0];
        t1 = (CTank) components[1];
        t2 = (CTank) components[2];
        workingTime0 = -1;
        workingTime1 = -1;
    }

    @Override
    public float getLoudness() {
        return sleeping || noPower ? 0.1f : ((workingTime0 > -1 ? 1 : 0.3f) + (workingTime1 > -1 ? 1 : 0.3f)) * speedScale;
    }

    private void updateOutputs() {
        int enabled = 0;
        for (int i = 3, k = 0; i < getDocks().length; i += 2, k++) {
            Structure<?> s = layer.getStructure(
                    x + getDocks()[i].x + getDocks()[i].dir.dx,
                    y + getDocks()[i].y + getDocks()[i].dir.dy);
            if (s instanceof FluidTubeStructure) {
                if (s instanceof Valve) {
                    Valve v = (Valve) s;
                    // disable wrong side of valve
                    if (getDocks()[i].dir == v.getDirection()) {
                        outputs[k] = null;
                        continue;
                    }
                }
                outputs[k] = s;
                enabled++;
            } else outputs[k] = null;
        }

        enabledOutputs = enabled;
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateOutputs();
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
        return recipe.power;
    }

    @Override
    public double acceptPower(double amount, double networkStrength) {
        if (sleeping) return amount;
        // limit to inflow of network
        double realAmount = Math.min(amount, networkStrength - powerReceivedThisTick);

        if (realAmount <= 0) return amount;

        double old = powerLevel;
        double add = Math.min(recipe.power, powerLevel + realAmount);

        powerReceivedThisTick += add - old;

        powerLevel = add;

        return amount - (add - old);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (!fromLoading && layer != null) {
            updateOutputs();
        }
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (!item.categories.contains(ItemCategory.Fluid)) return false;
        if (item == ItemType.CrudeOil) {
            return isNextToDock(x, y, dir, getDocks()[1]) && t0.hasSpace();
        } else if (item == ItemType.IntermediateOilToRefinery) {
            boolean any = false;
            for (int i = 2; i < getDocks().length; i += 2) {
                if (isNextToDock(x, y, dir, getDocks()[i])) {
                    any = true;
                    break;
                }
            }
            if (!any) return false;

            return t1.hasSpace();
        }

        return false;
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (item == ItemType.CrudeOil) {
            return t0.addWithRest(item, amount);
        } else if (item == ItemType.IntermediateOilToRefinery) {
            return t1.addWithRest(item, amount);
        }

        return amount;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);
        if (workingTime0 > 0) {
            shaper.setColor(0, 0.5f, 0, 1);
            float progress = 1 - workingTime0 / recipe.crudeTime;
            shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * Const.TILE_SIZE, 8);
        }

        if (workingTime1 > 0) {
            shaper.setColor(0, 0.5f, 0, 1);
            float progress = 1 - workingTime1 / recipe.refTime;
            shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE + 10, progress * getWidth() * Const.TILE_SIZE, 8);
        }

        if ((workingTime0 > 0 || workingTime1 > 0) && framesPassedWithPower < 10) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(ProducerStructure.nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
        }

        float scale = powerLevelMean.getMean() / recipe.power;
        if (scale > 0)
            spriter.add(x * Const.TILE_SIZE + 1, y * Const.TILE_SIZE + 1, Const.Z_STATES,
                    4, getHeight() * Const.TILE_SIZE * scale - 2,
                    Substation.powermeterTex.getU(), Substation.powermeterTex.getV2(), Substation.powermeterTex.getU2(),
                    Substation.powermeterTex.getV2() + (Substation.powermeterTex.getV() - Substation.powermeterTex.getV2()) * scale);

        drawBoostState(spriter);

        if ((enabledOutputs == 0 && !t0.isEmpty()) || !t1.hasSpace() || !t2.hasSpace()) {
            drawFullState(spriter);
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (gameSpeed == 0) {
            powerLevelMean.addValue(powerLevelMean.getLatest());
        } else {
            powerLevelMean.addValue((float) powerLevel);
        }

        if (dirtyBounds.touches(this)) {
            updateOutputs();
        }

        if (clicked) {
            updateUI();
        }

        if (sleeping) {
            pauseSfx();
            return;
        }

        if (gameSpeed == 0 || (workingTime0 == -1 && workingTime1 == -1) || noPower) {
            pauseSfx();
        } else {
            playSfx();
        }

        if (workingTime0 > -1 || workingTime1 > -1 && gameSpeed > 0) {
            noPower = powerLevel < recipe.power;
            if (!noPower) {
                powerLevel = Math.max(0, powerLevel - recipe.power * 60 * deltaTime * gameSpeed);
                framesPassedWithPower++;
                if (framesPassedWithPower > 10) framesPassedWithPower = 10;
            } else {
                framesPassedWithPower = 0;
            }
        } else {
            noPower = false;
        }

        // update recipe working

        if (workingTime0 == -1 && t0.getCount() >= recipe.crudeInput) {
            workingTime0 = recipe.crudeTime;
        }

        // working starts at >50% of power but at 0% of speed
        float powerSpeedScale = Math.max(0, (powerLevelMean.getMean() / recipe.power) * 2 - 1);

        if (t0.getCount() >= recipe.crudeInput && enabledOutputs > 0 && workingTime0 >= 0 && !noPower) {

            workingTime0 -= deltaTime * gameSpeed * powerSpeedScale;
            if (workingTime0 <= 0) {
                int dif = Math.round(recipe.crudeInput / (float) enabledOutputs);

                for (int i = 0; i < outputs.length; i++) {
                    if (outputs[i] != null) {
                        t0.removeUnsafe(dif - (int) (outputs[i].acceptFluid(ItemType.IntermediateOilToColumn, (int) (Math.min(t0.getCount(), dif) * CRUDE_OIL_PROCESSING_RATIO), this) / CRUDE_OIL_PROCESSING_RATIO));
                    }
                }

                workingTime0 = -1;
            }
        }

        if (workingTime1 == -1 && t1.getCount() >= recipe.refInput) {
            workingTime1 = recipe.refTime;
        }

        if (t1.getCount() >= recipe.refInput && t2.getCount() + recipe.refOutput <= t2.getSize() && workingTime1 >= 0 && !noPower) {
            workingTime1 -= deltaTime * gameSpeed * powerSpeedScale;
            if (workingTime1 <= 0) {
                t1.removeUnsafe(recipe.refInput - (int) (t2.addWithRest(ItemType.RefinedOil, recipe.refOutput) / OIL_PROCESSING_RATIO));
                workingTime1 = -1;
            }
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<>();
        }
        if (ui == null) {
            ui = new Table();
            ui.defaults().uniform().growX().pad(10);

            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, ItemType.CrudeOil, GameUi.formatResourceAmount(t0.getCount() / 1000f, true) + "L"));
            ui.row();

            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, ItemType.IntermediateOilToRefinery, GameUi.formatResourceAmount(t1.getCount() / 1000f, true) + "L"));
            ui.row();

            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, ItemType.RefinedOil, GameUi.formatResourceAmount(t2.getCount() / 1000f, true) + "L"));
            ui.row();

            container.setActor(ui);
        } else {
            ((Label) ((Table) (ui.getChildren().get(0))).getChildren().get(1)).setText(GameUi.formatResourceAmount(t0.getCount() / 1000f, true) + "L");
            ((TextTooltip) ui.getChildren().get(0).getListeners().get(0)).getActor().setText(GameUi.formatResourceAmount(t0.getCount() / 1000f, true) + "L " + ItemType.CrudeOil.title);

            ((Label) ((Table) (ui.getChildren().get(1))).getChildren().get(1)).setText(GameUi.formatResourceAmount(t1.getCount() / 1000f, true) + "L");
            ((TextTooltip) ui.getChildren().get(1).getListeners().get(0)).getActor().setText(GameUi.formatResourceAmount(t1.getCount() / 1000f, true) + "L " + ItemType.IntermediateOilToRefinery.title);

            ((Label) ((Table) (ui.getChildren().get(2))).getChildren().get(1)).setText(GameUi.formatResourceAmount(t2.getCount() / 1000f, true) + "L");
            ((TextTooltip) ui.getChildren().get(2).getListeners().get(0)).getActor().setText(GameUi.formatResourceAmount(t2.getCount() / 1000f, true) + "L " + ItemType.RefinedOil.title);
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(container).grow();
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Double("power", powerLevel)
                .Float("wt0", workingTime0)
                .Float("wt1", workingTime1);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        try {
            powerLevel = tag.Double("power");
        } catch (NBTException e) {
            powerLevel = tag.Float("power", 0);
        }
        workingTime0 = tag.Float("wt0", -1);
        workingTime1 = tag.Float("wt1", -1);
    }
}
