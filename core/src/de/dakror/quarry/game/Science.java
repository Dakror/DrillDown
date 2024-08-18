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

package de.dakror.quarry.game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;

/**
 * @author Maximilian Stark | Dakror
 */
public class Science {
    public static final ScienceType[] sciences = new ScienceType[256 /*for now*/];

    /**
     * The amount of items needed to buy the most expensive science
     */
    private static int maxBuyCosts = 0;

    public static int getMaxBuyCosts() {
        return maxBuyCosts;
    }

    public enum ScienceType {
        Start(0, null, null, 0),
        Routers(1, "icon_filter", new Items(ItemType.IronIngot, 50, ItemType.StoneBrick, 80), 45, Start),
        OreProcessing(2, "icon_crusher", new Items(ItemType.IronOre, 60, ItemType.CopperOre, 50), 37, Start),
        Metalworking(3, "icon_fe_ingot", new Items(ItemType.IronIngot, 75, ItemType.CopperIngot, 50), 55, Start),
        SteelProduction(4, "icon_steel_ingot", new Items(ItemType.IronIngot, 30), 40, Start),
        BetterStorage(5, "icon_storage", new Items(ItemType.Scaffolding, 60, ItemType.StoneBrick, 90), 46, SteelProduction),
        WaterUsage(6, "icon_water", new Items(ItemType.SteelIngot, 50, ItemType.CopperTube, 30), 50, Metalworking),
        CharcoalProduction(8, "icon_charcoal", new Items(ItemType.Wood, 75), 35, Start),
        MineExpansion(9, "icon_drill_button", new Items(ItemType.IronIngot, 150, ItemType.SteelPlate, 10), 50, SteelProduction, WaterUsage),
        ConsiderateConstruction(11, "science_considerate", new Items(ItemType.Scaffolding, 30, ItemType.Brick, 60, ItemType.SteelIngot, 95), 45, SteelProduction),
        Magnetism(12, "icon_magnet", new Items(ItemType.IronIngot, 150), 25, Metalworking),
        Electricity(13, "science_power", new Items(ItemType.CopperWire, 100, ItemType.CarbonBlock, 10, ItemType.Magnet, 8, ItemType.BronzePlate, 5), 55, Magnetism),
        HighTech(22, "science_power", new Items(ItemType.MachineFrame, 40, ItemType.SteelWire, 60), 50, Electricity, WaterUsage),
        OilProcessing(14, "icon_crude_oil", new Items(ItemType.SteelTube, 70, ItemType.Dynamo, 4), 70, MineExpansion, HighTech),
        MineralExtraction(15, "icon_minerals", new Items(ItemType.StoneDust, 350), 105, Electricity),
        WaferGrowth(16, "icon_wafer", new Items(ItemType.SiliconDust, 90), 65, MineralExtraction),
        PlasticMolding(17, "icon_molding", new Items(ItemType.PlasticBeads, 200), 78, OilProcessing),
        ComponentAssembly(18, "icon_assembly", new Items(ItemType.SiliconWafer, 30, ItemType.PlasticCasing, 20, ItemType.GoldDust, 15, ItemType.TinWire, 250), 98, PlasticMolding, WaferGrowth),
        SolarPower(19, "icon_solar", new Items(ItemType.SiliconWafer, 2, ItemType.Glass, 90, ItemType.CopperWire, 300, ItemType.Battery, 25), 25, WaferGrowth),
        HighPower(20, "science_power", new Items(ItemType.SteelCable, 65, ItemType.TinPlate, 150, ItemType.Battery, 10), 70, HighTech),
        Boosting(21, "symb_ff", new Items(ItemType.BronzePlate, 240, ItemType.Battery, 40), 50, MineralExtraction),
        Blueprints(23, "icon_copy", new Items(ItemType.Wood, 250, ItemType.IronPlate, 250), 40, HighTech),
        AdvancedTransport(24, "icon_empty_barrel", new Items(ItemType.TinPlate, 80, ItemType.SteelPlate, 60, ItemType.WoodPlank, 120), 40, HighTech),

        ;

        public static final ScienceType[] values = values();

        public final TextureRegion icon;
        public final byte id;
        public final String title, description;
        public final Items costs;
        public final float workingTime;
        public final ScienceType[] required;

        ScienceType(int id, String icon, Items costs, float workingTime, ScienceType... required) {
            this.workingTime = workingTime;
            this.costs = costs;
            this.id = (byte) id;
            this.required = required;

            if (id > 0) {
                this.icon = Quarry.Q.atlas.findRegion(icon);
                if (icon.equals("temp")) System.err.println("[ICON] science '" + name() + "'");

                title = Quarry.Q.i18n.get("science." + name().toLowerCase());
                description = Quarry.Q.i18n.get("science." + name().toLowerCase() + ".desc");

                int count = 0;
                for (Amount a : costs.entries) {
                    if (a.getCat() != null) throw new IllegalStateException("Cost categories are not allowed.");
                    count += a.getAmount();
                }
                maxBuyCosts = Math.max(maxBuyCosts, count);
            } else {
                this.icon = null;
                title = null;
                description = null;
            }

            if (sciences[id] != null)
                throw new IllegalStateException("Science with ID " + id + " is already taken!");
            sciences[id] = this;

        }
    }
}
