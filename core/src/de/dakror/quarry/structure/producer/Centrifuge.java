/*******************************************************************************
 * Copyright 2018 Maximilian Stark | Dakror <mail@dakror.de>
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

package de.dakror.quarry.structure.producer;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.ConstantSupplyAmount;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Centrifuge extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(1, StructureType.Centrifuge, 11, 11,
            "centrifuge",
            new Items(ItemType.MachineFrame, 25, ItemType.SteelIngot, 200, ItemType.BronzeIngot, 200, ItemType.Dynamo, 10, ItemType.Magnet, 200, ItemType.CarbonBlock, 60),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(60f, "titanium", 1000)
                            .input(new Amount(ItemType.IronDust, 30), new ConstantSupplyAmount(ItemType.Lubricant, 80))
                            .output(new Amount(ItemType.TitaniumDust, 1), new Amount(ItemType.StoneDust, 20)));
                    add(new Recipe(115f, "stone", 2900)
                            .input(new Amount(ItemType.StoneDust, 175), new ConstantSupplyAmount(ItemType.Lubricant, 200))
                            .output(new Amount(ItemType.QuartzDust, 85), new Amount(ItemType.PyriteDust, 60)));
                    add(new Recipe(80f, "pyrite", 1500)
                            .input(new Amount(ItemType.PyriteDust, 15), new ConstantSupplyAmount(ItemType.Lubricant, 100))
                            .output(new Amount(ItemType.IronDust, 5), new Amount(ItemType.SulfurDust, 10)));
                    add(new Recipe(185f, "gold", 10000)
                            .input(new Amount(ItemType.QuartzDust, 570), new ConstantSupplyAmount(ItemType.Lubricant, 500))
                            .output(new Amount(ItemType.GoldDust, 1), new Amount(ItemType.SiliconDust, 210)));
                }

                @Override
                public void validateRecipes(de.dakror.quarry.structure.base.Schema schema) {
                    super.validateRecipes(schema);
                    fluidInputs.set(0, 10_000); // increase lubricant tank size
                }
            },
            new Sfx("centrifuge" + Const.SFX_FORMAT),
            true,
            new Dock(10, 4, Direction.East, DockType.ItemOut),
            new Dock(10, 6, Direction.East, DockType.ItemOut),
            new Dock(5, 10, Direction.North, DockType.BigPower),
            new Dock(5, 0, Direction.South, DockType.Power),
            new Dock(0, 4, Direction.West, DockType.ItemIn, new DockFilter(ItemType.StoneDust, ItemType.QuartzDust, ItemType.PyriteDust, ItemType.IronDust)), new Dock(0, 6, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.MineralExtraction)
                    .flags(Flags.MirroredTextureHorizontal, Flags.MirroredTextureVertical, Flags.TextureAlwaysUpright);

    public static final ProducerSchema classSchemaV103 = new ProducerSchema(0, StructureType.Centrifuge, 11, 11,
            "centrifuge",
            new Items(ItemType.MachineFrame, 25, ItemType.SteelIngot, 200, ItemType.BronzeIngot, 200, ItemType.Dynamo, 10, ItemType.Magnet, 200, ItemType.CarbonBlock, 60),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(115f, "stone", 2900)
                            .input(new Amount(ItemType.StoneDust, 175), new ConstantSupplyAmount(ItemType.Lubricant, 200))
                            .output(new Amount(ItemType.QuartzDust, 85), new Amount(ItemType.PyriteDust, 60)));
                    add(new Recipe(80f, "pyrite", 1500)
                            .input(new Amount(ItemType.PyriteDust, 15), new ConstantSupplyAmount(ItemType.Lubricant, 100))
                            .output(new Amount(ItemType.IronDust, 5), new Amount(ItemType.SulfurDust, 10)));
                    add(new Recipe(185f, "gold", 8500)
                            .input(new Amount(ItemType.QuartzDust, 570), new ConstantSupplyAmount(ItemType.Lubricant, 500))
                            .output(new Amount(ItemType.GoldDust, 1), new Amount(ItemType.SiliconDust, 210)));
                }

                @Override
                public void validateRecipes(de.dakror.quarry.structure.base.Schema schema) {
                    super.validateRecipes(schema);
                    fluidInputs.set(0, 10_000); // increase lubricant tank size
                }
            },
            new Sfx("centrifuge" + Const.SFX_FORMAT),
            true,
            new Dock(10, 4, Direction.East, DockType.ItemOut),
            new Dock(10, 6, Direction.East, DockType.ItemOut),
            new Dock(5, 10, Direction.North, DockType.BigPower),
            new Dock(5, 0, Direction.South, DockType.Power),
            new Dock(0, 4, Direction.West, DockType.ItemIn, new DockFilter(ItemType.StoneDust, ItemType.QuartzDust, ItemType.PyriteDust)), new Dock(0, 6, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Lubricant)))
                    .sciences(ScienceType.MineralExtraction)
                    .flags(Flags.MirroredTextureHorizontal, Flags.MirroredTextureVertical, Flags.TextureAlwaysUpright);

    boolean tooLittleLubricant;

    public Centrifuge(int x, int y, int version) {
        super(x, y, selectSchema(version, classSchema, classSchemaV103));
    }

    public Centrifuge(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (sleeping || activeRecipe == null || !(hasCapacity = hasCapacityForProduction()) || noPower) {
            return;
        }

        CTank lubricantTank = (CTank) inputInventories[1];
        if (!lubricantTank.isEmpty()) {
            tooLittleLubricant = activeRecipe.getInput().entries[1].getAmount() / 60f > lubricantTank.getCount();
            if (!tooLittleLubricant) {
                lubricantTank.removeWithRest(ItemType.Lubricant, (int) Math.round(activeRecipe.getInput().entries[1].getAmount() * gameSpeed * deltaTime));

                Game.G.ui.updateStructureUIInventory();
            }
        }
    }

    @Override
    protected boolean additionalWorkBlockingCondition() {
        return tooLittleLubricant;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (activeRecipe != null && tooLittleLubricant) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(ItemType.Lubricant.icon, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f * 4,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
        }
    }
}
