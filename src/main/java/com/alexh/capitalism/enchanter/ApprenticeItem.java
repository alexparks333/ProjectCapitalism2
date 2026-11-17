package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

// A picked-up Apprentice: its armor (CONTAINER: head, chest, legs, feet), its kind and its stored XP. Placed like an
// armor stand (clicking a Transfer Tube booth stands it on the booth's floor); whoever places it owns it. Only made by
// picking up an Apprentice (an empty one won't place).
public class ApprenticeItem extends Item {

    public ApprenticeItem(Item.Properties properties) {
        super(properties);
    }

    public static Apprentice.@Nullable Kind kind(ItemStack stack) {
        return Apprentice.Kind.byName(stack.get(ModComponents.APPRENTICE_KIND.get()));
    }

    public static int storedXp(ItemStack stack) {
        return stack.getOrDefault(ModComponents.APPRENTICE_XP.get(), 0);
    }

    // The bottom block of the Transfer Tube booth at this spot (either half of a booth), or null
    private static @Nullable BlockPos boothFloor(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TransferTubeBlock)) return null;
        return switch (state.getValue(TransferTubeBlock.PART)) {
            case LEFT_BOTTOM, RIGHT_BOTTOM -> pos;
            case LEFT_TOP, RIGHT_TOP -> pos.below();
            default -> null;
        };
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getClickedFace() == Direction.DOWN) return InteractionResult.FAIL;
        ItemStack stack = context.getItemInHand();
        Apprentice.Kind kind = kind(stack);
        if (kind == null) {
            if (context.getPlayer() instanceof ServerPlayer player) player.sendOverlayMessage(Component.literal("This Apprentice is empty."));
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        BlockPos booth = boothFloor(level, context.getClickedPos());
        Vec3 at;
        AABB box;
        if (booth != null) {
            // clicked a Transfer Tube booth: stand it on that booth's floor
            at = Vec3.atBottomCenterOf(booth).add(0, 1.0 / 16.0, 0);
            box = ModEntities.APPRENTICE.get().getDimensions().makeBoundingBox(at.x(), at.y(), at.z());
            if (!level.getEntities(null, box).isEmpty()) return InteractionResult.FAIL;
        } else {
            at = Vec3.atBottomCenterOf(new BlockPlaceContext(context).getClickedPos());
            box = ModEntities.APPRENTICE.get().getDimensions().makeBoundingBox(at.x(), at.y(), at.z());
            if (!level.noCollision(null, box) || !level.getEntities(null, box).isEmpty()) return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            Apprentice apprentice = new Apprentice(ModEntities.APPRENTICE.get(), serverLevel);
            float yRot = Mth.floor((Mth.wrapDegrees(context.getRotation() - 180.0F) + 22.5F) / 45.0F) * 45.0F;
            apprentice.snapTo(at.x(), at.y(), at.z(), yRot, 0.0F);
            apprentice.wearFrom(stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY));
            apprentice.setup(kind, storedXp(stack), context.getPlayer() == null ? null : context.getPlayer().getUUID());
            Component name = stack.get(DataComponents.CUSTOM_NAME);
            if (name != null) apprentice.setNickname(name.getString());
            serverLevel.addFreshEntity(apprentice);
            level.playSound(null, at.x(), at.y(), at.z(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 0.75F, 0.8F);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
