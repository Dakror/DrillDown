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

import com.badlogic.gdx.math.RandomXS128;

import de.dakror.quarry.game.Tile.OreType;
import de.dakror.quarry.game.Tile.TileMeta;
import de.dakror.quarry.game.Tile.TileType;

/**
 * @author Maximilian Stark | Dakror
 */
public class Generator {

    public static final Generator G = new Generator();

    private RandomXS128 rng;

    private long seed;

    public static class VeinDebug {
        public float x, y, radius;

        public VeinDebug(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    private Generator() {
        rng = new RandomXS128();
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
        rng.setSeed(seed);
    }

    public long[] getState() {
        return new long[] { rng.getState(0), rng.getState(1) };
    }

    public void setState(long[] state) {
        rng.setState(state[0], state[1]);
    }

    public void generate(Layer layer) {
        for (OreType type : OreType.values) {
            generateVeins(layer, type);
        }
    }

    private void generateVeins(Layer layer, OreType type) {
        float probability = layer.getIndex() < 40 ? type.getPropability(layer.getIndex()) : rng.nextFloat() * (1 + rng.nextFloat());
        float radius = type.minVeinRadius + (type.maxVeinRadius - type.minVeinRadius) * probability;
        for (int i = 0; i < type.veins; i++) {
            if (probability > rng.nextFloat()) {
                generateVein(layer, radius, type.tile);
            }
        }

        // force copper on first level
        if (layer.getIndex() == 0 && type == OreType.CopperOre) {
            for (int i = 0; i <= 15;) {
                i += generateVein(layer, radius, type.tile);
            }
        }
    }

    private int generateVein(Layer layer, float rad, TileType type) {
        float x = rng.nextFloat() * layer.width;
        float y = rng.nextFloat() * layer.height;

        float radius = rad + rng.nextFloat() - 0.5f;

        layer.veinDebugs.add(new VeinDebug(x + 0.5f, y + 0.5f, radius));

        int produced = 0;
        for (int j = (int) (x - radius - 1); j < (int) (x + radius + 1); j++) {
            for (int k = (int) (y - radius - 1); k < (int) (y + radius + 1); k++) {
                float dist = (float) Math.sqrt((j - x) * (j - x) + (k - y) * (k - y));
                if (dist >= radius)
                    continue;

                if (type.base != null) {
                    float dst = dist / radius;
                    float gs = (float) Math.abs(rng.nextGaussian());
                    float rnd = (gs / 3) * dst;

                    if (0.2f >= rnd) {
                        layer.set(j, k, type.base);
                        if (layer.getIndex() > 0) {
                            layer.addMeta(j, k, TileMeta.FOG_OF_WAR);
                        }
                    } else {
                        continue;
                    }
                }

                float dst = dist / radius;
                float gs = (float) Math.abs(rng.nextGaussian());
                float rnd = (gs / 3) * dst;

                // for now "veins" like dirt just generate with 100% to avoid artifacts while blending tiles
                if (0.1f >= rnd) {
                    produced++;
                    layer.set(j, k, type);

                    if (layer.getIndex() > 0) {
                        layer.addMeta(j, k, TileMeta.FOG_OF_WAR);
                    }
                }
            }
        }

        return produced;
    }
}
