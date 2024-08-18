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

package de.dakror.quarry.desktop;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

/**
 * @author Maximilian Stark | Dakror
 */
public class ItemEdgeDetector {
    public static void main(String[] args) throws IOException {
        for (File f : new File("..\\Development\\item_icons").listFiles()) {
            if (f.getName().contains("_nil.png")) continue;

            ArrayList<Integer> edgePixels = new ArrayList<>();
            BufferedImage bi = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
            BufferedImage src = ImageIO.read(f);
            for (int i = 0; i < 24; i++) {
                for (int j = 0; j < 24; j++) {
                    int rgb = src.getRGB(i, j);
                    if (rgb >>> 24 > 0) {
                        boolean anyTransparent = i == 0 || j == 0 || i == 23 || j == 23;

                        if (!anyTransparent) {
                            o: for (int k = -1; k < 2; k++) {
                                for (int l = -1; l < 2; l++) {
                                    if (k != 0 && l != 0 || k == l)
                                        continue;
                                    int x = i + k;
                                    int y = j + l;
                                    if (x >= 0 && x <= 23 && y >= 0 && y <= 23) {
                                        int px = src.getRGB(x, y);
                                        if (px >>> 24 == 0) {
                                            anyTransparent = true;
                                            break o;
                                        }
                                    }
                                }
                            }
                        }

                        if (!anyTransparent) {
                            bi.setRGB(i, j, rgb);
                        } else {
                            edgePixels.add(j * 24 + i);
                        }
                    }
                }
            }

            for (int i : edgePixels) {
                int x = i % 24;
                int y = i / 24;

                int r = 0, g = 0, b = 0, c = 0;
                for (int k = -1; k < 2; k++) {
                    for (int l = -1; l < 2; l++) {
                        if (k == l && k == 0)
                            continue;
                        int x1 = x + k;
                        int y1 = y + l;
                        if (x1 >= 0 && x1 <= 23 && y1 >= 0 && y1 <= 23) {
                            if (edgePixels.contains(y1 * 24 + x1))
                                continue;
                            int px = src.getRGB(x1, y1);
                            if (px >>> 24 > 0) {
                                r += (px >> 16) & 0xff;
                                g += (px >> 8) & 0xff;
                                b += (px & 0xff);
                                c++;
                            }
                        }
                    }
                }

                if (c > 0) {
                    r /= c * 2;
                    g /= c * 2;
                    b /= c * 2;

                    bi.setRGB(x, y, 0xff000000 | r << 16 | g << 8 | b);
                } else {
                    bi.setRGB(x, y, 0xffff0000);
                }
            }

            ImageIO.write(bi, "PNG", f);
            ImageIO.write(bi, "PNG", new File(f.getParentFile().getParentFile() + "/Textures", f.getName()));
        }
    }
}
