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

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.common.libgdx.ui.CropDrawable;
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
import de.dakror.quarry.structure.base.PausableSchema;
import de.dakror.quarry.structure.base.PausableStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Sfx;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Boiler extends PausableStructure<PausableSchema> {
    public static class BoilerRecipe {
        public final int maxHeat = 1000, heatPerCoal = 150, heatDecay = 5, heatThreshold = 500;
        public final float heatDecayTime = 0.3f;

        public final int input = 3000, output = 5000;
        public final float workingTime = 1f;
    }

    public static final PausableSchema classSchema = new PausableSchema(0, StructureType.Boiler, true, 5,
            3,
            "boiler",
            new Items(ItemType.CopperPlate, 35, ItemType.CopperTube, 2),
            new Sfx("boiler" + Const.SFX_FORMAT),
            new Dock(2, 2, Direction.North, DockType.FluidOut),
            new Dock(0, 0, Direction.West, DockType.FluidIn, new DockFilter(ItemType.Water)), new Dock(4, 0, Direction.East, DockType.ItemIn, new DockFilter(ItemType.Charcoal, ItemType.CoalOre)))
                    .components(new CTank(10000), new CTank(10000, 0).setPumpOutDelay(0))
                    .sciences(ScienceType.WaterUsage);

    public static final BoilerRecipe recipe = new BoilerRecipe();

    CTank tank;

    static Table ui;
    static Widget thermo;
    static Container<Table> container;

    // 0 - 1000
    int heatLevel;
    float decay;
    float workingTime;

    public Boiler(int x, int y) {
        super(x, y, classSchema);
        tank = (CTank) components[0];
        decay = recipe.heatDecayTime;
        workingTime = recipe.workingTime;
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return (item.categories.contains(ItemCategory.Fluid) && isNextToDock(x, y, dir, getDocks()[1]) && tank.hasSpace() && getDocks()[1].filter.accepts(item))
                || (isNextToDock(x, y, dir, getDocks()[2]) && getDocks()[2].filter.accepts(item) && heatLevel <= recipe.maxHeat - recipe.heatPerCoal);
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (tank.hasSpace() && getDocks()[1].filter.accepts(item)) {
            int rest = tank.addWithRest(item, amount);
            if (clicked) updateUI();
            return rest;
        }
        return amount;
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (heatLevel > 0) {
            shaper.setColor(0, 0.5f, 0, 1);
            float progress = 1 - workingTime / recipe.workingTime;
            shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, progress * getWidth() * Const.TILE_SIZE, 8);
        }

        drawBoostState(spriter);

        if (!((IStorage) components[1]).hasSpace()) {
            drawFullState(spriter);
        }
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        if (getDocks()[2].filter.accepts(item) && heatLevel <= recipe.maxHeat - recipe.heatPerCoal) {
            heatLevel = Math.min(recipe.maxHeat, heatLevel + recipe.heatPerCoal);
            if (clicked) updateUI();
            return true;
        }
        return false;
    }

    @Override
    public float getLoudness() {
        return (sleeping || heatLevel < recipe.heatThreshold ? 0.1f : 1) * getSchema().loudness * speedScale;
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (gameSpeed == 0) {
            pauseSfx();
        } else {
            if (!sleeping && heatLevel > 0) {
                if (tank.getCount() >= recipe.input && heatLevel >= recipe.heatThreshold) {
                    playSfx();
                    workingTime -= deltaTime * gameSpeed;
                    if (workingTime <= 0) {
                        workingTime = 0;
                        if (((CTank) components[1]).hasSpace()) {
                            tank.removeWithRest(ItemType.Water, recipe.input);
                            ((CTank) components[1]).addUnsafe(ItemType.PressurizedSteam, recipe.output);
                            workingTime = recipe.workingTime;
                        }
                    }
                } else if (tank.isEmpty() || heatLevel < recipe.heatThreshold) {
                    pauseSfx();
                }

                decay -= deltaTime * gameSpeed;
                if (decay <= 0) {
                    heatLevel = Math.max(0, heatLevel - recipe.heatDecay * gameSpeed);
                    if (clicked) updateUI();
                    setItemNotifications();
                    decay = recipe.heatDecayTime;
                }
            } else {
                pauseSfx();
            }
        }
    }

    private void updateUI() {
        if (container == null) return;

        thermo.setUserObject(this);
        ((Label) ((Table) ui.getChildren().get(0)).getChildren().get(1)).setText(GameUi.formatResourceAmount(tank.getCount() / 1000f, true) + "L");
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        if (container == null) {
            container = new Container<>();
            container.fill();
            ui = new Table();
            ui.defaults().uniform().expand();
            ui.add(GameUi.createResourceTable(32, Quarry.Q.skin, "icon_water", "")).right();
            ui.add(thermo = new Widget() {
                final Scaling scale = Scaling.fill;
                final Drawable bg = Quarry.Q.skin.getDrawable("icon_thermo_outer");
                final CropDrawable fg = new CropDrawable((TextureRegionDrawable) Quarry.Q.skin.getDrawable("icon_thermo_inner"));

                @Override
                public void draw(Batch batch, float parentAlpha) {
                    super.draw(batch, parentAlpha);
                    Vector2 v = scale.apply(bg.getMinWidth(), bg.getMinHeight(), getWidth(), getHeight());
                    bg.draw(batch, getX() + (getWidth() - v.x) / 2, getY() + (getHeight() - v.y) / 2, v.x, v.y);

                    fg.setHeightRatio((float) ((Boiler) getUserObject()).heatLevel / recipe.maxHeat);
                    fg.draw(batch, getX() + (getWidth() - v.x) / 2, getY() + (getHeight() - v.y) / 2, v.x, v.y);
                }

                @Override
                public float getMinWidth() {
                    return bg.getMinWidth();
                }

                @Override
                public float getMinHeight() {
                    return bg.getMinHeight();
                }
            }).pad(20).fillY().expandX();
            container.setActor(ui);
        }

        updateUI();

        content.add(container).grow();
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        b.Int("heat", heatLevel)
                .Float("decay", decay)
                .Float("workingTime", workingTime);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        heatLevel = tag.Int("heat", 0);
        decay = tag.Float("decay", 0);
        workingTime = tag.Float("workingTime", 0);
    }
}
