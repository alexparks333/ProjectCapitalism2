package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

// Picking jobs from the job screen (/pick, client/JobPickScreen; Jobs.md > Picking jobs). (The old /job admin commands
// from early development are gone: jobs only come from /pick, Wishing Stars and Changing Stars.)
public final class JobCommands {

    private JobCommands() {
    }

    // From /pick, for everyone: a job not held goes into a free slot of its kind, for good.
    // A Changing Star (changing) lets ONE job change: click a job you hold to let it go, then pick ONE new job of that same
    // kind (Essential or Fun); that ends the change.
    public static void togglePicked(ServerPlayer player, int ordinal) {
        if (ordinal < 0 || ordinal >= Job.values().length) return;
        Job job = Job.values()[ordinal];
        JobData data = player.getData(ModAttachments.JOBS.get());
        Job.Category released = data.released();
        if (data.has(job)) {
            if (!data.changing()) {
                player.sendSystemMessage(Component.literal("Your jobs are for good. (Only a Changing Star lets you change one.)").withStyle(ChatFormatting.RED));
                return;
            }
            if (released != null) {
                player.sendSystemMessage(Component.literal("You've already let go of one job: now pick a new " + released.title + " job.").withStyle(ChatFormatting.RED));
                return;
            }
            data.remove(job);
            data.setReleased(job.category);
            player.sendSystemMessage(Component.literal("You let go of " + job.title + ". Now pick a new " + job.category.title + " job.").withStyle(ChatFormatting.YELLOW));
        } else {
            if (data.changing() && released == null && !data.hasFreeSlot(job.category)) {
                player.sendSystemMessage(Component.literal("First click the job you want to let go of.").withStyle(ChatFormatting.RED));
                return;
            }
            if (released != null && job.category != released) {
                player.sendSystemMessage(Component.literal("Pick a " + released.title + " job: that's the kind you let go of.").withStyle(ChatFormatting.RED));
                return;
            }
            if (!data.hasFreeSlot(job.category)) {
                player.sendSystemMessage(Component.literal("You have no free " + job.category.title + " job slot. (A " + job.category.title
                    + " Wishing Star at the Wishing Well gives one.)").withStyle(ChatFormatting.RED));
                return;
            }
            data.add(job);
            player.sendSystemMessage(Component.literal("You are now a " + job.title + "!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.7F, 1.2F);
            if (released != null) {
                data.setChanging(false);
                player.sendSystemMessage(Component.literal("Your new job is set.").withStyle(ChatFormatting.GOLD));
            }
        }
        player.setData(ModAttachments.JOBS.get(), data);
        JobEvents.sync(player);
    }
}
