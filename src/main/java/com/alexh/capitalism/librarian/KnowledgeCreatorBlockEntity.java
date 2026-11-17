package com.alexh.capitalism.librarian;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

// The Knowledge Creator's insides (Librarian.md): an Enchanted Book goes in, and after MAKE_TICKS (30 seconds) it's
// a Knowledge Piece whose quality comes from the book's power (KnowledgeQuality: levels added up, treasure +3). It
// waits if the output holds a different quality or is full. Hoppers: books in from the top and sides, pieces out of
// the bottom. What's inside (and how far along) is synced to clients for client/KnowledgeCreatorRenderer.
public class KnowledgeCreatorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int BOOK = 0;
    public static final int OUTPUT = 1;
    public static final int MAKE_TICKS = 600;
    private static final int[] IN = {BOOK};
    private static final int[] OUT = {OUTPUT};

    private NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return KnowledgeCreatorBlockEntity.this.progress;
        }

        @Override
        public void set(int index, int value) {
            KnowledgeCreatorBlockEntity.this.progress = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public KnowledgeCreatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.KNOWLEDGE_CREATOR.get(), pos, state);
    }

    public static boolean isBook(ItemStack stack) {
        return KnowledgeQuality.power(stack) > 0;
    }

    public ItemStack book() {
        return this.items.get(BOOK);
    }

    public int progress() {
        return this.progress;
    }

    private boolean canMake() {
        ItemStack book = this.items.get(BOOK);
        if (!isBook(book)) return false;
        ItemStack out = this.items.get(OUTPUT);
        if (out.isEmpty()) return true;
        ItemStack made = KnowledgeQuality.byPower(KnowledgeQuality.power(book)).stack(1);
        return ItemStack.isSameItemSameComponents(out, made) && out.getCount() < out.getMaxStackSize();
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, KnowledgeCreatorBlockEntity creator) {
        boolean working = creator.canMake();
        if (working) {
            creator.progress++;
            if (creator.progress % 20 == 0) {
                creator.setChanged();
                creator.syncToClients();
            }
            if (creator.progress % 40 == 0) {
                level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.25F, 0.6F + level.getRandom().nextFloat() * 0.3F);
            }
            if (creator.progress >= MAKE_TICKS) {
                creator.progress = 0;
                creator.finish();
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.0F);
                level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.4F, 1.6F);
                level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.15, 0.2, 0.15, 0.02);
            }
        } else if (creator.progress > 0 && !isBook(creator.items.get(BOOK))) {
            // the book was taken out: start over next time
            creator.progress = 0;
            creator.syncToClients();
        }
        if (state.getValue(KnowledgeCreatorBlock.WORKING) != working) {
            level.setBlock(pos, state.setValue(KnowledgeCreatorBlock.WORKING, working), Block.UPDATE_ALL);
        }
    }

    private void finish() {
        ItemStack book = this.items.get(BOOK);
        KnowledgeQuality quality = KnowledgeQuality.byPower(KnowledgeQuality.power(book));
        book.shrink(1);
        ItemStack out = this.items.get(OUTPUT);
        if (out.isEmpty()) this.items.set(OUTPUT, quality.stack(1));
        else out.grow(1);
        this.setChanged();
        this.syncToClients();
    }

    private void syncToClients() {
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Knowledge Creator");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new KnowledgeCreatorMenu(containerId, inventory, this, this.data);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(2, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
    }

    // ── Container ──

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUT : IN;
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
        return slot == BOOK && isBook(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public int getContainerSize() {
        return 2;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(BOOK).isEmpty() && this.items.get(OUTPUT).isEmpty();
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
        // a different book: start over
        if (slot == BOOK && !ItemStack.isSameItemSameComponents(stack, this.items.get(BOOK))) this.progress = 0;
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
