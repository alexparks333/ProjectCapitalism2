package com.alexh.capitalism.realestate;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.designer.DesignerBlockEntity;
import com.alexh.capitalism.economy.GoldMoney;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// The City Chest (RealEstate.md > Buying a lot): the only way to buy a lot off the city. An op (in creative) puts it on
// a lot and fills its top row (WANTED, shown ghosted) with what the city wants. Right under each wanted thing is its
// own slot (DEPOSIT): a Real Estate Agent puts that thing in it (a gold slot takes any gold: nuggets, ingots or blocks).
// What they put in stays (it can take several trips), and the moment every slot is filled (covers), those are used up,
// any extra comes back to them (gold over the price as change), the chest disappears and the lot is theirs.
public class CityChestBlockEntity extends DesignerBlockEntity {

    public static final int WANTED = 9;
    public static final int DEPOSIT = 9;
    public static final int SIZE = WANTED + DEPOSIT;

    public CityChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CITY_CHEST.get(), pos, state, "City Chest", SIZE);
    }

    public @Nullable Property lot() {
        return this.level == null ? null : Properties.at(this.level, this.worldPosition);
    }

    // ── matching: one wanted thing, the slot under it ──

    // Can this go in the slot under that wanted thing? (gold wanted: any gold)
    public static boolean fits(ItemStack want, ItemStack stack) {
        if (want.isEmpty() || stack.isEmpty()) return false;
        if (GoldMoney.valueOf(want) > 0) return GoldMoney.valueOf(stack) > 0;
        return ItemStack.isSameItem(want, stack);
    }

    // Is that slot filled? (gold: worth at least as much)
    public static boolean covers(ItemStack want, ItemStack have) {
        if (want.isEmpty()) return true;
        if (!fits(want, have)) return false;
        long wantGold = GoldMoney.valueOf(want);
        return wantGold > 0 ? GoldMoney.valueOf(have) >= wantGold : have.getCount() >= want.getCount();
    }

    // What paying would hand back if every wanted thing is covered by the slot under it; null if not yet. The lists are
    // slot by slot (WANTED and DEPOSIT, empty stacks included). Public for the tests.
    public static @Nullable List<ItemStack> leftover(List<ItemStack> wanted, List<ItemStack> deposit) {
        boolean any = false;
        List<ItemStack> back = new ArrayList<>();
        for (int i = 0; i < wanted.size(); i++) {
            ItemStack want = wanted.get(i);
            ItemStack have = i < deposit.size() ? deposit.get(i) : ItemStack.EMPTY;
            if (want.isEmpty()) {
                if (!have.isEmpty()) back.add(have.copy());
                continue;
            }
            any = true;
            if (!covers(want, have)) return null;
            long wantGold = GoldMoney.valueOf(want);
            if (wantGold > 0) back.addAll(GoldMoney.toStacks(GoldMoney.valueOf(have) - wantGold));
            else if (have.getCount() > want.getCount()) back.add(have.copyWithCount(have.getCount() - want.getCount()));
        }
        return any ? back : null;
    }

    private List<ItemStack> row(int from) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < WANTED; i++) out.add(this.items.get(from + i));
        return out;
    }

    public List<ItemStack> wanted() {
        return this.row(0);
    }

    public List<ItemStack> deposit() {
        return this.row(WANTED);
    }

    // Is everything in? Then the lot is the agent's, the rest goes back to them, and the chest is gone
    public boolean tryClaim(ServerPlayer agent) {
        if (!(this.level instanceof ServerLevel level)) return false;
        Property lot = this.lot();
        if (lot == null || !lot.cityOwned() || !Properties.isAgent(agent)) return false;
        List<ItemStack> back = leftover(this.wanted(), this.deposit());
        if (back == null) return false;
        // first every item in the chest is deleted (the price and what was paid), then the lot is theirs
        for (int i = 0; i < SIZE; i++) this.items.set(i, ItemStack.EMPTY);
        this.setChanged();
        String wrong = Properties.get(level.getServer()).claimFromCity(agent, lot);
        if (wrong != null) {
            agent.sendSystemMessage(Component.literal("[Real Estate] " + wrong));
            return false;
        }
        for (ItemStack stack : back) {
            if (!agent.getInventory().add(stack)) agent.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        BlockPos pos = this.worldPosition;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.1F);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        agent.sendSystemMessage(Component.literal("[Real Estate] You unlocked " + lot.name + ": it's yours to build on, sell or lease."));
        return true;
    }

    // Nothing goes in or out except by hand through its menu: no hoppers, hopper minecarts or other automation. (The
    // wanted row, the city's price, can only ever be taken by an op setting it up in creative.)
    @Override
    public boolean canTakeItem(net.minecraft.world.Container target, int slot, ItemStack stack) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // Broken (only an op in creative can; withers and the dragon can't, explosions can't): what an agent had put in
    // drops; the wanted list, the city's price, never does
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level == null) return;
        for (ItemStack stack : this.deposit()) Containers.dropItemStack(this.level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        boolean setup = com.alexh.capitalism.town.TownProtection.canBuild(player) && player.isCreative();
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                return setup ? 1 : 0;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        return new CityChestMenu(containerId, inventory, this, data);
    }

    @Override
    public Component getDisplayName() {
        Property lot = this.lot();
        return Component.literal(lot == null ? "City Chest" : "City Chest: " + lot.name);
    }
}
