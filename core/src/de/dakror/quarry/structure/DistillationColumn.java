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
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

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
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.PausableSchema;
import de.dakror.quarry.structure.base.PausableStructure;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class DistillationColumn extends PausableStructure<PausableSchema> {
    public static final PausableSchema classSchema = new PausableSchema(0, StructureType.DistillationColumn, true, 3, 4,
            "distillationcolumn",
            new Items(ItemType.MachineFrame, 8, ItemType.Brick, 80, ItemType.SteelTube, 15, ItemType.CarbonBlock, 50, ItemType.BronzePlate, 35),
            new Sfx("column" + Const.SFX_FORMAT),
            new Dock(2, 0, Direction.East, DockType.FluidOut),
            new Dock(0, 1, Direction.West, DockType.Power),
            new Dock(1, 0, Direction.South, DockType.FluidIn, new DockFilter(ItemType.IntermediateOilToColumn)),
            new Dock(0, -1, Direction.South, DockType.StackIn),
            new Dock(1, -1, Direction.South, DockType.StackIn),
            new Dock(2, -1, Direction.South, DockType.StackIn),
            new Dock(0, 4, Direction.North, DockType.StackOut),
            new Dock(1, 4, Direction.North, DockType.StackOut), new Dock(2, 4, Direction.North, DockType.StackOut))
                    .sciences(ScienceType.OilProcessing)
                    .components(new CTank(2000), new CTank(2000, 0))
                    .flags(Flags.Stackable, Flags.MirroredTextureHorizontal);

    public static class DistillationRecipe {
        public final float workingTime = 2f;
        public final int inputOil = 1000;
        public final float power = 84f;
        public final int outputOil = 600;
        public final int outputLubricant = 200;
        public final int outputGas = 50;
    }

    public static final DistillationRecipe recipe = new DistillationRecipe();

    static final TextureRegion top = Quarry.Q.atlas.findRegion("structure_distillationcolumn_top");

    double powerLevel;
    boolean noPower;
    final WindowedMean powerLevelMean = new WindowedMean(60);
    protected int framesPassedWithPower;

    int level;

    float workingTime;

    CTank inputTank;
    CTank outputTank0, outputTank1, outputTank2;

    DistillationColumn level1, level2;

    protected Container<Actor> ui;

    public DistillationColumn(int x, int y) {
        super(x, y, classSchema);
        level = 0;
        inputTank = (CTank) components[0];
        outputTank0 = (CTank) components[1];
        workingTime = -1;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        if (!item.categories.contains(ItemCategory.Fluid)) return false;
        if (item == ItemType.IntermediateOilToColumn) {
            return isNextToDock(x, y, dir, getDocks()[2]) && inputTank.hasSpace();
        }

        return false;
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (item == ItemType.IntermediateOilToColumn) {
            return inputTank.addWithRest(item, amount);
        }

        return amount;
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

    public int getLevels() {
        if (level == 0) {
            if (level2 != null && level1 != null) return 3;
            if (level1 != null) return 2;
            return 1;
        } else {
            Structure<?> level0 = layer.getStructure(x - getWidth() * level * upDirection.dx,
                    y - getHeight() * level * upDirection.dy);

            if (level0 == null || !(level0 instanceof DistillationColumn)) return 1;
            else return ((DistillationColumn) level0).getLevels();
        }
    }

    @Override
    public float getLoudness() {
        return (sleeping || workingTime == -1 || noPower ? 0.1f : 1f) * speedScale;
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

    private void updateLevels() {
        Structure<?> s = layer.getStructure(x + getWidth() * upDirection.dx, y + getHeight() * upDirection.dy);
        if (s instanceof DistillationColumn
                && s.x == x + getWidth() * upDirection.dx && s.y == y + getHeight() * upDirection.dy) {
            level1 = (DistillationColumn) s;
            outputTank1 = (CTank) s.getComponents()[1];
            level1.setLevel(1);
            level1.setDirty();

            s = layer.getStructure(x + getWidth() * 2 * upDirection.dx, y + getHeight() * 2 * upDirection.dy);
            if (s instanceof DistillationColumn
                    && s.x == x + getWidth() * 2 * upDirection.dx && s.y == y + getHeight() * 2 * upDirection.dy) {
                level2 = (DistillationColumn) s;
                outputTank2 = (CTank) s.getComponents()[1];
                level2.setLevel(2);
                level2.setDirty();
            } else {
                level2 = null;
                outputTank2 = null;
            }
        } else {
            level1 = null;
            outputTank1 = null;
            level2 = null;
            outputTank2 = null;
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();
        if (level == 0)
            updateLevels();
    }

    private void updateLevel(DistillationColumn level, double deltaTime, int gameSpeed) {
        if (level == null) return;

        if (gameSpeed == 0) {
            level.powerLevelMean.addValue(level.powerLevelMean.getLatest());
        } else {
            level.powerLevelMean.addValue((float) level.powerLevel);
        }

        if (sleeping) {
            level.pauseSfx();
            return;
        }

        if (gameSpeed == 0 || (workingTime == -1) || !canWork()) {
            level.pauseSfx();
        } else {
            level.playSfx();
        }

        if (workingTime > -1 && gameSpeed > 0) {
            level.noPower = level.powerLevel < recipe.power;
            if (!level.noPower) {
                level.powerLevel = Math.max(0, level.powerLevel - recipe.power * 60 * deltaTime * gameSpeed);
                level.framesPassedWithPower++;
                if (level.framesPassedWithPower > 10) level.framesPassedWithPower = 10;
            } else {
                level.framesPassedWithPower = 0;
            }
        } else {
            level.noPower = false;
        }
    }

    private boolean canWork() {
        return inputTank.getCount() >= recipe.inputOil && !noPower && outputTank0.hasSpace()
                && (level1 == null || level1.isSleeping() || (!level1.noPower && outputTank1.hasSpace()))
                && (level2 == null || level2.isSleeping() || (!level2.noPower && outputTank2.hasSpace()));
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (dirtyBounds.hasAnyFlag(Bounds.Flags.DESTRUCTION | Bounds.Flags.DISTILLATIONCOLUMN)) {
            setLevel(0);
            Structure<?> s = layer.getStructure(x - getWidth() * upDirection.dx, y - getHeight() * upDirection.dy);
            if (s instanceof DistillationColumn && s.x == x - s.getWidth() * upDirection.dx && s.y == y - s.getHeight() * upDirection.dy) {
                setLevel(((DistillationColumn) s).getLevel() + 1);
                setDirty();
            }
        }

        if (level == 0) {
            updateLevel(this, deltaTime, gameSpeed);
            updateLevel(level1, deltaTime, gameSpeed);
            updateLevel(level2, deltaTime, gameSpeed);

            // working starts at >50% of power but at 0% of speed
            // for upper levels, power determines yield

            float powerSpeedScale = Math.max(0, (powerLevelMean.getMean() / recipe.power) * 2 - 1);

            if (canWork() && workingTime >= 0) {
                workingTime -= deltaTime * gameSpeed * powerSpeedScale;
                if (workingTime <= 0) {
                    inputTank.removeWithRest(ItemType.IntermediateOilToColumn, recipe.inputOil);
                    outputTank0.addUnsafe(ItemType.IntermediateOilToRefinery, recipe.outputOil);
                    if (level1 != null && !level1.isSleeping()) {
                        float level1PowerSpeedScale = Math.max(0, (level1.powerLevelMean.getMean() / recipe.power) * 2 - 1);
                        outputTank1.addUnsafe(ItemType.Lubricant, (int) (recipe.outputLubricant * level1PowerSpeedScale));

                        // level 2 scaling off of level1 & 2 power
                        if (level2 != null && !level2.isSleeping()) {
                            float level2PowerSpeedScale = Math.max(0, (level2.powerLevelMean.getMean() / recipe.power) * 2 - 1) * level1PowerSpeedScale;
                            outputTank2.addUnsafe(ItemType.NaturalGas, (int) (recipe.outputGas * level2PowerSpeedScale));
                        }
                    }

                    workingTime = -1;
                }
            }

            // update recipe working
            if (workingTime == -1 && inputTank.getCount() >= recipe.inputOil) {
                workingTime = recipe.workingTime;
            }

        }
    }

    @Override
    public void postUpdate(Bounds dirtyBounds) {
        super.postUpdate(dirtyBounds);

        if (level == 0 && dirtyBounds.hasFlag(Bounds.Flags.DISTILLATIONCOLUMN)) {
            updateLevels();
        }
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        if (getDocks().length > 0)
            drawDocks(spriter);

        float x = this.x * Const.TILE_SIZE,
                y = this.y * Const.TILE_SIZE,
                z = Const.Z_STRUCTURES,
                width = schema.width * Const.TILE_SIZE,
                height = schema.height * Const.TILE_SIZE,
                originX, originY;

        if (schema.width % 2 == 0 && schema.height % 2 == 0) {
            originX = (schema.width / 2f) * Const.TILE_SIZE;
            originY = (schema.height / 2f) * Const.TILE_SIZE;
        } else {
            if (upDirection.dx != 0) {
                originX = getHeight() / 2f * Const.TILE_SIZE;
                originY = getWidth() / 2f * Const.TILE_SIZE;
                x += (getWidth() - getHeight()) / 2f * Const.TILE_SIZE;
                y += (getHeight() - getWidth()) / 2f * Const.TILE_SIZE;
            } else {
                originX = (schema.width / 2f) * Const.TILE_SIZE;
                originY = (schema.height / 2f) * Const.TILE_SIZE;
            }
        }

        spriter.add(level == 2 ? top : getSchema().tex, x, y, z, originX, originY,
                width * (getSchema().has(Flags.MirroredTextureHorizontal) ? 0.5f : 1),
                height * (getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : 0.5f),
                1, 1, upDirection.rot - Direction.North.rot);

        spriter.add(level == 2 ? top : getSchema().tex, x, y, z, originX, originY,
                width * (getSchema().has(Flags.MirroredTextureHorizontal) ? 0.5f : 1),
                height * (getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : 0.5f),
                getSchema().has(Flags.MirroredTextureHorizontal) ? -1 : 1,
                getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : -1,
                upDirection.rot - Direction.North.rot);
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        float scale = powerLevelMean.getMean() / recipe.power;
        if (scale > 0)
            spriter.add(x * Const.TILE_SIZE + 1, y * Const.TILE_SIZE + 1, Const.Z_STATES,
                    4, getHeight() * Const.TILE_SIZE * scale - 2,
                    Substation.powermeterTex.getU(), Substation.powermeterTex.getV2(), Substation.powermeterTex.getU2(),
                    Substation.powermeterTex.getV2() + (Substation.powermeterTex.getV() - Substation.powermeterTex.getV2()) * scale);

        drawBoostState(spriter);

        if (!outputTank0.hasSpace()) {
            drawFullState(spriter);
        }

        if (level == 0) {
            if (workingTime > 0) {
                if (framesPassedWithPower < 10) {
                    float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
                    spriter.add(ProducerStructure.nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2,
                            (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
                }
                if (level1 != null && level1.framesPassedWithPower < 10) {
                    float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
                    spriter.add(ProducerStructure.nopowerTex, (level1.x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 3,
                            (level1.y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
                }
                if (level2 != null && level2.framesPassedWithPower < 10) {
                    float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
                    spriter.add(ProducerStructure.nopowerTex, (level2.x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 3,
                            (level2.y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
                }

                shaper.setColor(0, 0.5f, 0, 1);
                float progress = 1 - workingTime / recipe.workingTime;
                shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * Const.TILE_SIZE, 8);
            }
        }
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);
        if (ui == null) {
            ui = new Container<>();
            ui.fill();
        }
        content.add(ui).width(360);
        ui.setActor(Game.G.ui.distRecipe);
    }

    @Override
    public Object clone() {
        DistillationColumn d = (DistillationColumn) super.clone();
        d.setLevel(level);
        return d;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        if (!Game.G.pasteMode) {
            this.level = level;
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Byte("level", (byte) level)
                .Double("power", powerLevel);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        level = tag.Byte("level", (byte) 0);

        try {
            powerLevel = tag.Double("power");
        } catch (NBTException e) {
            powerLevel = tag.Float("power", 0);
        }
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        b.Byte("level", (byte) level);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);
        level = tag.Byte("level", (byte) 0);
    }
}
