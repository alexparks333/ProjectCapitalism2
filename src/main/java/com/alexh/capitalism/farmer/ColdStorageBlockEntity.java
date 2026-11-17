package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.designer.DesignerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

// The Fridge and the Freezer (Farmer.md > Freshness): a chest (SLOTS) with a fuel slot. While it burns fuel, meat in it
// (Freshness) rots SLOWER times slower: the Fridge 2x (coal or charcoal, FRIDGE_FUEL each), the Freezer 6x (an Ice
// block ICE_FUEL, a Snowball only SNOWBALL_FUEL). It only burns while there's meat in it to keep. Every second it
// works out how much of the time since it last looked was powered (pulling in more fuel as it needs it, so time it
// spent unloaded counts the same) and pushes its meat's rot time back by that much, less the slowed-down share. Out of
// fuel it's just a chest, and meat in it rots as normal. Anyone can use one; Farmers and Cooks make them. A running
// Fridge also dry-ages the fine cuts in it (Freshness.DRY_AGED / PRIME_AGED); a Freezer doesn't.
public class ColdStorageBlockEntity extends DesignerBlockEntity {

    public static final int SLOTS = 27;
    public static final int FUEL = SLOTS;
    public static final int SIZE = SLOTS + 1;
    public static final int FRIDGE_FUEL = 5 * Freshness.MINUTE;
    public static final int ICE_FUEL = 5 * Freshness.MINUTE;
    // Packed Ice and Blue Ice work too, and last longer (they take 9 of the one before to make)
    public static final int PACKED_ICE_FUEL = 3 * ICE_FUEL;
    public static final int BLUE_ICE_FUEL = 9 * ICE_FUEL;
    public static final int SNOWBALL_FUEL = 30 * 20;

    public enum Kind {
        FRIDGE("Fridge", 2),
        FREEZER("Freezer", 6);

        public final String title;
        public final int slower;

        Kind(String title, int slower) {
            this.title = title;
            this.slower = slower;
        }

        // How long one of this fuel runs it, in ticks (0: not its fuel)
        public int fuel(ItemStack stack) {
            return switch (this) {
                case FRIDGE -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? FRIDGE_FUEL : 0;
                case FREEZER -> stack.is(Items.ICE) ? ICE_FUEL : stack.is(Items.PACKED_ICE) ? PACKED_ICE_FUEL
                    : stack.is(Items.BLUE_ICE) ? BLUE_ICE_FUEL : stack.is(Items.SNOWBALL) ? SNOWBALL_FUEL : 0;
            };
        }
    }

    private final Kind kind;
    private int burnLeft;
    private int burnTotal;
    private long lastUpdate = -1;
    // a part tick of time saved, kept for next time
    private double saved;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> ColdStorageBlockEntity.this.burnLeft;
                case 1 -> ColdStorageBlockEntity.this.burnTotal;
                default -> ColdStorageBlockEntity.this.kind.ordinal();
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) ColdStorageBlockEntity.this.burnLeft = value;
            else if (index == 1) ColdStorageBlockEntity.this.burnTotal = value;
        }

        @Override
        public int getCount() {
            return 3;
        }
    };

    public ColdStorageBlockEntity(BlockPos pos, BlockState state) {
        super(state.getBlock() instanceof ColdStorageBlock block && block.kind == Kind.FREEZER
            ? ModBlockEntities.FREEZER.get() : ModBlockEntities.FRIDGE.get(), pos, state,
            state.getBlock() instanceof ColdStorageBlock block2 ? block2.kind.title : "Fridge", SIZE);
        this.kind = state.getBlock() instanceof ColdStorageBlock block ? block.kind : Kind.FRIDGE;
    }

    public Kind kind() {
        return this.kind;
    }

    public int burnLeft() {
        return this.burnLeft;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ColdStorageBlockEntity entity) {
        long now = level.getGameTime();
        if (entity.lastUpdate < 0) entity.lastUpdate = now;
        if (now - entity.lastUpdate < 20) return;
        entity.update(now, level);
        boolean lit = entity.burnLeft > 0 && entity.hasMeat(now);
        if (state.getValue(ColdStorageBlock.LIT) != lit) level.setBlock(pos, state.setValue(ColdStorageBlock.LIT, lit), 3);
    }

    private boolean hasMeat(long now) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (Freshness.rots(stack) && !Freshness.isRotten(stack, now)) return true;
        }
        return false;
    }

    // Catches up to now: burns fuel for the time there was meat in it, pushes the meat's rot time back, then turns
    // anything whose time is up to Rotten Meat (public for the tests)
    public void update(long now, ServerLevel level) {
        long elapsed = this.lastUpdate < 0 ? 0 : Math.max(0, now - this.lastUpdate);
        this.lastUpdate = now;
        for (int i = 0; i < SLOTS; i++) Freshness.stamp(this.items.get(i), now, level.getRandom());
        if (elapsed > 0 && this.hasMeat(now - elapsed)) {
            long powered = 0;
            long need = elapsed;
            while (need > 0) {
                if (this.burnLeft <= 0) {
                    ItemStack fuel = this.items.get(FUEL);
                    int ticks = this.kind.fuel(fuel);
                    if (ticks <= 0) break;
                    fuel.shrink(1);
                    this.burnLeft = this.burnTotal = ticks;
                }
                long take = Math.min(need, this.burnLeft);
                this.burnLeft -= (int) take;
                powered += take;
                need -= take;
            }
            // a Fridge dry-ages the fine cuts in it while it runs (a Freezer stops it)
            if (this.kind == Kind.FRIDGE && powered > 0) {
                for (int i = 0; i < SLOTS; i++) {
                    ItemStack stack = this.items.get(i);
                    if (!Freshness.canAge(stack) || Freshness.isRotten(stack, now)) continue;
                    int aged = (int) Math.min(Freshness.PRIME_AGED, Freshness.aging(stack) + powered);
                    if (aged != Freshness.aging(stack)) stack.set(ModComponents.AGING.get(), aged);
                }
            }
            double push = powered * (1.0 - 1.0 / this.kind.slower) + this.saved;
            long whole = (long) Math.floor(push);
            this.saved = push - whole;
            if (whole > 0) {
                for (int i = 0; i < SLOTS; i++) {
                    ItemStack stack = this.items.get(i);
                    long at = Freshness.rotsAt(stack);
                    if (Freshness.rots(stack) && at >= 0) stack.set(ModComponents.ROTS_AT.get(), at + whole);
                }
            }
        }
        boolean running = this.burnLeft > 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.items.get(i);
            if (Freshness.isRotten(stack, now)) {
                this.items.set(i, Freshness.rotted(stack));
            } else if (Freshness.rots(stack) && Freshness.rotsAt(stack) >= 0) {
                // for the tooltip: how much slower, and the time left right now
                if (running) stack.set(ModComponents.CHILL.get(), new Chill(this.kind.slower, now, Freshness.rotsAt(stack) - now));
                else stack.remove(ModComponents.CHILL.get());
            }
        }
        this.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != FUEL || this.kind.fuel(stack) > 0;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ColdStorageMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.burnLeft = input.getIntOr("burn_left", 0);
        this.burnTotal = input.getIntOr("burn_total", 0);
        this.lastUpdate = input.getLongOr("last_update", -1L);
        this.saved = input.getDoubleOr("saved", 0.0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_left", this.burnLeft);
        output.putInt("burn_total", this.burnTotal);
        output.putLong("last_update", this.lastUpdate);
        output.putDouble("saved", this.saved);
    }
}
