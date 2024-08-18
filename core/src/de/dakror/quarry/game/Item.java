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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import de.dakror.quarry.Quarry;
import de.dakror.quarry.ui.PalletDrawable;

/**
 * @author Maximilian Stark | Dakror
 */
public class Item {
    private static final ItemType[] items = new ItemType[0x10000];
    /** from item to pallet */
    public static final ItemType[] palletSourceItems = new ItemType[0x10000];
    /** from pallet to item*/
    public static final ItemType[] palletTargetItems = new ItemType[0x100];
    private static final ItemCategory[] categories = new ItemCategory[0x100];
    private static final FluidType[] fluids = new FluidType[0x100];

    private static final Element[] elements = new Element[0x100];
    private static final Composite[] composites = new Composite[0x100];

    static {
        for (Element e : Element.values) {
            if (elements[e.value & 0xff] != null)
                throw new IllegalStateException("Element with ID " + e.value + " is already taken!");
            elements[e.value & 0xff] = e;
        }
        for (Composite e : Composite.values) {
            if (composites[e.value & 0xff] != null)
                throw new IllegalStateException("Composite with ID " + e.value + " is already taken!");
            composites[e.value & 0xff] = e;
        }
        for (ItemType t : ItemType.values) {
            if (items[t.value & 0xffff] != null)
                throw new IllegalStateException("Item with ID " + t.value + " is already taken!");
            items[t.value & 0xffff] = t;
            if (t.stackable != null) {
                palletSourceItems[t.stackable.value & 0xffff] = t;
                palletTargetItems[(t.value >> 8) & 0xff] = t.stackable;
            }
        }
        for (FluidType t : FluidType.values) {
            if (fluids[t.type.value & 0xff] != null)
                throw new IllegalStateException("Fluid with ID " + t.type.value + " is already taken!");
            fluids[t.type.value & 0xff] = t;
        }
        for (ItemCategory t : ItemCategory.values) {
            if (categories[t.id & 0xff] != null)
                throw new IllegalStateException("Category with ID " + t.id + " is already taken!");
            categories[t.id & 0xff] = t;
        }
    }

    public enum Element {
        C(1), Fe(2), Cu(3), Sn(4), Si(5), S(6), Au(7), Ti(8);

        public static final Element[] values = values();

        public final byte value;

        Element(int value) {
            this.value = (byte) value;
        }
    }

    public enum Composite {
        Stone(200),
        Steel(201),
        Bronze(202),
        Pyrite(203),
        Quartz(204),
        HardenedSteel(205),

        ;

        public static final Composite[] values = values();

        public final byte value;

        Composite(int value) {
            this.value = (byte) value;
        }
    }

    public enum ItemCategory {
        Nul(255, null),
        Abstract(1, null), /* not to be instantiated*/
        Material(2, "item_stone"),
        Fluid(3, "item_water"),
        RawOre(4, null),
        CoalFuel(5, "item_fuel"),
        Component(6, null),
        IntermediateOil(7, null),
        RawOreDust(8, "item_dust"),
        Stackable(9, "item_ore"),
        FilledPallet(10, "item_filled_pallet"),

        ;

        public static final ItemCategory[] values = values();

        public final byte id;
        public final TextureRegion icon;
        public final Drawable drawable;
        public final String title;

        ItemCategory(int id, String icon) {
            this.icon = Quarry.Q.atlas.findRegion(icon);
            drawable = icon != null ? Quarry.Q.skin.getDrawable(icon) : null;
            title = Quarry.Q.i18n.get("cat." + name());
            this.id = (byte) id;
        }
    }

    public enum ItemType {
        Nil(255, "nil", 0),
        Stone(1, "stone", 1, ItemCategory.Material),
        Wood(2, "wood", 2, ItemCategory.Material),
        StoneBrick(3, "stone_brick", 151, ItemCategory.Material),
        Dirt(4, "dirt", 1, ItemCategory.Material),
        Scaffolding(5, "scaffolding", 122, ItemCategory.Material),
        Charcoal(6, "charcoal", 100, ItemCategory.CoalFuel),
        Clay(7, "clay", 1, ItemCategory.Material),
        Brick(8, "brick", 136, ItemCategory.Material),
        WoodPlank(9, "planks", 41, ItemCategory.Material),
        Sand(10, "sand", 70, ItemCategory.Material),
        Glass(11, "glass", 1414, ItemCategory.Material),
        Cement(12, "cement", 122, ItemCategory.Material),
        ConcretePowder(13, "concretepowder", 306, ItemCategory.Material),
        SyntheticQuartz(14, "syntheticquartz", 895742, ItemCategory.Material),

        _Ore(20, "ore", 15, ItemCategory.RawOre, ItemCategory.Abstract),
        CoalOre(_Ore, 15, Element.C, ItemCategory.CoalFuel),
        IronOre(_Ore, 15, Element.Fe),
        //        GoldOre(_Ore, Element.Au),
        CopperOre(_Ore, 15, Element.Cu),
        //        ZincOre(_Ore, Element.Zn),
        TinOre(_Ore, 15, Element.Sn),

        _Gravel(21, "gravel", 48, ItemCategory.Abstract),
        IronGravel(_Gravel, 48, Element.Fe, ItemCategory.RawOre),
        //        GoldGravel(_Gravel, Element.Au, ItemCategory.RawOre),
        CopperGravel(_Gravel, 48, Element.Cu, ItemCategory.RawOre),
        //        ZincGravel(_Gravel, Element.Zn, ItemCategory.RawOre),
        TinGravel(_Gravel, 48, Element.Sn, ItemCategory.RawOre),
        StoneGravel(_Gravel, 41, Composite.Stone),

        _Dust(220, "dust", 66, ItemCategory.Abstract),
        CoalDust(_Dust, 45, Element.C),
        IronDust(_Dust, 66, Element.Fe, ItemCategory.RawOre, ItemCategory.RawOreDust),
        GoldDust(_Dust, 4612516, Element.Au, ItemCategory.RawOre, ItemCategory.RawOreDust),
        CopperDust(_Dust, 66, Element.Cu, ItemCategory.RawOre, ItemCategory.RawOreDust),
        //        ZincDust(_Dust, Element.Zn, ItemCategory.RawOre),
        TinDust(_Dust, 66, Element.Sn, ItemCategory.RawOre, ItemCategory.RawOreDust),
        SiliconDust(_Dust, 21964, Element.Si),
        SulfurDust(_Dust, 36270, Element.S),
        TitaniumDust(_Dust, 356560, Element.Ti),
        StoneDust(_Dust, 64, Composite.Stone),
        BronzeDust(_Dust, 278, Composite.Bronze, ItemCategory.RawOre, ItemCategory.RawOreDust),
        PyriteDust(_Dust, 8180, Composite.Pyrite),
        QuartzDust(_Dust, 5774, Composite.Quartz),

        _MoltenMetal(40, "molten_metal", 0, ItemCategory.Fluid, ItemCategory.Abstract),
        MoltenIron(_MoltenMetal, 0, Element.Fe),
        MoltenGold(_MoltenMetal, 11532, Element.Au),
        MoltenCopper(_MoltenMetal, 0, Element.Cu),
        //        MoltenZinc(_MoltenMetal, Element.Zn),
        MoltenTin(_MoltenMetal, 0, Element.Sn),
        MoltenSilicon(_MoltenMetal, 55, Element.Si),
        MoltenSteel(_MoltenMetal, 1, Composite.Steel),
        MoltenBronze(_MoltenMetal, 1, Composite.Bronze),
        MoltenTitanium(_MoltenMetal, 1, Element.Ti),

        _Ingot(23, "ingot", 490, ItemCategory.Abstract),
        IronIngot(_Ingot, 490, Element.Fe),
        GoldIngot(_Ingot, 11532029, Element.Au),
        CopperIngot(_Ingot, 490, Element.Cu),
        //        ZincIngot(_Ingot, Element.Zn),
        TinIngot(_Ingot, 490, Element.Sn),
        SteelIngot(_Ingot, 1452, Composite.Steel),
        BronzeIngot(_Ingot, 1434, Composite.Bronze),
        TitaniumIngot(_Ingot, 1000000, Element.Ti),

        _Wire(24, "wire", 0, ItemCategory.Abstract),
        //                IronWire(_Wire, Element.Fe),
        GoldWire(_Wire, 1441579, Element.Au),
        CopperWire(_Wire, 136, Element.Cu),
        //        SilverWire(Wire, Element.Ag),
        TinWire(_Wire, 136, Element.Sn),
        SteelWire(_Wire, 257, Composite.Steel),
        TitaniumWire(_Wire, 0, Element.Ti),
        //        BronzeWire(Wire, Composite.Bronze),

        _Plate(25, "plate", 690, ItemCategory.Abstract),
        IronPlate(_Plate, 690, Element.Fe),
        GoldPlate(_Plate, 11532229, Element.Au),
        CopperPlate(_Plate, 690, Element.Cu),
        //        ZincPlate(_Plate, Element.Zn),
        TinPlate(_Plate, 690, Element.Sn),
        SteelPlate(_Plate, 1652, Composite.Steel),
        BronzePlate(_Plate, 1634, Composite.Bronze),
        TitaniumPlate(_Plate, 1000200, Element.Ti),
        HardenedSteelPlate(_Plate, 1078904, Composite.HardenedSteel),

        _Tube(26, "tube", 0, ItemCategory.Abstract),
        //        IronTube(Tube, Element.Fe),
        CopperTube(_Tube, 810, Element.Cu),
        SteelTube(_Tube, 3505, Composite.Steel),

        Magnet(50, "magnet", 1151, ItemCategory.Component),
        CoalChunk(51, "coalchunk", 460, ItemCategory.Component),
        CarbonBlock(52, "carbonblock", 1940, ItemCategory.Component),
        Dynamo(53, "dynamo", 18874, ItemCategory.Component),
        PlasticBeads(54, "plasticbeads", 1000, ItemCategory.Component),
        SiliconWafer(55, "siliconwafer", 4757, ItemCategory.Component),
        PlasticCasing(56, "plasticcasing", 244490, ItemCategory.Component),
        Battery(57, "battery", 889218, ItemCategory.Component),
        Chip(58, "chip", 136641952, ItemCategory.Component),
        Turbine(59, "turbine", 1078847, ItemCategory.Component),
        Rotor(60, "rotor", 156747, ItemCategory.Component),
        MachineFrame(61, "machineframe", 29208, ItemCategory.Component),
        AdvancedMachineFrame(62, "advancedmachineframe", 6679663, ItemCategory.Component),
        PlateStack(63, "platestack", 1078504, ItemCategory.Component),
        SteelCable(64, "steel_cable", 39688, ItemCategory.Component),
        WoodChips(65, "wood_chips", 1, ItemCategory.Component),
        Cellulose(66, "cellulose", 33502, ItemCategory.Component),
        Paper(67, "paper", 167534, ItemCategory.Component),
        Blueprint(68, "blueprint", 677712, ItemCategory.Component),
        Gunpowder(71, "gunpowder", 72850, ItemCategory.Component),
        Dynamite(70, "dynamite", 1640401, ItemCategory.Component),

        Water(100, "water", 100, ItemCategory.Fluid),
        Steam(101, "steam", 200, ItemCategory.Fluid),
        PressurizedSteam(102, "pressurized_steam", 400, ItemCategory.Fluid),

        CrudeOil(103, "crude_oil", 500, ItemCategory.Fluid),
        IntermediateOilToColumn(104, "oil", 0, ItemCategory.Fluid, ItemCategory.IntermediateOil),
        IntermediateOilToRefinery(105, "oil", 0, ItemCategory.Fluid, ItemCategory.IntermediateOil),
        RefinedOil(106, "oil", 1000, ItemCategory.Fluid),
        Lubricant(107, "lubricant", 3000, ItemCategory.Fluid),
        NaturalGas(108, "gas", 1000000, ItemCategory.Fluid),

        EmptyBarrel(150, "empty_barrel", 46990),
        WaterBarrel(151, "water_barrel", 50793),
        CrudeOilBarrel(152, "crude_oil_barrel", 53993),
        RefinedOilBarrel(153, "oil_barrel", 57993),
        LubricantBarrel(154, "lubricant_barrel", 73993),

        EmptyTank(170, "empty_tank", 66894),
        GasTank(171, "gas_tank", 12071242),
        PressurizedSteamTank(172, "pressurized_steam_tank", 76041),

        _AnyStackable(190, "ore", 0),
        Pallet(191, "pallet", 0),
        _FilledPallet(Pallet, 1, "filled", 0),
        // Auto generated, add new ones at the bottom
        StonePallet(Pallet, 2, Stone),
        WoodPallet(Pallet, 3, Wood),
        StoneBrickPallet(Pallet, 4, StoneBrick),
        DirtPallet(Pallet, 5, Dirt),
        ScaffoldingPallet(Pallet, 6, Scaffolding),
        CharcoalPallet(Pallet, 7, Charcoal),
        ClayPallet(Pallet, 8, Clay),
        BrickPallet(Pallet, 9, Brick),
        WoodPlankPallet(Pallet, 10, WoodPlank),
        SandPallet(Pallet, 11, Sand),
        GlassPallet(Pallet, 12, Glass),
        CementPallet(Pallet, 13, Cement),
        ConcretePowderPallet(Pallet, 14, ConcretePowder),
        SyntheticQuartzPallet(Pallet, 15, SyntheticQuartz),

        CoalOrePallet(Pallet, 16, CoalOre),
        IronOrePallet(Pallet, 17, IronOre),
        CopperOrePallet(Pallet, 18, CopperOre),
        TinOrePallet(Pallet, 19, TinOre),

        IronGravelPallet(Pallet, 20, IronGravel),
        CopperGravelPallet(Pallet, 21, CopperGravel),
        TinGravelPallet(Pallet, 22, TinGravel),
        StoneGravelPallet(Pallet, 23, StoneGravel),

        CoalDustPallet(Pallet, 24, CoalDust),
        IronDustPallet(Pallet, 25, IronDust),
        GoldDustPallet(Pallet, 26, GoldDust),
        CopperDustPallet(Pallet, 27, CopperDust),
        TinDustPallet(Pallet, 28, TinDust),
        SiliconDustPallet(Pallet, 29, SiliconDust),
        SulfurDustPallet(Pallet, 30, SulfurDust),
        TitaniumDustPallet(Pallet, 31, TitaniumDust),
        StoneDustPallet(Pallet, 32, StoneDust),
        BronzeDustPallet(Pallet, 33, BronzeDust),
        PyriteDustPallet(Pallet, 34, PyriteDust),
        QuartzDustPallet(Pallet, 35, QuartzDust),

        IronIngotPallet(Pallet, 36, IronIngot),
        GoldIngotPallet(Pallet, 37, GoldIngot),
        CopperIngotPallet(Pallet, 38, CopperIngot),
        TinIngotPallet(Pallet, 39, TinIngot),
        SteelIngotPallet(Pallet, 40, SteelIngot),
        BronzeIngotPallet(Pallet, 41, BronzeIngot),
        TitaniumIngotPallet(Pallet, 42, TitaniumIngot),

        GoldWirePallet(Pallet, 43, GoldWire),
        CopperWirePallet(Pallet, 44, CopperWire),
        TinWirePallet(Pallet, 45, TinWire),
        SteelWirePallet(Pallet, 46, SteelWire),
        TitaniumWirePallet(Pallet, 47, TitaniumWire),

        IronPlatePallet(Pallet, 48, IronPlate),
        GoldPlatePallet(Pallet, 49, GoldPlate),
        CopperPlatePallet(Pallet, 50, CopperPlate),
        TinPlatePallet(Pallet, 51, TinPlate),
        SteelPlatePallet(Pallet, 52, SteelPlate),
        BronzePlatePallet(Pallet, 53, BronzePlate),
        TitaniumPlatePallet(Pallet, 54, TitaniumPlate),
        HardenedSteelPlatePallet(Pallet, 55, HardenedSteelPlate),

        CopperTubePallet(Pallet, 56, CopperTube),
        SteelTubePallet(Pallet, 57, SteelTube),

        MagnetPallet(Pallet, 58, Magnet),
        CoalChunkPallet(Pallet, 59, CoalChunk),
        CarbonBlockPallet(Pallet, 60, CarbonBlock),
        DynamoPallet(Pallet, 61, Dynamo),
        PlasticBeadsPallet(Pallet, 62, PlasticBeads),
        SiliconWaferPallet(Pallet, 63, SiliconWafer),
        PlasticCasingPallet(Pallet, 64, PlasticCasing),
        BatteryPallet(Pallet, 65, Battery),
        ChipPallet(Pallet, 66, Chip),
        TurbinePallet(Pallet, 67, Turbine),
        RotorPallet(Pallet, 68, Rotor),
        MachineFramePallet(Pallet, 69, MachineFrame),
        AdvancedMachineFramePallet(Pallet, 70, AdvancedMachineFrame),
        PlateStackPallet(Pallet, 71, PlateStack),
        SteelCablePallet(Pallet, 72, SteelCable),

        WoodChipsPallet(Pallet, 73, WoodChips),
        CellulosePallet(Pallet, 74, Cellulose),
        PaperPallet(Pallet, 75, Paper),
        BlueprintPallet(Pallet, 76, Blueprint),
        GunpowderPallet(Pallet, 77, Gunpowder),
        DynamitePallet(Pallet, 78, Dynamite),

        EmptyBarrelPallet(Pallet, 79, EmptyBarrel),
        WaterBarrelPallet(Pallet, 80, WaterBarrel),
        CrudeOilBarrelPallet(Pallet, 81, CrudeOilBarrel),
        RefinedOilBarrelPallet(Pallet, 82, RefinedOilBarrel),
        LubricantBarrelPallet(Pallet, 83, LubricantBarrel),

        EmptyTankPallet(Pallet, 84, EmptyTank),
        GasTankPallet(Pallet, 85, GasTank),
        PressurizedSteamTankPallet(Pallet, 86, PressurizedSteamTank),

        ;

        public static final ItemType[] values = values();

        public final short value;
        public final String title;
        public final String name;
        public final int worth;
        public final EnumSet<ItemCategory> categories;
        public TextureRegion icon;
        public Drawable drawable;

        public final ItemType stackable;

        ItemType(ItemType type, int worth, Element element, ItemCategory... categories) {
            this(type, element.value, element.name().toLowerCase(), worth, categories);
        }

        ItemType(ItemType type, int worth, Composite composite, ItemCategory... categories) {
            this(type, composite.value, composite.name().toLowerCase(), worth, categories);
        }

        ItemType(ItemType type, int meta, String name, int worth, ItemCategory... categories) {
            value = (short) (type.value | (((byte) meta) << 8));
            this.worth = worth;

            this.categories = EnumSet.copyOf(type.categories);
            this.categories.remove(ItemCategory.Abstract);
            this.categories.add(ItemCategory.Stackable);
            Collections.addAll(this.categories, categories);

            this.title = Quarry.Q.i18n.get("item." + name().replace("_", ""));
            this.name = name + "_" + type.name;
            icon = Quarry.Q.atlas.findRegion("item_" + this.name);
            if (icon == null) {
                icon = type.icon;
                drawable = type.drawable;
            } else {
                drawable = Quarry.Q.skin.getDrawable("item_" + this.name);
            }

            stackable = null;

            if (type.name.equals("molten_metal")) {
                icon = Quarry.Q.atlas.findRegion("item_molten_metal_actual");
                drawable = Quarry.Q.skin.getDrawable("item_molten_metal_actual");
            }
        }

        ItemType(int value, String name, int worth, ItemCategory... categories) {
            this.value = (short) (((byte) value) & 0xff);

            this.worth = worth;

            this.name = name;
            this.title = Quarry.Q.i18n.get("item." + name().replace("_", ""));

            this.categories = EnumSet.noneOf(ItemCategory.class);
            this.categories.add(ItemCategory.Stackable);
            Collections.addAll(this.categories, categories);

            stackable = null;

            icon = Quarry.Q.atlas.findRegion("item_" + name);
            if (icon == null) throw new IllegalArgumentException("could not find texture for item " + this);
            drawable = Quarry.Q.skin.getDrawable("item_" + name);
        }

        // pallet constructor
        ItemType(ItemType type, int meta, ItemType stackable) {
            value = (short) (type.value | (((byte) meta) << 8));
            this.worth = 24 * stackable.worth;

            this.categories = EnumSet.copyOf(type.categories);
            this.categories.add(ItemCategory.FilledPallet);
            this.stackable = stackable;
            this.name = stackable.name + "_" + type.name;
            this.title = stackable.title + " " + type.title;

            // TODO: create icons / drawables
            icon = type.icon;
            drawable = new PalletDrawable((TextureRegionDrawable) type.drawable, (TextureRegionDrawable) stackable.drawable);
        }
    }

    public enum FluidType {
        MoltenMetal(1, ItemType._MoltenMetal, 1f, 0.2f),
        Water(2, ItemType.Water, 0.2f, 0.5f),
        Steam(3, ItemType.Steam, 0f, 0.7f),
        PressurizedSteam(4, ItemType.PressurizedSteam, 0f, 0.9f),
        CrudeOil(5, ItemType.CrudeOil, 0.5f, 0.4f),
        IntermediateOilToColumn(6, ItemType.IntermediateOilToColumn, 0.4f, 0.4f),
        IntermediateOilToRefinery(7, ItemType.IntermediateOilToRefinery, 0.4f, 0.4f),
        RefinedOil(8, ItemType.RefinedOil, 0.25f, 0.6f),
        Lubricant(9, ItemType.Lubricant, 0.4f, 0.4f),
        Gas(10, ItemType.NaturalGas, 0, 0.9f),

        ;

        public static final FluidType[] values = values();

        public final byte id;
        public final ItemType type;
        public final TextureRegion icon;
        public final float viscosity;
        public final float pressure;

        FluidType(int id, ItemType type, float viscosity, float pressure) {
            this.id = (byte) id;
            this.icon = Quarry.Q.atlas.findRegion("fluid_" + type.name);

            this.viscosity = viscosity;
            this.pressure = pressure;
            this.type = type;
        }
    }

    public static class Items {
        public static class Amount {
            private ItemType item;
            private ItemCategory cat;
            private int amount;

            public Amount(ItemType item, int amount) {
                this.item = item;
                this.amount = amount;
                cat = null;
            }

            public Amount(ItemCategory cat, int amount) {
                this.cat = cat;
                this.amount = amount;
                this.item = null;
            }

            @Override
            public String toString() {
                if (cat != null)
                    return "C " + cat + "=" + getAmount();
                return item + "=" + getAmount();
            }

            public int getAmount() {
                return amount;
            }

            public ItemType getItem() {
                return item;
            }

            public void setItem(ItemType item) {
                this.item = item;
            }

            public void setCat(ItemCategory cat) {
                this.cat = cat;
            }

            public ItemCategory getCat() {
                return cat;
            }
        }

        public final Amount[] entries;

        public Items(Amount[] entries) {
            this.entries = entries;
        }

        public Items(Object... param) {
            if (param.length % 2 != 0)
                throw new IllegalArgumentException("Must be multiple of 2 parameter count");

            entries = new Amount[param.length / 2];

            for (int i = 0; i < param.length / 2; i++) {
                try {
                    Object o = param[i * 2];
                    if (o instanceof ItemType) entries[i] = new Amount(((ItemType) o), (Integer) param[i * 2 + 1]);
                    else entries[i] = new Amount(((ItemCategory) o), (Integer) param[i * 2 + 1]);
                } catch (RuntimeException e) {
                    throw new IllegalArgumentException("Illegal arguments given for Items", e);
                }
            }
        }

        public int getAmount(ItemType item) {
            for (Amount e : entries) {
                if (e != null && e.item == item)
                    return e.getAmount();
            }
            return 0;
        }

        public int getAmount(ItemCategory cat) {
            for (Amount e : entries) {
                if (e != null && e.cat == cat)
                    return e.getAmount();
            }
            return 0;
        }

        public void sort() {
            try {
                Arrays.sort(entries, new Comparator<Amount>() {
                    @Override
                    public int compare(Amount a, Amount b) {
                        ItemType ab = Item.base(a.item);
                        ItemType bb = Item.base(b.item);
                        if (ab == bb) return (a.item.value & 0xffff) - (b.item.value & 0xffff);
                        else return ab.value - bb.value;
                    }
                });
            } catch (NullPointerException e) {}
        }

        @Override
        public String toString() {
            return Arrays.toString(entries);
        }
    }

    public static ItemType get(short value) {
        return items[value & 0xffff];
    }

    public static ItemCategory category(byte value) {
        return categories[value & 0xff];
    }

    public static FluidType fluid(byte value) {
        return fluids[value & 0xff];
    }

    public static ItemType get(ItemType base, ItemType variant) {
        ItemType t = items[(base.value & 0xff) | (variant.value & 0xff00)];
        if (t == base || t == null)
            throw new IllegalArgumentException("Variant " + variant + " of " + base + " does not exist");
        return t;
    }

    public static boolean exists(ItemType base, ItemType variant) {
        return items[(base.value & 0xff) | (variant.value & 0xff00)] != null;
    }

    public static ItemType base(ItemType type) {
        return items[type.value & 0xff];
    }

    public static Element element(ItemType type) {
        return elements[(type.value >> 8) & 0xff];
    }

    public static Composite composite(ItemType type) {
        return composites[(type.value >> 8) & 0xff];
    }
}
