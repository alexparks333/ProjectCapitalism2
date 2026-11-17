package com.alexh.capitalism.designer;

import com.alexh.capitalism.back.Back;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;


// What's in the bag on a player's back (Designer.md > Bags). The items live in the worn bag itself (its
// minecraft:container component), but ONLY while it's worn: the moment a bag comes off your back (taken out of the Back
// slot, swapped, shift-clicked away, dying...) everything in it spills on the ground and you get the empty bag. So a bag
// in your inventory, a chest or on the ground is always empty: no stacking full bags for endless space.
// The bag slots (BagSlot) share one Holder per inventory (both sides): a live container loaded from the worn bag and
// written back into it whenever it changes, loaded again when a different bag goes on. (Kept with the inventory, not
// in a map by player: a player being made has no id yet to look it up by.)
public final class Bags {

    private Bags() {
    }

    // The bag on this player's back, or null
    public static @Nullable Bag worn(Player player) {
        return Bag.of(Back.peek(player));
    }

    // The container behind one inventory's bag slots: Bag.MAX_SLOTS long; a bag uses its first `slots`
    public static final class Holder {
        private final Player player;
        private @Nullable Live live;

        public Holder(Player player) {
            this.player = player;
        }

        public SimpleContainer container() {
            ItemStack worn = Back.peek(this.player);
            if (this.live == null || this.live.bag != worn) this.live = new Live(worn);
            return this.live;
        }
    }

    // Everything in this bag (the stack itself is changed: it's empty afterwards). Dropped at the player's feet on the
    // server; on the client it's just emptied (the server does the dropping).
    public static void spill(Player player, ItemStack bag) {
        ItemContainerContents contents = bag.get(DataComponents.CONTAINER);
        if (contents == null || Bag.of(bag) == null) return;
        bag.remove(DataComponents.CONTAINER);
        if (player.level().isClientSide()) return;
        contents.nonEmptyItemCopyStream().forEach(item -> player.drop(item, false, net.minecraft.util.Prediction.SERVER_ONLY));
    }

    public static boolean hasContents(ItemStack bag) {
        ItemContainerContents contents = bag.get(DataComponents.CONTAINER);
        return contents != null && contents.nonEmptyItemCopyStream().findAny().isPresent();
    }

    // The bag slots' container: the worn bag's contents, written back into it on every change
    private static final class Live extends SimpleContainer {
        private final ItemStack bag;
        private boolean loading;

        Live(ItemStack bag) {
            super(Bag.MAX_SLOTS);
            this.bag = bag;
            ItemContainerContents contents = bag.get(DataComponents.CONTAINER);
            if (contents != null) {
                NonNullList<ItemStack> items = NonNullList.withSize(Bag.MAX_SLOTS, ItemStack.EMPTY);
                contents.copyInto(items);
                this.loading = true;
                for (int i = 0; i < Bag.MAX_SLOTS; i++) this.setItem(i, items.get(i));
                this.loading = false;
            }
        }

        @Override
        public void setChanged() {
            super.setChanged();
            if (this.loading || Bag.of(this.bag) == null) return;
            boolean empty = true;
            for (int i = 0; i < this.getContainerSize(); i++) {
                if (!this.getItem(i).isEmpty()) empty = false;
            }
            if (empty) this.bag.remove(DataComponents.CONTAINER);
            else this.bag.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.getItems()));
        }
    }
}
