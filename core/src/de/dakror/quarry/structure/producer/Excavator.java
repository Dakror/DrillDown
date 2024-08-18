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

import java.util.EnumMap;
import java.util.Map;

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Excavator extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0,
            StructureType.Excavator, 4, 4,
            "excavator",
            new Items(ItemType.StoneBrick, 300, ItemType.MachineFrame, 20, ItemType.AdvancedMachineFrame, 2, ItemType.SteelCable, 50, ItemType.Rotor, 10),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(3f, "ore", 3334)
                            .input(new Amount(ItemType.Dynamite, 1))
                            .output(new Amount(ItemType._Dust, 30), new Amount(ItemType._Dust, 30)));
                }
            }, new Sfx("excavator" + Const.SFX_FORMAT),
            true,
            new Dock(0, 2, Direction.West, DockType.ItemIn, new DockFilter(ItemType.Dynamite)),
            new Dock(0, 0, Direction.South, DockType.ItemOut),
            new Dock(3, 0, Direction.South, DockType.ItemOut),
            new Dock(3, 2, Direction.East, DockType.Power))
                    .sciences(ScienceType.HighTech)
                    .flags(Flags.MirroredTextureHorizontal);

    public static final int MIN_ORE_TILES = 7;
    public static final float BASE_TIMEOUT = 3f;

    public ItemType mineableItem;

    int successiveProductions;

    public Excavator(int x, int y) {
        super(x, y, classSchema);
        ((CInventory) outputInventories[0]).setPumpOutSpeed(0);
        ((CInventory) outputInventories[1]).setPumpOutSpeed(0);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);
        if (fromLoading) return;
        updateMineableItem();
    }

    @Override
    public void postLoad() {
        super.postLoad();
        updateMineableItem();
    }

    protected void updateMineableItem() {
        Layer l = layer;
        if (l == null) l = Game.G.layer;

        if (l != null) {
            mineableItem = null;

            EnumMap<TileType, Integer> counts = new EnumMap<>(TileType.class);

            for (int i = 0; i < getWidth(); i++) {
                for (int j = 0; j < getHeight(); j++) {
                    TileType tile = l.get(x + i, y + j);
                    if (tile == TileType.CopperOre
                            || tile == TileType.IronOre
                            || tile == TileType.TinOre
                            || tile == TileType.CoalOre) {
                        counts.put(tile, (counts.containsKey(tile) ? counts.get(tile) : 0) + 1);
                    }
                }
            }

            for (Map.Entry<TileType, Integer> e : counts.entrySet()) {
                if (e.getValue() >= Excavator.MIN_ORE_TILES) {
                    mineableItem = Item.get(ItemType._Dust, e.getKey().itemDrop);
                    break;
                }
            }
        }
    }

    @Override
    protected void doProductionStep() {
        if (mineableItem != null) {
            outputInventories[0].addUnsafe(mineableItem, activeRecipe.getOutput().entries[0].getAmount());
            outputInventories[1].addUnsafe(mineableItem, activeRecipe.getOutput().entries[1].getAmount());
            successiveProductions++;
        }
    }
}
