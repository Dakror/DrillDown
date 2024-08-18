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

import java.lang.reflect.Constructor;

import de.dakror.quarry.structure.Boiler;
import de.dakror.quarry.structure.Booster;
import de.dakror.quarry.structure.DistillationColumn;
import de.dakror.quarry.structure.Refinery;
import de.dakror.quarry.structure.ScienceLab;
import de.dakror.quarry.structure.ShaftDrill;
import de.dakror.quarry.structure.ShaftDrillHead;
import de.dakror.quarry.structure.logistics.BrickChannel;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.ConveyorBridge;
import de.dakror.quarry.structure.logistics.CopperTube;
import de.dakror.quarry.structure.logistics.Distributor;
import de.dakror.quarry.structure.logistics.ElectricConveyor;
import de.dakror.quarry.structure.logistics.ElectricConveyorCore;
import de.dakror.quarry.structure.logistics.Filter;
import de.dakror.quarry.structure.logistics.Hopper;
import de.dakror.quarry.structure.logistics.ItemLift;
import de.dakror.quarry.structure.logistics.ItemLiftBelow;
import de.dakror.quarry.structure.logistics.SteelTube;
import de.dakror.quarry.structure.logistics.TubeShaft;
import de.dakror.quarry.structure.logistics.TubeShaftBelow;
import de.dakror.quarry.structure.logistics.VacuumPump;
import de.dakror.quarry.structure.logistics.Valve;
import de.dakror.quarry.structure.power.AnchorPortal;
import de.dakror.quarry.structure.power.CableShaft;
import de.dakror.quarry.structure.power.CableShaftBelow;
import de.dakror.quarry.structure.power.Capacitor;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.structure.power.GasTurbine;
import de.dakror.quarry.structure.power.HighPowerShaft;
import de.dakror.quarry.structure.power.HighPowerShaftBelow;
import de.dakror.quarry.structure.power.PowerPole;
import de.dakror.quarry.structure.power.PowerPoleGhost;
import de.dakror.quarry.structure.power.SolarPanel;
import de.dakror.quarry.structure.power.SolarPanelOutlet;
import de.dakror.quarry.structure.power.SteamTurbine;
import de.dakror.quarry.structure.power.Substation;
import de.dakror.quarry.structure.power.SuperCapacitor;
import de.dakror.quarry.structure.power.WaterWheel;
import de.dakror.quarry.structure.producer.AirPurifier;
import de.dakror.quarry.structure.producer.ArcWelder;
import de.dakror.quarry.structure.producer.Assembler;
import de.dakror.quarry.structure.producer.BallMill;
import de.dakror.quarry.structure.producer.BarrelDrainer;
import de.dakror.quarry.structure.producer.BlastFurnace;
import de.dakror.quarry.structure.producer.Carpenter;
import de.dakror.quarry.structure.producer.Centrifuge;
import de.dakror.quarry.structure.producer.CharcoalMound;
import de.dakror.quarry.structure.producer.Compactor;
import de.dakror.quarry.structure.producer.Condenser;
import de.dakror.quarry.structure.producer.Crucible;
import de.dakror.quarry.structure.producer.DeviceFabricator;
import de.dakror.quarry.structure.producer.Excavator;
import de.dakror.quarry.structure.producer.FillingMachine;
import de.dakror.quarry.structure.producer.Furnace;
import de.dakror.quarry.structure.producer.GroundwaterPump;
import de.dakror.quarry.structure.producer.InductionFurnace;
import de.dakror.quarry.structure.producer.IngotMold;
import de.dakror.quarry.structure.producer.InjectionMolder;
import de.dakror.quarry.structure.producer.Kiln;
import de.dakror.quarry.structure.producer.Lumberjack;
import de.dakror.quarry.structure.producer.Mason;
import de.dakror.quarry.structure.producer.Mine;
import de.dakror.quarry.structure.producer.Mixer;
import de.dakror.quarry.structure.producer.OilWell;
import de.dakror.quarry.structure.producer.Polarizer;
import de.dakror.quarry.structure.producer.Polymerizer;
import de.dakror.quarry.structure.producer.RockCrusher;
import de.dakror.quarry.structure.producer.RollingMachine;
import de.dakror.quarry.structure.producer.SawMill;
import de.dakror.quarry.structure.producer.Stacker;
import de.dakror.quarry.structure.producer.TubeBender;
import de.dakror.quarry.structure.producer.WireDrawer;
import de.dakror.quarry.structure.storage.Barrel;
import de.dakror.quarry.structure.storage.DigitalStorage;
import de.dakror.quarry.structure.storage.Silo;
import de.dakror.quarry.structure.storage.Storage;
import de.dakror.quarry.structure.storage.Tank;
import de.dakror.quarry.structure.storage.Warehouse;

/**
 * @author Maximilian Stark | Dakror
 */
public enum StructureType {
    // tube
    Conveyor(1, Conveyor.class),
    ConveyorBridge(2, ConveyorBridge.class),
    BrickChannel(3, BrickChannel.class),
    CopperTube(4, CopperTube.class),
    SteelTube(5, SteelTube.class),
    ElectricConveyor(6, ElectricConveyor.class),
    ElectricConveyorCore(7, ElectricConveyorCore.class),

    // distributors
    Filter(10, Filter.class),
    Distributor(11, Distributor.class),
    Valve(12, Valve.class),
    Hopper(13, Hopper.class),
    VacuumPump(14, VacuumPump.class),

    // storage
    Storage(20, Storage.class),
    Warehouse(21, Warehouse.class),
    Tank(22, Tank.class),
    Barrel(23, Barrel.class),
    Silo(24, Silo.class),
    DigitalStorage(25, DigitalStorage.class),

    // producers
    Mine(40, Mine.class),
    Lumberjack(41, Lumberjack.class),
    GroundwaterPump(42, GroundwaterPump.class),
    ShaftDrill(43, ShaftDrill.class),
    AirPurifier(44, AirPurifier.class),
    OilWell(45, OilWell.class),
    Excavator(46, Excavator.class),

    // processors
    Carpenter(70, Carpenter.class),
    Furnace(71, Furnace.class),
    RockCrusher(72, RockCrusher.class),
    BallMill(73, BallMill.class),
    IngotMold(74, IngotMold.class),
    CharcoalMound(75, CharcoalMound.class),
    Mason(76, Mason.class),
    Boiler(77, Boiler.class),
    WireDrawer(78, WireDrawer.class),
    RollingMachine(79, RollingMachine.class),
    BlastFurnace(80, BlastFurnace.class),
    Condenser(81, Condenser.class),
    Mixer(82, Mixer.class),
    Kiln(83, Kiln.class),
    SawMill(84, SawMill.class),
    TubeBender(85, TubeBender.class),
    Polarizer(86, Polarizer.class),
    Compactor(87, Compactor.class),
    Assembler(88, Assembler.class),
    Refinery(89, Refinery.class),
    DistillationColumn(90, DistillationColumn.class),
    Polymerizer(91, Polymerizer.class),
    Centrifuge(92, Centrifuge.class),
    InjectionMolder(93, InjectionMolder.class),
    Crucible(94, Crucible.class),
    DeviceFabricator(95, DeviceFabricator.class),
    InductionFurnace(96, InductionFurnace.class),
    FillingMachine(97, FillingMachine.class),
    BarrelDrainer(98, BarrelDrainer.class),
    ArcWelder(99, ArcWelder.class),

    // power management
    Substation(100, Substation.class),
    CopperCable(101, CopperCable.class),
    Capacitor(102, Capacitor.class),
    PowerPole(103, PowerPole.class),
    PowerPoleGhost(104, PowerPoleGhost.class),
    AnchorPortal(105, AnchorPortal.class),
    SuperCapacitor(106, SuperCapacitor.class),

    // generators
    WaterWheel(110, WaterWheel.class),
    SteamTurbine(111, SteamTurbine.class),
    SolarPanelOutlet(112, SolarPanelOutlet.class),
    SolarPanel(113, SolarPanel.class),
    GasTurbine(114, GasTurbine.class),

    // Science
    ScienceLab(150, ScienceLab.class),

    Booster(175, Booster.class),

    // other
    ShaftDrillHead(200, ShaftDrillHead.class),

    // shafts
    ItemLift(201, ItemLift.class),
    ItemLiftBelow(202, ItemLiftBelow.class),
    TubeShaft(203, TubeShaft.class),
    TubeShaftBelow(204, TubeShaftBelow.class),
    CableShaft(205, CableShaft.class),
    CableShaftBelow(206, CableShaftBelow.class),
    HighPowerShaft(207, HighPowerShaft.class),
    HighPowerShaftBelow(208, HighPowerShaftBelow.class),

    // Processors cnt.
    Stacker(210, Stacker.class),

    ;

    public static final StructureType[] values = values();

    public final byte id;
    public final Constructor<?> constr;
    public final boolean versionSwitch;

    StructureType(int id, Class<?> clazz) {
        this.id = (byte) id;
        Constructor<?> con = null;
        boolean switch_ = false;
        try {
            con = clazz.getConstructor(int.class, int.class, int.class);
            clazz.getConstructor(int.class, int.class); // ensure second constructor exists too
            switch_ = true;
        } catch (Exception e) {
            try {
                con = clazz.getConstructor(int.class, int.class);
                switch_ = false;
            } catch (Exception e1) {
                throw new IllegalStateException("Structure type " + name() + " does not contain appropriate constructor (int, int) or (int, int, int)");
            }
        }
        this.constr = con;
        this.versionSwitch = switch_;
        if (Structure.types[id] != null)
            throw new IllegalStateException("Structure with ID " + id + " is already taken!");
        Structure.types[id] = this;
    }
}
