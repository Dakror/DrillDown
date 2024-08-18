/*******************************************************************************
 * Copyright 2019 Maximilian Stark | Dakror <mail@dakror.de>
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

import de.dakror.common.BiCallback;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.structure.base.Schema.ButtonDef.ButtonType;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class PausableSchema extends Schema {
    public PausableSchema(int version, StructureType type, boolean clickable, int width, int height, String tex, Items buildCosts, Sfx sfx, Dock... docks) {
        super(version, type, clickable, width, height, tex, buildCosts, sfx, docks);
        button(new ButtonDef("state_sleeping", "button.sleep", ButtonType.StateToggle,
                new BiCallback<Boolean, Structure<?>>() {
                    @Override
                    public void call(Boolean on, Structure<?> data) {
                        ((PausableStructure<?>) data).setSleeping(on);
                    }
                }));
    }
}
