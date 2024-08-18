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

import java.util.EnumSet;
import java.util.Objects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.IntSet;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.ListTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.math.QuadTree.Region;
import de.dakror.common.libgdx.render.SpriteRenderer;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Layer;
import de.dakror.quarry.game.power.PowerNetwork;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.Booster;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.Schema.Flags;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Savable;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public abstract class Structure<T extends Schema> implements Savable, Region {
    public static final StructureType[] types = new StructureType[Const.STRUCTURE_COUNT];

    public int x, y;
    protected T schema;
    public Layer layer;

    protected boolean clicked;

    protected long sfxId = -1;
    protected boolean sfxPlaying;

    protected Component[] components;

    public static final TextureRegion fullStateIcon = Quarry.Q.atlas.findRegion("icon_cinventory");
    public static final TextureRegion powerStateIcon = Quarry.Q.atlas.findRegion("icon_power");
    public static final TextureRegion boostStateIcon = Quarry.Q.atlas.findRegion("symb_ff");

    public static final Color selectionColor = Color.valueOf("#4286f4a0");

    protected Booster nearbyBooster;

    protected PowerNetwork powerNetwork;
    protected float powerReceivedThisTick;

    protected int speedScale = 1;

    protected int gameSpeed;
    protected float time;

    protected int index;

    protected byte version;

    // relevant to item entities still referring to their source
    protected boolean destroyed;

    /**
     * for rotation
     */
    protected Direction upDirection;
    protected Dock[] docks;

    protected Structure(int x, int y, T schema) {
        this.x = x;
        this.y = y;
        this.schema = schema;
        this.version = schema.version;
        this.upDirection = Direction.North;
        docks = schema.copyDocks(this);

        components = schema.copyComponents(this);

        index = -1;
    }

    public Direction getUpDirection() {
        return upDirection;
    }

    public void setUpDirection(Direction upDirection) {
        this.upDirection = upDirection;
        docks = schema.copyDocks(this);
        setDirty();
    }

    public Dock[] getDocks() {
        return docks;
    }

    public Booster getNearbyBooster() {
        return nearbyBooster;
    }

    public void setNearbyBooster(Booster booster) {
        nearbyBooster = booster;
        if (booster == null) speedScale = 1;
    }

    public T getSchema() {
        return schema;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    public int getIndex() {
        if (layer == null) {
            return Game.G.layer.getIndex() * (Game.G.layer.width * Game.G.layer.height) + x * Game.G.layer.height + y;
        }

        if (index == -1) {
            index = layer.getIndex() * (layer.width * layer.height) + x * layer.height + y;
        }
        return index;
    }

    /**
     * 0 means none at all
     * @return
     */
    public int getDonorPriority() {
        return 0;
    }

    /**
     * 0 means none at all
     * @return
     */
    public int getReceiverPriority() {
        return 0;
    }

    public double getPowerLevel() {
        return 0;
    }

    public double getPowerCapacity() {
        return 0;
    }

    public double getPowerRatio() {
        double level = getPowerLevel();
        if (level == 0) return 0;
        return level / getPowerCapacity();
    }

    /**
     * Try to accept power, return rest
     */
    public double acceptPower(double power, double networkStrength) {
        return power;
    }

    public double requestPower(double power, double networkStrength) {
        return 0;
    }

    public void refundPower(double power) {}

    public double getPowerReceivedThisTick() {
        return powerReceivedThisTick;
    }

    public PowerNetwork getPowerNetwork() {
        return powerNetwork;
    }

    public void setPowerNetwork(PowerNetwork network) {
        powerNetwork = network;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSchema().type, getIndex());
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Structure && obj.hashCode() == hashCode();
    }

    @Override
    public int getWidth() {
        return upDirection.dx != 0 ? schema.height : schema.width;
    }

    @Override
    public int getHeight() {
        return upDirection.dx != 0 ? schema.width : schema.height;
    }

    public float getLoudness() {
        return getSchema().loudness * speedScale;
    }

    public int getSpeedScale() {
        return speedScale;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        this.gameSpeed = gameSpeed;

        if (gameSpeed > 0) {
            this.time += deltaTime;
            if (this.time > 1) this.time--;
        }

        for (Component c : components)
            c.update(deltaTime, gameSpeed, dirtyBounds);
    }

    public void postUpdate(Bounds dirtyBounds) {
        if (nearbyBooster != null)
            speedScale = nearbyBooster.isBoostActive() ? Booster.BOOST_FACTOR : 1;

        powerReceivedThisTick = 0;
    }

    public void postLoad() {
        for (Component c : components)
            c.postLoad();
    }

    public void reloadPowerNetwork() {
        if (getSchema().powerDocks > 0) {
            powerNetwork.updateConnections(this);
        }
    }

    protected void setDirty() {
        if (layer != null) {
            layer.dirtyBounds.add(this, 0);
        }
    }

    /**
     * @param item
     * @param x    item position adjacent to this structures position of a possible dock
     * @param y
     * @param dir  direction of item
     * @return
     */
    public boolean canAccept(ItemType item, int x, int y, Direction dir) {
        return false;
    }

    /**
     * try to accept item
     *
     * @param item
     * @param dir TODO
     * @return
     */
    public boolean acceptItem(ItemType item, Structure<?> source, Direction dir) {
        return false;
    }

    /**
     * Try to accept fluid, return the rest of the amount that could not be accepted
     *
     * @param item
     * @param amount
     * @param source
     * @return
     */
    public int acceptFluid(ItemType item, int amount, Structure<?> source) {
        return amount;
    }

    public boolean isNextToDock(int x, int y, Direction dir, Dock d) {
        return x == this.x + d.x + d.dir.dx && y == this.y + d.y + d.dir.dy && dir != null && d.dir == dir.inv();
    }

    protected boolean isAdjacentTo(Structure<?> other) {
        return (x + getWidth() == other.x || other.x + other.getWidth() == x)
                || (y + getHeight() == other.y || other.y + other.getHeight() == y);
    }

    public boolean putBack(ItemType item, int amount) {
        System.err.println("Operation putBack not supported on " + getClass());
        return false;
    }

    public void drawFrame(SpriteRenderer spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        for (Component c : components)
            c.drawFrame(spriter, shaper);

        if (clicked) {
            drawHighlighting(shaper);
        }

        /*if (Game.DRAW_DEBUG && nearbyBooster != null) {
            shaper.setColor(1, 1, 0, 1);
            shaper.rectLine((x + getWidth() / 2f) * Const.TILE_SIZE, (y + getHeight() / 2f) * Const.TILE_SIZE,
                    (nearbyBooster.x + nearbyBooster.getWidth() / 2f) * Const.TILE_SIZE, (nearbyBooster.y + nearbyBooster.getHeight() / 2f) * Const.TILE_SIZE, 4);
        }*/
    }

    protected void drawBoostState(SpriteRenderer spriter) {
        if (gameSpeed > 1) {
            float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
            spriter.add(Structure.boostStateIcon, (x) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 + Const.STATE_SIZE * .25f,
                    (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
        }
    }

    public void drawHighlighting(ShapeRenderer shaper) {
        shaper.setColor(selectionColor);
        // lu -> ru
        shaper.line(x * Const.TILE_SIZE, y * Const.TILE_SIZE, (x + getWidth()) * Const.TILE_SIZE, y * Const.TILE_SIZE);
        // lo -> ro
        shaper.line(x * Const.TILE_SIZE, (y + getHeight()) * Const.TILE_SIZE, (x + getWidth()) * Const.TILE_SIZE, (y + getHeight()) * Const.TILE_SIZE);

        // lu -> lo
        shaper.line(x * Const.TILE_SIZE, y * Const.TILE_SIZE, x * Const.TILE_SIZE, (y + getHeight()) * Const.TILE_SIZE);
        // ru -> ro
        shaper.line((x + getWidth()) * Const.TILE_SIZE, y * Const.TILE_SIZE, (x + getWidth()) * Const.TILE_SIZE, (y + getHeight()) * Const.TILE_SIZE);

        shaper.setColor(selectionColor.r, selectionColor.g, selectionColor.b, 0.25f);
        shaper.rect(x * Const.TILE_SIZE, y * Const.TILE_SIZE, getWidth() * Const.TILE_SIZE, getHeight() * Const.TILE_SIZE);
    }

    public void draw(SpriteRenderer spriter) {
        if (docks.length > 0)
            drawDocks(spriter);

        float x = this.x * Const.TILE_SIZE,
                y = this.y * Const.TILE_SIZE,
                z = Const.Z_STRUCTURES,
                width = schema.width * Const.TILE_SIZE,
                height = schema.height * Const.TILE_SIZE,
                originX, originY;

        if (upDirection.dx != 0) {
            originX = getHeight() / 2f * Const.TILE_SIZE;
            originY = getWidth() / 2f * Const.TILE_SIZE;
            x += (getWidth() - getHeight()) / 2f * Const.TILE_SIZE;
            y += (getHeight() - getWidth()) / 2f * Const.TILE_SIZE;
        } else {
            originX = (schema.width / 2f) * Const.TILE_SIZE;
            originY = (schema.height / 2f) * Const.TILE_SIZE;
        }

        float rotation = upDirection.rot - Direction.North.rot;

        if (getSchema().has(Flags.TextureAlwaysUpright)) {
            rotation = 0;
        }

        // mirrored, saving texture space
        if (getSchema().has(Flags.MirroredTextureHorizontal) || getSchema().has(Flags.MirroredTextureVertical)) {
            if (getSchema().has(Flags.MirroredTextureHorizontal) && getSchema().has(Flags.MirroredTextureVertical)) {
                spriter.add(getSchema().tex, x, y, z, originX, originY, width * 0.5f, height * 0.5f, 1, 1, rotation);
                spriter.add(getSchema().tex, x, y, z, originX, originY, width * 0.5f, height * 0.5f, 1, -1, rotation);
                spriter.add(getSchema().tex, x, y, z, originX, originY, width * 0.5f, height * 0.5f, -1, -1, rotation);
                spriter.add(getSchema().tex, x, y, z, originX, originY, width * 0.5f, height * 0.5f, -1, 1, rotation);
            } else {
                spriter.add(getSchema().tex, x, y, z, originX, originY,
                        width * (getSchema().has(Flags.MirroredTextureHorizontal) ? 0.5f : 1),
                        height * (getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : 0.5f),
                        1, 1, rotation);

                spriter.add(getSchema().tex, x, y, z, originX, originY,
                        width * (getSchema().has(Flags.MirroredTextureHorizontal) ? 0.5f : 1),
                        height * (getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : 0.5f),
                        getSchema().has(Flags.MirroredTextureHorizontal) ? -1 : 1,
                        getSchema().has(Flags.MirroredTextureHorizontal) ? 1 : -1,
                        rotation);
            }
        } else {
            spriter.add(getSchema().tex, x, y, z, originX, originY, width, height, 1, 1, rotation);
        }
    }

    protected void drawFullState(SpriteRenderer spriter) {
        float size = Const.STATE_SIZE * (1 + 0.3f * (MathUtils.sin(time * 2 * MathUtils.PI) * 0.5f + 0.5f));
        spriter.add(Structure.fullStateIcon, (x + getWidth()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f,
                (y + getHeight()) * Const.TILE_SIZE - (size - Const.STATE_SIZE) / 2 - Const.STATE_SIZE * 1.25f, Const.Z_STATES, size, size);
    }

    protected void drawDocks(SpriteRenderer spriter) {
        int o = Const.TUBE_OFFSET * 2;
        float s = Const.TILE_SIZE - o;

        for (Dock d : getDocks()) {
            float dx = (x + d.x) * Const.TILE_SIZE + Const.TUBE_OFFSET + d.dir.dx * (Const.TILE_SIZE - Const.TUBE_OFFSET);
            float dy = (y + d.y) * Const.TILE_SIZE + Const.TUBE_OFFSET + d.dir.dy * (Const.TILE_SIZE - Const.TUBE_OFFSET);

            TextureRegion tr = d.type.tex;
            if (tr != null)
                spriter.add(tr, dx, dy, d.type == DockType.Power ? Const.Z_CABLES + 0.01f : Const.Z_TUBES + 0.01f, s / 2, s / 2, s, 13, 1, 1, d.dir.ordinal() * -90);
        }
    }

    @Override
    public Object clone() {
        try {
            Structure<?> s = null;
            if (getSchema().type.versionSwitch)
                s = (Structure<?>) getSchema().type.constr.newInstance(x, y, version);
            else
                s = (Structure<?>) getSchema().type.constr.newInstance(x, y);
            s.setUpDirection(upDirection);
            return s;
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            return null;
        }
    }

    public void onDestroy() {
        destroyed = true;
    }

    public void onPlacement(boolean fromLoading) {
        if (!fromLoading) {
            for (Component c : components)
                c.onPlacement();
        }
    }

    public boolean getButtonState(int buttonIndex) {
        return false;
    }

    public void onClick(Table content) {
        clicked = true;
    }

    public boolean isClicked() {
        return clicked;
    }

    public void onUnclick() {
        clicked = false;
    }

    public Component[] getComponents() {
        return components;
    }

    public void setItemNotifications() {
        if (getSchema().inputDocks == 0 || layer == null) return;
        for (Dock d : getDocks())
            if (d.type == DockType.ItemIn)
                layer.setItemNotification(x + d.x + d.dir.dx, y + d.y + d.dir.dy);
    }

    protected void playSfx() {
        if (sfxPlaying || getSchema().sfx == null || !Quarry.Q.sound.isPlaySound() || Game.G.layer != layer)
            return;

        if (sfxId != -1) {
            Game.G.spatializedPlayer.resume(sfxId);
            sfxPlaying = true;
        } else {
            sfxId = Game.G.spatializedPlayer.play(this, getSchema().sfx.sound, getSchema().sfx.pitch, true, true);
            sfxPlaying = sfxId != -1;
        }
    }

    protected void pauseSfx() {
        if (!sfxPlaying || sfxId == -1) return;

        Game.G.spatializedPlayer.pause(sfxId);
        sfxPlaying = false;
    }

    public void stopSfx() {
        if (sfxId == -1) return;

        Game.G.spatializedPlayer.stop(sfxId);
        sfxId = -1;
        sfxPlaying = false;
    }

    @Override
    public final void save(Builder b) {
        b
                .Compound()
                .Byte("type", getSchema().type.id)
                .Int("x", x)
                .Int("y", y);

        saveData(b);

        b.End();
    }

    public final CompoundTag copy(int[] copyRegion) {
        NBT.Builder b = new NBT.Builder(null)
                .Byte("type", getSchema().type.id)
                .Byte("version", version)
                .Int("x", x - copyRegion[0])
                .Int("y", y - copyRegion[1]);

        copyData(copyRegion, b);

        b.End();
        return b.Get();
    }

    protected void copyData(int[] copyRegion, Builder b) {
        b.Byte("upDir", (byte) upDirection.ordinal());
    }

    public final void paste(int[] pasteRegion, CompoundTag tag) {
        pasteData(pasteRegion, tag);
    }

    protected void pasteData(int[] pasteRegion, CompoundTag tag) {
        if (tag.has("upDir")) {
            setUpDirection(Direction.values[tag.Byte("upDir", (byte) 0)]);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(getIndex());
    }

    protected void saveData(Builder b) {
        b
                .Byte("upDir", (byte) upDirection.ordinal())
                .Byte("version", version);

        if (components.length > 0) {
            b.List("Components", TagType.Compound);
            for (Component c : components)
                c.saveData(b);

            b.End();
        }
    }

    protected void loadData(CompoundTag tag) throws NBTException {
        if (tag.has("upDir")) {
            setUpDirection(Direction.values[tag.Byte("upDir", (byte) 0)]);
        }

        if (components.length > 0) {
            try {
                ListTag list = tag.List("Components");
                for (int i = 0; i < list.data.size; i++)
                    components[i].loadData((CompoundTag) list.data.get(i));
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }
    }

    static EnumSet<StructureType> schemaVersionCheck = EnumSet.noneOf(StructureType.class);

    public static <T extends Schema> T selectSchema(int version, T... schemas) {
        // sanity check
        if (!schemaVersionCheck.contains(schemas[0].type)) {
            IntSet s = new IntSet();
            for (T t : schemas) {
                if (s.contains(t.version)) throw new RuntimeException(t.type.name() + ": Multiple schemas with same version!");
                s.add(t.version);
            }
            schemaVersionCheck.add(schemas[0].type);
        }

        for (T t : schemas)
            if (t.version == version) return t;
        return null;
    }

    public static Structure<?> load(CompoundTag tag) throws NBTException {
        byte b = tag.Byte("type");

        StructureType t = types[b & 0xff];
        if (t == null) {
            System.err.println("Not loading Structure with id: " + b);
            return null;
        }

        Structure<?> s = null;

        int x = tag.Int("x");
        int y = tag.Int("y");
        int version = tag.Byte("version", (byte) 0) & 0xff;

        try {
            if (t.versionSwitch)
                s = (Structure<?>) t.constr.newInstance(x, y, version);
            else
                s = (Structure<?>) t.constr.newInstance(x, y);
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }

        s.loadData(tag);

        return s;
    }

    public static Structure<?> loadPaste(int[] pasteRegion, CompoundTag tag) throws NBTException {
        byte b = tag.Byte("type");

        StructureType t = types[b & 0xff];
        if (t == null) {
            System.err.println("Not loading Structure with id: " + b);
            return null;
        }

        Structure<?> s = null;

        int x = tag.Int("x") + pasteRegion[0];
        int y = tag.Int("y") + pasteRegion[1];
        int version = tag.Byte("version", (byte) 0) & 0xff;

        try {
            if (t.versionSwitch)
                s = (Structure<?>) t.constr.newInstance(x, y, version);
            else
                s = (Structure<?>) t.constr.newInstance(x, y);
        } catch (Exception e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }

        return s;
    }
}
