package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Changing Star (Jobs.md > Changing jobs): from an admin or the Traveling Merchant. Drop it in the Wishing Well
// (right-click the well with it, occupation/WishingWellBlock) or right-click it anywhere: your jobs are yours to change, and
// the job screen opens: click a job you hold to let it go and pick another, until every slot is full again. Used up.
public class ChangingStar extends Item {

    public ChangingStar(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        JobData data = sp.getData(ModAttachments.JOBS.get());
        if (data.changing()) {
            sp.sendOverlayMessage(Component.literal("You can already change your jobs: type /pick.").withStyle(ChatFormatting.LIGHT_PURPLE));
            return InteractionResult.FAIL;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isCreative()) stack.shrink(1);
        startChanging(sp);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new com.alexh.capitalism.network.OpenJobPickPacket());
        ((ServerLevel) level).sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return InteractionResult.SUCCESS;
    }

    // they may now let ONE job go in /pick and pick ONE new one of the same kind
    public static void startChanging(ServerPlayer player) {
        JobData data = player.getData(ModAttachments.JOBS.get());
        data.setChanging(true);
        data.setReleased(null);
        player.setData(ModAttachments.JOBS.get(), data);
        JobEvents.sync(player);
        player.sendSystemMessage(Component.literal("✨ Your jobs can change! ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
            .append(Component.literal("Click ONE job you hold to let it go, then pick ONE new job of that kind (or type /pick later).").withStyle(ChatFormatting.LIGHT_PURPLE)));
    }
}
