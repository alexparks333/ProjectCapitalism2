package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// The Lapis Compressor's insides: one slot for up to a stack of Lapis Lazuli (put in through its menu,
// LapisCompressorMenu, or by a hopper from any side). Every PRESS_TICKS it crushes one Lapis into XP_PER_LAPIS XP
// points, spat out of its front as XP orbs. Lit while it has Lapis. Breaking it drops what's left.
public class LapisCompressorBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {

    public static final int PRESS_TICKS = 40;
    public static final int XP_PER_LAPIS = 4;
    private static final int[] SLOTS = {0};

    private NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    private int progress;
    // synced to the menu: how far the press is
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return LapisCompressorBlockEntity.this.progress;
        }

        @Override
        public void set(int index, int value) {
            LapisCompressorBlockEntity.this.progress = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public LapisCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAPIS_COMPRESSOR.get(), pos, state);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, LapisCompressorBlockEntity compressor) {
        ItemStack lapis = compressor.items.get(0);
        boolean working = !lapis.isEmpty();
        if (working && ++compressor.progress >= PRESS_TICKS) {
            compressor.progress = 0;
            lapis.shrink(1);
            Direction front = state.getValue(LapisCompressorBlock.FACING);
            Vec3 mouth = Vec3.atCenterOf(pos).add(front.getStepX() * 0.6, -0.25, front.getStepZ() * 0.6);
            Vec3 push = new Vec3(front.getStepX() * 0.15, 0.1, front.getStepZ() * 0.15);
            ExperienceOrb.awardWithDirection(level, mouth, push, XP_PER_LAPIS);
            level.sendParticles(ParticleTypes.ENCHANT, mouth.x, mouth.y + 0.2, mouth.z, 4, 0.15, 0.1, 0.15, 0.3);
            if (level.getRandom().nextInt(4) == 0) {
                level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.25F, 1.4F + level.getRandom().nextFloat() * 0.2F);
            }
            compressor.setChanged();
        }
        if (!working) compressor.progress = 0;
        boolean lit = !compressor.items.get(0).isEmpty();
        if (state.getValue(LapisCompressorBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(LapisCompressorBlock.LIT, lit), Block.UPDATE_ALL);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Lapis Compressor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new LapisCompressorMenu(containerId, inventory, this, this.data);
    }

    public int stored() {
        return this.items.get(0).getCount();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null) Containers.dropContents(this.level, pos, this.items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("progress", this.progress);
    }

    // ── Container (hoppers put Lapis in from any side; nothing comes out) ──

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return stack.is(Items.LAPIS_LAZULI);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI);
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.items.get(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, count);
        if (!out.isEmpty()) this.setChanged();
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
