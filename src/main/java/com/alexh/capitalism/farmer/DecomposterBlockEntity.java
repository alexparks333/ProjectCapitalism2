package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.designer.DesignerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// The Decomposter (Farmer.md > Decomposter): like a furnace without fuel. Three ingredient slots (in any order) rot
// down for TICKS into the output: Bone Meal, Rotten Flesh (or Rotten Meat) and Dirt make 2 Yield Fertilizer; Bone Meal and two
// Redstone make 1 Super Yield Fertilizer; Bone Meal, Sugar and Dirt make 2 Fast Fertilizer. It keeps going while the
// ingredients last and the output has room.
public class DecomposterBlockEntity extends DesignerBlockEntity {

    public static final int INPUTS = 3;
    public static final int OUTPUT = INPUTS;
    public static final int SIZE = OUTPUT + 1;
    public static final int TICKS = 200;

    // What it makes: its three ingredients (any order) and how many come out
    public enum Recipe {
        FERTILIZER(ModItems.FERTILIZER, 2, Items.BONE_MEAL, Items.ROTTEN_FLESH, Items.DIRT),
        SUPER_FERTILIZER(ModItems.SUPER_FERTILIZER, 1, Items.BONE_MEAL, Items.REDSTONE, Items.REDSTONE),
        FAST_FERTILIZER(ModItems.FAST_FERTILIZER, 2, Items.BONE_MEAL, Items.SUGAR, Items.DIRT);

        private final Supplier<? extends Item> result;
        public final int count;
        public final List<Item> ingredients;

        Recipe(Supplier<? extends Item> result, int count, Item... ingredients) {
            this.result = result;
            this.count = count;
            this.ingredients = List.of(ingredients);
        }

        public ItemStack result() {
            return new ItemStack(this.result.get(), this.count);
        }

        // These three stacks hold exactly this recipe's ingredients, in any order
        boolean matches(List<ItemStack> inputs) {
            List<Item> wanted = new ArrayList<>(this.ingredients);
            for (ItemStack stack : inputs) {
                if (stack.isEmpty() || !wanted.remove(standIn(stack))) return false;
            }
            return wanted.isEmpty();
        }
    }

    private int progress;
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return DecomposterBlockEntity.this.progress;
        }

        @Override
        public void set(int index, int value) {
            DecomposterBlockEntity.this.progress = value;
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public DecomposterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DECOMPOSTER.get(), pos, state, "Decomposter", SIZE);
    }

    // Rotten Meat (farmer/Freshness) does in place of Rotten Flesh
    static Item standIn(ItemStack stack) {
        return stack.is(ModItems.ROTTEN_MEAT.get()) ? Items.ROTTEN_FLESH : stack.getItem();
    }

    public static @Nullable Recipe find(List<ItemStack> inputs) {
        for (Recipe recipe : Recipe.values()) {
            if (recipe.matches(inputs)) return recipe;
        }
        return null;
    }

    // Anything a recipe takes
    public static boolean isIngredient(ItemStack stack) {
        for (Recipe recipe : Recipe.values()) {
            if (recipe.ingredients.contains(standIn(stack))) return true;
        }
        return false;
    }

    private @Nullable Recipe working() {
        Recipe recipe = find(this.items.subList(0, INPUTS));
        if (recipe == null) return null;
        ItemStack out = this.items.get(OUTPUT);
        ItemStack made = recipe.result();
        boolean room = out.isEmpty() || (ItemStack.isSameItemSameComponents(out, made) && out.getCount() + made.getCount() <= out.getMaxStackSize());
        return room ? recipe : null;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, DecomposterBlockEntity entity) {
        Recipe recipe = entity.working();
        boolean busy = recipe != null;
        // full while any ingredient is in it
        boolean full = false;
        for (int i = 0; i < INPUTS; i++) full |= !entity.items.get(i).isEmpty();
        if (state.getValue(DecomposterBlock.WORKING) != busy || state.getValue(DecomposterBlock.FULL) != full) {
            level.setBlock(pos, state.setValue(DecomposterBlock.WORKING, busy).setValue(DecomposterBlock.FULL, full), 3);
        }
        if (recipe == null) {
            if (entity.progress != 0) {
                entity.progress = 0;
                entity.setChanged();
            }
            return;
        }
        entity.progress++;
        if (entity.progress % 40 == 0) {
            level.sendParticles(ParticleTypes.COMPOSTER, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 3, 0.25, 0.05, 0.25, 0.0);
            level.playSound(null, pos, SoundEvents.COMPOSTER_FILL, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
        if (entity.progress >= TICKS) {
            entity.progress = 0;
            for (int i = 0; i < INPUTS; i++) entity.items.get(i).shrink(1);
            ItemStack out = entity.items.get(OUTPUT);
            if (out.isEmpty()) entity.items.set(OUTPUT, recipe.result());
            else out.grow(recipe.count);
            level.playSound(null, pos, SoundEvents.COMPOSTER_READY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        entity.setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < INPUTS && isIngredient(stack);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new DecomposterMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.progress = input.getIntOr("progress", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("progress", this.progress);
    }
}
