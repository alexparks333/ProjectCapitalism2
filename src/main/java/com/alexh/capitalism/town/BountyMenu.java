package com.alexh.capitalism.town;

import com.alexh.capitalism.bounty.Bounties;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// Talking to the Bounty Clerk (client/BountyClerkScreen; the rules are bounty/Bounties): no slots, only buttons.
//  - TAKE: pay the 2-nugget fee and take the posted task (it replaces theirs; the screen asks first).
//  - TURN_IN / ABANDON: turn in (or give up) their task.
//  - CLAIM: their milestone star.
// The screen reads the player's bounties straight from their synced data (ModAttachments.BOUNTIES). Their inventory is in
// the menu, hidden (no slots on screen), only so its changes reach their screen while it's open (gold, deliveries).
public class BountyMenu extends AbstractContainerMenu {

    public static final int TAKE = 100;
    public static final int CLAIM = 101;
    public static final int TURN_IN = 200;
    public static final int ABANDON = 300;
    public static final int INV_START = 0;

    private final @Nullable BountyClerk clerk;

    public BountyMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public BountyMenu(int containerId, Inventory inventory, @Nullable BountyClerk clerk) {
        super(ModMenus.BOUNTY_CLERK.get(), containerId);
        this.clerk = clerk;
        for (int i = 0; i < 36; i++) {
            this.addSlot(new net.minecraft.world.inventory.Slot(inventory, i, -2000, -2000) {
                @Override
                public boolean isActive() {
                    return false;
                }
            });
        }
    }

    public @Nullable BountyClerk clerk() {
        return this.clerk;
    }

    private static void say(ServerPlayer player, String line) {
        player.sendOverlayMessage(Component.literal(line).withStyle(s -> s.withColor(TextColor.fromRgb(BountyClerk.COLOR)).withItalic(true)));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer p)) return true;
        if (id == TAKE) {
            switch (Bounties.take(p)) {
                case OK -> say(p, "\"Signed and stamped. It's yours, take as long as you need.\"");
                case ALREADY_TODAY -> say(p, "\"One a day, friend. Come back tomorrow.\"");
                case CANT_DO_IT -> say(p, "\"That one's not for you, I'm afraid. Another will be posted soon.\"");
                case NO_GOLD -> say(p, "\"The fee's two gold nuggets, I'm afraid.\"");
            }
            return true;
        }
        if (id == CLAIM) {
            if (!Bounties.claim(p)) say(p, "\"Nothing for you to claim in my ledger, I'm afraid.\"");
            return true;
        }
        if (id == TURN_IN) {
            if (!Bounties.turnIn(p, this.clerk)) say(p, "\"That one isn't done yet.\"");
            return true;
        }
        if (id == ABANDON) {
            if (Bounties.abandon(p)) say(p, "\"Struck from the ledger. No refunds, mind.\"");
            return true;
        }        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.clerk == null || (this.clerk.isAlive() && player.distanceToSqr(this.clerk) < 8 * 8);
    }
}
