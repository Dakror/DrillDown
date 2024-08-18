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

package de.dakror.quarry.structure.producer;

import java.util.TreeSet;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Mine extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Mine, 2, 2,
            "mine", new Items(ItemType.Stone, 10, ItemType.Scaffolding, 2),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(12f, "stone").output(new Amount(ItemType.Stone, 2)));
                    add(new Recipe(5f, "dirt").output(new Amount(ItemType.Dirt, 1)));
                    add(new Recipe(18f, "clay").output(new Amount(ItemType.Clay, 2)));
                    add(new Recipe(30f, "ore").output(new Amount(ItemType._Ore, 1)));
                }
            }, new Sfx("mine" + Const.SFX_FORMAT),
            true, new Dock(0, 1, Direction.North, DockType.ItemOut))
                    .flags(Flags.TextureAlwaysUpright);

    protected ItemType activeItem;

    public TreeSet<ItemType> mineableItems = new TreeSet<>();

    public Mine(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (fromLoading) return;
        updateMineableItems();
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateMineableItems();
    }

    protected void updateMineableItems() {
        Layer l = layer;
        if (l == null) l = Game.G.layer;

        if (l != null) {
            mineableItems.clear();
            for (int i = 0; i < getWidth(); i++) {
                for (int j = 0; j < getHeight(); j++) {
                    TileType t = l.get(x + i, y + j);
                    if (t.itemDrop != null) {
                        boolean anyRecipe = false;
                        for (Recipe r : getSchema().recipeList.recipes) {
                            if (r.getOutput().entries[0].getItem() == t.itemDrop || r.getOutput().entries[0].getItem() == Item.base(t.itemDrop)) {
                                anyRecipe = true;
                                break;
                            }
                        }

                        if (anyRecipe) {
                            mineableItems.add(t.itemDrop);
                        }
                    }
                }
            }
        }
    }

    public int getMineableItems() {
        updateMineableItems();

        return mineableItems.size();
    }

    @Override
    protected void pickRandomActiveRecipe() {
        int x = (int) (Math.random() * 2);
        int y = (int) (Math.random() * 2);

        TileType t = layer.get(this.x + x, this.y + y);

        boolean anyRecipe = false;
        int i = 0;
        for (Recipe r : getSchema().recipeList.recipes) {
            if (r.getOutput().entries[0].getItem() == t.itemDrop || r.getOutput().entries[0].getItem() == Item.base(t.itemDrop)) {
                activeRecipeIndex = i;
                activeRecipe = getSchema().recipeList.recipes[i];
                anyRecipe = true;
                break;
            }
            i++;
        }

        if (anyRecipe) {
            activeItem = t.itemDrop;

            workDelay = activeRecipe.workingTime;
            updateUI();
        } else {
            // search for tile with recipe
            for (int k = 0; k < getWidth(); k++) {
                if (anyRecipe) break;
                for (int j = 0; j < getHeight(); j++) {
                    if (anyRecipe) break;

                    t = layer.get(this.x + k, this.y + j);

                    anyRecipe = false;
                    i = 0;
                    for (Recipe r : getSchema().recipeList.recipes) {
                        if (r.getOutput().entries[0].getItem() == t.itemDrop || r.getOutput().entries[0].getItem() == Item.base(t.itemDrop)) {
                            activeRecipeIndex = i;
                            activeRecipe = getSchema().recipeList.recipes[i];
                            anyRecipe = true;
                            break;
                        }
                        i++;
                    }

                    if (anyRecipe) {
                        activeItem = t.itemDrop;

                        workDelay = activeRecipe.workingTime;
                        updateUI();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void draw(SpriteRenderer spriter) {
        super.draw(spriter);

        // draw mineable items
        int i = 0;
        for (ItemType t : mineableItems) {
            spriter.add(t.icon, this.x * Const.TILE_SIZE + 12 + ((this.getWidth() * Const.TILE_SIZE - 24) - mineableItems.size() * 20) / 2 + i * 20,
                    this.y * Const.TILE_SIZE + 12, Const.Z_STATES, 16, 16);
            i++;
        }
    }

    @Override
    protected void doProductionStep() {
        if (activeItem != null)
            outputInventories[0].add(activeItem, activeRecipe.getOutput().entries[0].getAmount());
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        if (activeItem != null)
            b.Short("activeItem", activeItem.value);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        short item = tag.Short("activeItem", (short) 0);
        activeItem = item == 0 ? null : Item.get(item);
    }
}
