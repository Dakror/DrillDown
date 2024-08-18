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

package de.dakror.quarry.scenes;

import static com.badlogic.gdx.scenes.scene2d.actions.Actions.*;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Button.ButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TooltipManager;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.dakror.common.BiCallback;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.ConstantSupplyAmount;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.Boiler;
import de.dakror.quarry.structure.Booster;
import de.dakror.quarry.structure.DistillationColumn;
import de.dakror.quarry.structure.Refinery;
import de.dakror.quarry.structure.ScienceLab;
import de.dakror.quarry.structure.ShaftDrill;
import de.dakror.quarry.structure.ShaftDrillHead;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.GeneratorStructure;
import de.dakror.quarry.structure.base.GeneratorStructure.GeneratorRecipe;
import de.dakror.quarry.structure.base.IFlippable;
import de.dakror.quarry.structure.base.IRotatable;
import de.dakror.quarry.structure.base.ProducerStructure;
import de.dakror.quarry.structure.base.ProducerStructure.ProducerSchema;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.Schema.ButtonDef;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.base.component.CRecipeSlotStorage;
import de.dakror.quarry.structure.base.component.CTank;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.structure.base.component.IStorage;
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
import de.dakror.quarry.structure.logistics.SteelTube;
import de.dakror.quarry.structure.logistics.TubeShaft;
import de.dakror.quarry.structure.logistics.VacuumPump;
import de.dakror.quarry.structure.logistics.Valve;
import de.dakror.quarry.structure.power.AnchorPortal;
import de.dakror.quarry.structure.power.CableShaft;
import de.dakror.quarry.structure.power.Capacitor;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.structure.power.GasTurbine;
import de.dakror.quarry.structure.power.HighPowerShaft;
import de.dakror.quarry.structure.power.PowerPole;
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
import de.dakror.quarry.ui.Alert;
import de.dakror.quarry.ui.BoilerRecipe;
import de.dakror.quarry.ui.Confirm;
import de.dakror.quarry.ui.DistillationRecipe;
import de.dakror.quarry.ui.EndOfGame;
import de.dakror.quarry.ui.ItemSelection;
import de.dakror.quarry.ui.LayerSelection;
import de.dakror.quarry.ui.Menu;
import de.dakror.quarry.ui.Prompt;
import de.dakror.quarry.ui.RefineryRecipe;
import de.dakror.quarry.ui.SeedPrompt;
import de.dakror.quarry.ui.TileUI;
import de.dakror.quarry.ui.Toast;
import de.dakror.quarry.ui.Tutorial;
import de.dakror.quarry.ui.Ui;
import de.dakror.quarry.ui.Upgrade;
import de.dakror.quarry.util.Util;

/**
 * @author Maximilian Stark | Dakror
 */
public class GameUi implements Ui {
    public static final int ICONS_PER_COLUMN = 3;
    public static final int TOOLTIP_ICON_SIZE = 32;
    public static final int RESOURCES_ICON_SIZE = 24;

    static DecimalFormat intFormat;
    static DecimalFormat decFormat;
    public static DateFormat dateFormat;

    Stage stage;

    // Tooltip
    Label tooltipDescription;
    Table tooltipCosts, tooltipRecipes;
    Table tooltip;
    ImageButton tooltipClose, tooltipCollapse;
    public Structure<?> tooltipCurrentStructure;
    public Recipe currentRecipe;

    public boolean canAffordStructure;

    // Build menu
    VerticalGroup[] buildCategories;
    ButtonGroup<Button> buildTabButtons;
    ButtonGroup<Button>[] buildCategoryButtons;
    ScrollPane buildScrollPane;
    HorizontalGroup buildTabs;
    ButtonStyle menuButtonStyle;
    ImageButton buildClose, buildBack;
    Array<Actor> buildMenuStars = new Array<>();
    public EnumSet<ScienceType> buildMenuSciences = EnumSet.noneOf(ScienceType.class);

    // Science menu
    VerticalGroup scienceMenu;
    ScrollPane scienceScrollPane;
    EnumMap<ScienceType, Button> scienceButtons;
    ButtonGroup<Button> scienceGroup;
    ImageButton scienceClose;

    // Other Buttons
    ImageButton buildButton;
    ImageButton bulkButton;
    ImageButton bulkCableButton;
    ImageButton rotateButton;
    ImageButton flipButton;
    public ImageButton destroyButton;
    ImageButton cableRemoveButton;
    public ImageButton pauseButton;
    ImageButton copyButton;

    // Resources
    VerticalGroup resources;
    Table[] resourceRows;

    // Structure UI
    Table structureUI;
    Label structureUITitle;
    Table structureUIContent;
    ImageButton structureUIStorage;
    ImageButton structureUIRecipes;
    public Structure<?> currentClickedStructure;
    ImageButton[] structureUIButtons;
    BiCallback<Boolean, Structure<?>>[] structureUIButtonCallbacks;
    VerticalGroup structureUIInventory;
    ScrollPane structureUIInventoryScrollPane;
    EnumMap<ItemType, Integer> structureUIInventorySum;
    EnumMap<ItemType, Table> structureUIInventoryCells;

    Table copyTable, copyCosts;

    public Alert alert;
    public Toast toast;
    public Confirm confirm;
    public Upgrade upgrade;
    public Prompt prompt;
    public SeedPrompt seedPrompt;
    public ItemSelection itemSelection;
    LayerSelection layerSelection;
    TileUI tileUI;
    public Menu menu;

    BoilerRecipe boilerRecipe;
    RefineryRecipe refRecipe;
    public DistillationRecipe distRecipe;
    public EndOfGame endOfGame;
    public Tutorial tutorial;

    public Color c = Color.WHITE;

    static final Color off = new Color(0, 0, 0, 0.25f);

    static final Vector2 tmp = new Vector2();

    float h, s, v;

    public static final Drawable trButton = Quarry.Q.skin.newDrawable("button", 1, 1, 1, 0.25f);

    public GameUi(Viewport viewport) {
        Batch batch = null;
        //        try {
        //            batch = new ArrayTextureSpriteBatch(8191, 2048, 2048, 4, GL30.GL_LINEAR, GL30.GL_LINEAR);
        //        } catch (Exception e) {
        //            e.printStackTrace();
        batch = new SpriteBatch();
        //        }

        stage = new Stage(new FitViewport(Const.UI_W, Const.UI_H), batch) {
            @Override
            public void draw() {
                if (Game.UI_VISIBLE) {
                    try {
                        super.draw();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void addActor(Actor actor) {
                if (actor.getName() != null && !actor.getName().equals("menu"))
                    clampToInsets(actor);
                super.addActor(actor);
            }
        };
        stage.setActionsRequestRendering(true);
        stage.addCaptureListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                TooltipManager.getInstance().hideAll();
                if (buildScrollPane.isVisible() && stage.hit(x, y, true) == null) {
                    Game.G.resetActiveStructure();
                    hideTooltip();
                    hideBuildMenu();

                    return true;
                } else if (scienceScrollPane.isVisible() && stage.hit(x, y, true) == null) {
                    hideScienceUI();
                    return true;
                }

                return false;
            }
        });

        Game.G.input.addProcessor(0, stage);

        intFormat = (DecimalFormat) NumberFormat.getNumberInstance(Quarry.Q.i18n.getLocale());
        intFormat.setGroupingUsed(false);
        decFormat = (DecimalFormat) NumberFormat.getNumberInstance(Quarry.Q.i18n.getLocale());
        decFormat.setGroupingUsed(false);
        decFormat.setMinimumFractionDigits(3);
        decFormat.setMaximumFractionDigits(3);
        dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Quarry.Q.i18n.getLocale());

        Skin skin = Quarry.Q.skin;

        initButtons(skin);
        initResources(skin);
        initBuildMenu(skin);
        initScienceUI(skin);
        initStructureUI(skin);
        initTooltip(skin);
        initCopyTable(skin);

        menu = new Menu(stage, skin);
        boilerRecipe = new BoilerRecipe(skin);
        refRecipe = new RefineryRecipe(skin);
        distRecipe = new DistillationRecipe(skin);

        initWindows(skin);

        tutorial = new Tutorial(stage, skin);
        endOfGame = new EndOfGame(stage);
    }

    public void clampToInsets(Actor actor) {
        if (actor.getX() < Quarry.Q.safeInsets[0])
            actor.setX(Quarry.Q.safeInsets[0]);
        if (actor.getY() < Quarry.Q.safeInsets[3])
            actor.setY(Quarry.Q.safeInsets[3]);
        if (actor.getY() + actor.getHeight() >= Const.UI_H - Quarry.Q.safeInsets[1])
            actor.setY(Const.UI_H - Quarry.Q.safeInsets[1] - actor.getHeight());
        if (actor.getX() + actor.getWidth() >= Const.UI_W - Quarry.Q.safeInsets[2])
            actor.setX(Const.UI_W - Quarry.Q.safeInsets[2] - actor.getWidth());

    }

    protected void initButtons(Skin skin) {
        layerSelection = Util.lml("layer-selection");
        // immobilize clicking behind table
        layerSelection.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        Game.G.layerChangeNotifier.addListener(layerSelection);

        layerSelection.setName("layerSelection");
        stage.addActor(layerSelection);
        layerSelection.setX(layerSelection.getX() + 40);

        // parse but do not care about return
        Util.lml("buttons");
        rotateButton = Util.id("rotate");
        rotateButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                rotateActiveStructure();
            }
        });
        rotateButton.getImageCell().size(64);
        rotateButton.setPosition(Const.UI_W / 2 - rotateButton.getWidth() * 1.25f, 500);
        rotateButton.setName("rotateButton");
        stage.addActor(rotateButton);

        flipButton = Util.id("flip");
        flipButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                flipActiveStructure();
            }
        });
        flipButton.getImageCell().size(64);
        flipButton.setPosition(Const.UI_W / 2 + flipButton.getWidth() / 4, 500);
        flipButton.setName("flipButton");
        stage.addActor(flipButton);

        buildButton = roundButton(skin, skin.getDrawable("icon_build"), new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (((ImageButton) event.getListenerActor()).isChecked()) {
                    destroyButton.setChecked(false);
                    copyButton.setChecked(false);
                    showBuildMenu();
                    hideTooltip();
                    hideCopyTable();
                } else {
                    Game.G.resetActiveStructure();
                    hideTooltip();
                    hideBuildMenu();
                }
            }
        }, "button.build");
        buildButton.setPosition(Const.UI_W / 2 - Const.BUILD_RING_ITEM_SIZE - 10, 20);
        buildButton.setName("buildButton");
        stage.addActor(buildButton);

        cableRemoveButton = roundButton(skin, skin.getDrawable("icon_cableremove"), new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Game.G.resetActiveStructure();
                hideTooltip();
                if (((ImageButton) actor).isChecked()) {
                    bulkButton.setChecked(false);
                    bulkCableButton.setChecked(false);
                }
                Game.G.cableDestroyMode = ((ImageButton) actor).isChecked();
                Game.G.structureDestroyMode = !((ImageButton) actor).isChecked();
            }
        }, "button.cable");
        cableRemoveButton.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        cableRemoveButton.setVisible(false);
        cableRemoveButton.setName("cableRemoveButton");
        stage.addActor(cableRemoveButton);

        bulkCableButton = roundButton(skin, skin.getDrawable("icon_cableremovebulk"), new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Game.G.resetActiveStructure();
                hideTooltip();
                if (((ImageButton) actor).isChecked()) {
                    bulkButton.setChecked(false);
                    cableRemoveButton.setChecked(false);
                }
                Game.G.bulkCableMode = ((ImageButton) actor).isChecked();
                Game.G.structureDestroyMode = !((ImageButton) actor).isChecked();
            }
        }, "button.cablebulk");
        bulkCableButton.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        bulkCableButton.setVisible(false);
        bulkCableButton.setName("bulkCableButton");
        stage.addActor(bulkCableButton);

        destroyButton = roundButton(skin, skin.getDrawable("icon_destroy"), new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (destroyButton.isChecked()) {
                    if (Game.G.hasScience(ScienceType.Electricity) || Game.GOD_MODE) {
                        cableRemoveButton.addAction(sequence(
                                alpha(0),
                                visible(true),
                                moveTo(destroyButton.getX(), destroyButton.getY()),
                                parallel(
                                        fadeIn(0.1f),
                                        moveBy(Const.BUILD_RING_ITEM_SIZE + 20, 0, 0.15f, Interpolation.swingOut))));
                        bulkCableButton.addAction(sequence(
                                alpha(0),
                                visible(true),
                                moveTo(destroyButton.getX(), destroyButton.getY()),
                                parallel(
                                        fadeIn(0.1f),
                                        moveBy(Const.BUILD_RING_ITEM_SIZE + 20, Const.BUILD_RING_ITEM_SIZE + 20, 0.15f,
                                                Interpolation.swingOut))));
                    }
                    bulkButton.addAction(sequence(alpha(0), visible(true),
                            moveTo(destroyButton.getX(), destroyButton.getY()), parallel(fadeIn(0.1f),
                                    moveBy(0, Const.BUILD_RING_ITEM_SIZE + 20, 0.15f, Interpolation.swingOut))));
                    hideStructureUI();
                    hideTooltip();
                    hideCopyTable();
                    tileUI.hide();
                    copyButton.setChecked(false);
                    hideBuildMenu();
                } else {
                    if (Game.G.hasScience(ScienceType.Electricity) || Game.GOD_MODE) {
                        cableRemoveButton.addAction(sequence(parallel(fadeOut(0.1f),
                                moveTo(destroyButton.getX(), destroyButton.getY(), 0.15f, Interpolation.swingOut)),
                                visible(false)));
                        cableRemoveButton.setChecked(false);
                        bulkCableButton.addAction(sequence(parallel(fadeOut(0.1f),
                                moveTo(destroyButton.getX(), destroyButton.getY(), 0.15f, Interpolation.swingOut)),
                                visible(false)));
                        bulkCableButton.setChecked(false);
                    }
                    bulkButton.addAction(sequence(
                            parallel(fadeOut(0.1f),
                                    moveTo(destroyButton.getX(), destroyButton.getY(), 0.15f, Interpolation.swingOut)),
                            visible(false)));
                    bulkButton.setChecked(false);
                }
                Game.G.resetActiveStructure();
                Game.G.structureDestroyMode = destroyButton.isChecked();
            }
        }, "button.destroy");
        destroyButton.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        destroyButton.setPosition(Const.UI_W / 2 + 10, 20);

        bulkButton = roundButton(skin, skin.getDrawable("icon_destroybulk"), new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Game.G.resetActiveStructure();
                hideTooltip();
                hideCopyTable();
                if (((ImageButton) actor).isChecked()) {
                    bulkCableButton.setChecked(false);
                    cableRemoveButton.setChecked(false);
                }
                Game.G.bulkDestroyMode = ((ImageButton) actor).isChecked();
                Game.G.structureDestroyMode = !((ImageButton) actor).isChecked();
            }
        }, "button.destroybulk");
        bulkButton.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        bulkButton.setVisible(false);
        bulkButton.setName("bulkButton");
        stage.addActor(bulkButton);

        destroyButton.setName("destroyButton");
        stage.addActor(destroyButton);

        copyButton = roundButton(skin, skin.getDrawable("icon_copy"), new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (copyButton.isChecked()) {
                    destroyButton.setChecked(false);
                    buildButton.setChecked(false);
                    hideBuildMenu();
                    hideTooltip();

                    showCopyTable();
                    Game.G.resetActiveStructure();
                } else {
                    Game.G.pasteMode = false;
                    Arrays.fill(Game.G.copyRegion, 0);
                    Game.G.copyCost.clear();
                    hideCopyTable();
                }
                Game.G.copyMode = copyButton.isChecked();
            }
        }, "button.copy");
        copyButton.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        copyButton.setPosition(Const.UI_W / 2 - 2 * Const.BUILD_RING_ITEM_SIZE - 30, 20);
        copyButton.setName("copyButton");
        copyButton.setVisible(false);
        stage.addActor(copyButton);

        pauseButton = roundButton(skin, skin.getDrawable("symb_pause"), new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.resetSpeed();
                Game.G.setPaused(pauseButton.isChecked());
            }
        }, "button.pause");
        ImageButtonStyle style = pauseButton.getStyle();
        style.imageChecked = skin.getDrawable("symb_play");
        pauseButton.setStyle(style);
        pauseButton.getImageCell().size(36);
        pauseButton.setPosition(24 + Const.BUILD_RING_ITEM_SIZE, Const.UI_H - (Const.BUILD_RING_ITEM_SIZE + 12));
        pauseButton.setName("pauseButton");
        stage.addActor(pauseButton);
    }

    public void rotateActiveStructure() {
        if (tooltipCurrentStructure != null) {
            if (tooltipCurrentStructure instanceof IRotatable) {
                if (!Game.G.activeStructureTrail.isEmpty() && Game.G.endB.x > -1) {
                    int tx = (int) Game.G.endA.x, ty = (int) Game.G.endA.y;
                    Game.G.endA.set(Game.G.endB);
                    Game.G.endB.set(tx, ty);

                    Structure<?> last = null;
                    Collections.reverse(Game.G.activeStructurePath);
                    for (int i : Game.G.activeStructurePath) {
                        Structure<?> s = Game.G.activeStructureTrail.get(i);
                        if (last != null) {
                            Direction rotation = null;
                            if (s.x == last.x) {
                                rotation = s.y > last.y ? Direction.South : Direction.North;
                            } else {
                                rotation = s.x > last.x ? Direction.West : Direction.East;
                            }

                            ((IRotatable) s).setRotation(rotation);
                        } else {
                            ((IRotatable) s).setRotation(((IRotatable) s).getDirection().inv());
                        }
                        last = s;
                    }

                    // apply updates after all rotations done
                    for (int i : Game.G.activeStructurePath) {
                        Structure<?> s = Game.G.activeStructureTrail.get(i);
                        s.update(0, 1, Game.G.layer.dirtyBounds);
                    }

                    Game.G.camControl.updateTrail();
                } else {
                    ((IRotatable) tooltipCurrentStructure).rotate();
                }
            } else if (!tooltipCurrentStructure.getSchema().has(Flags.NotRotatable)) {
                tooltipCurrentStructure.setUpDirection(tooltipCurrentStructure.getUpDirection().next());
            }
            Game.G.camControl.updateActiveElementPlaceable();
        } else if (currentClickedStructure instanceof IRotatable) {
            ((IRotatable) currentClickedStructure).rotate();
        }
    }

    public void flipActiveStructure() {
        if (tooltipCurrentStructure instanceof IFlippable) {
            ((IFlippable) tooltipCurrentStructure).flip();
            Game.G.camControl.updateActiveElementPlaceable();
            for (Structure<?> s : Game.G.activeStructureTrail.values())
                ((IFlippable) s).flip();
        } else if (currentClickedStructure instanceof IFlippable) {
            ((IFlippable) currentClickedStructure).flip();
        }
    }

    protected void initTooltip(Skin skin) {
        // Setup libgdx tooltips
        TooltipManager man = TooltipManager.getInstance();
        man.initialTime = 0;
        man.animations = false;
        man.resetTime = 0;
        man.instant();

        tooltip = Util.lml("tooltip");
        tooltip.setPosition((Const.UI_W - tooltip.getWidth()) / 2, 130);

        // immobilize clicking behind table
        tooltip.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        });
        tooltipCosts = Util.id("costs");
        tooltipRecipes = Util.id("recipes");
        tooltipDescription = Util.id("desc_label");

        Table descTable = Util.id("desc");
        descTable.setBackground(getDrawable(skin, "panel_metal", 0, 0, 0, 0));
        tooltip.setName("tooltip");
        stage.addActor(tooltip);

        tooltipClose = createXButton();
        tooltipClose.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.resetActiveStructure();
                hideTooltip();
            }
        });
        tooltipClose.setPosition(tooltip.getX() + tooltip.getWidth() - 40, tooltip.getY() + tooltip.getHeight() - 40);
        tooltipClose.setName("tooltipClose");
        stage.addActor(tooltipClose);

        tooltipCollapse = new ImageButton(skin, "collapse");
        tooltipCollapse.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                tooltip.clearActions();
                tooltipClose.clearActions();
                tooltipCollapse.clearActions();

                float delta = tooltip.getHeight() - 60;

                if (tooltipCollapse.isChecked()) {
                    tooltip.addAction(sequence(moveBy(0, -delta, 0.15f, Interpolation.circle)));
                    tooltipClose.addAction(sequence(moveBy(0, -delta, 0.15f, Interpolation.circle)));
                    tooltipCollapse.addAction(sequence(moveBy(0, -delta, 0.15f, Interpolation.circle)));

                    flipButton.addAction(sequence(moveBy(0, -delta, 0.15f, Interpolation.circle)));
                    rotateButton.addAction(sequence(moveBy(0, -delta, 0.15f, Interpolation.circle)));
                } else {
                    tooltip.addAction(sequence(moveBy(0, delta, 0.15f, Interpolation.circle)));
                    tooltipClose.addAction(sequence(moveBy(0, delta, 0.15f, Interpolation.circle)));
                    tooltipCollapse.addAction(sequence(moveBy(0, delta, 0.15f, Interpolation.circle)));

                    flipButton.addAction(sequence(moveBy(0, delta, 0.15f, Interpolation.circle)));
                    rotateButton.addAction(sequence(moveBy(0, delta, 0.15f, Interpolation.circle)));
                }
            }
        });
        tooltipCollapse.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        tooltipCollapse.setPosition(tooltip.getX() + 36, tooltip.getY() + tooltip.getHeight() - 26);
        tooltipCollapse.setName("tooltipCollapse");
        stage.addActor(tooltipCollapse);
    }

    @SuppressWarnings("unchecked")
    protected void initBuildMenu(Skin skin) {
        menuButtonStyle = new ButtonStyle(null, trButton, trButton);

        buildTabs = new HorizontalGroup();
        buildTabs.wrap(true).top().left().rowAlign(Align.topLeft);
        buildTabs.setSize(600, Const.UI_H - 575 - Quarry.Q.safeInsets[1] - Quarry.Q.safeInsets[3]);

        buildTabButtons = new ButtonGroup<>();

        int tube = buildMenuTab(Conveyor.classSchema.icon, Quarry.Q.i18n.get("tab.conveyors"));
        int rout = buildMenuTab(Filter.classSchema.icon, Quarry.Q.i18n.get("tab.routers"));
        int stor = buildMenuTab(Storage.classSchema.icon, Quarry.Q.i18n.get("tab.storage"));
        int mine = buildMenuTab(Mine.classSchema.icon, Quarry.Q.i18n.get("tab.raw"));
        int furn = buildMenuTab(Furnace.classSchema.icon, Quarry.Q.i18n.get("tab.ore"));
        int iron = buildMenuTab(skin.getDrawable("icon_fe_ingot"), Quarry.Q.i18n.get("tab.metal"));
        int misc = buildMenuTab(skin.getDrawable("icon_misc"), Quarry.Q.i18n.get("tab.misc"));
        int wate = buildMenuTab(skin.getDrawable("icon_liquids"), Quarry.Q.i18n.get("tab.water"));
        int powe = buildMenuTab(skin.getDrawable("icon_power"), Quarry.Q.i18n.get("tab.power"));
        int high = buildMenuTab(Polymerizer.classSchema.icon, Quarry.Q.i18n.get("tab.hightech"));

        buildCategories = new VerticalGroup[buildTabs.getChildren().size];
        buildCategoryButtons = new ButtonGroup[buildTabs.getChildren().size];
        for (int i = 0; i < buildCategories.length; i++) {
            VerticalGroup t = new VerticalGroup();
            t.expand().fill();
            buildCategories[i] = t;
            ButtonGroup<Button> bg = new ButtonGroup<>();
            bg.setMinCheckCount(0);
            buildCategoryButtons[i] = bg;
        }

        buildMenuItem(tube, new Conveyor(-1, 0));
        buildMenuItem(tube, new ConveyorBridge(-1, 0));
        buildMenuItem(tube, new BrickChannel(-1, 0));
        buildMenuItem(tube, new CopperTube(-1, 0));
        buildMenuItem(tube, new SteelTube(-1, 0));
        buildMenuItem(tube, new CopperCable(-1, 0));
        buildMenuItem(tube, new ElectricConveyor(-1, 0));
        buildMenuItem(tube, new ElectricConveyorCore(-1, 0));

        buildMenuItem(rout, new Hopper(-1, 0));
        buildMenuItem(rout, new Filter(-1, 0));
        buildMenuItem(rout, new Distributor(-1, 0));
        buildMenuItem(rout, new Valve(-1, 0));
        buildMenuItem(rout, new VacuumPump(-1, 0));
        buildMenuItem(rout, new ItemLift(-1, 0));
        buildMenuItem(rout, new TubeShaft(-1, 0));
        buildMenuItem(rout, new CableShaft(-1, 0));
        buildMenuItem(rout, new HighPowerShaft(-1, 0));

        buildMenuItem(mine, new Mine(-1, 0));
        buildMenuItem(mine, new Lumberjack(-1, 0));
        buildMenuItem(mine, new Carpenter(-1, 0));
        buildMenuItem(mine, new Mason(-1, 0));
        buildMenuItem(mine, new SawMill(-1, 0));
        buildMenuItem(mine, new Excavator(-1, 0));

        buildMenuItem(furn, new Kiln(-1, 0));
        buildMenuItem(furn, new Furnace(-1, 0));
        buildMenuItem(furn, new IngotMold(-1, 0));
        buildMenuItem(furn, new RockCrusher(-1, 0));
        buildMenuItem(furn, new CharcoalMound(-1, 0));
        buildMenuItem(furn, new BallMill(-1, 0));
        buildMenuItem(furn, new BlastFurnace(-1, 0));
        buildMenuItem(furn, new Crucible(-1, 0));

        buildMenuItem(iron, new RollingMachine(-1, 0));
        buildMenuItem(iron, new TubeBender(-1, 0));
        buildMenuItem(iron, new WireDrawer(-1, 0));
        buildMenuItem(iron, new Polarizer(-1, 0));
        buildMenuItem(iron, new ArcWelder(-1, 0));

        buildMenuItem(stor, new Storage(-1, 0));
        buildMenuItem(stor, new Barrel(-1, 0));
        buildMenuItem(stor, new Warehouse(-1, 0));
        buildMenuItem(stor, new DigitalStorage(-1, 0));
        buildMenuItem(stor, new Tank(-1, 0));
        buildMenuItem(stor, new Silo(-1, 0));
        buildMenuItem(stor, new Capacitor(-1, 0));
        buildMenuItem(stor, new SuperCapacitor(-1, 0));

        buildMenuItem(misc, new ScienceLab(-1, 0));
        buildMenuItem(misc, new Mixer(-1, 0));
        buildMenuItem(misc, new ShaftDrill(-1, 0));
        buildMenuItem(misc, new AirPurifier(-1, 0));
        buildMenuItem(misc, new Booster(-1, 0));
        buildMenuItem(misc, new Stacker(-1, 0));
        buildMenuItem(misc, new FillingMachine(-1, 0));
        buildMenuItem(misc, new BarrelDrainer(-1, 0));

        buildMenuItem(high, new Assembler(-1, 0));
        buildMenuItem(high, new Compactor(-1, 0));
        buildMenuItem(high, new InductionFurnace(-1, 0));
        buildMenuItem(high, new Polymerizer(-1, 0));
        buildMenuItem(high, new Centrifuge(-1, 0));
        buildMenuItem(high, new InjectionMolder(-1, 0));
        buildMenuItem(high, new DeviceFabricator(-1, 0));

        buildMenuItem(wate, new GroundwaterPump(-1, 0));
        buildMenuItem(wate, new Boiler(-1, 0));
        buildMenuItem(wate, new Condenser(-1, 0));
        buildMenuItem(wate, new OilWell(-1, 0));
        buildMenuItem(wate, new Refinery(-1, 0));
        buildMenuItem(wate, new DistillationColumn(-1, 0));

        buildMenuItem(powe, new Substation(-1, 0));
        buildMenuItem(powe, new PowerPole(-1, 0));
        buildMenuItem(powe, new AnchorPortal(-1, 0));
        buildMenuItem(powe, new WaterWheel(-1, 0));
        buildMenuItem(powe, new SteamTurbine(-1, 0));
        buildMenuItem(powe, new SolarPanelOutlet(-1, 0));
        buildMenuItem(powe, new SolarPanel(-1, 0));
        buildMenuItem(powe, new GasTurbine(-1, 0));

        buildScrollPane = new ScrollPane(buildTabs, skin, "container");
        ScrollPaneStyle sps = new ScrollPaneStyle(buildScrollPane.getStyle());
        sps.background = getDrawable(skin, "panel_metalDark", 25, 25, 25, 25);
        buildScrollPane.setStyle(sps);
        buildScrollPane.setScrollingDisabled(true, true);
        buildScrollPane.setSize(600, Const.UI_H - 575 - Quarry.Q.safeInsets[1] - Quarry.Q.safeInsets[3]);
        buildScrollPane.setPosition((Const.UI_W - buildScrollPane.getWidth()) / 2, 434);

        buildScrollPane.setVisible(false);

        buildClose = createXButton();
        buildClose.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                Game.G.resetActiveStructure();
                hideTooltip();
                hideBuildMenu();
            }
        });
        buildClose.setPosition(buildScrollPane.getX() + buildScrollPane.getWidth() - 40,
                buildScrollPane.getY() + buildScrollPane.getHeight() - 40);

        buildBack = Util.lml("back-button");
        buildBack.getImageCell().size(40);
        buildBack.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                buildTabButtons.uncheckAll();
                if (buildScrollPane.getActor() instanceof VerticalGroup) {
                    for (Actor c : ((WidgetGroup) buildScrollPane.getActor()).getChildren())
                        if (c instanceof Button)
                            ((Button) c).setChecked(false);
                }

                buildScrollPane.setActor(buildTabs);
                buildScrollPane.setScrollingDisabled(true, true);
                buildBack.addAction(sequence(fadeOut(0.1f), visible(false)));
            }
        });
        buildBack.setPosition(buildScrollPane.getX() - buildBack.getWidth() + 20,
                buildScrollPane.getY() + buildScrollPane.getHeight() - buildBack.getHeight() - 20);
        buildBack.setVisible(false);

        buildBack.setName("buildBack");
        stage.addActor(buildBack);
        buildScrollPane.setName("buildScrollPane");
        stage.addActor(buildScrollPane);
        buildClose.setName("buildClose");
        stage.addActor(buildClose);
    }

    protected void initScienceUI(Skin skin) {
        scienceGroup = new ButtonGroup<>();
        scienceGroup.setMinCheckCount(0);
        scienceButtons = new EnumMap<>(ScienceType.class);

        scienceMenu = new VerticalGroup();
        scienceMenu.grow();

        scienceScrollPane = new ScrollPane(scienceMenu, skin);
        ScrollPaneStyle sps = new ScrollPaneStyle(scienceScrollPane.getStyle());
        sps.background = getDrawable(skin, "panel_metalDark", 30, 30, 30, 30);
        scienceScrollPane.setStyle(sps);
        scienceScrollPane.setScrollingDisabled(true, false);
        scienceScrollPane.setSize(600, Const.UI_H - 575);
        scienceScrollPane.setPosition((Const.UI_W - scienceScrollPane.getWidth()) / 2, 434);
        scienceScrollPane.setVisible(false);
        scienceScrollPane.setName("scienceScrollPane");
        stage.addActor(scienceScrollPane);

        scienceClose = createXButton();
        scienceClose.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                hideScienceUI();
            }
        });
        scienceClose.setPosition(scienceScrollPane.getX() + scienceScrollPane.getWidth() - 40,
                scienceScrollPane.getY() + scienceScrollPane.getHeight() - 40);
        scienceClose.setName("scienceClose");
        stage.addActor(scienceClose);
    }

    protected void initResources(Skin skin) {
        resources = new VerticalGroup();
        resources.left().columnLeft().space(2);
        ItemType[] t = ItemType.values;
        resourceRows = new Table[t.length];

        for (int i = 0; i < t.length; i++) {
            resourceRows[i] = createResourceTable(RESOURCES_ICON_SIZE, skin, t[i], "");
            resourceRows[i].setTouchable(Touchable.enabled);
        }

        ScrollPane scroll = new ScrollPane(resources);
        scroll.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                stage.setScrollFocus(event.getTarget());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                stage.setScrollFocus(toActor);
            }
        });
        scroll.setScrollingDisabled(true, false);
        scroll.setOverscroll(false, false);
        Table table = new Table();
        table.pad(60, 40, 40, 60);
        table.add(scroll).maxHeight(400);
        table.setBackground(skin.getDrawable("panel_boltsDetailSquare"));
        table.setName("resources");
        stage.addActor(table);

        updateResources(true);
    }

    protected void initWindows(Skin skin) {
        toast = Util.lml("toast");
        toast.setName("toast");
        stage.addActor(toast);

        seedPrompt = Util.lml("seed-prompt");
        prompt = Util.lml("prompt");
        alert = Util.lml("alert");
        confirm = Util.lml("confirm");
        if (!Quarry.Q.fullVersion) {
            upgrade = Util.lml("upgrade");
        }
    }

    @SuppressWarnings("unchecked")
    protected void initStructureUI(Skin skin) {
        itemSelection = new ItemSelection(skin, null);
        itemSelection.setName("itemSelection");
        stage.addActor(itemSelection);

        tileUI = Util.lml("tileui");
        tileUI.setName("tileUI");
        stage.addActor(tileUI);

        structureUI = new Table();
        structureUI.setTouchable(Touchable.enabled);
        structureUI.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // immobilize clicking behind table
                return true;
            }
        });
        structureUI.setBackground(skin.getDrawable("panel_boltsBlue"));
        structureUI.setSize(450, 300);
        structureUI.pad(30).top();

        structureUI.setPosition((Const.UI_W - structureUI.getWidth()) / 2, 130);

        structureUITitle = new Label("", skin);
        structureUITitle.setFontScale(1.25f);

        Table titleTable = new Table();
        titleTable.setBackground(new TiledDrawable(skin.getRegion("patternStripesShadow_large")));
        titleTable.add(structureUITitle).height(48).expandX().left().padLeft(10);
        structureUI.add(titleTable).growX().height(48);

        structureUI.row().padTop(6).grow();
        structureUIContent = new Table();
        structureUI.add(structureUIContent);

        structureUIButtons = new ImageButton[3];
        structureUIButtonCallbacks = new BiCallback[3];
        for (int i = 0; i < 3; i++) {
            final ImageButton ib = new ImageButton(skin, "round");
            ib.setSize(80, 80);
            ib.getImageCell().maxSize(50);
            ib.setPosition(structureUI.getX() + structureUI.getWidth() + 8, 130 + (2 - i) * 90 + 20);
            ib.setVisible(false);
            final int j = i;
            ib.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    boolean c = ib.isChecked();
                    if (c) {
                        // unclick other buttons
                        for (int i = 0; i < 3; i++) {
                            if (i != j && structureUIButtons[i] != null
                                    && structureUIButtons[i].getUserObject() == ButtonType.TempRadio)
                                structureUIButtons[i].setChecked(false);
                        }
                    }
                    if (structureUIButtonCallbacks[j] != null)
                        structureUIButtonCallbacks[j].call(c, currentClickedStructure);
                }
            });
            ib.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Quarry.Q.sound.play(Quarry.Q.clickSfx);
                }
            });
            structureUIButtons[i] = ib;
            ib.setName("structure_button_" + i);
            stage.addActor(ib);
        }

        structureUIStorage = new ImageButton(skin, "round");
        ImageButtonStyle ibs = new ImageButtonStyle(structureUIStorage.getStyle());
        ibs.imageUp = Quarry.Q.skin.getDrawable("icon_cinventory");
        structureUIStorage.setStyle(ibs);
        structureUIStorage.setSize(80, 80);
        structureUIStorage.getImageCell().size(40);
        structureUIStorage.setPosition(structureUI.getX() - 88, 330);
        structureUIStorage.setVisible(false);
        structureUIStorage.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (structureUIStorage.isChecked()) {
                    structureUIRecipes.setChecked(false);
                    for (int i = 0; i < 3; i++) {
                        if (structureUIButtons[i] != null
                                && structureUIButtons[i].getUserObject() == ButtonType.TempRadio)
                            structureUIButtons[i].setChecked(false);
                    }

                    structureUIContent.clear();
                    structureUITitle.setText(Quarry.Q.i18n.get("ui.inventory"));
                    structureUIContent.add(structureUIInventoryScrollPane).growX().top().left().expand();
                    updateStructureUIInventory();
                } else {
                    structureUIContent.clear();
                    structureUITitle.setText(currentClickedStructure.getSchema().name);
                    currentClickedStructure.onClick(structureUIContent);
                }
            }
        });
        structureUIStorage.setName("structureUIStorage");
        stage.addActor(structureUIStorage);

        structureUIRecipes = new ImageButton(skin, "round");
        ibs = new ImageButtonStyle(structureUIRecipes.getStyle());
        ibs.imageUp = Quarry.Q.skin.getDrawable("icon_crusher");
        structureUIRecipes.setStyle(ibs);
        structureUIRecipes.setSize(80, 80);
        structureUIRecipes.getImageCell().size(40);
        structureUIRecipes.setPosition(structureUI.getX() - 88, 240);
        structureUIRecipes.setVisible(false);
        structureUIRecipes.addListener(new ClickListener() {

            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (structureUIRecipes.isChecked()) {
                    structureUIStorage.setChecked(false);
                    for (int i = 0; i < 3; i++) {
                        if (structureUIButtons[i] != null
                                && structureUIButtons[i].getUserObject() == ButtonType.TempRadio)
                            structureUIButtons[i].setChecked(false);
                    }

                    structureUIContent.clear();
                    structureUITitle.setText(Quarry.Q.i18n.get("ui.recipes"));
                    buildRecipes(currentClickedStructure, structureUIContent, tooltipDescription, false);
                } else {
                    structureUIContent.clear();
                    structureUITitle.setText(currentClickedStructure.getSchema().name);
                    currentClickedStructure.onClick(structureUIContent);
                }
            }
        });
        structureUIRecipes.setName("structureUIRecipes");
        stage.addActor(structureUIRecipes);

        structureUI.setVisible(false);
        structureUI.setName("structureUI");
        stage.addActor(structureUI);

        // inventory
        structureUIInventory = new VerticalGroup();
        structureUIInventory.columnLeft().grow();

        structureUIInventoryScrollPane = new ScrollPane(structureUIInventory);
        structureUIInventoryScrollPane.setScrollingDisabled(true, false);

        structureUIInventoryCells = new EnumMap<>(ItemType.class);
        structureUIInventorySum = new EnumMap<>(ItemType.class);
    }

    protected void initCopyTable(Skin skin) {
        copyTable = Util.lml("copy");
        copyTable.setPosition((Const.UI_W - tooltip.getWidth()) / 2, 130);

        copyTable.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // immobilize clicking behind table
                return true;
            }
        });
        copyCosts = Util.id("costs");
        copyTable.setName("copyTable");
        stage.addActor(copyTable);
    }

    protected ImageButton createXButton() {
        ImageButton ib = Util.lml("x-button");
        ib.getImageCell().size(24);
        return ib;
    }

    protected void showBuildMenu() {
        // Fixes not only the visual bug, but a plethora of gameplay bugs involving buggy state of structure placement
        buildTabButtons.uncheckAll();
        if (buildScrollPane.getActor() instanceof VerticalGroup) {
            for (Actor c : ((WidgetGroup) buildScrollPane.getActor()).getChildren())
                if (c instanceof Button)
                    ((Button) c).setChecked(false);
        }

        for (Actor c : buildMenuStars)
            c.setVisible(true);
        buildScrollPane.setActor(buildTabs);
        buildScrollPane.setScrollingDisabled(true, true);

        Game.G.resetActiveStructure();
        buildClose.addAction(sequence(alpha(0), visible(true), fadeIn(0.1f)));
        buildScrollPane.addAction(sequence(alpha(0), visible(true), fadeIn(0.1f)));
        updateBuildMenuResources();
        updateBuildMenuSciences();
    }

    public void hideBuildMenu() {
        hideBuildMenu(false);
    }

    public void hideBuildMenu(boolean instantly) {
        buildButton.setChecked(false);
        if (instantly) {
            buildClose.setVisible(false);
            buildScrollPane.setVisible(false);
            buildBack.setVisible(false);
        } else {
            buildClose.addAction(sequence(fadeOut(0.1f), visible(false)));
            buildScrollPane.addAction(sequence(fadeOut(0.1f), visible(false)));
            buildBack.addAction(sequence(fadeOut(0.1f), visible(false)));
        }
        for (Actor c : buildMenuStars)
            c.setVisible(false);
        stage.unfocusAll();
    }

    protected int buildMenuTab(Drawable icon, String title) {
        Table t = new Table();
        t.add(new Image(icon)).size(80);

        t.row();

        Label l = new Label(title, Quarry.Q.skin, "small-font", Color.WHITE);
        l.setAlignment(Align.center);
        t.add(l).growX().top().center();

        final int index = buildTabs.getChildren().size;

        final Button c = new Button(menuButtonStyle) {
            @Override
            public float getPrefHeight() {
                return isVisible() ? super.getPrefHeight() : 0;
            }

            @Override
            public float getPrefWidth() {
                return isVisible() ? super.getPrefWidth() : 0;
            }
        };
        c.add(t).size((buildTabs.getWidth() - 50) / 2, (buildTabs.getHeight() - 40) / 5).top();
        buildTabs.addActor(c);

        buildTabButtons.add(c);

        c.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (c.isChecked())
                    buildBack.addAction(sequence(alpha(0), visible(true), fadeIn(0.1f)));
                buildScrollPane.setActor(buildCategories[index]);
                buildScrollPane.setScrollingDisabled(true, false);
                updateBuildMenuResources();
                for (Actor c : buildCategories[index].getChildren()) {
                    if (c instanceof Button)
                        ((Button) c).setChecked(false);
                }
                hideTooltip();
            }
        });
        c.addCaptureListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });

        return buildTabs.getChildren().size - 1;
    }

    protected void buildMenuItem(int tab, Structure<?> structure) {
        final Button t = new Button(menuButtonStyle) {
            @Override
            public float getPrefHeight() {
                return isVisible() ? super.getPrefHeight() : 0;
            }
        };
        t.padTop(10);
        final Label l = new Label(structure.getSchema().name, Quarry.Q.skin);
        t.add(new Image(structure.getSchema().icon)).size(80).spaceRight(10);
        t.add(l).grow().top().left().pad(10);
        sep(t).spaceTop(10).colspan(2).fillX();

        t.setUserObject(structure);

        t.getListeners().insert(0, new ClickListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                super.touchUp(event, x, y, pointer, button);
                if (t.isChecked() && (t.getChildren().get(1).getColor().equals(Color.WHITE) || Game.GOD_MODE)) {
                    tooltipClose.setVisible(true);
                    tooltipCollapse.setVisible(true);
                    hideBuildMenu();
                    Game.G.structureDestroyMode = false;
                    Game.G.activeStructure = (Structure<?>) t.getUserObject();
                    //                    Quarry.Q.activeStructure.layer = Quarry.Q.layer;
                    Game.G.activeStructure.x = -1;
                    if (!(Game.G.activeStructure instanceof ElectricConveyorCore))
                        Game.G.activeStructure.setUpDirection(Direction.North);

                    showOrHideRotateButton();
                    showOrHideFlipButton();
                }
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
            }
        });
        t.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // if the capture listener hid the build menu already, we dont want to remove the tooltip.
                // TODO: maybe find out how to cancel the event that ends up here and we do not want

                if (!buildScrollPane.isVisible() || buildScrollPane.getActions().size > 0)
                    return;
                if (t.isChecked()) {
                    showTooltip((Structure<?>) t.getUserObject());
                    tooltipClose.setVisible(false);
                    tooltipCollapse.setChecked(false);
                    tooltipCollapse.setVisible(false);
                } else {
                    hideTooltip();
                }
            }
        });
        buildCategories[tab].addActor(t);
        buildCategoryButtons[tab].add(t);
    }

    protected Button scienceMenuItem(ScienceType science) {
        Button b = scienceButtons.get(science);
        if (b != null)
            return b;

        final Button t = new Button(menuButtonStyle) {
            @Override
            public float getPrefHeight() {
                return isVisible() ? super.getPrefHeight() : 0;
            }
        };
        t.padTop(10);
        t.add(new Image(science.icon)).size(80).spaceRight(10).top();

        Table text = new Table();
        text.defaults().growX();
        text.add(new Label(science.title, Quarry.Q.skin));
        text.row();
        Label l = new Label(science.description, Quarry.Q.skin, "small-font", Color.LIGHT_GRAY);
        l.setWrap(true);
        text.add(l).fill();

        t.add(text).grow().top().left().space(10);

        t.row();

        HorizontalGroup hg = new HorizontalGroup();
        hg.space(10).left();
        for (Amount a : science.costs.entries) {
            hg.addActor(createResourceTable(TOOLTIP_ICON_SIZE, Quarry.Q.skin, a.getItem(),
                    formatResourceAmount(a.getAmount())));
        }

        t.add();
        t.add(hg).growX().left();

        sep(t).spaceTop(10).colspan(2).fillX();

        t.setUserObject(science);
        t.getListeners().insert(0, new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                if (currentClickedStructure instanceof ScienceLab && t.isChecked()
                        && (((Table) t.getChildren().get(1)).getChildren().get(0).getColor().equals(Color.WHITE)
                                || Game.GOD_MODE)) {
                    ScienceType s = (ScienceType) t.getUserObject();
                    if (Game.DRAW_DEBUG) {
                        Game.G.addScience(s);
                    } else {
                        ((ScienceLab) currentClickedStructure).setActiveScience(s);
                    }
                    hideScienceUI();
                }
            }
        });

        scienceButtons.put(science, t);
        scienceGroup.add(t);
        return t;
    }

    protected ImageButton roundButton(Skin skin, Drawable icon, EventListener listener, String tooltip) {
        ImageButton ib = new ImageButton(skin, "round") {
            @Override
            public float getPrefWidth() {
                return super.getWidth();
            }

            @Override
            public float getPrefHeight() {
                return super.getHeight();
            }
        };
        ib.setSize(Const.BUILD_RING_ITEM_SIZE, Const.BUILD_RING_ITEM_SIZE);
        ImageButtonStyle ibs = new ImageButtonStyle(ib.getStyle());
        ibs.imageUp = icon;
        ib.setStyle(ibs);
        ib.addListener(listener);
        ib.getImageCell().size(60);
        ib.addListener(new TextTooltip(Quarry.Q.i18n.get(tooltip), skin));
        return ib;
    }

    @Override
    public void show(Window w) {
        stage.addActor(w);
        w.addAction(sequence(alpha(0), fadeIn(0.15f, Interpolation.fade)));
    }

    @Override
    public void hide(Window w) {
        w.addAction(sequence(fadeOut(0.15f, Interpolation.fade), Actions.removeActor()));
    }

    protected void showOrHideRotateButton() {
        if (rotateButton == null || tooltip == null)
            return;

        if (Game.G.activeStructure == tooltipCurrentStructure || currentClickedStructure != null) {
            try {
                rotateButton.clearActions();
                if ((tooltipCurrentStructure != null && !(tooltipCurrentStructure.getSchema().has(Flags.NotRotatable))
                        || currentClickedStructure instanceof IRotatable)) {
                    if (!rotateButton.isVisible()) {
                        rotateButton.addAction(sequence(
                                moveTo(rotateButton.getX(), tooltip.getY() + 200),
                                alpha(1),
                                visible(true),
                                moveTo(rotateButton.getX(), tooltip.getY() + tooltip.getHeight() + 20, 0.15f,
                                        Interpolation.swingOut)));
                    } else {
                        // show animation might not finish in time
                        rotateButton.setColor(Color.WHITE); // clear alpha
                        rotateButton.setPosition(rotateButton.getX(), tooltip.getY() + tooltip.getHeight() + 20);
                    }
                } else if (rotateButton.isVisible()) {
                    rotateButton.addAction(sequence(parallel(
                            moveTo(rotateButton.getX(), tooltip.getY() + tooltip.getHeight() + 20),
                            moveTo(rotateButton.getX(), tooltip.getY() + 200, 0.15f, Interpolation.swingOut),
                            fadeOut(0.1f)),
                            visible(false),
                            alpha(1)));
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

    protected void showOrHideFlipButton() {
        if (flipButton == null || tooltip == null)
            return;

        if (Game.G.activeStructure == tooltipCurrentStructure || currentClickedStructure != null) {
            flipButton.clearActions();
            if (tooltipCurrentStructure instanceof IFlippable || currentClickedStructure instanceof IFlippable) {
                flipButton.addAction(sequence(alpha(1), visible(true), moveTo(flipButton.getX(),
                        tooltip.getY() + tooltip.getHeight() + 20, 0.15f, Interpolation.swingOut)));
            } else {
                flipButton.addAction(sequence(
                        parallel(moveTo(flipButton.getX(), tooltip.getY() + 200, 0.15f, Interpolation.swingOut),
                                fadeOut(0.1f)),
                        visible(false), alpha(1)));
            }
        }
    }

    boolean wantToUpdateResources;

    public void updateResources(boolean justRegister) {
        // update resource cost colors in tooltip
        if (tooltip != null && tooltip.isVisible()) {
            updateTooltipResources();
        }
        if (copyTable != null && copyTable.isVisible()) {
            updateCopyTableResources(false);
        }

        if (buildScrollPane != null && buildScrollPane.isVisible()) {
            updateBuildMenuResources();
        }

        if (justRegister) {
            wantToUpdateResources = true;
            return;
        } else if (!wantToUpdateResources) {
            return;
        }

        //TODO: find out why this exception occurs randomly on load
        try {
            // @Speed @Robustness find a better way to do this
            resources.clear();
            //            Table t0 = resourceRows[0];
            //            ((Label) t0.getCells().get(1).getActor()).setText("" + Quarry.Q.getMoney());
            //            ((TextTooltip) t0.getListeners().get(0)).getActor().setText(Quarry.Q.getMoney() + "$");
            //            resources.addActor(t0);

            for (Map.Entry<ItemType, Integer> r : Game.G.getAllResources()) {
                Table ac = resourceRows[r.getKey().ordinal()];
                ((Label) ac.getCells().get(1).getActor()).setText("" + r.getValue());
                ((TextTooltip) ac.getListeners().get(0)).getActor().setText(r.getValue() + " " + r.getKey().title);
                if (Game.G.hasSeenResource(r.getKey()))
                    resources.addActor(ac);
            }

            resources.fill();
            resources.pack();
        } catch (IndexOutOfBoundsException e) {
            // Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
        int w = (int) resources.getWidth();

        int round = 50;

        resources.setWidth(w + (round - w % round));

        ScrollPane p = (ScrollPane) resources.getParent();
        Table t = (Table) p.getParent();
        t.pack();
        t.setPosition(Const.UI_W - t.getWidth() + 40, Const.UI_H - t.getHeight() + 40);

        clampToInsets(t);
        t.moveBy(40, 40);

        wantToUpdateResources = false;
    }

    public void updateBuildMenuResources() {
        if (buildScrollPane.getActor() != buildTabs) {
            for (Actor c : ((VerticalGroup) buildScrollPane.getActor()).getChildren()) {
                if (c instanceof Button) {
                    boolean canAfford = true;

                    if (tutorial.getStepNum() > -1) {
                        if (tutorial.getStepNum() < 10) {
                            canAfford = c.getUserObject() instanceof Mine;
                        } else {
                            canAfford = c.getUserObject().getClass().equals(Conveyor.class);
                        }
                    }

                    if (canAfford) {
                        for (Amount a : ((Structure<?>) c.getUserObject()).getSchema().buildCosts.entries) {
                            if (Game.G.getResource(a.getItem()) < a.getAmount()) {
                                canAfford = false;
                                break;
                            }
                        }
                    }

                    if (canAfford)
                        ((Table) c).getChildren().get(1).setColor(Color.WHITE);
                    else
                        ((Table) c).getChildren().get(1).setColor(off);
                }
            }
        }
    }

    public void updateBuildMenuSciences() {
        int i = 0;

        if (buildMenuSciences.isEmpty()) {
            buildMenuSciences.addAll(Game.G.sciences);
        }

        Array<Actor> categoryAddStar = new Array<>();
        Array<Actor> structureAddStar = new Array<>();

        for (VerticalGroup vg : buildCategories) {
            boolean any = false;
            int _new = 0;
            for (Actor c : vg.getChildren()) {
                if (c instanceof Button) {
                    Structure<?> s = ((Structure<?>) c.getUserObject());
                    Collection<ScienceType> sciences = s.getSchema().sciencesRequired;
                    boolean vis = Game.G.hasSciences(sciences);
                    if (vis && !buildMenuSciences.containsAll(sciences)) {
                        _new++;
                        structureAddStar.add(c);
                    } else if (s instanceof ProducerStructure) {
                        // check if new recipe
                        for (Recipe r : ((ProducerSchema) s.getSchema()).recipeList.recipes) {
                            if (Game.G.hasSciences(r.getRequiredSciences())
                                    && !buildMenuSciences.containsAll(r.getRequiredSciences())) {
                                _new++;
                                structureAddStar.add(c);
                            }
                        }
                    }

                    c.setVisible(vis);
                    if (vis)
                        any = true;
                }
            }
            vg.invalidate();
            vg.validate();

            Actor a = buildTabs.getChildren().get(i);

            if (!any) {
                a.setVisible(false);
            } else {
                boolean vis = a.isVisible();
                a.setVisible(true);
                if (!vis || _new > 0) {
                    categoryAddStar.add(a);
                }
            }
            i++;
        }

        if (!categoryAddStar.isEmpty() || !structureAddStar.isEmpty()) {
            for (Actor c : buildMenuStars)
                c.remove();
            buildMenuStars.clear();
        }

        buildTabs.invalidate();
        buildTabs.validate();

        for (final Actor a : categoryAddStar) {
            if (buildMenuStars == null)
                buildMenuStars = new Array<>();
            final Image bg = new Image(Quarry.Q.skin.getDrawable("icon_new_bg")) {
                @Override
                public boolean isVisible() {
                    return buildButton.isChecked() && buildScrollPane.getActor() == buildTabs;
                }
            };
            bg.setTouchable(Touchable.disabled);
            bg.setSize(50, 50);
            bg.setOrigin(bg.getWidth() / 2, bg.getHeight() / 2);
            bg.addAction(forever(rotateBy(360, 30)));
            bg.setPosition(160, 110);

            buildMenuStars.add(bg);
            bg.setName("star_bg");
            ((Group) a).addActor(bg);
            final Image fg = new Image(Quarry.Q.skin.getDrawable("icon_new_fg")) {
                @Override
                public boolean isVisible() {
                    return buildButton.isChecked() && buildScrollPane.getActor() == buildTabs;
                }
            };
            ;
            fg.setSize(bg.getWidth(), bg.getHeight());
            fg.setTouchable(Touchable.disabled);
            fg.setPosition(bg.getX(), bg.getY());
            fg.setName("star_fg");
            ((Group) a).addActor(fg);
            buildMenuStars.add(fg);

            a.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    fg.remove();
                    bg.remove();
                    a.removeListener(this);
                }
            });
        }

        for (final Actor a : structureAddStar) {
            if (buildMenuStars == null)
                buildMenuStars = new Array<>();
            final Image bg = new Image(Quarry.Q.skin.getDrawable("icon_new_bg")) {
                @Override
                public boolean isVisible() {
                    return buildButton.isChecked() && buildScrollPane.getActor() == a.getParent();
                }
            };
            bg.setTouchable(Touchable.disabled);
            bg.setSize(50, 50);
            bg.setOrigin(bg.getWidth() / 2, bg.getHeight() / 2);
            bg.addAction(forever(rotateBy(360, 30)));
            bg.setPosition(50, 50);
            buildMenuStars.add(bg);
            bg.setName("star_bg");
            ((Group) a).addActor(bg);

            final Image fg = new Image(Quarry.Q.skin.getDrawable("icon_new_fg")) {
                @Override
                public boolean isVisible() {
                    return buildButton.isChecked() && buildScrollPane.getActor() == a.getParent();
                }
            };
            ;
            fg.setSize(bg.getWidth(), bg.getHeight());
            fg.setTouchable(Touchable.disabled);
            fg.setPosition(bg.getX(), bg.getY());
            fg.setName("star_fg");
            ((Group) a).addActor(fg);
            buildMenuStars.add(fg);

            a.addCaptureListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    fg.remove();
                    bg.remove();
                    a.removeListener(this);
                }
            });
        }

        buildMenuSciences.addAll(Game.G.sciences);
    }

    public void showTooltip(Structure<?> structure) {
        hideStructureUI();
        tileUI.hide();

        tooltipCurrentStructure = structure;

        tooltip.setVisible(true);
        tooltipClose.setVisible(true);
        tooltipCollapse.setVisible(true);

        Skin skin = Quarry.Q.skin;

        tooltipCosts.clear();
        Table tiles = createResourceTable(TOOLTIP_ICON_SIZE, skin, "icon_tiles",
                structure.getWidth() + "x" + structure.getHeight());
        tiles.setBackground(skin.getDrawable("shadow02-bg"));
        tiles.pad(0, 5, 0, 5);
        tiles.left();
        Cell<Table> tilesCell = tooltipCosts.add(tiles).padRight(5);

        Items costs = structure.getSchema().buildCosts;

        if (costs != null) {
            int len = costs.entries.length;
            if (len > ICONS_PER_COLUMN) {
                tilesCell.colspan(2);
            }

            tooltipCosts.row();

            for (int i = 0; i < Math.min(len, ICONS_PER_COLUMN); i++) {
                Amount e = costs.entries[i];
                tooltipCosts.add(
                        createResourceTable(TOOLTIP_ICON_SIZE, skin, e.getItem(), formatResourceAmount(e.getAmount())))
                        .padRight(15);
                if (len > i + ICONS_PER_COLUMN) {
                    Amount f = costs.entries[i + ICONS_PER_COLUMN];
                    tooltipCosts.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, f.getItem(),
                            formatResourceAmount(f.getAmount())));
                }
                tooltipCosts.row();
            }
        }

        tooltipRecipes.clear();

        buildRecipes(structure, tooltipRecipes, tooltipDescription, true);

        updateTooltipResources();
    }

    public void showCopyTable() {
        hideStructureUI();
        hideTooltip();
        copyTable.setVisible(true);

        updateCopyTableResources(true);
    }

    protected void buildRecipes(Structure<?> structure, Table recipeTable, final Label description,
            boolean allRecipes) {
        if (structure instanceof ProducerStructure) {
            Array<Recipe> recipes = new Array<>(((ProducerSchema) structure.getSchema()).recipeList.recipes);

            if (structure instanceof GasTurbine) {
                recipes.clear();
                recipes.addAll(((GasTurbine) structure).getInstancedRecipes());
            }

            if (structure instanceof Mine && !allRecipes) {
                recipes.clear();
                Mine m = (Mine) structure;

                outer: for (Recipe r : ((ProducerSchema) structure.getSchema()).recipeList.recipes) {
                    ItemType outItem = r.getOutput().entries[0].getItem();
                    ItemCategory outCat = r.getOutput().entries[0].getCat();
                    for (ItemType t : m.mineableItems) {
                        if (outItem == t || Item.base(t) == outItem || t.categories.contains(outCat)) {
                            recipes.add(r);
                            continue outer;
                        }
                    }
                }
            }

            for (Iterator<Recipe> iter = recipes.iterator(); iter.hasNext();) {
                Recipe r = iter.next();
                if (!Game.G.hasSciences(r.getRequiredSciences()))
                    iter.remove();
            }

            final Array<Recipe> finalRecipes = recipes;

            if (recipes.size == 1) {
                Recipe r = recipes.get(0);
                currentRecipe = r;
                Table table = renderRecipe(Quarry.Q.skin, r, true);
                recipeTable.add(table).expandY().right().top().height(145).minWidth(300);
                description.setText(r.getDescription());
            } else {
                final float width = 360;

                final ImageButton chevronLeft = new ImageButton(Quarry.Q.skin.getDrawable("caret_left"));
                chevronLeft.getImageCell().padLeft(10);

                final ImageButton chevronRight = new ImageButton(Quarry.Q.skin.getDrawable("caret_right"));
                chevronRight.getImageCell().padRight(10);

                recipeTable.add(chevronLeft).growY();
                Table t = new Table();
                t.defaults().growX();

                final ScrollPane scroll = new ScrollPane(t) {
                    @Override
                    public void act(float delta) {
                        super.act(delta);

                        float mod = getScrollX() % width;
                        float vis = getVisualScrollX();
                        float max = getMaxX();
                        if ((isFlinging() || isPanning()) && (mod < 10 || mod > 340) && vis >= 0 && vis <= max) {
                            visualScrollX(Math.round(getScrollX() / width) * width);
                        }
                        if (!isFlinging() && !isPanning()) {
                            setScrollX(Math.round(getScrollX() / width) * width);
                        }

                        if (isFlinging() || isPanning() || getVisualScrollX() != getScrollX()) {
                            Recipe r = finalRecipes
                                    .get(MathUtils.clamp(Math.round(getScrollX() / width), 0, finalRecipes.size - 1));
                            currentRecipe = r;
                            description.setText(r.getDescription());
                            float scroll = getScrollX();
                            chevronLeft.setVisible(scroll > 0);
                            chevronRight.setVisible(scroll < max);
                        }
                    }

                };

                chevronLeft.setVisible(false);
                chevronLeft.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Quarry.Q.sound.play(Quarry.Q.clickSfx);
                        scroll.setScrollX(scroll.getScrollX() - 300);
                    }
                });

                chevronRight.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Quarry.Q.sound.play(Quarry.Q.clickSfx);
                        scroll.setScrollX(scroll.getScrollX() + 300);
                    }
                });

                for (Recipe r : recipes) {
                    t.add(renderRecipe(Quarry.Q.skin, r, false)).width(width);
                }
                description.setText(recipes.get(0).getDescription());
                currentRecipe = recipes.get(0);

                scroll.setScrollingDisabled(false, true);
                recipeTable.add(scroll).pad(0, 10, 0, 10).width(width).height(145);
                recipeTable.add(chevronRight).growY();
            }
        } else {
            description.setText(structure.getSchema().description);

            if (structure instanceof Storage || structure instanceof Tank || structure instanceof Barrel) {
                for (Component c : structure.getSchema().getComponents()) {
                    if (c instanceof CTank) {
                        recipeTable
                                .add(createResourceTable(TOOLTIP_ICON_SIZE, Quarry.Q.skin, "icon_tank",
                                        formatResourceAmount(((CTank) c).getSize() / 1000f) + "L"))
                                .top().expand();
                    } else {
                        recipeTable.add(createResourceTable(TOOLTIP_ICON_SIZE, Quarry.Q.skin, "icon_cinventory",
                                ((IStorage) c).getSize() + "")).top().expand();
                    }
                }
            } else if (structure instanceof Substation) {
                recipeTable.add(createResourceTable(TOOLTIP_ICON_SIZE, Quarry.Q.skin, "icon_power",
                        formatPowerAmount(((Substation) structure).getSchema().capacity))).top().expand();
            } else if (structure instanceof SolarPanelOutlet || structure instanceof SolarPanel) {
                recipeTable.add(createResourceTable(TOOLTIP_ICON_SIZE, Quarry.Q.skin, "icon_power",
                        formatPowerAmount(SolarPanelOutlet.POWER_OUT) + "/s")).top().expand();
            } else if (structure instanceof Boiler) {
                recipeTable.add(boilerRecipe).expandY().right().top().height(145).minWidth(300);
            } else if (structure instanceof Refinery) {
                recipeTable.add(refRecipe).expandY().right().top().height(145).minWidth(340);
            } else if (structure instanceof DistillationColumn) {
                recipeTable.add(distRecipe).expandY().right().top().height(145).minWidth(340);
            }
        }
    }

    public void updateTooltipResources() {
        if (tooltipCurrentStructure == null)
            return;

        boolean canAfford = true;

        // Update costs colors
        Items costs = tooltipCurrentStructure.getSchema().buildCosts;

        for (Cell<?> c : tooltipCosts.getCells()) {
            Table t = (Table) c.getActor();
            if (t.getUserObject() == null)
                continue;

            ItemType res = (ItemType) t.getUserObject();

            int scale = 1;

            if (Game.G.endB.x > -1) {
                scale = Game.G.activeStructureTrail.size;
            }

            boolean more = Game.G.getResource(res) >= scale * costs.getAmount(res);

            Label l = ((Label) t.getCells().get(1).getActor());
            l.setText(formatResourceAmount(costs.getAmount(res) * scale));
            l.setColor(more ? Color.WHITE : Color.FIREBRICK);

            if (!more)
                canAfford = false;
        }

        canAffordStructure = canAfford;
    }

    public void updateCopyTableResources(boolean reset) {
        if (reset) {
            Skin skin = Quarry.Q.skin;
            copyCosts.clear();
            Table tiles = createResourceTable(TOOLTIP_ICON_SIZE, skin, "icon_tiles",
                    Game.G.copyRegion[2] + "x" + Game.G.copyRegion[3]);
            tiles.setBackground(skin.getDrawable("shadow02-bg"));
            tiles.pad(0, 5, 0, 5);
            tiles.left();
            copyCosts.add(tiles).padRight(5);
            int i = 1;

            if (!Game.G.pasteMode) {
                Game.G.copyCost.put(ItemType.Blueprint, 0);
            } else {
                Game.G.copyCost.remove(ItemType.Blueprint);
            }

            for (Map.Entry<ItemType, Integer> x : Game.G.copyCost.entrySet()) {
                if (i++ % 4 == 0)
                    copyCosts.row();
                copyCosts.add(
                        createResourceTable(TOOLTIP_ICON_SIZE, skin, x.getKey(), formatResourceAmount(x.getValue())))
                        .padRight(15);
            }
        }

        boolean canAfford = true;

        // Update costs colors
        for (Cell<?> c : copyCosts.getCells()) {
            Table t = (Table) c.getActor();
            if (t.getUserObject() == null) {
                // this is the tiles cell
                Label l = ((Label) t.getCells().get(1).getActor());
                l.setText(Game.G.copyRegion[2] + "x" + Game.G.copyRegion[3]);
                continue;
            }
            ItemType res = (ItemType) t.getUserObject();

            boolean more = Game.G.getResource(res) >= Game.G.copyCost.get(res);

            Label l = ((Label) t.getCells().get(1).getActor());
            l.setText(formatResourceAmount(Game.G.copyCost.get(res)));
            l.setColor(more ? Color.WHITE : Color.FIREBRICK);

            if (!more)
                canAfford = false;
        }

        canAffordStructure = canAfford;
    }

    public void hideTooltip() {
        tooltipRecipes.clear();
        tooltipCurrentStructure = null;
        tooltip.setVisible(false);
        tooltipClose.setVisible(false);
        tooltipCollapse.setVisible(false);
        tooltipCollapse.setChecked(false);

        rotateButton.addAction(sequence(visible(false), moveTo(rotateButton.getX(), tooltip.getY() + 200)));
        flipButton.addAction(sequence(visible(false), moveTo(flipButton.getX(), tooltip.getY() + 200)));

        stage.unfocusAll();
    }

    public void hideCopyTable() {
        Game.G.copyMode = false;
        Game.G.pasteMode = false;
        copyTable.setVisible(false);
        copyButton.setChecked(false);
        stage.unfocusAll();
    }

    public void showStructureUI(Structure<?> s) {
        if (currentClickedStructure == s)
            return;

        if (s instanceof ShaftDrillHead) {
            s = ((ShaftDrillHead) s).getDrill();
        }

        hideStructureUI();
        tileUI.hide();

        currentClickedStructure = s;

        if (s instanceof IRotatable) {
            showOrHideRotateButton();
        }

        if (s instanceof IFlippable) {
            showOrHideFlipButton();
        }

        if (s instanceof Conveyor && !(s instanceof ElectricConveyorCore)) {
            s.onClick(structureUIContent);
            return;
        }

        if (s instanceof ProducerStructure) {
            for (Dock d : s.getDocks())
                if (d.type == DockType.ItemIn || d.type == DockType.FluidIn) {
                    structureUIStorage.setVisible(true);
                    break;
                }
        }

        structureUIRecipes.setVisible(s instanceof ProducerStructure
                || s instanceof GeneratorStructure
                || s.getSchema().type == StructureType.Refinery
                || s.getSchema().type == StructureType.Boiler
                || s.getSchema().type == StructureType.DistillationColumn);

        Arrays.fill(structureUIButtonCallbacks, null);
        structureUITitle.setText(s.getSchema().name);
        structureUIContent.clear();

        int len = s.getSchema().buttons.size;

        for (int i = 0; i < 3; i++) {
            ImageButton ib = structureUIButtons[i];
            if (i < len) {
                final ButtonDef b = s.getSchema().buttons.get(i);
                ImageButtonStyle ibs = new ImageButtonStyle(ib.getStyle());
                ibs.imageUp = Quarry.Q.skin.getDrawable(b.icon);

                if (b.type == ButtonType.SinglePress) {
                    ibs.checked = null;
                } else {
                    ibs.checked = Quarry.Q.skin.getDrawable("round_metalActive");
                }

                if (b.type == ButtonType.StateToggle)
                    ib.setChecked(s.getButtonState(i));
                else if (b.type == ButtonType.TempRadio)
                    ib.setChecked(false);

                ib.setStyle(ibs);
                ib.setUserObject(b.type);
                for (Iterator<EventListener> iter = ib.getListeners().iterator(); iter.hasNext();) {
                    if (iter.next() instanceof TextTooltip) {
                        iter.remove();
                        break;
                    }
                }
                ib.addListener(new TextTooltip(Quarry.Q.i18n.get(b.tooltip), Quarry.Q.skin));

                structureUIButtonCallbacks[i] = b.listener;
                ib.setVisible(true);
            } else
                ib.setVisible(false);
        }

        s.onClick(structureUIContent);

        structureUI.setVisible(true);
    }

    public void hideStructureUI() {
        if (currentClickedStructure != null) {
            currentClickedStructure.onUnclick();
            for (int i = 0; i < 3; i++) {
                if (structureUIButtons[i] != null && structureUIButtons[i].getUserObject() == ButtonType.TempRadio)
                    structureUIButtons[i].setChecked(false);
            }
            structureUI.setVisible(false);
            currentClickedStructure = null;
            for (int i = 0; i < 3; i++) {
                ImageButton ib = structureUIButtons[i];
                ib.setVisible(false);
            }
            // hide the rotate button
            showOrHideRotateButton();
            showOrHideFlipButton();
        }

        itemSelection.hide();

        structureUIStorage.setChecked(false);
        structureUIStorage.setVisible(false);

        structureUIRecipes.setChecked(false);
        structureUIRecipes.setVisible(false);
        stage.unfocusAll();
    }

    public void showScienceUI() {
        if (scienceScrollPane.isVisible())
            return;
        scienceMenu.clear();

        for (ScienceType s : ScienceType.values) {
            if ((s.required.length == 0 || (Game.G.hasScience(s) && !Game.GOD_MODE) || Game.G.hasCurrentScience(s)))
                continue;

            if (Game.G.hasSciences(s.required) || Game.GOD_MODE) {
                Button button = scienceMenuItem(s);
                scienceMenu.addActor(button);
            }
        }

        scienceScrollPane.addAction(sequence(alpha(0), visible(true), fadeIn(0.1f)));
        scienceClose.addAction(sequence(alpha(0), visible(true), fadeIn(0.1f)));
    }

    public void hideScienceUI() {
        hideScienceUI(false);
    }

    public void hideScienceUI(boolean instantly) {
        if (!scienceScrollPane.isVisible())
            return;

        if (currentClickedStructure instanceof ScienceLab)
            structureUIButtons[0].setChecked(false);

        if (instantly) {
            scienceScrollPane.setVisible(false);
            scienceClose.setVisible(false);
        } else {
            scienceScrollPane.addAction(sequence(fadeOut(0.1f), visible(false)));
            scienceClose.addAction(sequence(fadeOut(0.1f), visible(false)));
        }
        stage.unfocusAll();
    }

    public void updateStructureUIInventory() {
        if (currentClickedStructure == null || !(currentClickedStructure instanceof ProducerStructure))
            return;

        structureUIInventory.clear();
        structureUIInventorySum.clear();

        Structure<?> structure = currentClickedStructure;

        IStorage[] inputs = ((ProducerStructure) structure).getInputInventories();

        // accumulate inventory counts
        for (int i = 0, j = 0; i < structure.getDocks().length; i++) {
            if (structure.getDocks()[i].type == DockType.ItemIn) {
                for (Map.Entry<ItemType, Integer> e : ((CRecipeSlotStorage) inputs[j]).getAll()) {
                    Integer val = structureUIInventorySum.get(e.getKey());
                    if (val == null)
                        val = 0;
                    structureUIInventorySum.put(e.getKey(), val + e.getValue());
                }
                j++;
            } else if (structure.getDocks()[i].type == DockType.FluidIn) {
                CTank tank = (CTank) inputs[j];
                if (!tank.isEmpty())
                    structureUIInventorySum.put(tank.getFluid(), tank.get(tank.getFluid()));
                j++;
            }
        }

        // create UI
        for (Map.Entry<ItemType, Integer> e : structureUIInventorySum.entrySet()) {
            Table t = structureUIInventoryCells.get(e.getKey());
            String text = "";
            if (e.getKey().categories.contains(ItemCategory.Fluid)) {
                text = GameUi.formatResourceAmount(e.getValue() / 1000f, true) + "L";
            } else {
                text = GameUi.formatResourceAmount(e.getValue());
            }
            if (t == null) {
                t = createResourceTable(32, Quarry.Q.skin, e.getKey(), text, e.getKey());
                structureUIInventoryCells.put(e.getKey(), t);
                ((Label) t.getCells().get(1).getActor()).setAlignment(Align.left);
            } else {
                ((Label) t.getChildren().get(1)).setText(text);
                ((TextTooltip) t.getListeners().get(0)).getActor().setText(text + " " + e.getKey().title);
            }

            structureUIInventory.addActor(t);
        }
    }

    public void update(double deltaTime) {
        try {
            stage.act((float) deltaTime);
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
        tutorial.update();

        updateResources(false);

        /*System.out.println(String.format("FPS: %d\nUpd-time: %.2fms\nDraw-time: %.2fms",
                Gdx.graphics.getFramesPerSecond(),
                Quarry.Q.getUpdateTime(),
                Quarry.Q.getFrameTime()));*/
        menu.fps.setText(String.format("FPS: %d\nUPS: %d",
                Gdx.graphics.getFramesPerSecond(),
                Math.round(1 / Math.max(1 / 60.0f, Quarry.Q.getUpdateTime() / 1000))));
    }

    public void draw() {
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height);
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    boolean debug = false;

    public void toggleDebug() {
        stage.setDebugAll(debug = !debug);
    }

    public void onScienceChange() {
        copyButton.setVisible(Game.G.hasScience(ScienceType.Blueprints));
        menu.onScienceChange();
        layerSelection.onScienceChange();
    }

    ///////////////////////////////

    public static Cell<?> sep(Table t) {
        t.row();
        return t.add(new Image(Quarry.Q.skin.getDrawable("button"))).height(1).top();
    }

    public static String formatResourceAmount(int amount) {
        return intFormat.format(amount);
    }

    public static String formatResourceAmount(float amount) {
        return intFormat.format(amount);
    }

    public static String formatResourceAmount(float amount, boolean fix) {
        if (fix)
            return decFormat.format(amount);
        else
            return intFormat.format(amount);
    }

    public static String formatPowerAmount(double rawAmount) {
        int amount = (int) Math.round(rawAmount);

        if (amount >= 1_000_000_000)
            return formatResourceAmount(Math.round(amount / 1_000_000_00) / 10f) + " GJ";
        if (amount >= 1_000_000)
            return formatResourceAmount(Math.round(amount / 1_000_00) / 10f) + " MJ";
        if (amount >= 1_000)
            return formatResourceAmount(Math.round(amount / 1_00) / 10f) + " kJ";
        return formatResourceAmount(amount) + " J";
    }

    private static HashMap<String, NinePatchDrawable> drawableCache = new HashMap<>();

    public static NinePatchDrawable getDrawable(Skin skin, String name, int padTop, int padLeft, int padRight,
            int padBot) {
        String key = name + "-" + padTop + "-" + padLeft + "-" + padRight + "-" + padBot;
        NinePatchDrawable d = drawableCache.get(key);
        if (d != null)
            return d;

        d = (NinePatchDrawable) skin.newDrawable(name);
        d.setMinWidth(1);
        d.setMinHeight(1);
        d.setTopHeight(padTop);
        d.setLeftWidth(padLeft);
        d.setRightWidth(padRight);
        d.setBottomHeight(padBot);

        drawableCache.put(key, d);
        return d;
    }

    // TODO: resolve actual used items and not only the general recipe
    // Also see ProductionStructure.updateUI()
    public static Table renderRecipe(Skin skin, Recipe recipe,
            /*Items activeInputs, Items activeOutputs, */boolean singleMode) {
        Table table = new Table();
        table.setBackground(skin.getDrawable("panel_metal"));
        table.pad(20);
        table.defaults();
        if (recipe.getInput() != null) {
            Table inputs = new Table();
            inputs.defaults().left();
            for (Amount e : recipe.getInput().entries) {
                boolean fluid = e.getCat() == ItemCategory.Fluid
                        || (e.getItem() != null && e.getItem().categories.contains(ItemCategory.Fluid));

                String text = formatResourceAmount(fluid ? e.getAmount() / 1000f : e.getAmount());
                if (fluid)
                    text += "L";

                if (e instanceof ConstantSupplyAmount) {
                    text += "/s";
                }

                Amount a = e;

                if (a.getCat() != null)
                    inputs.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, a.getCat(), text)).padRight(5);
                else
                    inputs.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, a.getItem(), text)).padRight(5);
            }
            table.add(inputs).expandX();
        }

        table.row();
        Table t = new Table();

        if (recipe.getPower() > 0 && !(recipe instanceof GeneratorRecipe)) {
            Cell<?> c = t.add(createResourceTable(25, skin, skin.getDrawable("icon_power"),
                    formatPowerAmount(recipe.getPower() * 60) + "/s", "small-font"));
            if (recipe.getInput() == null)
                c.padRight(5);
        }

        if (recipe.getInput() != null) {
            Image arrow = new Image(skin.getDrawable("symb_rightarrow"));
            arrow.setOrigin(12, 7);
            arrow.setRotation(-90);
            t.add(arrow).size(24, 15).pad(10);
        }

        Table timeTable = createResourceTable(25, skin, skin.getDrawable("icon_time"),
                formatResourceAmount(recipe.workingTime) + "s", "small-font");
        timeTable.getCells().get(1).getActor().setName("time");
        t.add(timeTable);

        table.add(t);

        if (recipe.getOutput() != null || recipe instanceof GeneratorRecipe) {
            table.row();
            Table outputs = new Table();
            outputs.defaults().left();

            if (recipe instanceof GeneratorRecipe) {
                outputs.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, skin.getDrawable("icon_power"),
                        formatPowerAmount(((GeneratorRecipe) recipe).getPowerGeneration()) + "/s")).padRight(5);
            }

            if (recipe.getOutput() != null) {
                for (Amount e : recipe.getOutput().entries) {
                    boolean fluid = e.getCat() == ItemCategory.Fluid
                            || (e.getItem() != null && e.getItem().categories.contains(ItemCategory.Fluid));

                    String text = formatResourceAmount(fluid ? e.getAmount() / 1000f : e.getAmount());
                    if (fluid)
                        text += "L";

                    Amount a = e;

                    if (a.getCat() != null)
                        outputs.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, a.getCat(), text)).padRight(5);
                    else
                        outputs.add(createResourceTable(TOOLTIP_ICON_SIZE, skin, a.getItem(), text)).padRight(5);
                }
            }

            Cell<?> c = table.add(outputs).center();
            if (recipe.getInput() == null)
                c.expandX();
        }

        return table;
    }

    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    /// @Refactor to ResourceLabel
    public static Table createResourceTable(int iconSize, Skin skin, String icon, String text) {
        return createResourceTable(iconSize, skin, skin.getDrawable(icon), text);
    }

    public static Table createResourceTable(int iconSize, Skin skin, Drawable icon, String text, Object... userObject) {
        return createResourceTable(iconSize, skin, icon, text, "default-font", userObject);
    }

    public static Table createResourceTable(int iconSize, Skin skin, Drawable icon, String text, String font,
            Object... userObject) {
        Table t = new Table();
        if (userObject.length > 0)
            t.setUserObject(userObject[0]);
        Image i = new Image(icon);
        t.add(i).size(iconSize);
        Label l = new Label(text, skin, font, Color.WHITE);
        l.setAlignment(Align.right);
        t.add(l).height(iconSize + 4).padLeft(5).right().grow();

        return t;
    }

    public static Table createResourceTable(int iconSize, Skin skin, ItemType item, String text, Object... userObject) {
        Table t = createResourceTable(iconSize, skin, item.drawable, text, item, userObject);
        t.setTouchable(Touchable.enabled);
        t.addListener(new TextTooltip(text.trim() + " " + item.title, skin));
        return t;
    }

    public static Table createResourceTable(int iconSize, Skin skin, ItemCategory cat, String text,
            Object... userObject) {
        Table t = createResourceTable(iconSize, skin, cat.drawable, text, userObject);
        t.setTouchable(Touchable.enabled);
        t.addListener(new TextTooltip(text.trim() + " " + cat.title, skin));
        return t;
    }

}
