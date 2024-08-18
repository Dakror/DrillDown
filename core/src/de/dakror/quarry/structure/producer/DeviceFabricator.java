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

import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
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
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class DeviceFabricator extends ProducerStructure {
    public static final ProducerSchema classSchema = new ProducerSchema(0, StructureType.DeviceFabricator, 24, 9,
            "devicefabricator",
            new Items(ItemType.Glass, 650, ItemType.MachineFrame, 400, ItemType.AdvancedMachineFrame, 100,
                    ItemType.Dynamo, 250, ItemType.BronzePlate, 500, ItemType.SteelCable, 300),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new Recipe(860f, "chip", 60000)
                            .input(new Amount(ItemType.PlasticCasing, 2), new Amount(ItemType.GoldWire, 80),
                                    new Amount(ItemType.SiliconWafer, 5),
                                    new Amount(ItemType.TinWire, 50), new Amount(ItemType.SyntheticQuartz, 5))
                            .output(new Amount(ItemType.Chip, 1)));
                }
            }, new Sfx("devicefabricator" + Const.SFX_FORMAT),
            true,
            new Dock(23, 4, Direction.East, DockType.ItemOut),
            new Dock(0, 2, Direction.West, DockType.ItemIn, new DockFilter(ItemType.PlasticCasing)),
            new Dock(0, 3, Direction.West, DockType.ItemIn, new DockFilter(ItemType.GoldWire)),
            new Dock(0, 4, Direction.West, DockType.ItemIn, new DockFilter(ItemType.SiliconWafer)),
            new Dock(0, 5, Direction.West, DockType.ItemIn, new DockFilter(ItemType.TinWire)),
            new Dock(0, 6, Direction.West, DockType.ItemIn, new DockFilter(ItemType.SyntheticQuartz)),
            new Dock(4, 8, Direction.North, DockType.BigPower), new Dock(4, 0, Direction.South, DockType.BigPower))
            .sciences(ScienceType.ComponentAssembly)
            .flags(Flags.MirroredTextureVertical);

    public DeviceFabricator(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    protected void doProductionStep() {
        super.doProductionStep();

        if (!Game.G.isInfinite()) {
            Game.G.ui.endOfGame.show();
        }
    }
}
