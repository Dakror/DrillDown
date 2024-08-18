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

package de.dakror.quarry.util;

import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pool.Poolable;
import com.badlogic.gdx.utils.Pools;

/**
 * @author Maximilian Stark | Dakror
 */
public class Param implements Poolable {
    public static final Pool<Param> pool = Pools.get(Param.class, 10);

    public static Param make(String name, Type type) {
        return pool.obtain().set(name, type);
    }

    public static void free(Param p) {
        pool.free(p);
    }

    public enum Type {
        String, Long, Double, Boolean, Bytes
    }

    private String name;
    private Type type;

    public Param() {}

    public Param set(String name, Type type) {
        this.name = name;
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    @Override
    public void reset() {
        name = null;
        type = null;
    }

    @Override
    public String toString() {
        if (type != null)
            return String.format("Param [name=%s, type=%s]", name, type.name());
        else return "uninit Param";
    }
}
