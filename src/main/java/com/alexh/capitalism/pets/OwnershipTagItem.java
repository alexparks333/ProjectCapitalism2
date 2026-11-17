package com.alexh.capitalism.pets;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The Ownership Tag (a Name Tag + a Diamond, Pet Trainers only): anyone can use it. Right-click a pet (an animal a Pet
// Trainer has already trained) that isn't anyone's and it's yours (Pets.claim). Used up.
public class OwnershipTagItem extends Item {

    public OwnershipTagItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Animal animal)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        String problem = Pets.claim(animal, player);
        if (problem != null) {
            serverPlayer.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        serverPlayer.sendOverlayMessage(Component.literal("You now own this ").append(Pets.name(animal)).append("!").withStyle(ChatFormatting.GREEN));
        player.level().playSound(null, animal.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
        Pets.sparkle(animal);
        if (!player.isCreative()) stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
