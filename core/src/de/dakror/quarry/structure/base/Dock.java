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

import java.util.Arrays;
import java.util.EnumSet;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;

/**
 * @author Maximilian Stark | Dakror
 */
public class Dock {
    public static final ConnectionType[] connectionTypes = new ConnectionType[10];

    public enum ConnectionType {
        No(1),
        Open(2),
        Dock(3),
        Closed(4),

        // for fluids
        DockOutput(8),
        DockInput(9);

        public final byte value;

        ConnectionType(int value) {
            this.value = (byte) value;
            connectionTypes[value] = this;
        }
    }

    public static class DockFilter {
        final EnumSet<ItemCategory> categories = EnumSet.noneOf(ItemCategory.class);
        final EnumSet<ItemCategory> excludedCategories = EnumSet.noneOf(ItemCategory.class);
        final EnumSet<ItemType> items = EnumSet.noneOf(ItemType.class);
        final EnumSet<ItemType> excludedItems = EnumSet.noneOf(ItemType.class);

        public DockFilter(DockFilter o) {
            categories.addAll(o.categories);
            excludedCategories.addAll(o.excludedCategories);
            items.addAll(o.items);
            excludedItems.addAll(o.excludedItems);
        }

        public DockFilter(Object... filters) {
            for (Object o : filters) {
                if (o instanceof ItemCategory) categories.add((ItemCategory) o);
                else if (o instanceof ItemType) items.add((ItemType) o);
                else throw new IllegalArgumentException("Illegal parameter type for " + o);
            }
        }

        public DockFilter exclude(ItemType... items) {
            excludedItems.addAll(Arrays.asList(items));
            return this;
        }

        public DockFilter exclude(ItemCategory... cat) {
            excludedCategories.addAll(Arrays.asList(cat));
            return this;
        }

        public boolean accepts(ItemType item) {
            if (excludedItems.contains(item) || excludedItems.contains(Item.base(item))) return false;
            for (ItemCategory c : excludedCategories)
                if (item.categories.contains(c)) return false;

            if (items.contains(item) || items.contains(Item.base(item))) return true;
            for (ItemCategory c : categories) {
                if (item.categories.contains(c)) return true;
            }

            return false;
        }

        // just for validation purposes
        public boolean accepts(ItemCategory category) {
            for (ItemCategory c : excludedCategories)
                if (category == c) return false;

            for (ItemCategory c : categories)
                if (category == c) return true;

            return false;
        }
    }

    public enum DockType {
        ItemIn("itemin"),
        ItemOut("itemout"),
        FluidIn("fluidin"),
        FluidOut("fluidout"),
        Power("power"),
        BigPower("bigpower"),
        StackIn(null),
        StackOut(null);

        public final TextureRegion tex;

        DockType(String name) {
            if (name != null) {
                tex = Quarry.Q.atlas.findRegion("dock_" + name);
            } else {
                tex = null;
            }
        }
    }

    public final int x, y;
    public final Direction dir;
    public final DockType type;
    public final DockFilter filter;

    public Dock(int x, int y, Direction dir, DockType type, DockFilter filter) {
        this.x = x;
        this.y = y;
        this.dir = dir;
        this.type = type;
        this.filter = filter;
    }

    public Dock(int x, int y, Direction dir, DockType type) {
        this(x, y, dir, type, null);
    }

    public Dock(Dock o) {
        x = o.x;
        y = o.y;
        dir = o.dir;
        type = o.type;
        if (o.filter != null) {
            filter = new DockFilter(o.filter);
        } else {
            filter = null;
        }
    }
}
