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

import java.util.EnumSet;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;

import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class RecipeList {
    public static class ParameterizedAmount extends Amount {
        public final int inputParameter;

        public ParameterizedAmount(ItemType item, int amount, int inputParameter) {
            super(item, amount);
            this.inputParameter = inputParameter;
        }

        public ParameterizedAmount(ItemCategory cat, int amount, int inputParameter) {
            super(cat, amount);
            this.inputParameter = inputParameter;
        }

        @Override
        public String toString() {
            return super.toString() + " (p" + inputParameter + ")";
        }
    }

    public static class SameAmount extends Amount {
        public final int inputParameter;

        public SameAmount(int inputParameter, int amount) {
            super((ItemCategory) null, amount);
            this.inputParameter = inputParameter;
        }

        @Override
        public String toString() {
            return super.toString() + " (s" + inputParameter + ")";
        }
    }

    public static class Recipe {
        public final float workingTime;
        private String description;
        public final String name;
        private float power;
        protected Items input, output;

        protected EnumSet<ScienceType> requiredSciences;

        public Recipe(float workingTime, String description) {
            this(workingTime, description, 0);
        }

        public Recipe(float workingTime, String description, int power) {
            this.workingTime = workingTime;
            this.description = description;
            this.power = power;
            this.name = description;
            this.description = description;
            requiredSciences = EnumSet.noneOf(ScienceType.class);
        }

        public String getDescription() {
            return description;
        }

        public Items getInput() {
            return input;
        }

        public Items getOutput() {
            return output;
        }

        public EnumSet<ScienceType> getRequiredSciences() {
            return requiredSciences;
        }

        public Recipe input(Amount... amounts) {
            input = new Items(amounts);
            return this;
        }

        public Recipe output(Amount... amounts) {
            output = new Items(amounts);
            return this;
        }

        public Recipe science(ScienceType... sciences) {
            for (ScienceType s : sciences)
                requiredSciences.add(s);
            return this;
        }

        public float getPower() {
            return power;
        }
    }

    public final Recipe[] recipes;

    public final EnumSet<ItemType> outputItems;
    public final EnumSet<ItemCategory> outputCategories;

    public boolean usesPower;

    private final Array<Recipe> rec = new Array<>(Recipe.class);

    public final boolean hasInputs;

    public final IntArray outputSizes, fluidInputs;

    public RecipeList() {
        init();
        recipes = rec.toArray();
        rec.clear();

        boolean inputs = false;
        boolean withoutInputs = false;

        outputItems = EnumSet.noneOf(ItemType.class);
        outputCategories = EnumSet.noneOf(ItemCategory.class);
        outputSizes = new IntArray();
        fluidInputs = new IntArray();
        for (Recipe r : recipes) {
            if (r.output != null) {
                for (int i = 0; i < r.output.entries.length; i++) {
                    Amount e = r.output.entries[i];

                    if (outputSizes.size <= i) {
                        outputSizes.add(e.getAmount() * 2);
                    } else {
                        outputSizes.set(i, Math.max(e.getAmount() * 2, outputSizes.get(i)));
                    }

                    if (e.getCat() != null)
                        outputCategories.add(e.getCat());
                    if (e.getItem() != null)
                        outputItems.add(e.getItem());
                }
            }
            if (r.input != null) {
                for (int i = 0; i < r.input.entries.length; i++) {
                    Amount e = r.input.entries[i];

                    if (e.getCat() == ItemCategory.Fluid
                            || (e.getItem() != null && e.getItem().categories.contains(ItemCategory.Fluid))) {
                        if (fluidInputs.size <= i) {
                            fluidInputs.add(e.getAmount() * 2);
                        } else {
                            fluidInputs.set(i, Math.max(e.getAmount() * 2, fluidInputs.get(i)));
                        }
                    }
                }

                inputs = true;
            } else {
                withoutInputs = true;
            }
        }

        if (inputs && withoutInputs) throw new IllegalArgumentException("Either all or no recipes have to contain inputs!");

        hasInputs = inputs;

    }

    public void validateRecipes(Schema schema) {
        // Every inventory belongs to a single group of items (a single category / itemtype)  
        int outputs = 0, inputs = 0;

        Array<DockFilter> filters = new Array<>();

        for (Dock d : schema.getDocks()) {
            if (d.type == DockType.ItemIn || d.type == DockType.FluidIn) {
                inputs++;
                if (d.filter != null)
                    filters.add(d.filter);
            } else if (d.type == DockType.ItemOut || d.type == DockType.FluidOut) {
                outputs++;
            }
        }

        int maxIns = 0;

        for (Recipe r : recipes) {
            if (r.getInput() != null)
                maxIns = Math.max(maxIns, r.getInput().entries.length);
            r.description = Quarry.Q.i18n.get("structure." + schema.type.name().toLowerCase() + ".recipe." + r.description);
        }

        if (maxIns != inputs) {
            System.out.println(schema.type.name() + ": " + inputs + " docks, but up to " + maxIns + " inputs");
        }

        if (filters.size == 0)
            return;

        for (Recipe r : recipes) {
            if (r.input == null) continue;
            for (Amount a : r.input.entries) {
                if (a instanceof ParameterizedAmount)
                    throw new IllegalArgumentException("Parameterized input amounts are not allowed!");

                boolean any = false;
                for (DockFilter f : filters) {
                    if ((a.getItem() != null && f.accepts(a.getItem())) || (a.getCat() != null && f.accepts(a.getCat()))) {
                        any = true;
                        break;
                    }
                }

                if (!any)
                    System.out.println(schema.type + ": I think no filter accepts item '" + (a.getItem() == null ? a.getCat() : a.getItem()) + "'!");
            }

            if (r.output != null) {
                if (r.output.entries.length > outputs)
                    throw new IllegalArgumentException("Invalid amount of output amounts!");
                for (Amount a : r.output.entries) {
                    if (a.getCat() != null)
                        throw new IllegalArgumentException("Output categories are not allowed!");
                    if (a instanceof ParameterizedAmount) {
                        Amount in = r.input.entries[((ParameterizedAmount) a).inputParameter];
                        if (in == null)
                            throw new IllegalArgumentException("Invalid parameterized output!");
                    } else if (a instanceof SameAmount) {
                        Amount src = r.input.entries[((SameAmount) a).inputParameter];
                        a.setCat(src.getCat());
                        a.setItem(src.getItem());
                    }
                }
            }
            if (r.getPower() > 0) usesPower = true;
        }
    }

    protected void add(Recipe r) {
        rec.add(r);
    }

    public boolean containsOutput(ItemType item) {
        return outputItems.contains(item);
    }

    public boolean containsOutput(ItemCategory cat) {
        return outputCategories.contains(cat);
    }

    protected abstract void init();
}
