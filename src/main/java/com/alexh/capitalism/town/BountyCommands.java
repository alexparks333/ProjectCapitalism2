package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

// Admin commands for the Bounty Clerk (Town.md, Bounties.md), under /npc (ops):
//  - /npc bountyclerk: he stands exactly where you are, facing the way you face (any other Bounty Clerk goes)
//  - /npc bountyclerk remove: every Bounty Clerk goes
//  - /npc bountyclerk tasks <player> [reset]: their task, count and gold; reset wipes them. Only ever by hand.
//  - /npc bountyclerk winners [reset]: the first 3 to turn in 50 bounties (Fun Wishing Stars); reset clears the places.
//    Only ever by hand.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BountyCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private BountyCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("bountyclerk").executes(BountyCommands::place)
                .then(Commands.literal("remove").executes(BountyCommands::remove))
                .then(Commands.literal("tasks")
                    .then(Commands.argument("player", EntityArgument.player()).executes(BountyCommands::showTasks)
                        .then(Commands.literal("reset").executes(BountyCommands::resetTasks))))
                .then(Commands.literal("winners").executes(BountyCommands::showWinners)
                    .then(Commands.literal("reset").executes(BountyCommands::resetWinners)))));
    }

    // every Bounty Clerk in a loaded part of any world
    private static List<BountyClerk> all(CommandSourceStack source) {
        List<BountyClerk> found = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            found.addAll(level.getEntities(ModEntities.BOUNTY_CLERK.get(), m -> true));
        }
        return found;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int removed = 0;
        for (BountyClerk old : all(context.getSource())) {
            old.leave();
            removed++;
        }
        ServerLevel level = player.level();
        BountyClerk clerk = new BountyClerk(ModEntities.BOUNTY_CLERK.get(), level);
        clerk.placeAt(player.position(), player.getYRot());
        level.addFreshEntity(clerk);
        int moved = removed;
        context.getSource().sendSuccess(() -> Component.literal(moved > 0 ? "The Bounty Clerk moved here." : "The Bounty Clerk will stand here.")
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        List<BountyClerk> found = all(context.getSource());
        if (found.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Bounty Clerk here (he has to be in a loaded area)."));
            return 0;
        }
        for (BountyClerk f : found) f.leave();
        context.getSource().sendSuccess(() -> Component.literal("The Bounty Clerk is gone.").withStyle(ChatFormatting.YELLOW), true);
        return found.size();
    }

    private static int showTasks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        var data = com.alexh.capitalism.bounty.Bounties.of(target);
        StringBuilder sb = new StringBuilder(target.getGameProfile().name() + ": " + data.completed() + " / " + com.alexh.capitalism.bounty.Bounties.MILESTONE
            + " turned in, " + com.alexh.capitalism.economy.GoldMoney.format(data.earned()) + " earned" + (data.claimed() ? " (star claimed)" : ""));
        for (var o : data.open()) {
            var task = o.task();
            if (task != null) sb.append("\n  ").append(o.title()).append(" (").append(task.tier.title).append("): ")
                .append(com.alexh.capitalism.bounty.Bounties.progress(target, o)).append(" / ").append(o.amount());
        }
        String text = sb.toString();
        context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GOLD), false);
        return data.open().size();
    }

    private static int resetTasks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        com.alexh.capitalism.bounty.Bounties.reset(target);
        context.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().name() + "'s bounties were reset.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int showWinners(CommandContext<CommandSourceStack> context) {
        var places = com.alexh.capitalism.bounty.BountyBoard.get(context.getSource().getServer()).places();
        StringBuilder sb = new StringBuilder("Bounty milestone (" + com.alexh.capitalism.bounty.Bounties.MILESTONE + " turned in): ");
        if (places.isEmpty()) sb.append("nobody yet, 3 Fun Wishing Stars left.");
        for (int i = 0; i < places.size(); i++) {
            var p = places.get(i);
            sb.append("\n  ").append(i + 1).append(". ").append(p.name()).append(p.claimed() ? " (star claimed)" : " (star not claimed yet)");
        }
        String text = sb.toString();
        context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GOLD), false);
        return places.size();
    }

    private static int resetWinners(CommandContext<CommandSourceStack> context) {
        com.alexh.capitalism.bounty.Bounties.resetWinners(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("The bounty milestone places were cleared.").withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
}