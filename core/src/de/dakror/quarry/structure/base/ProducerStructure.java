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

package de.dakror.quarry.structure.base;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.WindowedMean;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.ListTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure.ProducerSchema;
import de.dakror.quarry.structure.base.RecipeList.ParameterizedAmount;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.RecipeList.SameAmount;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.base.component.CRecipeSlotStorage;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class ProducerStructure extends PausableStructure<ProducerSchema> {
    public static class ProducerSchema extends PausableSchema {
        public final RecipeList recipeList;
        public final boolean outputBuffer;

        public ProducerSchema(int version, StructureType type, int width, int height, String tex, Items buildCosts, RecipeList recipes, Sfx sfx, boolean outputBuffer, Dock... docks) {
            super(version, type, true, width, height, tex, buildCosts, sfx, docks);

            this.outputBuffer = outputBuffer;
            recipes.validateRecipes(this);
            this.recipeList = recipes;
        }

        @Override
        public Component[] copyComponents(Structure<?> structure) {
            Component[] c = new Component[components.size + recipeList.outputSizes.size];
            int i = 0;
            for (Component comp : this.components) {
                c[i] = comp.clone();
                c[i].setStructure(structure);
                c[i].init();

                i++;
            }
            return c;
        }
    }

    protected float workDelay;
    protected int activeRecipeIndex;
    protected Recipe activeRecipe;

    protected final IStorage[] inputInventories;
    protected final IStorage[] outputInventories;
    protected Items activeItems;

    protected boolean hasCapacity;

    protected double powerLevel;
    protected double powerCapacity;

    protected final WindowedMean powerLevelMean = new WindowedMean(60);

    protected boolean noPower;
    protected int framesPassedWithPower;

    protected Container<Actor> ui;
    protected Label timeLabel;

    protected Label waitingLabel;

    public static final TextureRegion nopowerTex = Quarry.Q.atlas.findRegion("state_nopower");

    protected ProducerStructure(int x, int y, ProducerSchema schema) {
        super(x, y, schema);

        inputInventories = new IStorage[schema.inputDocks];
        outputInventories = new IStorage[schema.outputDocks];

        initInventories();
    }

    protected void initInventories() {
        int in = 0, fluidIn = 0, out = 0, comps = schema.components.size;

        for (int i = 0; i < docks.length; i++) {
            Dock d = docks[i];
            if (d.type == DockType.ItemIn) {
                inputInventories[in] = new CRecipeSlotStorage(schema.recipeList, in);
                in++;
            } else if (d.type == DockType.FluidIn) {
                inputInventories[in] = new CTank(schema.recipeList.fluidInputs.get(fluidIn));
                in++;
                fluidIn++;
            } else if (d.type == DockType.ItemOut) {
                Component c = new CInventory(schema.outputBuffer ? schema.recipeList.outputSizes.get(out) : 0, i);
                c.setStructure(this);
                c.init();
                outputInventories[out] = (IStorage) c;
                components[comps++] = c;
                out++;
            } else if (d.type == DockType.FluidOut) {
                Component c = new CTank(schema.outputBuffer ? schema.recipeList.outputSizes.get(out) : 0, i);
                c.setStructure(this);
                c.init();
                outputInventories[out] = (IStorage) c;
                components[comps++] = c;
                out++;
            }
        }
    }

    @Override
    public float getLoudness() {
        return (sleeping || activeRecipe == null || !hasCapacity || noPower ? 0.1f : 1) * getSchema().loudness * speedScale;
    }

    @Override
    public double acceptPower(double amount, double networkStrength) {
        if (sleeping) return amount;

        // limit to inflow of network
        double realAmount = Math.min(amount, networkStrength - powerReceivedThisTick);

        if (realAmount <= 0) return amount;

        double old = powerLevel;
        double add = Math.min(powerCapacity, powerLevel + realAmount);

        powerReceivedThisTick += add - old;

        powerLevel = add;

        return amount - (add - old);
    }

    @Override
    public double getPowerCapacity() {
        return powerCapacity;//(sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction()) || additionalWorkBlockingCondition()) ? 0 : powerCapacity;
    }

    @Override
    public double getPowerLevel() {
        return powerLevel;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        int i = 0;
        boolean isFluid = item.categories.contains(ItemCategory.Fluid);
        for (Dock d : getDocks()) {
            if (d.type == DockType.ItemIn) {
                if (!isFluid && isNextToDock(x, y, dir, d))
                    return inputInventories[i].canAccept(item) && (d.filter == null || d.filter.accepts(item));
                i++;
            } else if (d.type == DockType.FluidIn && d.filter != null) {
                if (isFluid && isNextToDock(x, y, dir, d)) {
                    CTank tank = (CTank) inputInventories[i];
                    return (tank.getFluid() == null || tank.getFluid() == item) && inputInventories[i].hasSpace() && d.filter.accepts(item);
                }
                i++;
            }
        }
        return false;
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        int i = 0;
        for (Dock d : getDocks()) {
            if (d.type == DockType.ItemIn) {
                if ((d.filter == null || d.filter.accepts(item)) && inputInventories[i].canAccept(item)) {
                    inputInventories[i].add(item, 1);

                    if (isClicked())
                        Game.G.ui.updateStructureUIInventory();

                    if (activeRecipe == null) {
                        pickRandomActiveRecipe();
                    }
                    return true;
                }
                i++;
            } else if (d.type == DockType.FluidIn) {
                i++;
            }
        }
        return false;
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        int i = 0;
        for (Dock d : getDocks()) {
            if (d.type == DockType.FluidIn && d.filter != null) {
                if (inputInventories[i].hasSpace() && d.filter.accepts(item)) {
                    int rest = inputInventories[i].addWithRest(item, amount);

                    if (isClicked())
                        Game.G.ui.updateStructureUIInventory();

                    if (activeRecipe == null) {
                        pickRandomActiveRecipe();
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

    public IStorage[] getInputInventories() {
        return inputInventories;
    }

    public boolean isNoPower() {
        return noPower;
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);
        if (ui == null) {
            ui = new Container<>();
            ui.fill();
            if (waitingLabel == null) {
                waitingLabel = new Label(Quarry.Q.i18n.get("structure.UI.waiting_for_inputs"), Quarry.Q.skin, "small-font", Color.WHITE);
                waitingLabel.setAlignment(Align.center);
            }
        }
        content.add(ui).grow();

        updateUI();
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (layer != null && activeRecipe == null)
            pickRandomActiveRecipe();
    }

    public Recipe getActiveRecipe() {
        return activeRecipe;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSfx();
    }

    protected void pickRandomActiveRecipe() {
        RecipeList s = getSchema().recipeList;
        if (s.hasInputs) {
            // pick first suiting recipe according to filter inventory
            for (int index = 0; index < s.recipes.length; index++) {

                Recipe r = s.recipes[index];
                boolean ready = true;
                Amount[] inputs = new Amount[r.input.entries.length];
                for (int i = 0; i < r.input.entries.length; i++) {
                    IStorage ci = inputInventories[i];
                    Amount en = r.input.entries[i];
                    if (en != null) {
                        if (en.getCat() != null) {
                            Amount[] es = ci.get(en.getCat());
                            if (es == null || es.length == 0) {
                                ready = false;
                                break;
                            }

                            int sum = 0;
                            for (Amount e : es) {
                                if (e != null) {
                                    sum += e.getAmount();
                                }
                            }
                            if (sum < en.getAmount()) {
                                ready = false;
                                break;
                            } else {
                                if (es.length == 1 || es[0].getAmount() == en.getAmount()) {
                                    inputs[i] = new Amount(es[0].getItem(), en.getAmount());
                                } else {
                                    inputs[i] = en;
                                }
                            }
                        } else if (en.getItem() == Item.base(en.getItem())) {
                            // else if the item given is a parent item like "Ore"
                            Amount[] e = ci.getSimilar(en.getItem());
                            if (e == null) {
                                ready = false;
                                break;
                            }

                            Amount correctAmount = null;

                            for (Amount amount : e) {
                                if (amount.getAmount() >= en.getAmount()) {
                                    correctAmount = amount;
                                    break;
                                }
                            }

                            if (correctAmount == null) {
                                ready = false;
                                break;
                            }

                            inputs[i] = new Amount(correctAmount.getItem(), en.getAmount());
                        } else {
                            // given item is a specific item like "CoalOre"
                            int e = ci.get(en.getItem());
                            if (e < en.getAmount()) {
                                ready = false;
                                break;
                            }
                            inputs[i] = new Amount(en.getItem(), en.getAmount());
                        }
                    }
                }

                if (ready) {
                    for (int i = 0; i < r.input.entries.length; i++) {
                        Amount e = inputs[i];
                        if (e != null) {
                            if (e.getCat() != null) {
                                inputInventories[i].remove(e.getCat(), e.getAmount());
                            } else {
                                inputInventories[i].remove(e.getItem(), e.getAmount());
                            }
                        }
                    }

                    if (isClicked())
                        Game.G.ui.updateStructureUIInventory();

                    activeItems = new Items(inputs);
                    setRecipe(index);
                    return;
                }
            }

            setRecipe(-1);
        } else {
            setRecipe(MathUtils.random(s.recipes.length - 1));
        }
    }

    protected void setRecipe(int index) {
        setItemNotifications();
        activeRecipeIndex = index;
        if (index > -1) {
            activeRecipe = getSchema().recipeList.recipes[activeRecipeIndex];
            workDelay = activeRecipe.workingTime;
            powerCapacity = activeRecipe.getPower();
        } else {
            activeRecipe = null;
            activeItems = null;
        }
        updateUI();
    }

    protected void updateUI() {
        if (ui == null)
            return;
        if (activeRecipe == null)
            ui.setActor(waitingLabel);
        else {
            ui.setActor(GameUi.renderRecipe(Quarry.Q.skin, activeRecipe, /* activeItems, null, */true));
            timeLabel = ui.findActor("time");
        }
        ui.invalidateHierarchy();
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (gameSpeed == 0) {
            powerLevelMean.addValue(powerLevelMean.getLatest());
        } else {
            powerLevelMean.addValue((float) powerLevel);
        }

        if (activeRecipe != null && activeRecipe.getPower() > 0 && gameSpeed > 0) {
            noPower = powerLevel < activeRecipe.getPower() / 2;
            if (!noPower) {
                powerLevel = Math.max(0, powerLevel - activeRecipe.getPower() * 60 * deltaTime * gameSpeed);
                framesPassedWithPower++;
                if (framesPassedWithPower > 10) framesPassedWithPower = 10;
            } else {
                framesPassedWithPower = 0;
            }
        } else noPower = false;

        if (sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction()) || noPower || additionalWorkBlockingCondition()) {
            pauseSfx();
            return;
        }

        if (gameSpeed == 0) {
            pauseSfx();
        } else {
            // working starts at >50% of power but at 0% of speed
            workDelay -= deltaTime * gameSpeed * Math.max(0, (activeRecipe.getPower() == 0 ? 1 : (powerLevelMean.getMean() / activeRecipe.getPower()) * 2 - 1));
            playSfx();
        }

        if (ui != null)
            timeLabel.setText(GameUi.formatResourceAmount(Math.min((int) workDelay + 1, activeRecipe.workingTime)) + "s");

        if (workDelay <= 0) {
            doProductionStep();
            setRecipe(-1);
            // this is only relevant for structures without inputs, since they work all the time
            pickRandomActiveRecipe();
        }
    }

    protected boolean additionalWorkBlockingCondition() {
        return false;
    }

    protected void drawRecipeProgress(ShapeRenderer shaper) {
        if (activeRecipe != null && hasCapacity) {
            shaper.setColor(0, 0.5f, 0, 1);
            shaper.set(ShapeType.Filled);
            float progress = 1 - workDelay / activeRecipe.workingTime;
            shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * Const.TILE_SIZE, 8);
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        drawRecipeProgress(shaper);

        if (activeRecipe != null && activeRecipe.getPower() > 0 && framesPassedWithPower < 10) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(nopowerTex, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 2, Const.Z_STATES, size, size);
        }

        if (powerCapacity > 0) {
            float scale = (float) Math.min(1, powerLevelMean.getMean() / powerCapacity);
            if (scale > 0)
                spriter.add(x * Const.TILE_SIZE + 1, y * Const.TILE_SIZE + 1, Const.Z_STATES,
                        4, (getHeight() * Const.TILE_SIZE - 2) * scale,
                        Substation.powermeterTex.getU(), Substation.powermeterTex.getV2(), Substation.powermeterTex.getU2(),
                        Substation.powermeterTex.getV2() + (Substation.powermeterTex.getV() - Substation.powermeterTex.getV2()) * scale);
        }
        drawBoostState(spriter);

        if (activeRecipe != null && !hasCapacity) {
            drawFullState(spriter);
        }
    }

    protected boolean hasCapacityForProduction() {
        if (activeRecipe.output != null) {
            for (int i = 0; i < activeRecipe.output.entries.length; i++) {
                Amount a = activeRecipe.output.entries[i];
                if (a != null && ((getSchema().outputBuffer && (outputInventories[i].getSize() - outputInventories[i].getCount()) < a.getAmount()) || (!getSchema().outputBuffer && !outputInventories[i].isEmpty()))) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void doProductionStep() {
        if (activeRecipe.output != null) {
            for (int i = 0; i < activeRecipe.output.entries.length; i++) {
                Amount a = activeRecipe.output.entries[i];
                if (a != null) {
                    if (a instanceof ParameterizedAmount) {
                        if (activeItems == null) {
                            setRecipe(-1);
                            return;
                        }

                        outputInventories[i].addUnsafe(Item.get(a.getItem(), activeItems.entries[((ParameterizedAmount) a).inputParameter].getItem()), a.getAmount());
                    } else if (a instanceof SameAmount) {
                        if (activeItems == null) {
                            setRecipe(-1);
                            return;
                        }

                        outputInventories[i].addUnsafe(activeItems.entries[((SameAmount) a).inputParameter].getItem(), a.getAmount());
                    } else {
                        outputInventories[i].addUnsafe(a.getItem(), a.getAmount());
                    }
                }
            }
        }
    }

    @Override
    public boolean putBack(ItemType item, int amount) {
        if (outputInventories.length > 1) {
            if (getSchema().recipeList.containsOutput(item)) {
                // find out correct inventory to put item back in
                for (Recipe r : getSchema().recipeList.recipes) {
                    for (int i = 0; i < r.getOutput().entries.length; i++) {
                        Amount a = r.getOutput().entries[i];
                        if (a != null && (a.getItem() == item || Item.base(item) == a.getItem())) {
                            outputInventories[i].addUnsafe(item, amount);
                            return true;
                        }
                    }
                }
            }
        } else {
            outputInventories[0].addUnsafe(item, amount);
        }
        return false;
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b
                .Float("workDelay", workDelay)
                .Int("recipe", activeRecipeIndex)
                .Double("power", powerLevel);

        b.List("filterInventories", TagType.Compound);
        for (IStorage in : inputInventories) {
            if (in != null)
                ((Component) in).saveData(b);
        }
        b.End();

        if (activeItems != null) {
            short[] types = new short[activeItems.entries.length];
            byte[] categories = new byte[activeItems.entries.length];
            int[] amounts = new int[types.length];
            for (int i = 0; i < types.length; i++) {
                Amount e = activeItems.entries[i];
                if (e != null) {
                    if (e.getItem() != null) {
                        types[i] = e.getItem().value;
                    } else {
                        categories[i] = e.getCat().id;
                    }
                    amounts[i] = e.getAmount();
                }
            }

            b
                    .ByteArray("activeCats", categories)
                    .ShortArray("activeTypes", types)
                    .IntArray("activeAmounts", amounts);
        }
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        workDelay = tag.Float("workDelay", 0);

        try {
            powerLevel = tag.Double("power");
        } catch (NBTException e) {
            powerLevel = tag.Float("power", 0);
        }

        activeRecipeIndex = tag.Int("recipe", -1);
        if (activeRecipeIndex > -1) {
            try {
                activeRecipe = getSchema().recipeList.recipes[activeRecipeIndex];
                powerCapacity = activeRecipe.getPower();
                short[] types = tag.ShortArray("activeTypes", null);
                byte[] categories = tag.ByteArray("activeCats", null);
                if (types != null && categories != null) {
                    int[] amounts = tag.IntArray("activeAmounts");
                    Amount[] entries = new Amount[types.length];
                    for (int i = 0; i < types.length; i++) {
                        if (types[i] != 0) {
                            entries[i] = new Amount(Item.get(types[i]), amounts[i]);
                        } else if (categories[i] != 0) {
                            entries[i] = new Amount(Item.category(categories[i]), amounts[i]);
                        }
                    }
                    activeItems = new Items(entries);
                }
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                setRecipe(-1);
            }
        }

        try {
            ListTag filterInvs = tag.List("filterInventories");
            for (int i = 0; i < inputInventories.length; i++) {
                if (inputInventories[i] != null)
                    ((Component) inputInventories[i]).loadData((CompoundTag) filterInvs.data.get(i));
            }
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    @Override
    public int getReceiverPriority() {
        return 100;
    }
}
