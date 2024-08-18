/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
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

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Array;

import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.ConstantSupplyAmount;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.GeneratorStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class GasTurbine extends GeneratorStructure {
    public static class GasTurbineRecipe extends GeneratorRecipe {
        private final float scale;

        public GasTurbineRecipe(String description, int power, float scale) {
            super(description, power);
            this.scale = scale;
        }

        public float getScale() {
            return scale;
        }
    }

    public static final GeneratorSchema classSchema = new GeneratorSchema(0, StructureType.GasTurbine, 9, 5,
            "gasturbine",
            new Items(ItemType.AdvancedMachineFrame, 10, ItemType.BronzePlate, 200, ItemType.Turbine, 10, ItemType.SteelTube, 150, ItemType.SteelCable, 180),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new GasTurbineRecipe("single", 500_000, 1.1f)
                            .input(new Amount(ItemType.NaturalGas, 1000), new ConstantSupplyAmount(ItemType.Lubricant, 1500)));
                    add(new GasTurbineRecipe("dual", 600_000, 1.75f)
                            .input(new Amount(ItemType.NaturalGas, 1000), new ConstantSupplyAmount(ItemType.Lubricant, 1200), new Amount(ItemType.Water, 5000))
                            .output(new Amount(ItemType.PressurizedSteam, 9000)));
                }

                @Override
                public void validateRecipes(de.dakror.quarry.structure.base.Schema schema) {
                    super.validateRecipes(schema);

                    // increase tank sizes
                    fluidInputs.set(0, 20_000);
                    fluidInputs.set(1, 20_000);
                    fluidInputs.set(2, 20_000);
                }
            },
            new Sfx("turbine" + Const.SFX_FORMAT, 2f),
            new Dock(0, 2, Direction.West, DockType.FluidIn, new DockFilter(ItemType.NaturalGas)),
            new Dock(1, 0, Direction.South, DockType.FluidIn, new DockFilter(ItemType.Lubricant)),
            new Dock(1, 4, Direction.North, DockType.FluidIn, new DockFilter(ItemType.Water)),
            new Dock(4, 4, Direction.North, DockType.BigPower),

            new Dock(7, 0, Direction.South, DockType.FluidOut),
            new Dock(-1, 0, Direction.West, DockType.StackIn),
            new Dock(-1, 1, Direction.West, DockType.StackIn),
            new Dock(-1, 2, Direction.West, DockType.StackIn),
            new Dock(-1, 3, Direction.West, DockType.StackIn),

            new Dock(-1, 4, Direction.West, DockType.StackIn),
            new Dock(9, 0, Direction.East, DockType.StackOut),
            new Dock(9, 1, Direction.East, DockType.StackOut),
            new Dock(9, 2, Direction.East, DockType.StackOut),
            new Dock(9, 3, Direction.East, DockType.StackOut), new Dock(9, 4, Direction.East, DockType.StackOut))
                    .sciences(ScienceType.OilProcessing, ScienceType.HighPower)
                    .flags(Flags.Stackable, Flags.MirroredTextureVertical);

    int currentScaling;

    boolean isLevel0;

    GasTurbine level0;

    Array<GasTurbine> levels;

    public class InstancedScalingGasTurbineRecipe extends GasTurbineRecipe {
        public InstancedScalingGasTurbineRecipe(GasTurbineRecipe recipe) {
            super(recipe.getDescription(), recipe.getPowerGeneration(), recipe.getScale());

            // wrap in scaling amounts
            Amount[] entries = recipe.getInput().entries;
            Amount[] newEntries = new Amount[entries.length];
            for (int i = 0; i < entries.length; i++) {
                if (entries[i] instanceof ConstantSupplyAmount) {
                    newEntries[i] = new InstancedScalingConstantSupplyAmount((ConstantSupplyAmount) entries[i]);
                } else {
                    newEntries[i] = new InstancedScalingAmount(entries[i]);
                }
            }
            input(newEntries);

            if (recipe.getOutput() != null) {
                entries = recipe.getOutput().entries;
                newEntries = new Amount[entries.length];
                for (int i = 0; i < entries.length; i++) {
                    newEntries[i] = new InstancedScalingAmount(entries[i]);
                }
                output(newEntries);
            }
        }

        // only power generation scales with recipe scale, to benefit the user
        // the other properties scale linearly with the number of machines
        @Override
        public int getPowerGeneration() {
            if (currentScaling == 1) return super.getPowerGeneration();

            return (int) (super.getPowerGeneration() * getScale() * currentScaling);
        }
    }

    public class InstancedScalingAmount extends Amount {
        public InstancedScalingAmount(Amount a) {
            super(a.getItem(), a.getAmount());
        }

        @Override
        public int getAmount() {
            return super.getAmount() * currentScaling;
        }
    }

    public class InstancedScalingConstantSupplyAmount extends ConstantSupplyAmount {
        public InstancedScalingConstantSupplyAmount(ConstantSupplyAmount a) {
            super(a.getItem(), a.getAmount());
        }

        @Override
        public int getAmount() {
            return super.getAmount() * currentScaling;
        }
    }

    InstancedScalingGasTurbineRecipe[] instancedRecipes;

    public GasTurbine(int x, int y) {
        super(x, y, classSchema);

        currentScaling = 1;
        levels = new Array<>();

        instancedRecipes = new InstancedScalingGasTurbineRecipe[schema.recipeList.recipes.length];
        for (int i = 0; i < instancedRecipes.length; i++) {
            instancedRecipes[i] = new InstancedScalingGasTurbineRecipe((GasTurbineRecipe) schema.recipeList.recipes[i]);
        }
    }

    @Override
    protected void initInventories() {
        int in = 0, fluidIn = 0, out = 0, comps = schema.getComponents().size;

        for (int i = 0; i < docks.length; i++) {
            Dock d = docks[i];
            if (d.type == DockType.FluidIn) {
                inputInventories[in] = new CTank(schema.recipeList.fluidInputs.get(fluidIn));
                in++;
                fluidIn++;
            } else if (d.type == DockType.FluidOut) {
                Component c = new CTank(schema.outputBuffer ? schema.recipeList.outputSizes.get(out) : 0, i).setMaxOutput(10_000).setPumpOutDelay(0);
                c.setStructure(this);
                c.init();
                outputInventories[out] = (IStorage) c;
                components[comps++] = c;
                out++;
            }
        }
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        int i = 0;
        for (Dock d : getDocks()) {
            if (d.type == DockType.FluidIn && d.filter != null) {
                if (inputInventories[i].hasSpace() && d.filter.accepts(item)) {
                    int rest = inputInventories[i].addWithRest(item, amount);

                    if (isClicked()) {
                        Game.G.ui.updateStructureUIInventory();
                    }

                    if (level0.activeRecipe == null) {
                        level0.pickRandomActiveRecipe();
                    }
                    return rest;
                }
                i++;
            } else if (d.type == DockType.ItemIn) {
                i++;
            }
        }
        return amount;
    }

    @Override
    protected void offerPower(double deltaTime, int gameSpeed) {
        if (!isLevel0 || gameSpeed == 0 || sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction()) || noPower || workDelay == -1 || additionalWorkBlockingCondition()) {
            return;
        }

        // distribute power output across all levels

        double power = ((GeneratorRecipe) activeRecipe).getPowerGeneration() * deltaTime * gameSpeed;

        double sliceSize = (float) Math.ceil(power / currentScaling);

        for (GasTurbine t : levels) {
            double slice = Math.min(sliceSize, power);

            if (t.powerNetwork != null) {
                t.powerNetwork.offerPower(deltaTime, gameSpeed, slice, t);
            }

            power -= slice;
        }
    }

    @Override
    protected void drawRecipeProgress(ShapeRenderer shaper) {
        if (activeRecipe != null && hasCapacity) {
            shaper.setColor(0, 0.5f, 0, 1);
            shaper.set(ShapeType.Filled);
            float progress = 1 - workDelay / activeRecipe.workingTime;
            Direction fwd = getForwardDirection();
            if (fwd.dy == 0) {
                shaper.rect((fwd.dx > 0 ? x : x - (levels.size - 1) * getWidth()) * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * currentScaling * Const.TILE_SIZE, 8);
            } else {
                shaper.rect(x * Const.TILE_SIZE, (fwd.dy > 0 ? y : y - (levels.size - 1) * getHeight()) * Const.TILE_SIZE, 8, progress * getHeight() * currentScaling * Const.TILE_SIZE);
            }
        }
    }

    @Override
    protected void doProductionStep() {
        // evenly distribute output steam across levels
        if (activeRecipe.getOutput() != null) {
            Amount steam = activeRecipe.getOutput().entries[0];
            int amount = steam.getAmount();
            int slice = amount / levels.size;
            for (GasTurbine t : levels) {
                int min = Math.min(slice, amount);
                amount -= min;
                t.outputInventories[0].addUnsafe(steam.getItem(), min);
            }

            // leftover, add to level 0
            if (amount > 0) {
                outputInventories[0].addUnsafe(steam.getItem(), amount);
            }
        }
    }

    @Override
    protected boolean hasCapacityForProduction() {
        for (GasTurbine t : levels) {
            if (!t.outputInventories[0].hasSpace()) return false;
        }

        return true;
    }

    @Override
    protected void pickRandomActiveRecipe() {
        if (!isLevel0) {
            setRecipe(-1);
            return;
        }
        int water = 0;
        int lubricant = 0;

        for (GasTurbine t : levels) {
            lubricant += t.inputInventories[1].getCount();
            water += t.inputInventories[2].getCount();
        }

        int waterNeeded = instancedRecipes[1].getInput().getAmount(ItemType.Water);
        int lubricantNeeded = 0;
        int gasNeeded = 0;
        int recipe = 0;

        if (water >= waterNeeded) {
            lubricantNeeded = instancedRecipes[1].getInput().getAmount(ItemType.Lubricant);
            gasNeeded = instancedRecipes[1].getInput().getAmount(ItemType.NaturalGas);
            recipe = 1;
        } else {
            lubricantNeeded = instancedRecipes[0].getInput().getAmount(ItemType.Lubricant);
            gasNeeded = instancedRecipes[0].getInput().getAmount(ItemType.NaturalGas);
        }

        if (lubricant >= lubricantNeeded && inputInventories[0].getCount() >= gasNeeded) {
            inputInventories[0].remove(ItemType.NaturalGas, gasNeeded);

            int lubricantSlice = (int) Math.ceil(lubricantNeeded / (float) levels.size);
            int waterSlice = (int) Math.ceil(waterNeeded / (float) levels.size);

            for (GasTurbine t : levels) {
                int lubeIn = Math.min(lubricantNeeded, lubricantSlice);
                lubricantNeeded -= lubeIn - t.inputInventories[1].removeWithRest(ItemType.Lubricant, lubeIn);
                int waterIn = Math.min(waterNeeded, waterSlice);
                waterNeeded -= waterIn + t.inputInventories[2].removeWithRest(ItemType.Water, waterIn);
            }

            // if not all depleted, get remaining from first that has it

            for (GasTurbine t : levels) {
                if (lubricantNeeded > 0)
                    lubricantNeeded = t.inputInventories[1].removeWithRest(ItemType.Lubricant, lubricantNeeded);
                if (waterNeeded > 0)
                    waterNeeded = t.inputInventories[2].removeWithRest(ItemType.Water, waterNeeded);
            }

            setRecipe(recipe);

            for (GasTurbine t : levels) {
                if (t.isClicked()) {
                    Game.G.ui.updateStructureUIInventory();
                }
            }
        } else {
            setRecipe(-1);
        }
    }

    @Override
    protected void setRecipe(int index) {
        setItemNotifications();
        activeRecipeIndex = index;
        if (index > -1) {
            // use instanced recipes
            activeRecipe = instancedRecipes[activeRecipeIndex];
            workDelay = activeRecipe.workingTime;
        } else {
            activeRecipe = null;
            activeItems = null;
        }
        updateUI();
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        if (fromLoading) return;

        if (layer != null) updateLevel();
        super.onPlacement(fromLoading);
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateLevel();
        if (layer != null && activeRecipe == null)
            pickRandomActiveRecipe();
    }

    @Override
    protected void playSfx() {
        super.playSfx();

        if (isLevel0) {
            for (GasTurbine t : levels) {
                if (t != this) t.playSfx();
            }
        }
    }

    @Override
    protected void pauseSfx() {
        super.pauseSfx();

        if (isLevel0) {
            for (GasTurbine t : levels) {
                if (t != this) t.pauseSfx();
            }
        }
    }

    @Override
    public void stopSfx() {
        super.stopSfx();

        if (isLevel0) {
            for (GasTurbine t : levels) {
                if (t != this) t.stopSfx();
            }
        }
    }

    public InstancedScalingGasTurbineRecipe[] getInstancedRecipes() {
        return instancedRecipes;
    }

    @Override
    public void setSleeping(boolean sleeping) {
        super.setSleeping(sleeping);
        layer.dirtyBounds.add(this, 0);
    }

    private Structure<?> getStructureAt(int x, int y) {
        Structure<?> s = layer.getStructure(x, y);
        if (s != null && s.x == x && s.y == y && s.getUpDirection() == upDirection) return s;
        return null;
    }

    public Direction getForwardDirection() {
        return Direction.values[(upDirection.ordinal() - Direction.North.ordinal() + Direction.East.ordinal()) % 4];
    }

    private void updateLevel() {
        Direction dir = getForwardDirection();

        isLevel0 = !(getStructureAt(x - getWidth() * dir.dx, y - getHeight() * dir.dy) instanceof GasTurbine);
        currentScaling = 1;

        if (isLevel0) {
            level0 = this;
            levels.clear();
            levels.add(this);
            for (int i = schema.width; i < Const.DEFAULT_LAYER_SIZE; i += schema.width) {
                Structure<?> s = getStructureAt(x + i * dir.dx, y + i * dir.dy);
                if (s instanceof GasTurbine) {
                    levels.add((GasTurbine) s);
                    ((GasTurbine) s).setLevel0(this);
                } else {
                    break;
                }
            }
        }
    }

    public int getScaling() {
        return currentScaling;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (isLevel0) {
            if (gameSpeed > 0) {
                currentScaling = 0;
                for (GasTurbine t : levels)
                    if (!t.isSleeping()) currentScaling++;
            }
            super.update(deltaTime, gameSpeed, dirtyBounds);
        } else {
            // sub-levels only are ghosts
            this.gameSpeed = gameSpeed;

            if (gameSpeed > 0) {
                this.time += deltaTime;
                if (this.time > 1) this.time--;
            }

            for (Component c : components)
                c.update(deltaTime, gameSpeed, dirtyBounds);
        }
        if (dirtyBounds.hasFlag(de.dakror.quarry.util.Bounds.Flags.GASTURBINE)) {
            updateLevel();
        }
    }

    @Override
    protected void updateUI() {
        if (ui == null)
            return;
        if (level0.activeRecipe == null)
            ui.setActor(waitingLabel);
        else {
            ui.setActor(GameUi.renderRecipe(Quarry.Q.skin, level0.activeRecipe, /* activeItems, null, */true));
            timeLabel = ui.findActor("time");
        }
        ui.invalidateHierarchy();
    }

    public boolean isLevel0() {
        return isLevel0;
    }

    public void setLevel0(GasTurbine level0) {
        this.level0 = level0;
    }

    public int getLevel() {
        return currentScaling;
    }

    public GasTurbine getLevel0() {
        return level0;
    }
}
