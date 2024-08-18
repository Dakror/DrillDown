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
import de.dakror.quarry.structure.base.component.CTank;

/**
 * @author Maximilian Stark | Dakror
 */
public class TubeShaft extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.TubeShaft, true, 1, 1,
            "tubeshaft",
            new Items(ItemType.SteelIngot, 10, ItemType.SteelTube, 8),
            null,
            new Dock(0, 0, Direction.West, DockType.FluidOut), new Dock(0, 0, Direction.East, DockType.FluidIn))
                    .components(new CTank(20_000, 0).setPumpOutDelay(0).setMaxOutput(1000))
                    .sciences(ScienceType.MineExpansion, ScienceType.Routers, ScienceType.WaterUsage);

    TubeShaft other;
    int direction;

    public TubeShaft(int x, int y) {
        this(x, y, classSchema, 1);
    }

    protected TubeShaft(int x, int y, Schema schema, int direction) {
        super(x, y, schema);
        this.direction = direction;
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        if (!fromLoading && layer != null) {
            if (direction == 1) {
                Layer otherLayer = Game.G.getLayer(layer.getIndex() + direction);
                if (otherLayer != null) {
                    this.other = new TubeShaftBelow(x, y);
                    this.other.setUpDirection(upDirection);
                    this.other.other = this;
                    otherLayer.addStructure(this.other);
                }
            }
        }
    }

    @Override
    public void postLoad() {
        super.postLoad();

        other = (TubeShaft) Game.G.getLayer(layer.getIndex() + direction).getStructure(x, y);
    }

    @Override
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return item.categories.contains(ItemCategory.Fluid) && isNextToDock(x, y, dir, getDocks()[1])
                && ((CTank) other.getComponents()[0]).hasSpace();
    }

    @Override
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        if (!item.categories.contains(ItemCategory.Fluid)) return amount;
        return ((CTank) other.getComponents()[0]).addWithRest(item, amount);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (other != null) other.layer.removeStructure(other);
    }
}
