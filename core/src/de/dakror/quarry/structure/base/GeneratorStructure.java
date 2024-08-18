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

package de.dakror.quarry.structure.base;

import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class GeneratorStructure extends ProducerStructure {
    public static class GeneratorRecipe extends Recipe {
        private int powerGeneration;

        public GeneratorRecipe(String description, int powerGeneration) {
            super(1f, description);

            this.powerGeneration = powerGeneration;
        }

        public int getPowerGeneration() {
            return powerGeneration;
        }
    }

    public static class GeneratorSchema extends ProducerSchema {
        public GeneratorSchema(int version, StructureType type, int width, int height, String tex, Items buildCosts, RecipeList recipes, Sfx sfx, Dock... docks) {
            super(version, type, width, height, tex, buildCosts, recipes, sfx, true, docks);

            for (Recipe r : recipes.recipes) {
                if (!(r instanceof GeneratorRecipe)) {
                    throw new RuntimeException("Recipe is not of type GeneratorRecipe");
                }
            }
        }
    }

    public GeneratorStructure(int x, int y, GeneratorSchema schema) {
        super(x, y, schema);

        // dump fast after initInventories()
        for (Component c : components) {
            if (c instanceof CTank) {
                ((CTank) c).setMaxOutput(10_000).setPumpOutDelay(0);
            }
        }
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        offerPower(deltaTime, gameSpeed);
    }

    protected void offerPower(double deltaTime, int gameSpeed) {
        if (sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction())
                || noPower || workDelay == -1 || additionalWorkBlockingCondition() || gameSpeed == 0) {
            return;
        }

        powerNetwork.offerPower(deltaTime, gameSpeed, ((GeneratorRecipe) activeRecipe).getPowerGeneration() * deltaTime * gameSpeed, this);
    }

    // deactivate all power accepting

    @Override
    protected void setRecipe(int index) {
        setItemNotifications();
        activeRecipeIndex = index;
        if (index > -1) {
            activeRecipe = getSchema().recipeList.recipes[activeRecipeIndex];
            workDelay = activeRecipe.workingTime;
        } else {
            activeRecipe = null;
            activeItems = null;
        }
        updateUI();
    }

    @Override
    public double acceptPower(double amount, double networkStrength) {
        return amount;
    }

    @Override
    public double getPowerCapacity() {
        return 0;
    }

    @Override
    public double getPowerLevel() {
        return 0;
    }

    @Override
    public int getReceiverPriority() {
        return 0;
    }
}
