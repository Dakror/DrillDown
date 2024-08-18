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

package de.dakror.quarry.structure.power;

import de.dakror.quarry.Const;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.GeneratorStructure;
import de.dakror.quarry.structure.base.RecipeList;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class SteamTurbine extends GeneratorStructure {
    public static final GeneratorSchema classSchema = new GeneratorSchema(0, StructureType.SteamTurbine, 3, 3,
            "steamturbine",
            new Items(ItemType.SteelPlate, 80, ItemType.CopperTube, 20, ItemType.Dynamo, 2, ItemType.Rotor, 2),
            new RecipeList() {
                @Override
                protected void init() {
                    add(new GeneratorRecipe("steam", 300 * 60)
                            .input(new Amount(ItemType.PressurizedSteam, 1000))
                            .output(new Amount(ItemType.Steam, 2500)));
                }
            },
            new Sfx("turbine" + Const.SFX_FORMAT),
            new Dock(0, 1, Direction.West, DockType.FluidIn, new DockFilter(ItemType.PressurizedSteam)),
            new Dock(1, 2, Direction.North, DockType.Power), new Dock(2, 1, Direction.East, DockType.FluidOut))
                    .sciences(ScienceType.WaterUsage, ScienceType.Electricity);

    public SteamTurbine(int x, int y) {
        super(x, y, classSchema);
    }
}
