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

package de.dakror.quarry.structure.base;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;

import de.dakror.common.BiCallback;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.Items;
import de.dakror.quarry.game.Science.ScienceType;
import de.dakror.quarry.structure.base.Dock.DockFilter;
import de.dakror.quarry.structure.base.Dock.DockType;
import de.dakror.quarry.structure.base.ProducerStructure.ProducerSchema;
import de.dakror.quarry.structure.base.component.Component;
import de.dakror.quarry.util.Sfx;

/**
 * @author Maximilian Stark | Dakror
 */
public class Schema {
    public enum Flags {
        Indestructible,
        Draggable,
        ConfirmDestruction,
        NoDustEffect,
        MirroredTextureHorizontal,
        MirroredTextureVertical,
        NotRotatable,
        TextureAlwaysUpright,
        Stackable
    }

    public static class ButtonDef {
        public enum ButtonType {
            SinglePress, StateToggle, TempRadio
        }

        public final String icon;
        public final String tooltip;
        public final ButtonType type;
        public final BiCallback<Boolean, Structure<?>> listener;

        public ButtonDef(String icon, String tooltip, ButtonType type, BiCallback<Boolean, Structure<?>> callback) {
            this.icon = icon;
            this.tooltip = tooltip;
            this.type = type;
            this.listener = callback;
        }
    }

    public final int width, height;
    private final Dock[] docks;
    public final TextureRegion tex;
    public final Drawable icon;
    public final String name, texName, description;
    public final boolean clickable;
    public EnumSet<Flags> flags;
    public final Sfx sfx;
    public float loudness;
    public byte version;

    public int outputDocks;
    public int inputDocks;
    public int powerDocks;
    public boolean highPower;
    public boolean lowPower;

    public final Items buildCosts;

    public final Array<ButtonDef> buttons;

    public final StructureType type;

    public final EnumSet<ScienceType> sciencesRequired;

    protected final Array<Component> components;

    public Schema(int version, StructureType type, boolean clickable, int width, int height, String tex, Items buildCosts, Sfx sfx, Dock... docks) {
        this.clickable = clickable;
        this.width = width;
        this.type = type;
        this.height = height;
        this.texName = tex;
        this.sfx = sfx;
        this.version = (byte) version;
        loudness = 1;

        if (this.sfx != null) {
            this.sfx.sound = Quarry.Q.assets.get("sfx/" + sfx.sfx);
        }

        TextureRegion tr = Quarry.Q.atlas.findRegion("structure_" + tex);

        if (tr == null) {
            tr = Quarry.Q.atlas.findRegion("structure_redstone");
            System.err.println("[TEX]  structure '" + type + "'");
        }
        this.name = Quarry.Q.i18n.get("structure." + type.name().toLowerCase());

        this.tex = tr;

        Drawable icon = null;
        try {
            icon = Quarry.Q.skin.getDrawable("icon_" + tex);
        } catch (Exception e) {
            System.err.println("[ICON] structure '" + type + "'");
        }
        if (icon == null) {
            try {
                icon = Quarry.Q.skin.getDrawable("structure_" + tex);
            } catch (Exception e) {
                icon = Quarry.Q.skin.getDrawable("structure_redstone");
            }
        }
        this.icon = icon;

        this.docks = docks;

        sciencesRequired = EnumSet.noneOf(ScienceType.class);
        buttons = new Array<>(3);
        components = new Array<>();

        this.description = this instanceof ProducerSchema ? "" : Quarry.Q.i18n.get("structure." + type.name().toLowerCase() + ".desc");
        this.buildCosts = buildCosts;
        buildCosts.sort();

        for (Dock d : docks) {
            if (d.type == DockType.ItemIn || d.type == DockType.FluidIn) {
                inputDocks++;
            } else if (d.type == DockType.ItemOut || d.type == DockType.FluidOut)
                outputDocks++;
            else if (d.type == DockType.Power || d.type == DockType.BigPower) {
                powerDocks++;
                if (d.type == DockType.Power) lowPower = true;
                if (d.type == DockType.BigPower) highPower = true;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T flags(Flags... flags) {
        if (this.flags != null) throw new IllegalStateException("Multiple calls to flags!");

        this.flags = EnumSet.noneOf(Flags.class);
        for (Flags f : flags)
            this.flags.add(f);

        if (has(Flags.TextureAlwaysUpright) && width != height) {
            throw new RuntimeException("Width must equal width");
        }

        return (T) this;

    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T removeFlags(Flags... flags) {
        this.flags.removeAll(Arrays.asList(flags));
        return (T) this;
    }

    public boolean has(Flags flag) {
        return flags != null && flags.contains(flag);
    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T sciences(ScienceType... sciences) {
        Collections.addAll(sciencesRequired, sciences);
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T components(Component... components) {
        this.components.addAll(components);

        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T loudness(float loudness) {
        this.loudness = loudness;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public <T extends Schema> T button(ButtonDef button) {
        if (buttons.size >= 3)
            throw new IllegalArgumentException("3 buttons is the maximum");
        this.buttons.add(button);
        return (T) this;
    }

    public Component[] copyComponents(Structure<?> structure) {
        if (components == null)
            return new Component[0];
        Component[] c = new Component[components.size];
        int i = 0;
        for (Component comp : this.components) {
            c[i] = comp.clone();
            c[i].setStructure(structure);
            c[i].init();

            i++;
        }
        return c;
    }

    static final Vector2 tmp = new Vector2();

    public Dock[] copyDocks(Structure<?> structure) {
        Dock[] docks = new Dock[this.docks.length];
        for (int i = 0; i < docks.length; i++) {
            Dock d = this.docks[i];
            if (structure.getUpDirection() != Direction.North) {
                float originX, originY;

                if (structure.getUpDirection().dx != 0) {
                    originX = structure.getHeight() / 2f - 0.5f;
                    originY = structure.getWidth() / 2f - 0.5f;
                } else {
                    originX = width / 2f - 0.5f;
                    originY = height / 2f - 0.5f;
                }

                tmp
                        .set(d.x, d.y)
                        .sub(originX, originY)
                        .rotate(structure.getUpDirection().rot - Direction.North.rot)
                        .add(originX, originY);

                if (structure.getUpDirection().dx != 0) {
                    tmp.x += (structure.getWidth() - structure.getHeight()) / 2f;
                    tmp.y += (structure.getHeight() - structure.getWidth()) / 2f;
                }

                Direction newDir = Direction.values[(d.dir.ordinal() + (structure.getUpDirection().ordinal() - Direction.North.ordinal())) % 4];

                if (d.filter != null) {
                    docks[i] = new Dock(Math.round(tmp.x), Math.round(tmp.y), newDir, d.type, new DockFilter(d.filter));
                } else {
                    docks[i] = new Dock(Math.round(tmp.x), Math.round(tmp.y), newDir, d.type);
                }
            } else {
                docks[i] = new Dock(d);
            }
        }

        return docks;
    }

    public Array<Component> getComponents() {
        return components;
    }

    public Dock[] getDocks() {
        return docks;
    }
}
