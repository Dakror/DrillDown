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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;

import javax.imageio.ImageIO;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.Composite;
import de.dakror.quarry.game.Item.Element;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.game.Tile.OreType;
import de.dakror.quarry.structure.base.ProducerStructure.ProducerSchema;
import de.dakror.quarry.structure.base.RecipeList.ParameterizedAmount;
import de.dakror.quarry.structure.base.RecipeList.Recipe;
import de.dakror.quarry.structure.base.Schema;
import de.dakror.quarry.structure.base.StructureType;

/**
 * @author Maximilian Stark | Dakror
 */
public class MetaGen {
    static Color[] colors = {
            Color.blue, Color.cyan, Color.green, Color.magenta, Color.orange,
            Color.pink, Color.red, Color.yellow, Color.black
    };

    static void generateOreGraph() {
        int layers = 25;
        int w = 200;
        int h = 400;
        int o = 20;
        float m = 1.1f;
        BufferedImage img = new BufferedImage(w * layers, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.white);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());

        g.setColor(Color.black);
        g.fillRect(0, img.getHeight() - o, img.getWidth(), o);
        for (int i = 0; i < layers; i++) {
            g.setColor(Color.black);
            g.drawLine(w * i + w / 2, 0, w * i + w / 2, h);
            g.setColor(Color.lightGray);
            g.drawLine(w * i, 0, w * i, h - o);
            g.setColor(Color.white);
            g.drawString(i + "", w * i + w / 2 - 3, img.getHeight() - 5);
        }

        for (int i = 0; i < m * 10; i++) {
            if (i % 5 == 0) g.setColor(Color.black);
            else g.setColor(Color.lightGray);
            int y = (int) (img.getHeight() - o - (i / 10f) * ((h - o) / m));
            g.drawLine(0, y, img.getWidth(), y);
        }

        int strX = 20;

        g.setColor(Color.darkGray);
        g.fillRect(0, 0, OreType.values().length * 75, 30);

        for (OreType type : OreType.values()) {
            g.setColor(colors[type.ordinal()]);

            g.drawString(type.name(), strX, 20);
            strX += type.name().length() * 10;

            for (int i = 0; i < w * layers; i++) {
                double x = i / (double) w - 0.5;
                double y = (type.max - type.min) * Math.pow(MathUtils.E, -Math.pow((x - type.offset) / (2 * (x < type.offset ? type.leftUphold : type.rightUphold)), 2)) + type.min;
                if (y < 0.000001f) y = 0;

                if (y > 0)
                    g.fillOval(i - 1, (int) (img.getHeight() - o - (y * ((h - o) / m))) - 1, 3, 3);
            }
        }

        g.dispose();
        try {
            ImageIO.write(img, "PNG", new File("../Development/stats/ores.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        //        System.exit(0);
    }

    static void calculateItemWorths() {
        EnumMap<ItemType, Float> cache = new EnumMap<>(ItemType.class);
        EnumMap<ItemCategory, Float> catCache = new EnumMap<>(ItemCategory.class);

        // collect recipes
        Array<Recipe> recipes = new Array<>();
        for (StructureType st : StructureType.values()) {
            try {
                Schema schema = (Schema) st.constr.getDeclaringClass().getField("classSchema").get(null);
                if (schema instanceof ProducerSchema) {
                    recipes.addAll(((ProducerSchema) schema).recipeList.recipes);
                }
            } catch (Exception e1) {
                System.err.println(st + " no class schema");
            }
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("../Development/stats/worths.csv")));

            // two pass to avoid circular dependencies with already resolved values
            for (ItemType i : ItemType.values()) {
                float worth = i.worth;
                if (worth == 0) continue;

                cache.put(i, worth);
                long time = (long) worth;
                bw.write(String.format(Locale.ENGLISH, "%s;%,.0f;%dh:%02dmin:%02ds.%03dms\n", i.name(), worth, time / 3600_000, (time % 3600_000) / 60_000, time % 60_000 / 1000, time % 1000));
            }

            for (ItemType i : ItemType.values()) {
                float worth = i.worth;
                if (worth == 0) {
                    worth = calculateItemWorth(i, recipes, cache, catCache);
                } else continue;

                cache.put(i, worth);
                long time = (long) worth;
                System.out.println(i.name() + ": " + Integer.toString((int) worth));
                bw.write(String.format(Locale.ENGLISH, "%s;%,.0f;%dh:%02dmin:%02ds.%03dms\n", i.name(), worth, time / 3600_000, (time % 3600_000) / 60_000, time % 60_000 / 1000, time % 1000));
            }
            bw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    static float calculateItemWorth(ItemType item, Array<Recipe> recipes, EnumMap<ItemType, Float> cache, EnumMap<ItemCategory, Float> catCache) {

        if (item == ItemType.EmptyBarrel) {
            int it = 0;
        }
        if (cache.containsKey(item)) return cache.get(item);

        float minWorth = Float.MAX_VALUE;
        recipeLoop: for (int i = 0; i < recipes.size; i++) {
            Recipe r = recipes.get(i);
            if (r.getOutput() != null) {
                for (Amount a : r.getOutput().entries) {
                    if (a.getItem() == item || Item.base(item) == a.getItem()) {
                        float s = r.workingTime * 10 * (r.getPower() > 0 ? (float) Math.pow(r.getPower() * 60, 0.5f) : 1);
                        Element el = Item.element(item);
                        Composite co = Item.composite(item);

                        if (r.getInput() != null) {
                            for (Amount b : r.getInput().entries) {
                                if (b.getItem() != null) {
                                    float amount = b.getAmount();
                                    if (b.getItem().ordinal() >= item.ordinal()) continue recipeLoop;
                                    if (b.getItem().categories.contains(ItemCategory.Fluid))
                                        amount /= 1000.0f;

                                    if (a instanceof ParameterizedAmount && (el != null || co != null)) {
                                        if (!Item.exists(b.getItem(), item)) continue recipeLoop;

                                        s += calculateItemWorth(Item.get(b.getItem(), item), recipes, cache, catCache) * amount;
                                    } else {
                                        s += calculateItemWorth(b.getItem(), recipes, cache, catCache) * amount;
                                    }
                                } else if (b.getCat() != null) {
                                    // find cheapest item for category
                                    if (catCache.containsKey(b.getCat())) {
                                        s += catCache.get(b.getCat()) * b.getAmount();
                                    } else {
                                        float min = 0;
                                        for (ItemType j : ItemType.values()) {
                                            if (j.categories.contains(b.getCat())) {
                                                float q = calculateItemWorth(j, recipes, cache, catCache);
                                                if (min == 0 || q < min)
                                                    min = q;
                                            }
                                        }
                                        if (min == 0)
                                            System.err.println("No items found for category: " + b.getCat());

                                        catCache.put(b.getCat(), min);

                                        s += min * b.getAmount();
                                    }
                                }
                            }
                        }

                        float value = s / a.getAmount();
                        minWorth = Math.min(minWorth, value);
                    }
                }
            }
        }

        if (minWorth == Float.MAX_VALUE) {
            System.err.println("No recipe found for product: " + item);
            minWorth = 0;
        }

        cache.put(item, minWorth);
        return minWorth;
    }

    public static void run() {
        new File("../Development/stats").mkdirs();

        calculateItemWorths();

        generateOreGraph();
        // dump translations

        /* for (ItemType t : ItemType.values()) {
            System.out.println("item." + t.name() + "=" + t.name().replaceAll("[A-Z]", " $0").trim());
        }
        
        System.out.println("===============================");
        
        for (ItemCategory t : ItemCategory.values()) {
            System.out.println("cat." + t.name() + "=" + t.name().replaceAll("[A-Z]", " $0").trim());
        }*/

        // generate stats       

        StringBuilder build = new StringBuilder();
        StringBuilder recipes = new StringBuilder();
        StringBuilder buildTree = new StringBuilder();
        StringBuilder tree = new StringBuilder();
        StringBuilder sciences = new StringBuilder();
        buildTree.append("strict digraph {\r\n\toverlap=false;graph [dpi=300]; node[shape=record]; edge[tailport=s; headport=n];ranksep=1;nodesep=1;\r\n");
        tree.append("strict digraph {\r\n\toverlap=false;\r\n\t node[shape=record]; edge[tailport=s; headport=n; color=grey];graph [dpi=300];ranksep=1;\r\n");
        sciences.append("strict digraph {\r\n\toverlap=false;rankdir=LR;splines=true\r\n\tnode [shape=box];graph [dpi=300];\r\n");

        recipes.append("Structure;Input;;;;;;;;;;Time;Power;Output\n");

        EnumMap<StructureType, EnumSet<ItemType>> building = new EnumMap<>(StructureType.class);
        EnumMap<StructureType, EnumSet<ItemType>> outputs = new EnumMap<>(StructureType.class);
        EnumMap<StructureType, EnumSet<ItemType>> inputs = new EnumMap<>(StructureType.class);

        //        sciences.append("\t__Start[xlabel=\"Start\"]\r\n");
        Array<ScienceType> start = new Array<>();
        for (ScienceType t : ScienceType.values()) {
            sciences.append("\t" + t.name() + "\r\n");
            sciences.append("\t" + t.name() + "_p [shape=point]\r\n");
            sciences.append("\t" + t.name() + " -> " + t.name() + "_p [arrowhead=none,headport=w]\r\n");
            if (t.required.length == 0) {
                start.add(t);
                //                sciences.append("\t__Start -> " + t.name() + "\r\n");
            } else {
                for (ScienceType s : t.required) {
                    sciences.append("\t" + s.name() + "_p -> " + t.name() + "\r\n");
                }
            }
        }
        Map<EnumSet<ScienceType>, EnumSet<StructureType>> sameSciences = new HashMap<>();
        Map<EnumSet<ScienceType>, EnumMap<StructureType, Array<Recipe>>> sameScienceRecipes = new HashMap<>();
        EnumSet<ScienceType> tmp = EnumSet.noneOf(ScienceType.class);
        for (StructureType st : StructureType.values()) {
            try {
                Schema schema = (Schema) st.constr.getDeclaringClass().getField("classSchema").get(null);
                if (schema.sciencesRequired.isEmpty()) continue;

                if (schema instanceof ProducerSchema) {
                    for (Recipe r : ((ProducerSchema) schema).recipeList.recipes) {
                        tmp.clear();
                        tmp.addAll(schema.sciencesRequired);
                        tmp.addAll(r.getRequiredSciences());

                        EnumMap<StructureType, Array<Recipe>> set = sameScienceRecipes.getOrDefault(tmp, new EnumMap<StructureType, Array<Recipe>>(StructureType.class));
                        Array<Recipe> re = set.containsKey(st) ? set.get(st) : new Array<Recipe>();
                        re.add(r);
                        set.put(st, re);
                        sameScienceRecipes.put(EnumSet.copyOf(tmp), set);
                    }
                }

                EnumSet<StructureType> set = sameSciences.getOrDefault(schema.sciencesRequired, EnumSet.noneOf(StructureType.class));
                set.add(st);
                sameSciences.put(schema.sciencesRequired, set);

            } catch (Exception e1) {
                System.err.println(st + " no class schema");
            }
        }

        for (Map.Entry<EnumSet<ScienceType>, EnumSet<StructureType>> e : sameSciences.entrySet()) {
            String name = "";
            for (StructureType st : e.getValue()) {
                name += st.name() + "<br/>";
            }
            name = name.trim();
            sciences.append("\t\tst_" + Math.abs(e.hashCode()) + " [shape=ellipse,label=<" + name + ">]\r\n");
            for (ScienceType st : e.getKey()) {
                sciences.append("\t\t" + st.name() + "_p -> st_" + Math.abs(e.hashCode()) + "\r\n");
            }
        }

        for (Map.Entry<EnumSet<ScienceType>, EnumMap<StructureType, Array<Recipe>>> e : sameScienceRecipes.entrySet()) {
            String name = "";
            for (Map.Entry<StructureType, Array<Recipe>> e1 : e.getValue().entrySet()) {
                name += "{" + e1.getKey().name() + "| {";
                for (int i = 0; i < e1.getValue().size; i++) {
                    name += e1.getValue().get(i).name;
                    if (i < e1.getValue().size - 1) name += " | ";
                }
                name += "} } |";
            }
            name = name.trim().substring(0, name.length() - 1);
            sciences.append("\t\tst_" + Math.abs(e.hashCode()) + " [shape=record,label=\"" + name + "\"]\r\n");
            for (ScienceType st : e.getKey()) {
                sciences.append("\t\t" + st.name() + "_p -> st_" + Math.abs(e.hashCode()) + "\r\n");
            }
        }

        sciences.append("\t{rank=same ");
        for (ScienceType s : start) {
            sciences.append(s.name() + " ");
        }
        sciences.append("}\r\n");

        for (StructureType type : StructureType.values()) {
            try {
                Schema schema = (Schema) type.constr.getDeclaringClass().getField("classSchema").get(null);
                if (schema instanceof ProducerSchema) {
                    tree.append("\t" + type.name() + "[label=\"" + type.name() + "\"]\r\n");
                    EnumSet<ItemType> out = EnumSet.noneOf(ItemType.class);
                    EnumSet<ItemType> in = EnumSet.noneOf(ItemType.class);
                    for (Recipe r : ((ProducerSchema) schema).recipeList.recipes) {
                        recipes.append(type.name()).append(";");

                        int i = 0;
                        if (r.getInput() != null) {
                            for (Amount o : r.getInput().entries) {
                                if (o.getItem() != null) {
                                    recipes.append(o.getItem()).append(";");
                                    in.add(o.getItem());
                                } else {
                                    recipes.append(o.getCat()).append(";");
                                    for (ItemType it : ItemType.values()) {
                                        if (it.categories.contains(o.getCat())) in.add(it);
                                    }
                                }
                                recipes.append(o.getAmount()).append(";");
                                i++;
                            }
                        }

                        while (i < 5) {
                            recipes.append(";;");
                            i++;
                        }
                        recipes.append((int) r.workingTime)
                                .append(";")
                                .append(r.getPower())
                                .append(";");

                        if (r.getOutput() != null) {
                            for (Amount o : r.getOutput().entries) {
                                if (o.getItem() != null) {
                                    out.add(o.getItem());
                                    recipes.append(o.getItem()).append(";");
                                } else {
                                    recipes.append(o.getCat()).append(";");
                                    for (ItemType it : ItemType.values()) {
                                        if (it.categories.contains(o.getCat())) out.add(it);
                                    }
                                }

                                recipes.append(o.getAmount()).append(";");
                            }
                        }

                        recipes.append("\n");
                    }

                    inputs.put(type, in);
                    outputs.put(type, out);
                } else if (type == StructureType.Boiler) {
                    tree.append("\t" + type.name() + "[label=\"" + type.name() + "\"]\r\n");
                    inputs.put(type, EnumSet.of(ItemType.Water, ItemType.CoalOre, ItemType.Charcoal));
                    outputs.put(type, EnumSet.of(ItemType.PressurizedSteam));
                } else if (type == StructureType.Refinery) {
                    tree.append("\t" + type.name() + "[label=\"" + type.name() + "\"]\r\n");
                    inputs.put(type, EnumSet.of(ItemType.CrudeOil, ItemType.IntermediateOilToRefinery));
                    outputs.put(type, EnumSet.of(ItemType.IntermediateOilToColumn, ItemType.RefinedOil));
                }

                EnumSet<ItemType> buildRes = EnumSet.noneOf(ItemType.class);

                buildTree.append("\t" + type.name() + "[label=\"" + type.name() + "\"]\r\n");

                build.append(type.name() + ";");
                for (Amount a : schema.buildCosts.entries) {
                    buildRes.add(a.getItem());
                    build.append(a.getItem().name() + ";" + a.getAmount() + ";");
                }

                building.put(type, buildRes);

                build.append("\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LinkedHashSet<String> buildTreeEdges = new LinkedHashSet<>();
        LinkedHashSet<String> treeEdges = new LinkedHashSet<>();

        tree.append("\r\n");
        for (Map.Entry<StructureType, EnumSet<ItemType>> e : outputs.entrySet()) {
            for (ItemType o : e.getValue()) {
                for (Map.Entry<StructureType, EnumSet<ItemType>> f : inputs.entrySet()) {
                    if (f.getValue().contains(o)
                            || f.getValue().contains(Item.base(o))) {
                        treeEdges.add("\t" + e.getKey().name() + " -> " + f.getKey().name() + " [xlabel=\"" + o.name() + "\"]\r\n");
                    }
                }

                for (Map.Entry<StructureType, EnumSet<ItemType>> f : building.entrySet()) {
                    if (f.getValue().contains(o) || f.getValue().contains(Item.base(o))) {
                        buildTreeEdges.add("\t" + e.getKey().name() + " -> " + f.getKey().name() + " [xlabel=\"" + o.name() + "\"]\r\n");
                    }
                }
            }
        }

        for (Map.Entry<StructureType, EnumSet<ItemType>> e : inputs.entrySet()) {
            for (ItemType o : e.getValue()) {
                for (Map.Entry<StructureType, EnumSet<ItemType>> f : outputs.entrySet()) {
                    if (f.getValue().contains(o) || f.getValue().contains(Item.base(o))) {
                        treeEdges.add("\t" + f.getKey().name() + " -> " + e.getKey().name() + " [xlabel=\"" + o.name() + "\"]\r\n");
                    }
                }
            }
        }

        for (Map.Entry<StructureType, EnumSet<ItemType>> e : building.entrySet()) {
            for (ItemType o : e.getValue()) {
                for (Map.Entry<StructureType, EnumSet<ItemType>> f : outputs.entrySet()) {
                    if (f.getValue().contains(o) || f.getValue().contains(Item.base(o))) {
                        buildTreeEdges.add("\t" + f.getKey().name() + " -> " + e.getKey().name() + " [xlabel=\"" + o.name() + "\"]\r\n");
                    }
                }
            }
        }

        for (String s : buildTreeEdges)
            buildTree.append(s);
        for (String s : treeEdges)
            tree.append(s);

        buildTree.append("}");
        tree.append("}");
        sciences.append("}");

        Gdx.files.absolute("../Development/stats/build.csv").writeString(build.toString(), false);
        Gdx.files.absolute("../Development/stats/recipes.csv").writeString(recipes.toString(), false);
        Gdx.files.absolute("../Development/stats/tree.dot").writeString(tree.toString(), false);
        Gdx.files.absolute("../Development/stats/buildtree.dot").writeString(buildTree.toString(), false);
        Gdx.files.absolute("../Development/stats/sciences.dot").writeString(sciences.toString(), false);

        try {
            Runtime.getRuntime().exec(new String[] { "cmd", "/c", "dot -T jpg tree.dot > tree.jpg" }, null, new File("../Development/stats/")).waitFor();
            //            Runtime.getRuntime().exec(new String[] { "cmd", "/c", "dot -T jpg buildtree.dot > buildtree.jpg" }, null, new File("../Development/stats/")).waitFor();
            Runtime.getRuntime().exec(new String[] { "cmd", "/c", "dot -T jpg sciences.dot > sciences.jpg" }, null, new File("../Development/stats/")).waitFor();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }
}
