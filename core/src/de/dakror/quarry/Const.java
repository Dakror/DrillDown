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

package de.dakror.quarry;

/**
 * @author Maximilian Stark | Dakror
 */
public class Const {
    public static final byte QSF_VERSION = 0x17;

    ///

    public static final int DEFAULT_LAYER_SIZE = 64;

    public static final int CHUNK_SIZE = 32;
    public static final int TILE_SIZE = 64;
    public static final int CHUNK_FULL_SIZE = CHUNK_SIZE * TILE_SIZE;

    public static final int STATE_SIZE = 24;

    public static float H;
    public static float W;
    public static float UI_H;
    public static float UI_W;

    public static String SFX_FORMAT = ".ogg";
    public static String MUSIC_FORMAT = ".ogg";

    public static final int ITEM_COUNT = 256;
    public static final int STRUCTURE_COUNT = 256;

    public static final float Z_STATES = -1;
    public static final float Z_CABLES = -2;
    public static final float Z_STRUCTURES = -3;
    public static final float Z_ITEMS = -4;
    public static final float Z_TUBES = -5;

    public static final int TUBE_OFFSET = 12;
    public static final int ITEM_OFFSET = 20;
    public static final int CABLE_OFFSET = 24;
    public static final int CABLE_WIDTH = 8;

    public static final float MAX_LOUDNESS = 1.25f;

    public static final int MSG_FILE_PERMISSION = 13;
    public static final int MSG_DPI = 14;
    public static final int MSG_PADDING = 15;
    public static final int MSG_COPY = 17;
    public static final int MSG_PASTE = 18;
    public static final int MSG_SELECT_ROOT = 19;

    public static final int MIN_AUTOSAVE_INTERVAL = 3 * 60 * 1000; // 3 minutes

    // BALANCING //
    public static final float ITEM_SPEED = 5.0f;
    public static final int ITEMS_PER_CONVEYOR = 7;
    public static final float REFUND_PERCENTAGE = 0.25f;
    public static final float REFUND_CONSIDERATE_PERCENTAGE = 0.8f;

    public static final int DEFAULT_PUMP_OUT_MAX_FLUID = 500;
    public static final float DEFAULT_PUMP_OUT_DELAY = 0.5f;

    // UI //
    public static final int BUILD_RING_ITEM_SIZE = 100;

}
