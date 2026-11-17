package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The Stitching Table's insides (Librarian.md): 12 cover slots round the middle (3 on each side) (Leather or Rabbit Hide), Paper in the
// middle, the thread in the fuel slot (an Iron Ingot or a Diamond: one per book) and the finished book. While there's
// Paper, a cover in EVERY ring slot and thread, it sews: after SEW_TICKS a book is done, using 1 Paper, 1 from each of
// the 12 cover slots (the whole ring) and 1 thread. A Diamond makes a Delicate Book, an Iron Ingot a Rough Book (whatever thread is in when
// it finishes). Without something it waits, keeping its progress. Hoppers: Paper and covers from the top, thread from
// the sides, books out of the bottom. Breaking it drops everything.
public class StitchingTableBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int COVERS = 12;
    public static final int PAPER = COVERS;
    public static final int THREAD = COVERS + 1;
    public static final int OUTPUT = COVERS + 2;
    public static final int SIZE = COVERS + 3;
    // saved with the items: 2 = the 12-slot ring (tables from before had 16 cover slots: Paper 16, thread 17, books 18)
    private static final int LAYOUT = 2;
    public static final int SEW_TICKS = 18000;   // 15 minutes

    private static final int[] TOP = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, PAPER};
    private static final int[] SIDES = {THREAD};
    private static final int[] BOTTOM = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return StitchingTableBlockEntity.this.progress;
        }

        @Override
        public void set(int index, int value) {
            StitchingTableBlockEntity.this.progress = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public StitchingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STITCHING_TABLE.get(), pos, state);
    }

    // For the renderer: the Paper, the thread and the finished books on the mat
    public ItemStack paper() {
        return this.items.get(PAPER);
    }

    public ItemStack thread() {
        return this.items.get(THREAD);
    }

    public ItemStack output() {
        return this.items.get(OUTPUT);
    }

    public static boolean isCover(ItemStack stack) {
        return stack.is(Items.LEATHER) || stack.is(Items.RABBIT_HIDE);
    }

    public static boolean isThread(ItemStack stack) {
        return stack.is(Items.IRON_INGOT) || stack.is(Items.DIAMOND);
    }

    // A cover in every one of the 12 ring slots
    public boolean ringFull() {
        for (int i = 0; i < COVERS; i++) {
            if (this.items.get(i).isEmpty()) return false;
        }
        return true;
    }

    // The book the thread in now makes, or null
    private @Nullable Item book() {
        ItemStack thread = this.items.get(THREAD);
        if (thread.is(Items.DIAMOND)) return ModItems.DELICATE_BOOK.get();
        if (thread.is(Items.IRON_INGOT)) return ModItems.ROUGH_BOOK.get();
        return null;
    }

    private boolean canSew() {
        Item book = this.book();
        if (book == null || this.items.get(PAPER).isEmpty() || !this.ringFull()) return false;
        ItemStack out = this.items.get(OUTPUT);
        return out.isEmpty() || (out.is(book) && out.getCount() < out.getMaxStackSize());
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, StitchingTableBlockEntity table) {
        boolean sewing = table.canSew();
        if (sewing) {
            table.progress++;
            if (table.progress % 100 == 0) table.setChanged();
            // the needle going through: a soft pull every second and a half, now and then the thread snapping tight
            if (table.progress % 30 == 0) {
                level.playSound(null, pos, SoundEvents.WOOL_HIT, SoundSource.BLOCKS, 0.35F, 1.6F + level.getRandom().nextFloat() * 0.3F);
            }
            if (table.progress % 90 == 45) {
                level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.15F, 1.8F);
            }
            if (table.progress >= SEW_TICKS) {
                table.progress = 0;
                table.finish();
                level.playSound(null, pos, SoundEvents.BOOK_PUT, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.4, pos.getY() + 1.0, pos.getZ() + 0.55, 10, 0.2, 0.1, 0.2, 0);
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 0.6F, 1.3F);
            }
        }
        if (state.getValue(StitchingTableBlock.SEWING) != sewing) {
            level.setBlock(pos, state.setValue(StitchingTableBlock.SEWING, sewing), Block.UPDATE_ALL);
        }
    }

    // A book is done: use the Paper, the covers (round the ring) and the thread
    private void finish() {
        Item book = this.book();
        if (book == null) return;
        this.items.get(PAPER).shrink(1);
        this.items.get(THREAD).shrink(1);
        for (int i = 0; i < COVERS; i++) this.items.get(i).shrink(1);
        ItemStack out = this.items.get(OUTPUT);
        if (out.isEmpty()) this.items.set(OUTPUT, new ItemStack(book));
        else out.grow(1);
        this.setChanged();
        this.syncToClients();
    }

    // What's on the mat (Paper, thread, the finished book) is drawn by client/StitchingTableRenderer: send it when it changes
    private void syncToClients() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Stitching Table");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new StitchingTableMenu(containerId, inventory, this, this.data);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        if (input.getIntOr("layout", 1) >= LAYOUT) {
            ContainerHelper.loadAllItems(input, this.items);
        } else {
            // an old 16-slot table: Paper, thread and books move to their new slots, the 4 extra covers join the others
            NonNullList<ItemStack> old = NonNullList.withSize(19, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(input, old);
            for (int i = 0; i < COVERS; i++) this.items.set(i, old.get(i));
            this.items.set(PAPER, old.get(16));
            this.items.set(THREAD, old.get(17));
            this.items.set(OUTPUT, old.get(18));
            for (int i = COVERS; i < 16; i++) {
                ItemStack extra = old.get(i);
                for (int j = 0; j < COVERS && !extra.isEmpty(); j++) {
                    ItemStack cover = this.items.get(j);
                    if (cover.isEmpty()) {
                        this.items.set(j, extra.copy());
                        extra.setCount(0);
                    } else if (ItemStack.isSameItemSameComponents(cover, extra)) {
                        int move = Math.min(extra.getCount(), cover.getMaxStackSize() - cover.getCount());
                        cover.grow(move);
                        extra.shrink(move);
                    }
                }
            }
        }
        this.progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("layout", LAYOUT);
        output.putInt("progress", this.progress);
    }

    // ── Container ──

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.UP ? TOP : side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return this.canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT && side == Direction.DOWN;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot < COVERS) return isCover(stack);
        if (slot == PAPER) return stack.is(Items.PAPER);
        if (slot == THREAD) return isThread(stack);
        return false;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, count);
        if (!out.isEmpty()) {
            this.setChanged();
            this.syncToClients();
        }
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
        this.syncToClients();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }
}
