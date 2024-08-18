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

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CInventory;
import de.dakror.quarry.structure.base.component.CRecipeSlotStorage;
import de.dakror.quarry.structure.base.component.CStackerRecipeSlotStorage;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.structure.base.component.IStorage;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Stacker extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.Stacker, 3,
            3, "stacker",
            new Items(ItemType.MachineFrame, 5, ItemType.SteelIngot, 80, ItemType.SteelWire, 50, ItemType.TinPlate, 80, ItemType.Dynamo, 15, ItemType.Rotor, 6),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(8f, "stack", 1000)
                            .input(new Amount(ItemType.Pallet, 1), new Amount(ItemCategory.Stackable, 24))
                            .output(new Amount(ItemType._FilledPallet, 1)));
                    add(new Recipe(8f, "unstack", 1000)
                            .input(new Amount(ItemCategory.FilledPallet, 1))
                            .output(new Amount(ItemType.Pallet, 1), new Amount(ItemType._AnyStackable, 24)));
                }
            }, new Sfx("stacker" + Const.SFX_FORMAT),
            true,
            new Dock(0, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemType.Pallet, ItemCategory.FilledPallet)),
            new Dock(2, 2, Direction.North, DockType.ItemIn, new DockFilter(ItemCategory.Stackable)),
            new Dock(0, 0, Direction.South, DockType.ItemOut),
            new Dock(2, 0, Direction.South, DockType.ItemOut),
            new Dock(0, 1, Direction.West, DockType.Power))
                    .sciences(ScienceType.AdvancedTransport);

    public Stacker(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    protected void initInventories() {
        int in = 0, out = 0, comps = schema.getComponents().size;

        for (int i = 0; i < docks.length; i++) {
            Dock d = docks[i];
            if (d.type == DockType.ItemIn) {
                if (in == 0) {
                    inputInventories[in] = new CRecipeSlotStorage(schema.recipeList, in);
                } else {
                    inputInventories[in] = new CStackerRecipeSlotStorage(schema.recipeList, in);
                }
                in++;
            } else if (d.type == DockType.ItemOut) {
                Component c = new CInventory(schema.outputBuffer ? schema.recipeList.outputSizes.get(out) : 0, i).setPumpOutSpeed(0);
                c.setStructure(this);
                c.init();
                outputInventories[out] = (IStorage) c;
                components[comps++] = c;
                out++;
            }
        }
    }

    @Override
    protected void doProductionStep() {
        if (activeRecipe.getOutput().entries.length == 1) {
            outputInventories[0].addUnsafe(Item.palletSourceItems[activeItems.entries[1].getItem().value & 0xffff], 1);
        } else {
            outputInventories[0].addUnsafe(ItemType.Pallet, 1);
            outputInventories[1].addUnsafe(Item.palletTargetItems[(activeItems.entries[0].getItem().value >> 8) & 0xff],
                    activeRecipe.getOutput().entries[1].getAmount());
        }
    }
}
