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

import java.util.EnumSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.ParticleEffectPool.PooledEffect;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.dakror.common.libgdx.ChangeNotifier.Event;
import de.dakror.common.libgdx.ChangeNotifier.Listener;
import de.dakror.common.libgdx.Pair;
import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.render.DepthSpriter;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Generator.VeinDebug;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Tile.TileMeta;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Direction;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ITube;
import de.dakror.quarry.structure.base.StorageStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.logistics.Hopper;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Savable;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Layer implements Disposable, Savable, Listener<Layer> {
    Chunk[] chunks;

    byte[] flags;

    public static final int FLAG_STRUCTURE_COLLISION = 1 << 0;
    public static final int FLAG_TUBE_COLLISION = 1 << 1;
    public static final int FLAG_ITEM_DOCK_COLLISION = 1 << 2;
    public static final int FLAG_FLUID_DOCK_COLLISION = 1 << 3;
    public static final int FLAG_POWER_DOCK_COLLISION = 1 << 4;

    // fast access arrays
    public Array<StorageStructure> storages;

    public EnumSet<ItemType> minerals;

    Array<PooledEffect> pfxBelow, pfxAbove;

    final Object chunkLock = new Object();
    final Object pfxLock = new Object();
    final Object loudnessLock = new Object();

    public int width;
    public int height;

    int chunksH;
    final int index;

    TileType defaultTile;

    float lastLoudnessCalculation;

    float[] loudness, initialLoudness, intermediateLoudness;
    boolean updateLoudnessFlag;
    static float[] kernel;
    static int kernelSize = 4; // 2 * kernelSize + 1 = final size
    static float[] zeros;

    public Array<VeinDebug> veinDebugs = new Array<>();

    boolean fromLoading;

    boolean initialized;

    public boolean fake;

    public final Bounds dirtyBounds = new Bounds();
    public final Bounds lastBounds = new Bounds();
    public final Bounds pendingBounds = new Bounds();

    public Layer(int index, int initialWidth, int initialHeight, TileType defaultTile, boolean initChunks, boolean initGL) {
        this.width = initialWidth;
        this.height = initialHeight;
        this.defaultTile = defaultTile;
        this.index = index;

        flags = new byte[initialWidth * initialHeight];

        storages = new Array<>();
        minerals = EnumSet.noneOf(ItemType.class);

        chunks = new Chunk[(initialWidth / Const.CHUNK_SIZE) * (initialHeight / Const.CHUNK_SIZE)];

        pfxAbove = new Array<>(false, 100);
        pfxBelow = new Array<>(false, 100);

        if (initChunks) {
            initChunks();
            for (int i = 0; i < width; i += Const.CHUNK_SIZE) {
                for (int j = 0; j < height; j += Const.CHUNK_SIZE) {
                    Chunk c = chunks[i / Const.CHUNK_SIZE * chunksH + j / Const.CHUNK_SIZE];
                    c.setFrameBuffer(Quarry.Q.chunkFBOs[i / Const.CHUNK_SIZE * chunksH + j / Const.CHUNK_SIZE]);
                }
            }
        }
    }

    @Override
    public void onChangeEvent(Event<Layer> event) {
        if (event.getData() != null && Math.abs(event.getData().getIndex() - getIndex()) > 3) {
            dispose();
        }
    }

    public void initChunks() {
        chunksH = (int) Math.ceil(height / Const.CHUNK_SIZE);

        for (int i = 0; i < width; i += Const.CHUNK_SIZE) {
            for (int j = 0; j < height; j += Const.CHUNK_SIZE) {
                Chunk c = new Chunk((i / Const.CHUNK_SIZE), (j / Const.CHUNK_SIZE), this);
                chunks[i / Const.CHUNK_SIZE * chunksH + j / Const.CHUNK_SIZE] = c;
            }
        }
    }

    public void addParticleEffect(PooledEffect e, boolean below) {
        synchronized (pfxLock) {
            if (below) pfxBelow.add(e);
            else pfxAbove.add(e);
        }
    }

    public void removeParticleEffect(PooledEffect e, boolean below) {
        synchronized (pfxLock) {
            boolean removed = false;
            if (below) removed = pfxBelow.removeValue(e, true);
            else removed = pfxAbove.removeValue(e, true);

            if (removed) e.free();
        }
    }

    public void updateLoudness(double deltaTime) {
        lastLoudnessCalculation -= deltaTime;
        if (lastLoudnessCalculation <= 0 || updateLoudnessFlag) {
            synchronized (loudnessLock) {
                if (loudness == null) {
                    loudness = new float[width * height];
                    intermediateLoudness = new float[width * height];
                    initialLoudness = new float[width * height];
                    if (zeros == null) {
                        zeros = new float[width * height];
                        float mean = 0;
                        float sigma = 1;

                        kernel = new float[2 * kernelSize + 1];

                        // calculate gaussian weights
                        float sum = 0;
                        for (int j = 0; j < kernel.length; j++) {
                            int x = j - kernelSize;
                            float val = (float) Math.exp(-0.5 * (Math.pow((x - mean) / sigma, 2)));
                            kernel[j] = val;
                            sum += val;
                        }
                        // normalize
                        for (int j = 0; j < kernel.length; j++) {
                            kernel[j] /= sum;
                        }

                    }
                }

                for (Chunk c : getChunks()) {
                    if (c != null && c.isInit()) {
                        for (Structure<?> s : c.getStructures()) {
                            float l = s.getLoudness();
                            for (int i = 0; i < s.getWidth(); i++) {
                                for (int j = 0; j < s.getHeight(); j++) {
                                    initialLoudness[(s.x + i) * height + (s.y + j)] = l;
                                }
                            }
                        }
                    }
                }

                // column by column
                for (int i = 0; i < width; i++) {
                    for (int j = 0; j < height; j++) {
                        float sum = 0;
                        for (int k = -kernelSize; k <= kernelSize; k++) {
                            if (j + k < 0 || j + k >= height) continue;
                            sum += initialLoudness[i * height + j + k] * kernel[k + kernelSize];
                        }
                        intermediateLoudness[i * height + j] = sum;
                    }
                }

                // row by row
                for (int i = 0; i < height; i++) {
                    for (int j = 0; j < width; j++) {
                        float sum = 0;
                        for (int k = -kernelSize; k <= kernelSize; k++) {
                            if (j + k < 0 || j + k >= width) continue;
                            sum += intermediateLoudness[(j + k) * height + i] * kernel[k + kernelSize];
                        }

                        // clamp down
                        loudness[j * height + i] = Math.min(1, sum / Const.MAX_LOUDNESS);
                    }
                }

                lastLoudnessCalculation = 1;
                updateLoudnessFlag = false;
            }
        }
    }

    long timeSum = 0;
    long last = 0;

    public void update(double deltaTime, int gameSpeed) {
        synchronized (chunkLock) {
            for (Chunk c : chunks)
                c.update(deltaTime, gameSpeed, dirtyBounds);
            for (Chunk c : chunks)
                c.postUpdate(dirtyBounds);
        }

        synchronized (pfxLock) {
            for (int i = pfxBelow.size - 1; i >= 0; i--) {
                PooledEffect effect = pfxBelow.get(i);
                effect.update((float) deltaTime);
                if (effect.isComplete()) {
                    effect.free();
                    pfxBelow.removeIndex(i);
                }
            }

            for (int i = pfxAbove.size - 1; i >= 0; i--) {
                PooledEffect effect = pfxAbove.get(i);
                effect.update((float) deltaTime);
                if (effect.isComplete()) {
                    effect.free();
                    pfxAbove.removeIndex(i);
                }
            }
        }

        if (gameSpeed > 0 && Game.G.layer == this) {
            updateLoudness(deltaTime);
        }
    }

    /**
     * Only called for non visible chunks
     */
    public void postUpdate() {
        if (!dirtyBounds.isEmpty() || fromLoading) {
            synchronized (loudnessLock) {
                updateLoudnessFlag = true;
            }

            lastBounds.set(dirtyBounds);

            pendingBounds.add(dirtyBounds);

            dirtyBounds.clear();
            fromLoading = false;
        }
    }

    public void draw(OrthographicCamera cam, OrthographicCamera fboCam, Viewport viewport,
            Batch batch, DepthSpriter spriter, ShapeRenderer shaper, SpriterDelegateBatch delegateBatch) {
        if (!initialized) {
            Game.G.layerChangeNotifier.addListener(this);
            initialized = true;
        }
        synchronized (chunkLock) {
            for (Chunk c : chunks) {
                if (c.isInBounds(cam, true) || dirtyBounds.intersects(c.ax, c.ay, Const.CHUNK_SIZE, Const.CHUNK_SIZE)) {
                    c.draw(cam, fboCam, viewport, batch);
                }
            }
        }

        synchronized (pfxLock) {
            for (int i = pfxBelow.size - 1; i >= 0; i--) {
                PooledEffect effect = pfxBelow.get(i);
                effect.draw(batch);
            }
        }

        batch.end();

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        spriter.setProjectionMatrix(cam.combined);
        spriter.begin(false);

        synchronized (chunkLock) {
            for (Chunk c : chunks) {
                if (c.isInBounds(cam, false) || dirtyBounds.intersects(c.ax, c.ay, Const.CHUNK_SIZE, Const.CHUNK_SIZE)) {
                    c.drawStructures(spriter, dirtyBounds);
                }
            }
        }

        if (!dirtyBounds.isEmpty() || fromLoading) {
            synchronized (loudnessLock) {
                updateLoudnessFlag = true;
            }

            lastBounds.set(dirtyBounds);
            dirtyBounds.clear();
            fromLoading = false;
        }

        spriter.end();

        batch.begin();

        synchronized (pfxLock) {
            for (int i = pfxAbove.size - 1; i >= 0; i--) {
                PooledEffect effect = pfxAbove.get(i);
                effect.draw(batch);
            }
        }

        batch.end();

        Gdx.gl.glDepthMask(true);
        Gdx.gl.glDepthFunc(GL20.GL_LESS);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        spriter.setProjectionMatrix(cam.combined);
        spriter.begin(true);

        shaper.setProjectionMatrix(cam.combined);
        shaper.begin(ShapeType.Filled);
        synchronized (chunkLock) {
            for (Chunk c : chunks) {
                if (c.isInBounds(cam, false)) {
                    c.drawFrameStructures(spriter, shaper, delegateBatch);
                }
            }
        }

        spriter.draw();
        spriter.end();

        shaper.flush();

        if (Game.DRAW_DEBUG && !lastBounds.isEmpty()) {
            shaper.setColor(0, 0, 1, 0.2f);
            shaper.rect(lastBounds.getX() * Const.TILE_SIZE, lastBounds.getY() * Const.TILE_SIZE, lastBounds.getWidth() * Const.TILE_SIZE, lastBounds.getHeight() * Const.TILE_SIZE);
        }

        shaper.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        if (Game.DRAW_DEBUG) {
            shaper.begin(ShapeType.Line);
            shaper.setColor(Color.GREEN);

            for (VeinDebug vd : veinDebugs) {
                shaper.circle(vd.x * Const.TILE_SIZE, vd.y * Const.TILE_SIZE, vd.radius * Const.TILE_SIZE);
            }

            shaper.end();
        }

        batch.begin();
    }

    public Chunk getChunk(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        synchronized (chunkLock) {
            Chunk c = chunks[(x / Const.CHUNK_SIZE) * chunksH + y / Const.CHUNK_SIZE];

            if (c == null)
                return null;

            if (!c.dataSet)
                c.init();

            return c;
        }
    }

    public Structure<?> getStructure(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return null;
        }
        synchronized (chunkLock) {
            Chunk c = getChunk(x, y);
            if (c == null) return null;
            return c.getStructure(x - c.ax, y - c.ay);
        }
    }

    public Structure<?> getStructure(int i) {
        return getStructure(i / height, i % height);
    }

    public CopperCable getCable(int i) {
        return getCable(i / height, i % height);
    }

    public CopperCable getCable(int x, int y) {
        synchronized (chunkLock) {
            Chunk c = getChunk(x, y);
            if (c == null) return null;
            return c.getCable(x - c.ax, y - c.ay);
        }
    }

    public Structure<?> getAny(int i, boolean structuresFirst) {
        if (structuresFirst) {
            Structure<?> s = getStructure(i);
            if (s != null) return s;
            return getCable(i);
        } else {
            CopperCable c = getCable(i);
            if (c != null) return c;
            return getStructure(i);
        }
    }

    public Structure<?> getAny(int x, int y, boolean structuresFirst) {
        if (structuresFirst) {
            Structure<?> s = getStructure(x, y);
            if (s != null) return s;
            return getCable(x, y);
        } else {
            CopperCable c = getCable(x, y);
            if (c != null) return c;
            return getStructure(x, y);
        }
    }

    public boolean addStructure(Structure<?> s) {
        if (s.x < 0 || s.y < 0 || s.x >= width || s.y >= height) {
            return false;
        }

        Chunk c = getChunk(s.x, s.y);
        if (c == null) return false;

        if (s instanceof CopperCable) {
            if (getCable(s.x, s.y) != null) return false;
        }

        return c.addStructure(s, false);
    }

    public boolean removeStructure(int x, int y) {
        Structure<?> s = getStructure(x, y);
        if (s == null) return false;

        return removeStructure(s);
    }

    public boolean removeStructure(Structure<?> s) {
        Chunk c = getChunk(s.x, s.y);
        if (c == null) return false;
        boolean res = c.removeStructure(s.x - c.ax, s.y - c.ay);

        return res;
    }

    public boolean removeCable(int x, int y) {
        Chunk c = getChunk(x, y);
        if (c == null) return false;
        return c.removeCable(x - c.ax, y - c.ay);
    }

    public boolean removeCable(CopperCable s) {
        return removeCable(s.x, s.y);
    }

    public void setItemNotification(int x, int y) {
        Structure<?> s = getStructure(x, y);
        if (s instanceof Conveyor) {
            ((Conveyor) s).setItemNotification();
        }

    }

    public boolean isPowerDockCollision(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return true;
        }
        return (flags[x * height + y] & FLAG_POWER_DOCK_COLLISION) != 0;
    }

    public Chunk[] getChunks() {
        return chunks;
    }

    protected void setCollision(Structure<?> s, boolean colliding) {
        for (int i = 0; i < s.getWidth(); i++) {
            int j = (s.x + i) * height + s.y;
            if (s instanceof ITube) {
                if (colliding) {
                    flags[j] |= FLAG_TUBE_COLLISION;
                } else {
                    flags[j] &= ~FLAG_TUBE_COLLISION;
                }
            } else {
                if (colliding) {
                    for (int k = 0; k < s.getHeight(); k++)
                        flags[j + k] |= FLAG_STRUCTURE_COLLISION;
                } else {
                    for (int k = 0; k < s.getHeight(); k++)
                        flags[j + k] &= ~FLAG_STRUCTURE_COLLISION;
                }
            }
        }
        for (Dock d : s.getDocks()) {
            if (d.type == DockType.Power) {
                if (colliding) {
                    flags[(s.x + d.x) * height + s.y + d.y] |= FLAG_POWER_DOCK_COLLISION;
                } else {
                    flags[(s.x + d.x) * height + s.y + d.y] &= ~FLAG_POWER_DOCK_COLLISION;
                }
            } else if (d.type == DockType.BigPower) {
                if (colliding) {
                    flags[(s.x + d.x) * height + s.y + d.y] |= FLAG_POWER_DOCK_COLLISION;
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] |= FLAG_POWER_DOCK_COLLISION | FLAG_STRUCTURE_COLLISION;
                } else {
                    flags[(s.x + d.x) * height + s.y + d.y] &= ~FLAG_POWER_DOCK_COLLISION;
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] &= ~(FLAG_POWER_DOCK_COLLISION | FLAG_STRUCTURE_COLLISION);
                }
            } else if (d.type == DockType.ItemIn || d.type == DockType.ItemOut) {
                if (colliding) {
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] |= FLAG_ITEM_DOCK_COLLISION;
                } else {
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] &= ~FLAG_ITEM_DOCK_COLLISION;
                }
            } else if (d.type == DockType.FluidIn || d.type == DockType.FluidOut) {
                if (colliding) {
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] |= FLAG_FLUID_DOCK_COLLISION;
                } else {
                    flags[(s.x + d.x + d.dir.dx) * height + s.y + d.y + d.dir.dy] &= ~FLAG_FLUID_DOCK_COLLISION;
                }
            }
        }
    }

    public boolean isCollision(int x, int y, boolean checkItemDocks, boolean checkFluidDocks) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return true;
        }
        byte flag = flags[x * height + y];
        byte query = FLAG_STRUCTURE_COLLISION | FLAG_TUBE_COLLISION;
        if (checkItemDocks) query |= FLAG_ITEM_DOCK_COLLISION;
        if (checkFluidDocks) query |= FLAG_FLUID_DOCK_COLLISION;

        return (flag & query) != 0;
    }

    public byte getFlags(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }
        return flags[x * height + y];
    }

    public boolean isColliding(Structure<?> s, boolean checkItemDocks, boolean checkFluidDocks) {
        if (s.x < 0)
            return false;

        for (int i = 0; i < s.getWidth(); i++) {
            for (int j = 0; j < s.getHeight(); j++) {
                if (s.x + i < 0 || s.x + i >= width) return true;
                if (s.y + j < 0 || s.y + j >= height) return true;

                if (isCollision(s.x + i, s.y + j, checkItemDocks, checkFluidDocks)) {
                    return true;
                }
            }
        }

        for (Dock d : s.getDocks()) {
            int sx = s.x + d.x + d.dir.dx;
            int sy = s.y + d.y + d.dir.dy;

            if (d.type == DockType.StackIn || d.type == DockType.StackOut) {
                continue;
            }

            if (sx < 0 || sy < 0 || sx >= width || sy >= height) {
                return true;
            }

            if (d.type == DockType.Power) {
                if ((flags[(sx) * height + sy] & FLAG_POWER_DOCK_COLLISION) != 0) {
                    return true;
                }
            } else if (d.type == DockType.BigPower) {
                if ((flags[(sx) * height + sy] & FLAG_POWER_DOCK_COLLISION) != 0 || (flags[(sx) * height + sy] & FLAG_STRUCTURE_COLLISION) != 0) {
                    return true;
                }
            } else if ((flags[(sx) * height + sy] & FLAG_STRUCTURE_COLLISION) != 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isInFogOfWar(int x, int y) {
        return (getMeta(x, y) & TileMeta.FOG_OF_WAR) != 0;
    }

    public boolean isNotInFogOfWar(Structure<?> s, boolean partly) {
        if (s.x < 0)
            return false;

        for (int i = 0; i < s.getWidth(); i++) {
            for (int j = 0; j < s.getHeight(); j++) {
                if ((getMeta(s.x + i, s.y + j) & TileMeta.FOG_OF_WAR) != 0) {
                    if (!partly) return false;
                } else if (partly) {
                    return true;
                }
            }
        }

        return true;
    }

    public synchronized TileType get(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            //            System.out.println("Get out of bounds: " + x + ":" + y);
            return TileType.Air;
        }
        Chunk c = getChunk(x, y);
        if (c == null)
            return TileType.Air;
        int addr = (x - c.ax) * Const.CHUNK_SIZE + (y - c.ay);

        return Tile.tiles[(byte) (c.data[addr] & 0xff)];
    }

    public synchronized int getMeta(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return TileMeta.NO_META;
        }

        Chunk c = getChunk(x, y);
        if (c == null)
            return TileMeta.NO_META;
        int addr = (x - c.ax) * Const.CHUNK_SIZE + (y - c.ay);

        return (c.data[addr] >> 8) & 0xff;
    }

    public synchronized void set(int x, int y, TileType tile) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            //            System.out.println("Set out of bounds: " + x + ":" + y);
            return;
        }

        Chunk c = getChunk(x, y);

        if (c == null)
            return;

        int addr = (x - c.ax) * Const.CHUNK_SIZE + (y - c.ay);

        if (tile.itemDrop != null) {
            minerals.add(tile.itemDrop);
        }

        c.data[addr] = tile.value;
        byte meta = tile.meta;
        if ((tile.meta & Tile.TileMeta.ALT_TEX) == Tile.TileMeta.ALT_TEX && Math.random() >= 0.5) {
            meta |= TileMeta.ALT_TEX;
        }

        if ((tile.meta & Tile.TileMeta.ROT_TEX) == Tile.TileMeta.ROT_TEX) {
            float rot = (float) Math.random();

            if (rot >= 0.75f) {
                meta |= TileMeta.ROT_TEX | TileMeta.ROT_TEX_270;
            } else if (rot >= 0.5f) {
                meta |= TileMeta.ROT_TEX | TileMeta.ROT_TEX_180;
            } else if (rot >= 0.25f) {
                meta |= TileMeta.ROT_TEX | TileMeta.ROT_TEX_90;
            }
        }
        c.data[addr] |= meta << 8;

        c.dirty = true;
        markSurroundingTilesAsDirty(x, y);
    }

    public synchronized void addMeta(int x, int y, int metaFlag) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            //            System.out.println("addMeta out of bounds: " + x + ":" + y);
            return;
        }

        Chunk c = getChunk(x, y);
        if (c == null)
            return;
        int addr = (x - c.ax) * Const.CHUNK_SIZE + (y - c.ay);

        c.data[addr] |= metaFlag << 8;
        c.dirty = true;
        markSurroundingTilesAsDirty(x, y);
    }

    public synchronized void removeMeta(int x, int y, int metaFlag) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            //            System.out.println("removeMeta out of bounds: " + x + ":" + y);
            return;
        }

        Chunk c = getChunk(x, y);
        if (c == null)
            return;
        int addr = (x - c.ax) * Const.CHUNK_SIZE + (y - c.ay);

        c.data[addr] &= ~(metaFlag << 8);
        c.dirty = true;
        markSurroundingTilesAsDirty(x, y);
    }

    protected void markSurroundingTilesAsDirty(int x, int y) {
        for (Direction d : Direction.values) {
            Chunk c = getChunk(x + d.dx, y + d.dy);
            if (c != null) c.dirty = true;
        }
    }

    public synchronized boolean addItemEntity(ItemType value, int x, int y, int lastSlot, int slot, Direction dir, Structure<?> source) {
        Structure<?> s = getStructure(x, y);
        if (!(s instanceof Conveyor)) return false;

        return ((Conveyor) s).addItemEntity(value, lastSlot, slot, dir, source);
    }

    public static int getStartingSlot(Direction dir) {
        switch (dir) {
            case East:
                return Const.ITEMS_PER_CONVEYOR;
            case North:
                return 0;
            case South:
                return Const.ITEMS_PER_CONVEYOR - 1;
            case West:
                return 2 * Const.ITEMS_PER_CONVEYOR - 1;
        }

        return 0;
    }

    public boolean isItemSlotFree(int x, int y, int slot) {
        Structure<?> s = getStructure(x, y);
        if (s instanceof Conveyor) {
            return ((Conveyor) s).isItemSlotFree(slot);
        } else {
            return false;
        }
    }

    public synchronized boolean addItemEntity(ItemType value, Structure<?> source, Direction dir, Structure<?> originalSource) {
        Structure<?> s = getStructure(source.x + dir.dx, source.y + dir.dy);
        Direction gotoDir = dir;

        int startingSlot = getStartingSlot(dir);

        if (s instanceof Conveyor) {
            gotoDir = ((Conveyor) s).getNextDirection(value, dir, Const.Z_ITEMS, startingSlot);
        } else {
            return false;
        }

        return addItemEntity(value, source.x + dir.dx, source.y + dir.dy,
                getStartingSlot(dir.inv()), startingSlot, gotoDir, originalSource == null ? source : originalSource);
    }

    public synchronized boolean addItemEntity(ItemType value, Structure<?> source, Dock dock, Structure<?> originalSource) {
        Structure<?> s = getStructure(source.x + dock.x + dock.dir.dx, source.y + dock.y + dock.dir.dy);
        Direction gotoDir = dock.dir;

        int startingSlot = getStartingSlot(dock.dir);

        if (s instanceof Conveyor) {
            gotoDir = ((Conveyor) s).getNextDirection(value, dock.dir, Const.Z_ITEMS, startingSlot);
        } else {
            return false;
        }

        return addItemEntity(value, source.x + dock.x + dock.dir.dx, source.y + dock.y + dock.dir.dy,
                getStartingSlot(dock.dir.inv()), startingSlot, gotoDir, originalSource == null ? source : originalSource);
    }

    public int getItemCount() {
        synchronized (chunkLock) {
            int sum = 0;
            for (Chunk c : chunks)
                if (c != null && c.isInit()) sum += c.getItemCount();
            return sum;
        }
    }

    public synchronized float getLoudness(int x, int y) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return 0;
        }

        synchronized (loudnessLock) {
            if (loudness == null) {
                updateLoudnessFlag = true;
                return 0;
            }

            return loudness[x * height + y];
        }
    }

    public synchronized float[] getLoudness() {
        if (loudness == null) {
            updateLoudnessFlag = true;
            return null;
        }

        return loudness;
    }

    @Override
    public void dispose() {
        synchronized (chunkLock) {
            for (Chunk c : chunks)
                c.dispose();
        }

        initialized = false;
        Game.G.layerChangeNotifier.removeListener(this);
    }

    public int getIndex() {
        return index;
    }

    public int getStructureCount() {
        synchronized (chunkLock) {
            int sum = 0;
            for (Chunk c : getChunks())
                if (c != null && c.isInit()) sum += c.getStructures().size;
            return sum;
        }
    }

    @Override
    public void save(Builder b) {
        b.Compound()
                .Byte("chunksW", (byte) chunksH)
                .Int("width", width)
                .Int("height", height)
                .Byte("defaultTile", defaultTile.value);

        synchronized (chunkLock) {
            b.List("Chunks", TagType.Compound);
            for (Chunk c : chunks) {
                c.save(b);
            }
            b.End();
        }

        b.End();
    }

    public void postAllLayersLoad(boolean firstCycle) {
        // call post load to re-fetch references that structures might need
        synchronized (chunkLock) {
            for (Chunk c : chunks)
                c.postAllLayersLoad(firstCycle);
        }
    }

    public void stopSfx() {
        synchronized (chunkLock) {
            for (Chunk c : chunks)
                c.stopSfx();
        }
    }

    public static Layer load(int index, CompoundTag tag) throws NBTException {
        int chunksW = tag.Byte("chunksW");
        Layer l = new Layer(index, tag.Int("width"), tag.Int("height"), Tile.tiles[tag.Byte("defaultTile")], false, false);
        l.chunksH = chunksW;
        l.fromLoading = true;

        Array<Pair<Chunk, CompoundTag>> pairs = new Array<>();

        for (Tag t : tag.List("Chunks", TagType.Compound).data) {
            try {
                Chunk c = Chunk.load((CompoundTag) t, l);
                l.chunks[c.x * chunksW + c.y] = c;
                c.setFrameBuffer(Quarry.Q.chunkFBOs[c.x * chunksW + c.y]);
                pairs.add(new Pair<>(c, (CompoundTag) t));
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }

        // analyze minerals
        for (int i = 0; i < l.width; i++) {
            for (int j = 0; j < l.height; j++) {
                ItemType t = l.get(i, j).itemDrop;
                if (t != null)
                    l.minerals.add(t);
            }
        }
        // load structures after all chunks are loaded for terrain lookup and stuff
        for (Pair<Chunk, CompoundTag> p : pairs) {
            try {
                p.getKey().loadStructures(p.getVal());
            } catch (NBTException e) {
                Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
            }
        }

        // call post load to re-fetch references that structures might need
        for (Chunk c : l.chunks)
            c.postLoad();

        return l;
    }

    public boolean hasHoppersAttached(Structure<?> s) {
        for (Dock d : s.getDocks()) {
            Structure<?> q = getStructure(s.x + d.x + d.dir.dx, s.y + d.y + d.dir.dy);
            if (q instanceof Hopper && ((Hopper) q).getDirection() == d.dir.inv()) {
                return true;
            }
        }

        return false;
    }

    public int removeAttachedHoppers(Structure<?> s) {
        int count = 0;
        for (Dock d : s.getDocks()) {
            Structure<?> q = getStructure(s.x + d.x + d.dir.dx, s.y + d.y + d.dir.dy);
            if (q instanceof Hopper && ((Hopper) q).getDirection() == d.dir.inv()) {
                removeStructure(q);
                count++;
            }
        }

        return count;
    }

}
