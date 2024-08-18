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

package de.dakror.quarry.structure.power;

import java.util.HashSet;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.scenes.GameUi;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.CopyRegionHelper;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class SolarPanelOutlet extends Structure<Schema> {
    public static final Schema classSchema = new Schema(0, StructureType.SolarPanelOutlet, true, 3,
            3,
            "solarpaneloutlet",
            new Items(ItemType.SiliconWafer, 40, ItemType.Glass, 50, ItemType.Battery, 5, ItemType.SteelCable, 40),
            null, new Dock(1, 2, Direction.North, DockType.BigPower))
                    .loudness(0.2f)
                    .flags(Flags.MirroredTextureHorizontal)
                    .sciences(ScienceType.SolarPower);

    public static final int POWER_OUT = 100 * 60;

    Table ui;
    Container<Table> container;

    Array<SolarPanel> connectedPanels = new Array<>();
    int[] panelsIndex;

    final HashSet<Integer> tmp = new HashSet<>();

    public SolarPanelOutlet(int x, int y) {
        super(x, y, classSchema);
    }

    @Override
    public void onPlacement(boolean fromLoading) {
        super.onPlacement(fromLoading);

        if (!fromLoading && layer != null) {
            resetAdjacentPanels();
            discoverAdjacentPanels(this, tmp);
        }
    }

    private void resetAdjacentPanels() {
        tmp.clear();
        for (SolarPanel p : connectedPanels) {
            p.outlet = null;
            layer.dirtyBounds.add(p, 0);
        }
        connectedPanels.clear();
    }

    private void discoverAdjacentPanels(Structure<?> of, HashSet<Integer> history) {
        if (((of instanceof SolarPanel && ((SolarPanel) of).outlet == null) || of == this) && !history.contains(of.getX() * layer.height + of.getY())) {
            history.add(of.getX() * layer.height + of.getY());
            if (of instanceof SolarPanel) {
                connectedPanels.add((SolarPanel) of);
                ((SolarPanel) of).outlet = this;
            }

            for (int i = 0; i < of.getWidth(); i++) {
                discoverAdjacentPanels(layer.getStructure(of.getX() - 1, of.getY() + i), history);
                discoverAdjacentPanels(layer.getStructure(of.getX() + i, of.getY() - 1), history);
                discoverAdjacentPanels(layer.getStructure(of.getX() + of.getWidth(), of.getY() + i), history);
                discoverAdjacentPanels(layer.getStructure(of.getX() + i, of.getY() + of.getHeight()), history);
            }
        }

        if (clicked) {
            updateUI();
        }
    }

    private void updateUI() {
        if (container == null) {
            container = new Container<>();
        }
        if (ui == null) {
            ui = GameUi.createResourceTable(32, Quarry.Q.skin, Quarry.Q.skin.getDrawable("icon_power"), "");
            container.setActor(ui);
        }

        ((Label) ui.getChildren().get(1)).setText(GameUi.formatPowerAmount((connectedPanels.size + 1) * POWER_OUT) + "/s");
    }

    @Override
    public void onClick(Table content) {
        super.onClick(content);

        updateUI();

        content.add(container).grow();
    }

    @Override
    public void postLoad() {
        super.postLoad();

        for (int i : panelsIndex) {
            Structure<?> p = layer.getStructure(i);
            if (p instanceof SolarPanel) {
                connectedPanels.add((SolarPanel) p);
                ((SolarPanel) p).outlet = this;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        resetAdjacentPanels();
    }

    @Override
    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        super.update(deltaTime, gameSpeed, dirtyBounds);

        if (gameSpeed > 0) {
            if (powerNetwork != null) {
                powerNetwork.offerPower(deltaTime, gameSpeed, POWER_OUT * (connectedPanels.size + 1) * deltaTime, this);
            }
        }

        if (!dirtyBounds.isEmpty()) {
            resetAdjacentPanels();
            discoverAdjacentPanels(this, tmp);
        }
    }

    @Override
    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        super.drawFrame(spriter, shaper, pfxBatch);

        if (clicked) {
            shaper.set(ShapeType.Filled);
            shaper.setColor(Color.PURPLE);
            shaper.rect(
                    (x + getWidth() * 0.5f) * Const.TILE_SIZE - 10,
                    (y + getHeight() * 0.5f) * Const.TILE_SIZE - 10, 20, 20);
            for (Structure<?> p : connectedPanels) {
                shaper.rectLine(
                        (x + getWidth() * 0.5f) * Const.TILE_SIZE,
                        (y + getHeight() * 0.5f) * Const.TILE_SIZE,
                        (p.x + p.getWidth() * 0.5f) * Const.TILE_SIZE,
                        (p.y + p.getHeight() * 0.5f) * Const.TILE_SIZE, 3);
                shaper.rect(
                        (p.x + p.getWidth() * 0.5f) * Const.TILE_SIZE - 6,
                        (p.y + p.getHeight() * 0.5f) * Const.TILE_SIZE - 6, 12, 12);
            }
        }
    }

    @Override
    public void postUpdate(Bounds dirtyBounds) {
        super.postUpdate(dirtyBounds);

        if (dirtyBounds.hasAnyFlag(Bounds.Flags.CONSTRUCTION | Bounds.Flags.DESTRUCTION)) {
            resetAdjacentPanels();
            discoverAdjacentPanels(this, tmp);
        }
    }

    @Override
    protected void saveData(Builder b) {
        super.saveData(b);
        int[] arr = new int[connectedPanels.size];
        for (int i = 0; i < arr.length; i++)
            arr[i] = connectedPanels.get(i).x * layer.height + connectedPanels.get(i).y;
        b.IntArray("panels", arr);
    }

    @Override
    protected void loadData(CompoundTag tag) throws NBTException {
        super.loadData(tag);
        panelsIndex = tag.IntArray("panels", new int[] {});
    }

    @Override
    protected void copyData(int[] copyRegion, Builder b) {
        super.copyData(copyRegion, b);
        int[] arr = new int[connectedPanels.size];
        for (int i = 0; i < arr.length; i++)
            arr[i] = CopyRegionHelper.indexGlobalToRelative(copyRegion, connectedPanels.get(i).x * layer.height + connectedPanels.get(i).y);
        b.IntArray("panels", arr);
    }

    @Override
    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        super.pasteData(pasteRegion, tag);

        int[] panels = tag.IntArray("panels", new int[] {});

        for (int i : panels) {
            Structure<?> p = CopyRegionHelper.getStructure(this, pasteRegion, i);
            if (p instanceof SolarPanel) {
                connectedPanels.add((SolarPanel) p);
                ((SolarPanel) p).outlet = this;
            }
        }
    }
}
