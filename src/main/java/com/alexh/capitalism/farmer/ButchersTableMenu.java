package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModMenus;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

// The Butcher's Table's menu (client/ButchersTableScreen): a slot for a Meat Slab and one for the Butcher Knife that
// cuts it (ButcherKnifeItem). Buttons: CUT starts cutting it up: the slab is taken right away (so walking off mid-cut
// can't save it) and the knife uses one cut of its sharpness (on its last cut it's spent and breaks); then the butchery game (client/ButcheryGame)
// sends SUCCESS (the cut itself, MeatCut.yield of it) or FAILED (that much plain raw meat). Closing the menu or Esc mid-cut
// counts as failed.
public class ButchersTableMenu extends AbstractContainerMenu {

    public static final int CUT = 0;
    public static final int SUCCESS = 1;
    public static final int FAILED = 2;

    public static final int SLAB_X = 80;
    public static final int SLAB_Y = 44;
    public static final int KNIFE_X = 16;
    public static final int KNIFE_Y = 24;
    private static final int INV_START = 2;
    private static final int INV_END = INV_START + 36;

    // the slab (given back when the menu closes); the knife is the table's own (ButchersTableBlockEntity), kept
    private final Container container = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            ButchersTableMenu.this.slotsChanged(this);
        }
    };
    private final Container knives;
    private final ContainerLevelAccess access;
    // 0: the slab being cut up (MeatCut ordinal + 1; 0 when none)
    private final ContainerData data = new SimpleContainerData(1);
    private @Nullable MeatCut cutting;
    // the slab being cut up: how much it gives (its lean, Freshness.yield) and when its cuts rot (its time left plus a quarter)
    private int cuttingCount;
    private long cuttingRotsAt;

    public ButchersTableMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(1), ContainerLevelAccess.NULL);
    }

    public ButchersTableMenu(int containerId, Inventory inventory, Container knives, ContainerLevelAccess access) {
        super(ModMenus.BUTCHERS_TABLE.get(), containerId);
        this.knives = knives;
        this.access = access;
        this.addSlot(new Slot(this.container, 0, SLAB_X, SLAB_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MeatCut.ofSlab(stack) != null;
            }
        });
        this.addSlot(new Slot(knives, 0, KNIFE_X, KNIFE_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ButcherKnifeItem.isKnife(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        this.addStandardInventorySlots(inventory, 8, 84);
        this.addDataSlots(this.data);
    }

    public ItemStack slab() {
        return this.slots.get(0).getItem();
    }

    public ItemStack knife() {
        return this.slots.get(1).getItem();
    }

    // The slab being cut up right now (synced), or null
    public @Nullable MeatCut cutting() {
        int index = this.data.get(0) - 1;
        return index < 0 || index >= MeatCut.values().length ? null : MeatCut.values()[index];
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return true;
        if (id == CUT) {
            MeatCut cut = MeatCut.ofSlab(this.slab());
            if (this.cutting != null || cut == null || ButcherKnifeItem.cutsLeft(this.knife()) <= 0) return false;
            long now = serverPlayer.level().getGameTime();
            Freshness.stamp(this.slab(), now, serverPlayer.getRandom());
            if (Freshness.isRotten(this.slab(), now)) return false;
            this.cutting = cut;
            this.cuttingCount = Freshness.yield(cut, this.slab());
            this.cuttingRotsAt = Freshness.cutRotsAt(now, Freshness.ticksLeft(this.slab(), now));
            this.slab().shrink(1);
            if (ButcherKnifeItem.useCut(this.knife())) {
                // its last cut: the knife is spent
                this.slots.get(1).set(ItemStack.EMPTY);
                this.access.execute((level, pos) -> level.playSound(null, pos, SoundEvents.ITEM_BREAK.value(), SoundSource.BLOCKS, 0.8F, 0.9F));
                serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Your Butcher Knife is spent."));
            }
            this.container.setChanged();
            this.data.set(0, cut.ordinal() + 1);
        } else if (id == SUCCESS || id == FAILED) {
            if (this.cutting == null) return false;
            this.finish(serverPlayer, id == SUCCESS);
        }
        this.broadcastChanges();
        return true;
    }

    // The slab is cut up: what it gives goes into the inventory (or drops)
    private void finish(ServerPlayer player, boolean success) {
        MeatCut cut = this.cutting;
        if (cut == null) return;
        this.cutting = null;
        this.data.set(0, 0);
        // a clean job: the cut, on the slab's timer plus a quarter; botched: plain meat, which doesn't rot
        // (cuts don't stack: each piece is its own item, all on the same timer)
        java.util.List<ItemStack> results = new java.util.ArrayList<>();
        if (success) {
            for (int i = 0; i < this.cuttingCount; i++) {
                ItemStack piece = cut.result(true).copyWithCount(1);
                piece.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), this.cuttingRotsAt);
                results.add(piece);
            }
        } else {
            results.add(cut.result(false).copyWithCount(this.cuttingCount));
        }
        for (ItemStack result : results) {
            if (player.isAlive() && !player.hasDisconnected()) player.getInventory().placeItemBackInInventory(result, net.minecraft.util.Prediction.SERVER_ONLY);
            else player.drop(result, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        this.access.execute((level, pos) -> level.playSound(null, pos,
            success ? SoundEvents.VILLAGER_WORK_BUTCHER : SoundEvents.SLIME_SQUISH, SoundSource.BLOCKS, 1.0F, success ? 1.0F : 0.8F));
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) this.finish(serverPlayer, false);
        this.access.execute((level, pos) -> this.clearContainer(player, this.container));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        if (slotIndex < INV_START) {
            if (!this.moveItemStackTo(stack, INV_START, INV_END, true)) return ItemStack.EMPTY;
        } else if (ButcherKnifeItem.isKnife(stack)) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (MeatCut.ofSlab(stack) == null || !this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == clicked.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return clicked;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.BUTCHERS_TABLE.get());
    }
}
