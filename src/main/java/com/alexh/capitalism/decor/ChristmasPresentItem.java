package com.alexh.capitalism.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

// A Christmas Present (Decorations.md, Calendar.md): what the Grand Christmas Tree gives each player on Christmas
// (right-click it). Right-click to unwrap it: cookies, a cake, fireworks and snowballs, sometimes a golden apple or an
// emerald, now and then a diamond. A burst of confetti and a happy sound.
public class ChristmasPresentItem extends Item {

    public ChristmasPresentItem(Properties properties) {
        super(properties);
    }

    // what's inside one present
    public static List<ItemStack> contents(RandomSource random) {
        List<ItemStack> out = new ArrayList<>();
        out.add(new ItemStack(Items.COOKIE, 4 + random.nextInt(5)));
        out.add(new ItemStack(Items.FIREWORK_ROCKET, 3 + random.nextInt(3)));
        out.add(new ItemStack(Items.SNOWBALL, 8 + random.nextInt(9)));
        if (random.nextFloat() < 0.5F) out.add(new ItemStack(Items.CAKE));
        if (random.nextFloat() < 0.3F) out.add(new ItemStack(Items.GOLDEN_APPLE));
        if (random.nextFloat() < 0.25F) out.add(new ItemStack(Items.EMERALD, 1 + random.nextInt(2)));
        if (random.nextFloat() < 0.08F) out.add(new ItemStack(Items.DIAMOND));
        return out;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            for (ItemStack gift : contents(level.getRandom())) {
                if (!player.getInventory().add(gift)) player.drop(gift, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.2, player.getZ(), 30, 0.4, 0.4, 0.4, 0.3);
            server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7F, 1.4F);
            server.playSound(null, player.blockPosition(), SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.PLAYERS, 1.0F, 1.0F);
            player.sendOverlayMessage(Component.literal("🎁 Merry Christmas!").withStyle(ChatFormatting.RED));
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }
}
