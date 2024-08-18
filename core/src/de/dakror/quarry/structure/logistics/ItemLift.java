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

package de.dakror.quarry.structure.logistics;

import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;

/**
 * @author Maximilian Stark | Dakror
 */
public class ItemLift extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.ItemLift, true, 1, 1,
            "itemlift",
            new Items(ItemType.SteelIngot, 10, ItemType.StoneBrick, 4),
            null,
            new Dock(0, 0, Direction.West, DockType.ItemOut), new Dock(0, 0, Direction.East, DockType.ItemIn))
                    .sciences(ScienceType.MineExpansion, ScienceType.Routers);

    ItemLift other;
    boolean hasOutput;
    boolean upper;

    ItemType currentItem;
    Structure<?> currentSource;

    public ItemLift(int x, int y) {
        this(x, y, true, classSchema);
    }

    protected ItemLift(int x, int y, boolean upper, Schema schema) {
        super(x, y, schema);
        this.upper = upper;
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        if (!fromLoading && layer != null && upper) {
            Layer below = Game.G.getLayer(layer.getIndex() + 1);
            if (below != null) {
                this.other = new ItemLiftBelow(x, y);
                this.other.setUpDirection(upDirection);
                this.other.other = this;
                below.addStructure(this.other);
            }
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();

        other = (ItemLift) Game.G.getLayer(layer.getIndex() + (upper ? 1 : -1)).getStructure(x, y);
        updateOutput();
        setItemNotifications();
        other.setItemNotifications();
    }

    protected void updateOutput() {
        Direction d = getDocks()[0].dir;
        Structure<?> s = layer.getStructure(x + d.dx, y + d.dy);
        hasOutput = s != null
                && ((s.getSchema().type == StructureType.Conveyor && ((Conveyor) s).getDirection() != d.inv())
                        || (s.getSchema().type == StructureType.ElectricConveyor && ((Conveyor) s).getDirection() != d.inv())
                        || (s.getSchema().type == StructureType.ElectricConveyorCore && ((Conveyor) s).getDirection() == d)
                        || (s.getSchema().type == StructureType.ConveyorBridge && (((ConveyorBridge) s).getDirection() != d.inv() || ((ConveyorBridge) s).getDirection2() != d.inv()))
                        || (s.getSchema().type == StructureType.Hopper && ((Conveyor) s).getDirection() == d));
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (dirtyBounds.touches(this)) {
            updateOutput();
            other.updateOutput();
            setItemNotifications();
            other.setItemNotifications();
        }

        if (currentItem != null && other.layer.addItemEntity(currentItem, other, other.getDocks()[0].dir, currentSource)) {
            currentItem = null;
            currentSource = null;
            setItemNotifications();
            other.setItemNotifications();
        }
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return !item.categories.contains(ItemCategory.Fluid) && isNextToDock(x, y, dir, getDocks()[1]) && other.hasOutput && currentItem == null;
    }

    @Override
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        if (item.categories.contains(ItemCategory.Fluid) || !other.hasOutput || currentItem != null) return false;
        currentItem = item;
        currentSource = source;
        setItemNotifications();
        other.setItemNotifications();
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        other.layer.removeStructure(other);
    }
}
