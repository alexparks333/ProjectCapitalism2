package com.alexh.capitalism.pets;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

// The Training Stick (a Lead, a Fishing Rod, a Diamond and two Sticks; Pet Trainers only): right-click an animal to
// train it into a pet (Pets.train), ready to be claimed with an Ownership Tag. Costs the Pet Trainer 25 levels; the stick lasts 5 trainings.
public class TrainingStickItem extends Item {

    public static final int USES = 5;

    public TrainingStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Animal animal)) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        if (!player.isCreative() && !JobEvents.has(player, Job.PET_TRAINER)) {
            serverPlayer.sendOverlayMessage(Component.literal("Only a Pet Trainer knows how to use this.").withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        String problem = Pets.train(animal, player);
        if (problem != null) {
            serverPlayer.sendOverlayMessage(Component.literal(problem).withStyle(ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        serverPlayer.sendOverlayMessage(Component.literal("Trained! Meet ").append(Pets.name(animal)).append(".").withStyle(ChatFormatting.GREEN));
        Pets.sparkle(animal);
        stack.hurtAndBreak(1, player, hand);
        return InteractionResult.SUCCESS;
    }
}
