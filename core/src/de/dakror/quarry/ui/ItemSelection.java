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

package de.dakror.quarry.ui;

import java.util.EnumMap;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.modified.TextTooltip;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import de.dakror.common.Callback;
import de.dakror.quarry.Const;
import de.dakror.quarry.Quarry;
import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.scenes.Game;
import de.dakror.quarry.scenes.GameUi;

/**
 * @author Maximilian Stark | Dakror
 */
public class ItemSelection extends Table {

    ImageButton x;
    ImageButton nil;
    ClickListener listener;
    Callback<ItemType> callback;

    HorizontalGroup group;
    ImageButton chevronLeft, chevronRight;

    EnumMap<ItemType, ImageButton> buttons;

    public ItemSelection(Skin skin, String style) {
        buttons = new EnumMap<>(ItemType.class);

        setVisible(false);
        setSize(700, 100);
        setPosition((Const.UI_W - getWidth()) / 2, 450);
        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                // immobilize clicking behind table
                return true;
            }
        });
        setBackground(GameUi.getDrawable(skin, "panel_metalDark", 16, 16, 16, 16));

        group = new HorizontalGroup();
        group.pad(20).center().left().grow().space(15);

        final ScrollPane scroll = new ScrollPane(group, Quarry.Q.skin) {
            @Override
            public void act(float delta) {
                super.act(delta);

                if (isFlinging() || isPanning() || getVisualScrollX() != getScrollX()) {
                    float scroll = getVisualScrollX();
                    chevronLeft.setVisible(scroll > 0);
                    chevronRight.setVisible(scroll < getMaxX());
                }
            }
        };

        ScrollPaneStyle sps = new ScrollPaneStyle(scroll.getStyle());
        sps.background = null;
        scroll.setStyle(sps);
        scroll.setTouchable(Touchable.enabled);
        scroll.setScrollbarsOnTop(true);
        scroll.setScrollingDisabled(false, true);

        x = new ImageButton(Quarry.Q.skin.getDrawable("symb_x"));
        listener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                callback.call((ItemType) event.getTarget().getParent().getUserObject());
                hide();
            }
        };
        x.addListener(listener);

        nil = new ImageButton(ItemType.Nil.drawable);
        nil.getImageCell().size(40);
        nil.setUserObject(ItemType.Nil);
        nil.addListener(listener);

        chevronLeft = new ImageButton(Quarry.Q.skin.getDrawable("caret_left"));
        chevronLeft.getImageCell().pad(0, 15, 0, 15);
        chevronLeft.setVisible(false);

        chevronRight = new ImageButton(Quarry.Q.skin.getDrawable("caret_right"));
        chevronRight.getImageCell().pad(0, 15, 0, 15);

        add(chevronLeft).growY();
        add(scroll).grow();
        add(chevronRight).growY();

        chevronLeft.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                scroll.setScrollX(scroll.getScrollX() - 300);
            }
        });

        chevronRight.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Quarry.Q.sound.play(Quarry.Q.clickSfx);
                scroll.setScrollX(scroll.getScrollX() + 300);
            }
        });

    }

    public void show(Callback<ItemType> callback, boolean withNil) {
        this.callback = callback;
        group.clearChildren();
        group.addActor(x);

        if (withNil)
            group.addActor(nil);

        for (ItemType t : Game.G.getSeenResources()) {
            if (t.categories.contains(ItemCategory.Fluid)) continue;
            ImageButton ib = buttons.get(t);
            if (ib == null) {
                ib = new ImageButton(t.drawable);
                ib.getImageCell().size(40);
                ib.setUserObject(t);
                ib.addListener(new TextTooltip(t.title.trim(), Quarry.Q.skin));

                ib.setSize(60, 60);
                ib.addListener(listener);
                buttons.put(t, ib);
            }
            group.addActor(ib);
        }

        setVisible(true);
        ((ScrollPane) group.getParent()).addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                super.enter(event, x, y, pointer, fromActor);
                event.getStage().setScrollFocus(event.getTarget());
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                super.exit(event, x, y, pointer, toActor);
                event.getStage().setScrollFocus(toActor);
            }
        });

        ((ScrollPane) group.getParent()).setScrollX(0);
        ((ScrollPane) group.getParent()).updateVisualScroll();
        ((ScrollPane) group.getParent()).invalidateHierarchy();
        ((ScrollPane) group.getParent()).validate();
        chevronRight.setVisible(((ScrollPane) group.getParent()).getMaxX() >= ((ScrollPane) group.getParent()).getWidth());
    }

    public void hide() {
        setVisible(false);
    }
}
