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
import com.badlogic.gdx.math.MathUtils;

import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemType;

/**
 * @author Maximilian Stark | Dakror
 */
public class Tile {
    public static final TileType[] tiles;

    static {
        tiles = new TileType[256];
        for (TileType t : TileType.values) {
            if (tiles[t.value] != null) throw new IllegalStateException("Tile with ID " + t.value + " is already taken!");
            tiles[t.value] = t;
        }
    }

    public static class TileMeta {
        public static final int NO_META = 0;
        public static final int FOG_OF_WAR = 1 << 0;
        public static final int ALT_TEX = 1 << 1;
        public static final int ROT_TEX = 1 << 2;
        public static final int ROT_TEX_90 = 1 << 3;
        public static final int ROT_TEX_180 = 1 << 4;
        public static final int ROT_TEX_270 = 1 << 5;
        public static final int BLENDING = 1 << 6;
        public static final int BASE_TILE = 1 << 7;
    }

    public enum TileType {
        Air(0, null, null, TileMeta.NO_META),
        Stone(1, "stone", ItemType.Stone, null, TileMeta.BASE_TILE),
        Dirt(2, "dirt", ItemType.Dirt, null, TileMeta.BLENDING | TileMeta.BASE_TILE),
        Clay(3, "clay", ItemType.Clay, Dirt, TileMeta.NO_META),
        //        Gravel(4, "gravel", ItemType.StoneGravel, Dirt, TileMeta.NO_META),
        IronOre(20, "iron", ItemType.IronOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),
        CoalOre(21, "coal", ItemType.CoalOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),
        CopperOre(22, "copper", ItemType.CopperOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),
        //        GoldOre(23, "gold", ItemType.GoldOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),
        //        ZincOre(24, "zinc", ItemType.ZincOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),
        TinOre(25, "tin", ItemType.TinOre, Stone, TileMeta.ALT_TEX | TileMeta.ROT_TEX),

        CrudeOil(50, "crude_oil", ItemType.CrudeOil, null, TileMeta.BLENDING),

        ;

        public static final TileType[] values = values();

        public final byte value;
        public final TileType base;
        public final String name;
        public final String title;
        public final byte meta;
        public final ItemType itemDrop;
        public TextureRegion tex, texAlt;

        TileType(int value, String name, TileType base, int meta) {
            this(value, name, null, base, meta);
        }

        TileType(int value, String name, ItemType itemDrop, TileType base, int meta) {
            this.value = (byte) value;
            this.title = name;
            this.name = name == null ? null : Quarry.Q.i18n.get("tile." + name);
            this.itemDrop = itemDrop;
            this.base = base;
            this.meta = (byte) meta;

            if (name != null) {
                // static fields in this class are being loaded after the main game file so this will be fine
                tex = Quarry.Q.atlas.findRegion("tile_" + name);
                if ((meta & TileMeta.ALT_TEX) == TileMeta.ALT_TEX) {
                    texAlt = Quarry.Q.atlas.findRegion("tile_" + name + "_alt");
                }
            }
        }

    }

    public enum OreType {
        // @off
        Dirt(     1,      TileType.Dirt, 1,     4,  6, 0.5f,  0.9f,   2f,    0.5f, 0.8f),
        Clay(     2,      TileType.Clay, 2,     3,  5,    0,  0.5f, 1.5f,    0.2f,    1),
        IronOre(  3,   TileType.IronOre, 2, 3.25f,  7,    0,  1.5f, 1.5f,   0.01f,    1),
        CopperOre(4, TileType.CopperOre, 2, 3.25f,  8,    3,    1f, 1.8f,   0.01f,    1),
        CoalOre(  5,   TileType.CoalOre, 2,     3, 10, 0.5f,  0.6f, 1.2f,   0.25f,    1),
        TinOre(   6,    TileType.TinOre, 2,  3.7f,  6,    5,  0.6f, 1.6f,  0.005f,    1),
        CrudeOil( 7,  TileType.CrudeOil, 2,  4.5f,  2,   13, 0.75f,    3, 0.0001f,    1),
        ;
        // @on
        //        static {
        //            System.out.println("f(x)=a * e^(-((x-b)/(2*c))^2)+d");
        //        }

        public final TileType tile;
        public final byte id;
        public final float minVeinRadius, maxVeinRadius;
        public final float min, max;
        public final int veins;
        public final float offset;
        public final float leftUphold, rightUphold;

        OreType(int id, TileType tile, float minVeinRadius, float maxVeinRadius, int veins,
                float offset, float leftUphold, float rightUphold, float min, float max) {

            this.tile = tile;
            this.minVeinRadius = minVeinRadius;
            this.maxVeinRadius = maxVeinRadius;
            this.veins = veins;
            this.offset = offset;
            this.leftUphold = leftUphold;
            this.rightUphold = rightUphold;
            this.min = min;
            this.max = max;
            this.id = (byte) id;
            //            System.out.println((max - min) + " * e^(-(x-" + offset + ")^2/(2* Wenn(x<" + offset + "," + leftUphold + "," + rightUphold + "))^2)+" + min);
            //            for (int i = 0; i < 10; i++) {
            //                System.out.println(i + ":\t" + getPropability(i));
            //            }
        }

        // ae^(-((x-b)/2c)^2)+d
        public float getPropability(int level) {
            float val = (max - min) * (float) Math.pow(MathUtils.E, -Math.pow((level - offset) / (2 * (level < offset ? leftUphold : rightUphold)), 2)) + min;
            if (val < 0.000001f) val = 0;
            return val;
        }

        public static final OreType[] values = values();
    }
}
