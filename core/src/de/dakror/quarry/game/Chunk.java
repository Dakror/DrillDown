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

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.IntMap.Entry;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.dakror.common.libgdx.PlatformInterface;
import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.io.NBT.Tag;
import de.dakror.common.libgdx.io.NBT.TagType;
import de.dakror.common.libgdx.render.DepthSpriter;
import de.dakror.common.libgdx.render.MeshBuilderDelegate;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Tile.TileMeta;
import de.dakror.quarry.game.Tile.TileType;
import de.dakror.quarry.game.power.PowerNetwork;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.structure.base.Dock;
import de.dakror.quarry.structure.base.StorageStructure;
import de.dakror.quarry.structure.base.Structure;
import de.dakror.quarry.structure.base.StructureType;
import de.dakror.quarry.structure.logistics.Conveyor;
import de.dakror.quarry.structure.power.CopperCable;
import de.dakror.quarry.util.Bounds;
import de.dakror.quarry.util.Bounds.Flags;
import de.dakror.quarry.util.FBOable;
import de.dakror.quarry.util.Savable;
import de.dakror.quarry.util.SpriterDelegateBatch;

/**
 * @author Maximilian Stark | Dakror
 */
public class Chunk implements Disposable, FBOable, Savable {
    public static final float WHITE = Color.WHITE.toFloatBits();

    static final TextureRegion overhang = Quarry.Q.atlas.findRegion("overhang");
    static final TextureRegion fogOverhang = Quarry.Q.atlas.findRegion("overhang_fog");
    static final TextureRegion fogOverhangInner = Quarry.Q.atlas.findRegion("overhang_fog_inner");
    static final TextureRegion fogOfWar = Quarry.Q.atlas.findRegion("tile_fog");

    // (base, (blend, tex))
    static final int COR = 0;
    static final int INN = 1;
    static final int MID = 2;
    static final int HOL = 3;
    static final HashMap<TileType, HashMap<TileType, TextureRegion[]>> texLookup = new HashMap<>();

    final Object structLock = new Object();

    // lower byte data, upper byte meta
    short[] data;
    int x, y;

    // obtained from spatial, incorporates the actual bounding size of the chunk
    int maxX, maxY;

    // x,y * Const.CHUNK_SIZE
    int ax, ay;

    Layer layer;

    public boolean dirty;
    boolean dataSet, glSet;

    IntMap<Structure<?>> spatial;
    Array<Structure<?>> structures;
    Array<Conveyor> conveyors;
    IntMap<CopperCable> cables;

    MeshBuilderDelegate builder;

    FrameBuffer fbo;

    private boolean inBounds;

    public Chunk(int x, int y, Layer layer) {
        this.x = x;
        this.y = y;
        this.ax = x * Const.CHUNK_SIZE;
        this.ay = y * Const.CHUNK_SIZE;
        this.maxX = ax + Const.CHUNK_SIZE;
        this.maxY = ay + Const.CHUNK_SIZE;
        this.layer = layer;

        dirty = true;
    }

    public void init() {
        if (dataSet)
            return;

        this.data = new short[Const.CHUNK_SIZE * Const.CHUNK_SIZE];

        if (layer.getIndex() == 0) {
            for (int i = 0; i < data.length; i++)
                data[i] = (short) (layer.defaultTile.value | (layer.defaultTile.meta << 8));
        } else {
            short val = (short) (layer.defaultTile.value | ((layer.defaultTile.meta | Tile.TileMeta.FOG_OF_WAR) << 8));

            for (int i = 0; i < data.length; i++)
                data[i] = val;
        }

        spatial = new IntMap<>();
        structures = new Array<>(false, 100, Structure.class);
        conveyors = new Array<>(false, 100, Conveyor.class);
        cables = new IntMap<>(100);

        dataSet = true;
    }

    public boolean isInit() {
        return dataSet;
    }

    private void initGL() {
        if (glSet) return;

        VertexAttributes attributes = new VertexAttributes(new VertexAttribute(Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));

        builder = new MeshBuilderDelegate(attributes, Quarry.Q.atlas.getTextures().first());

        glSet = true;
    }

    public void update(double deltaTime, int gameSpeed, Bounds dirtyBounds) {
        if (!dataSet) return;

        if (dirty && layer == Game.G.layer && Game.G.activeStructure != null) {
            Game.G.camControl.updateTrail();
            Game.G.camControl.updateActiveElementPlaceable();
        }

        boolean hasWidth = dirtyBounds.getWidth() > 0;

        synchronized (structLock) {
            for (Structure<?> st : structures.items) {
                if (st == null) break;
                if (hasWidth || st.getSchema().type != StructureType.Conveyor)
                    st.update(deltaTime, gameSpeed * st.getSpeedScale(), dirtyBounds);
            }

            // this update items has to happen after all updates because the normal updates will set item notifications
            for (Conveyor st : conveyors.items) {
                if (st == null) break;
                st.updateItems(deltaTime, gameSpeed, true);
            }

            if (dirtyBounds.hasFlag(Bounds.Flags.CABLE) || hasWidth) {
                for (CopperCable c : cables.values()) {
                    c.update(deltaTime, gameSpeed, dirtyBounds);
                }
            }
        }
    }

    public void postUpdate(Bounds dirtyBounds) {
        if (!dataSet) return;

        synchronized (structLock) {
            for (Structure<?> st : structures.items) {
                if (st == null) break;
                st.postUpdate(dirtyBounds);
            }

            if (dirtyBounds.hasAnyFlag(Bounds.Flags.CABLE | Bounds.Flags.POWERNODE)) {
                for (Structure<?> st : structures.items) {
                    if (st == null) break;
                    st.reloadPowerNetwork();
                }
            }

        }
    }

    public boolean isInBounds(OrthographicCamera cam, boolean compute) {
        if (compute) {
            inBounds = cam.position.x < maxX * Const.TILE_SIZE + cam.viewportWidth / 2 * cam.zoom
                    && cam.position.y < maxY * Const.TILE_SIZE + cam.viewportHeight / 2 * cam.zoom
                    && cam.position.x + cam.viewportWidth / 2 * cam.zoom > x * Const.CHUNK_FULL_SIZE
                    && cam.position.y + cam.viewportHeight / 2 * cam.zoom > y * Const.CHUNK_FULL_SIZE;
        }

        return inBounds;
    }

    public boolean isInDirtyBounds(Bounds dirtyBounds) {
        return dirtyBounds.intersects(ax, ay, Const.CHUNK_SIZE, Const.CHUNK_SIZE);
    }

    private void drawBaseTile(TileType t, int x, int y, Batch batch) {
        if (texLookup.isEmpty()) initTexCache();

        batch.draw(t.tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);

        // Make smooth transition to dirt
        TileType[][] neighbors = new TileType[3][3];

        boolean any = false;

        for (int j = -1; j < 2; j++) {
            for (int k = -1; k < 2; k++) {
                if (!(j == 0 && k == 0)) {
                    TileType u = layer.get(ax + x + j, ay + y + k);
                    if (u.base != null) u = u.base;
                    if ((u.meta & TileMeta.BLENDING) == TileMeta.BLENDING && u != t) {
                        neighbors[j + 1][k + 1] = u;
                        any = true;
                    }
                }
            }
        }

        if (any) {
            // if a 1x1 hole, fill it
            if (neighbors[1][0] != null && neighbors[1][2] != null && neighbors[0][1] != null && neighbors[2][1] != null) {
                layer.set(ax + x, ay + y, neighbors[1][0]);
                batch.draw(neighbors[1][0].tex, x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE, Const.TILE_SIZE);
                return;
            }

            // corners
            if (neighbors[0][0] != null && neighbors[1][0] == null && neighbors[0][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[0][0])[COR], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
            }
            if (neighbors[0][2] != null && neighbors[1][2] == null && neighbors[0][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[0][2])[COR], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, -90);
            }
            if (neighbors[2][2] != null && neighbors[1][2] == null && neighbors[2][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[2][2])[COR], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
            }
            if (neighbors[2][0] != null && neighbors[1][0] == null && neighbors[2][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[2][0])[COR], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
            }

            // middle pieces
            if (neighbors[1][0] != null && neighbors[0][1] == null && neighbors[2][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[1][0])[MID], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
            }
            if (neighbors[0][1] != null && neighbors[1][0] == null && neighbors[1][2] == null) {
                batch.draw(texLookup.get(t).get(neighbors[0][1])[MID], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, -90);
            }
            if (neighbors[1][2] != null && neighbors[0][1] == null && neighbors[2][1] == null) {
                batch.draw(texLookup.get(t).get(neighbors[1][2])[MID], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
            }
            if (neighbors[2][1] != null && neighbors[1][0] == null && neighbors[1][2] == null) {
                batch.draw(texLookup.get(t).get(neighbors[2][1])[MID], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
            }

            /**
             * 0,2 1,2 2,2
             * 0,1 1,1 2,1
             * 0,0 1,0 2,0
             */
            // holes vs inners
            if (neighbors[1][0] != null && neighbors[0][1] == neighbors[1][0] && neighbors[1][2] == neighbors[0][1]) {
                batch.draw(texLookup.get(t).get(neighbors[1][0])[HOL], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
            } else if (neighbors[0][1] != null && neighbors[1][2] == neighbors[0][1] && neighbors[2][1] == neighbors[0][1]) {
                batch.draw(texLookup.get(t).get(neighbors[0][1])[HOL], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, -90);
            } else if (neighbors[1][0] != null && neighbors[1][2] == neighbors[1][0] && neighbors[2][1] == neighbors[1][0]) {
                batch.draw(texLookup.get(t).get(neighbors[1][0])[HOL], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
            } else if (neighbors[0][1] != null && neighbors[2][1] == neighbors[0][1] && neighbors[1][0] == neighbors[0][1]) {
                batch.draw(texLookup.get(t).get(neighbors[0][1])[HOL], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
            } else {
                if (neighbors[1][0] != null && neighbors[0][1] == neighbors[1][0]) {
                    batch.draw(texLookup.get(t).get(neighbors[1][0])[INN], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
                }
                if (neighbors[0][1] != null && neighbors[1][2] == neighbors[0][1]) {
                    batch.draw(texLookup.get(t).get(neighbors[0][1])[INN], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, -90);
                }
                if (neighbors[2][1] != null && neighbors[1][2] == neighbors[2][1]) {
                    batch.draw(texLookup.get(t).get(neighbors[2][1])[INN], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
                }
                if (neighbors[1][0] != null && neighbors[2][1] == neighbors[1][0]) {
                    batch.draw(texLookup.get(t).get(neighbors[1][0])[INN], x * Const.TILE_SIZE, y * Const.TILE_SIZE, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
                }
            }
        }
    }

    public void setFrameBuffer(FrameBuffer fbo) {
        this.fbo = fbo;
    }

    @Override
    public void draw(OrthographicCamera cam, OrthographicCamera fboCam, Viewport viewport, Batch batch) {
        if (dirty) {
            if (!dataSet) {
                init();
            }

            batch.end();
            fbo.begin();
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

            batch.setProjectionMatrix(fboCam.combined);
            batch.begin();

            // set dirty before, so that spontaneous terrain changes can cause a rerender
            dirty = false;

            for (int i = 0; i < data.length; i++) {
                byte b = (byte) (data[i] & 0xff);
                int m = data[i] >> 8;

                TileType t = Tile.tiles[b];

                if (t.name == null)
                    continue;

                int x = (i / Const.CHUNK_SIZE);
                int y = (i % Const.CHUNK_SIZE);
                int tx = x * Const.TILE_SIZE;
                int ty = y * Const.TILE_SIZE;

                // don't draw it
                if ((m & Tile.TileMeta.FOG_OF_WAR) == Tile.TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOfWar, tx, ty, Const.TILE_SIZE, Const.TILE_SIZE);
                    continue;
                }

                if (t.base != null) {
                    drawBaseTile(t.base, x, y, batch);
                }

                TextureRegion tex = t.tex;

                if ((m & Tile.TileMeta.ALT_TEX) == Tile.TileMeta.ALT_TEX) {
                    // Clear wrong meta flag
                    if ((t.meta & Tile.TileMeta.ALT_TEX) == 0) {
                        m &= ~Tile.TileMeta.ALT_TEX;
                    } else tex = t.texAlt;
                }

                if ((m & Tile.TileMeta.ROT_TEX) == Tile.TileMeta.ROT_TEX) {
                    if ((t.meta & Tile.TileMeta.ROT_TEX) == 0) {
                        m &= ~Tile.TileMeta.ROT_TEX;
                    } else {
                        int rot = 0;
                        if ((m & Tile.TileMeta.ROT_TEX_90) == Tile.TileMeta.ROT_TEX_90)
                            rot = 90;
                        else if ((m & Tile.TileMeta.ROT_TEX_180) == Tile.TileMeta.ROT_TEX_180)
                            rot = 180;
                        else if ((m & Tile.TileMeta.ROT_TEX_270) == Tile.TileMeta.ROT_TEX_270)
                            rot = 270;

                        batch.draw(tex, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, rot);
                    }
                } else if ((m & TileMeta.BASE_TILE) == TileMeta.BASE_TILE) {
                    drawBaseTile(t, x, y, batch);
                } else {
                    batch.draw(tex, tx, ty, Const.TILE_SIZE, Const.TILE_SIZE);
                }

                // do not need the out of bounds checks, since getMeta does that too

                // tile above is in FOW
                if ((layer.getMeta(ax + x, ay + y + 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhang, tx, ty, Const.TILE_SIZE, Const.TILE_SIZE);
                }
                // tile below is in FOW
                if ((layer.getMeta(ax + x, ay + y - 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
                }
                // tile left is in FOW
                if ((layer.getMeta(ax + x - 1, ay + y) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
                }
                // tile right is in FOW
                if ((layer.getMeta(ax + x + 1, ay + y) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 270);
                }

                // check corners
                // bottom left
                if ((layer.getMeta(ax + x - 1, ay + y - 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x - 1, ay + y) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x, ay + y - 1) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhangInner, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
                }

                // top left
                if ((layer.getMeta(ax + x - 1, ay + y + 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x - 1, ay + y) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x, ay + y + 1) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhangInner, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
                }

                // bottom right
                if ((layer.getMeta(ax + x + 1, ay + y - 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x + 1, ay + y) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x, ay + y - 1) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhangInner, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, -90);
                }

                // top right
                if ((layer.getMeta(ax + x + 1, ay + y + 1) & TileMeta.FOG_OF_WAR) == TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x + 1, ay + y) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR
                        && (layer.getMeta(ax + x, ay + y + 1) & TileMeta.FOG_OF_WAR) != TileMeta.FOG_OF_WAR) {
                    batch.draw(fogOverhangInner, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 0);
                }

                if (ay + y == layer.height - 1) {
                    batch.draw(overhang, tx, ty, Const.TILE_SIZE, Const.TILE_SIZE);
                }
                if (ay + y == 0) {
                    batch.draw(overhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 180);
                }
                if (ax + x == 0) {
                    batch.draw(overhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 90);
                }
                if (ax + x == layer.width - 1) {
                    batch.draw(overhang, tx, ty, Const.TILE_SIZE / 2, Const.TILE_SIZE / 2, Const.TILE_SIZE, Const.TILE_SIZE, 1, 1, 270);
                }
            }

            batch.end();

            fbo.end();

            fbo.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);

            if (viewport != null) viewport.apply();

            batch.setProjectionMatrix(cam.combined);
            batch.begin();
        }

        if (!dataSet) return;
        batch.draw(fbo.getColorBufferTexture(), x * Const.CHUNK_FULL_SIZE, y * Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE, Const.CHUNK_FULL_SIZE, 0, 0, 1, 1);
    }

    public void drawStructures(DepthSpriter spriter, Bounds dirtyBounds) {
        if (!dataSet) return;

        if (isInDirtyBounds(dirtyBounds) || !glSet) {
            if (!glSet) initGL();

            builder.begin();
            synchronized (structLock) {
                for (Structure<?> st : structures.items) {
                    if (st == null) break;
                    st.draw(builder);
                }
                for (Entry<CopperCable> st : cables.entries()) {
                    st.value.draw(builder);
                }
            }
            builder.end();
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Mesh mesh = builder.getMesh();
        if (mesh != null)
            mesh.render(spriter.getShader(), GL20.GL_TRIANGLES);
    }

    public void drawFrameStructures(DepthSpriter spriter, ShapeRenderer shaper, SpriterDelegateBatch pfxBatch) {
        if (!dataSet) return;

        synchronized (structLock) {
            for (Structure<?> st : structures.items) {
                if (st == null) break;
                st.drawFrame(spriter, shaper, pfxBatch);
            }
            if (Game.DRAW_DEBUG) {
                for (Entry<CopperCable> st : cables.entries()) {
                    st.value.drawFrame(spriter, shaper, pfxBatch);
                }
            }
        }
    }

    public Structure<?> getStructure(int x, int y) {
        if (!dataSet) return null;

        synchronized (structLock) {
            return spatial.get(x * Const.CHUNK_SIZE + y);
        }
    }

    public CopperCable getCable(int x, int y) {
        if (!dataSet) return null;

        synchronized (structLock) {
            return cables.get(x * Const.CHUNK_SIZE + y);
        }
    }

    public boolean removeCable(int x, int y) {
        if (!dataSet) return false;

        CopperCable c = cables.remove(x * Const.CHUNK_SIZE + y);
        if (c != null) {
            c.onDestroy();
            layer.dirtyBounds.add(c, Flags.DESTRUCTION);
            return true;
        }
        return false;
    }

    public boolean removeStructure(int x, int y) {
        if (!dataSet) return false;

        synchronized (structLock) {
            // has to be in this chunk, so direct access
            Structure<?> q = spatial.remove(x * Const.CHUNK_SIZE + y);

            if (q != null) {
                for (int i = 0; i < q.getWidth(); i++)
                    for (int j = 0; j < q.getHeight(); j++)
                        removeSpatial(q.x + i, q.y + j);

                structures.removeValue(q, true);

                // recalc bound box
                maxX = ax + Const.CHUNK_SIZE;
                maxY = ay + Const.CHUNK_SIZE;
                for (int i = 0; i < structures.size; i++) {
                    Structure<?> s = structures.get(i);
                    maxX = Math.max(maxX, s.x + s.getWidth());
                    maxY = Math.max(maxY, s.y + s.getHeight());
                }

                layer.setCollision(q, false);
                q.onDestroy();
                layer.dirtyBounds.add(q, Flags.DESTRUCTION);

                if (q instanceof StorageStructure)
                    layer.storages.removeValue((StorageStructure) q, true);
                else if (q instanceof Conveyor)
                    conveyors.removeValue((Conveyor) q, true);

                if (q.getSchema().powerDocks > 0) {
                    if (q.getPowerNetwork() != null)
                        q.getPowerNetwork().removeVertex(q, true);
                }

                return true;
            }
            return false;
        }
    }

    // Handles when coordinates are out of this chunk, to call up the adjacent chunk
    // Accepts absolute coordinates
    private void removeSpatial(int x, int y) {
        if (x < ax || y < ay || x >= ax + Const.CHUNK_SIZE || y >= ay + Const.CHUNK_SIZE) {
            Chunk c = layer.getChunk(x, y);
            if (c == null) return;
            c.spatial.remove((x - c.ax) * Const.CHUNK_SIZE + (y - c.ay));
        } else {
            spatial.remove((x - ax) * Const.CHUNK_SIZE + (y - ay));
        }
    }

    // Handles when coordinates are out of this chunk, to call up the adjacent chunk
    // Accepts absolute coordinates
    private void setSpatial(int x, int y, Structure<?> s) {
        if (x < ax || y < ay || x >= ax + Const.CHUNK_SIZE || y >= ay + Const.CHUNK_SIZE) {
            Chunk c = layer.getChunk(x, y);
            if (c == null) return;
            c.spatial.put((x - c.ax) * Const.CHUNK_SIZE + (y - c.ay), s);
        } else {
            spatial.put((x - ax) * Const.CHUNK_SIZE + (y - ay), s);
        }
    }

    public boolean addStructure(Structure<?> s, boolean fromLoading) {
        if (!dataSet) {
            init();
        }

        s.layer = layer;

        synchronized (structLock) {
            int m = (s.x - ax) * Const.CHUNK_SIZE + (s.y - ay);
            if (s instanceof CopperCable) {
                if (cables.containsKey(m))
                    return false;

                cables.put(m, (CopperCable) s);
            } else {
                for (int i = 0; i < s.getWidth(); i++) {
                    for (int j = 0; j < s.getHeight(); j++) {
                        if (layer.getStructure(s.x + i, s.y + j) != null) {
                            return false;
                        }
                    }
                }

                // when loading, we have to wait for all chunks to be loaded to set spatial
                if (!fromLoading) {
                    for (int i = 0; i < s.getWidth(); i++) {
                        for (int j = 0; j < s.getHeight(); j++) {
                            setSpatial(s.x + i, s.y + j, s);
                        }
                    }
                }

                maxX = Math.max(maxX, s.x + s.getWidth());
                maxY = Math.max(maxY, s.y + s.getHeight());

                structures.add(s);
            }

            if (!(s instanceof CopperCable))
                layer.setCollision(s, true);

            if (!fromLoading) {
                layer.dirtyBounds.add(s, Flags.CONSTRUCTION);

                // notify dock spots in case they're tubes
                for (Dock d : s.getDocks()) {
                    layer.setItemNotification(s.x + d.x + d.dir.dx, s.y + d.y + d.dir.dy);
                }
            }

            // quick access 
            if (s instanceof StorageStructure) {
                layer.storages.add((StorageStructure) s);
            } else if (s instanceof Conveyor) {
                conveyors.add((Conveyor) s);
            }

            if (s.getSchema().powerDocks > 0) {
                PowerNetwork network = new PowerNetwork(Game.G.powerGrid);
                Game.G.powerGrid.addNetwork(network);
                network.addVertex(s);
            }
        }

        s.onPlacement(fromLoading);

        return true;
    }

    public IntMap<CopperCable> getCables() {
        return cables;
    }

    public Array<Structure<?>> getStructures() {
        return structures;
    }

    public int getItemCount() {
        synchronized (structLock) {
            int sum = 0;
            for (Structure<?> s : structures) {
                if (s instanceof Conveyor) {
                    sum += ((Conveyor) s).getItemCount();
                }
            }
            return sum;
        }
    }

    public void stopSfx() {
        synchronized (structLock) {
            for (Structure<?> s : structures)
                s.stopSfx();
        }
    }

    @Override
    public void dispose() {
        if (builder != null && builder.getMesh() != null) {
            builder.dispose();
        }
        glSet = false;
        dirty = true;
    }

    @Override
    public void save(Builder b) {
        if (!dataSet) init();

        b
                .Compound()
                .Byte("x", (byte) x)
                .Byte("y", (byte) y)
                .ShortArray("data", data);

        synchronized (structLock) {
            b.List("Structures", TagType.Compound);
            for (Structure<?> s : structures) {
                s.save(b);
            }
            b.End();
            b.List("Cables", TagType.Compound);
            for (CopperCable s : cables.values()) {
                s.save(b);
            }
            b.End();
        }

        b.End();
    }

    public void postLoad() {
        // when loading, we have to wait for all chunks to be loaded to set spatial
        for (Structure<?> s : structures) {
            for (int i = 0; i < s.getWidth(); i++) {
                for (int j = 0; j < s.getHeight(); j++) {
                    setSpatial(s.x + i, s.y + j, s);
                }
            }
        }
    }

    public void postAllLayersLoad(boolean firstCycle) {
        if (firstCycle) {
            for (Structure<?> s : structures) {
                s.postLoad();
            }
            for (CopperCable s : cables.values()) {
                s.postLoad();
            }
        } else {
            for (Structure<?> s : structures) {
                s.reloadPowerNetwork();
            }
        }
    }

    private static void initTexCache() {
        HashMap<TileType, TextureRegion[]> stone = new HashMap<>();
        stone.put(TileType.Dirt, new TextureRegion[] {
                Quarry.Q.atlas.findRegion("tile_dirt_corner"),
                Quarry.Q.atlas.findRegion("tile_dirt_inner"),
                Quarry.Q.atlas.findRegion("tile_dirt_middle"),
                Quarry.Q.atlas.findRegion("tile_dirt_hole"),
        });
        stone.put(TileType.CrudeOil, new TextureRegion[] {
                Quarry.Q.atlas.findRegion("tile_crude_oil_corner"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_inner"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_middle"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_hole"),
        });

        HashMap<TileType, TextureRegion[]> dirt = new HashMap<>();
        dirt.put(TileType.CrudeOil, new TextureRegion[] {
                Quarry.Q.atlas.findRegion("tile_crude_oil_dirt_corner"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_dirt_inner"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_dirt_middle"),
                Quarry.Q.atlas.findRegion("tile_crude_oil_dirt_hole"),
        });

        texLookup.put(TileType.Stone, stone);
        texLookup.put(TileType.Dirt, dirt);
    }

    public void loadStructures(CompoundTag tag) throws NBTException {
        try {
            for (Tag t : tag.List("Structures", TagType.Compound).data) {
                try {
                    Structure<?> s = Structure.load((CompoundTag) t);
                    if (s != null)
                        addStructure(s, true);
                } catch (NBTException e) {
                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                }
            }
        } catch (NBTException e1) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e1);
        }
        try {
            for (Tag t : tag.List("Cables", TagType.Compound).data) {
                try {
                    Structure<?> s = Structure.load((CompoundTag) t);
                    if (s != null)
                        addStructure(s, true);
                } catch (NBTException e) {
                    Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
                }
            }
        } catch (NBTException e) {
            Quarry.Q.pi.message(PlatformInterface.MSG_EXCEPTION, e);
        }
    }

    public static Chunk load(CompoundTag tag, Layer layer) throws NBTException {
        Chunk c = new Chunk(tag.Byte("x"), tag.Byte("y"), layer);
        c.init();
        c.data = new short[Const.CHUNK_SIZE * Const.CHUNK_SIZE];
        System.arraycopy(tag.ShortArray("data"), 0, c.data, 0, c.data.length);

        return c;
    }
}
