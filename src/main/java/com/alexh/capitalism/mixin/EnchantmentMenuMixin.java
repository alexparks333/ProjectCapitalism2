package com.alexh.capitalism.mixin;

import com.alexh.capitalism.enchanter.EnchantingLimit;
import com.alexh.capitalism.enchanter.NpcEnchanting;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Only Enchanters enchant past level 10 (enchanter/EnchantingLimit): the Enchanting Table works out its three costs
// (when an item goes in), and for anyone else each is capped at 10, so the enchantments rolled are level-10 ones too.
// Books are only for Librarians: anyone else gets 0 (no options) for a book.
// The Enchanter NPC's table (enchanter/NpcEnchanting) is this menu too, marked with its NPC: as if 15 bookshelves stood
// round it, no level-10 cap (books still Librarians' only), no lapis (a locked slot shows some), gold for each enchant,
// and open while you're near the NPC instead of a table.
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin implements NpcEnchanting.Menu {

    @Shadow @Final private Container enchantSlots;

    @Shadow @Final public int[] costs;

    @Unique
    private Player capitalism$player;

    @Unique
    private Entity capitalism$npc;

    @Unique
    private long capitalism$price;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL"))
    private void capitalism$rememberPlayer(int containerId, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.capitalism$player = inventory.player;
    }

    @Override
    public void capitalism$setNpc(Entity npc) {
        this.capitalism$npc = npc;
        NonNullList<Slot> slots = ((EnchantmentMenu) (Object) this).slots;
        Slot old = slots.get(1);
        Slot locked = new NpcEnchanting.LockedSlot(this.enchantSlots, 1, old.x, old.y);
        locked.index = 1;
        slots.set(1, locked);
        this.enchantSlots.setItem(1, new ItemStack(Items.LAPIS_LAZULI, 3));
    }

    @WrapOperation(method = "lambda$slotsChanged$0", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getEnchantmentCost(Lnet/minecraft/util/RandomSource;IILnet/minecraft/world/item/ItemStack;)I"))
    private int capitalism$capCost(RandomSource random, int slot, int bookcases, ItemStack stack, Operation<Integer> original) {
        // the NPC's table: a full set of bookshelves (NeoForge reads each shelf's enchanting power from the real blocks, so
        // pretending the spots hold shelves isn't enough; the cost is simply worked out with 15)
        int cost = original.call(random, slot, this.capitalism$npc != null ? 15 : bookcases, stack);
        if (this.capitalism$npc != null && !stack.is(Items.BOOK)) return cost;
        return EnchantingLimit.cost(this.capitalism$player, stack, cost);
    }

    // the NPC wants gold first (it's checked before the levels and the enchanting)
    @Inject(method = "clickMenuButton", at = @At("HEAD"), cancellable = true)
    private void capitalism$npcPrice(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        this.capitalism$price = 0;
        if (this.capitalism$npc == null || player.level().isClientSide() || id < 0 || id >= this.costs.length || this.costs[id] <= 0) return;
        long price = NpcEnchanting.price(this.costs[id], player);
        if (!NpcEnchanting.canPay(player, price)) {
            cir.setReturnValue(false);
            return;
        }
        this.capitalism$price = price;
    }

    // ...and takes it once it's enchanted, and tops the lapis back up
    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void capitalism$npcPaid(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        if (this.capitalism$npc == null || player.level().isClientSide()) return;
        if (cir.getReturnValueZ() && this.capitalism$price > 0) NpcEnchanting.paid(player, this.capitalism$price);
        this.capitalism$price = 0;
        this.enchantSlots.setItem(1, new ItemStack(Items.LAPIS_LAZULI, 3));
    }

    @Inject(method = "stillValid", at = @At("HEAD"), cancellable = true)
    private void capitalism$nearNpc(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (this.capitalism$npc != null) cir.setReturnValue(this.capitalism$npc.isAlive() && player.distanceToSqr(this.capitalism$npc) < 64.0);
    }

    // closing: the shown lapis isn't yours to keep (only your item comes back)
    @Inject(method = "removed", at = @At("HEAD"))
    private void capitalism$noFreeLapis(Player player, CallbackInfo ci) {
        if (this.capitalism$npc != null) this.enchantSlots.setItem(1, ItemStack.EMPTY);
    }
}
