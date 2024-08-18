package de.dakror.quarry.game;

import de.dakror.quarry.game.Item.ItemCategory;
import de.dakror.quarry.game.Item.ItemType;
import de.dakror.quarry.game.Item.Items.Amount;

public class ConstantSupplyAmount extends Amount {

    public ConstantSupplyAmount(ItemType item, int amount) {
        super(item, amount);
    }

    public ConstantSupplyAmount(ItemCategory cat, int amount) {
        super(cat, amount);
    }
}
