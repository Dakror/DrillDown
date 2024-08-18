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

package de.dakror.quarry.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.libgdx.io.NBT.Builder;
import de.dakror.common.libgdx.io.NBT.CompoundTag;
import de.dakror.common.libgdx.io.NBT.NBTException;
import de.dakror.common.libgdx.lml.CustomTag;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item;
import de.dakror.quarry.game.Item.ItemType;

/**
 * @author Maximilian Stark | Dakror
 */
public class Util {
    @SuppressWarnings("unchecked")
    public static <T extends Actor> T lml(String file) {
        Array<Actor> arr = Quarry.Q.lml.parseTemplate(Gdx.files.internal("lml/" + file + ".xml"));
        if (arr.size == 0) throw new RuntimeException("No actors found for LML file: lml/" + file + ".xml");
        T a = (T) arr.first();

        if (a instanceof CustomTag) {
            ((CustomTag) a).postInit();
        }

        return a;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Actor> T id(String id) {
        return (T) Quarry.Q.lml.getActorsMappedByIds().get(id);
    }

    public static String md5(String string) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new BigInteger(1, md.digest(string.getBytes())).toString(16).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static long hash(String string) {
        long h = 1125899906842597L; // prime
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = 31 * h + string.charAt(i);
        }
        return h;
    }

    public static void NBTwriteInventory(Builder builder, Map<ItemType, Integer> map) {
        short[] types = new short[map.size()];
        int[] amounts = new int[map.size()];
        int i = 0;
        for (Map.Entry<ItemType, Integer> r : map.entrySet()) {
            types[i] = r.getKey().value;
            amounts[i] = r.getValue();
            i++;
        }

        builder
                .ShortArray("Types", types)
                .IntArray("Amounts", amounts);
    }

    public static int NBTreadInventory(CompoundTag tag, Map<ItemType, Integer> map) throws NBTException {
        short[] types = tag.ShortArray("Types");
        int[] amounts = tag.IntArray("Amounts");

        if (types.length != amounts.length)
            throw new IllegalStateException("Not the same amount of item types and amounts!");

        int sum = 0;
        map.clear();
        for (int i = 0; i < types.length; i++) {
            ItemType t = Item.get(types[i]);
            if (t != null) {
                map.put(t, amounts[i]);
                sum += amounts[i];
            }
        }

        return sum;
    }
}
