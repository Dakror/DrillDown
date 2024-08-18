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

package de.dakror.quarry.structure.base.component;

import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;

/**
 * @author Maximilian Stark | Dakror
 */
public interface IStorage {
    int getCount();

    int getSize();

    void setSize(int size);

    boolean isEmpty();

    float getFillRatio();

    boolean hasSpace();

    int get(ItemType item);

    int getSum(ItemCategory cat);

    Amount[] getSimilar(ItemType item);

    Amount[] get(ItemCategory cat);

    boolean add(ItemType item, int amount);

    int addWithRest(ItemType item, int amount);

    void addUnsafe(ItemType item, int amount);

    boolean remove(ItemType item, int amount);

    boolean remove(ItemCategory category, int amount);

    int removeWithRest(ItemType item, int amount);

    boolean canAccept(ItemType item);

    int getOutput();

    void setOutput(int dock);
}
