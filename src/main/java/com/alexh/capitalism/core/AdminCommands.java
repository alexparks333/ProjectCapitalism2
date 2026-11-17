package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.occupation.JobData;
import com.alexh.capitalism.occupation.JobEvents;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

// /admin ... : server admin tools (ops only). More get added here over time.
//  - /admin jobs reset <player>: their jobs are cleared so they pick again from scratch in /pick (the job screen opens
//    for them). Their job slots (from Wishing Stars) are kept.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class AdminCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private AdminCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("admin").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("jobs")
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(AdminCommands::resetJobs)))));
    }

    private static int resetJobs(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        resetJobs(target);
        context.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().name()
            + "'s jobs were reset: they pick again in /pick (their job slots are kept).").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    // no jobs, not mid-change, the same number of slots; the job screen opens for them
    public static void resetJobs(ServerPlayer target) {
        JobData old = target.getData(ModAttachments.JOBS.get());
        JobData fresh = new JobData();
        for (com.alexh.capitalism.occupation.Job.Category category : com.alexh.capitalism.occupation.Job.Category.values()) {
            fresh.setSlots(category, old.slots(category));
        }
        target.setData(ModAttachments.JOBS.get(), fresh);
        JobEvents.sync(target);
        target.sendSystemMessage(Component.literal("An admin reset your jobs: pick them again.").withStyle(ChatFormatting.GOLD));
        PacketDistributor.sendToPlayer(target, new com.alexh.capitalism.network.OpenJobPickPacket());
    }
}
