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

// Admin commands for the Fisherman (Town.md), under /npc (ops):
//  - /npc fisherman: he stands exactly where you are, facing the way you face (any other Fisherman goes)
//  - /npc fisherman remove: every Fisherman goes
//  - /npc fisherman collection <player> [reset]: what they've caught for the Fisherman's Collection; reset wipes it (and lets
//    them claim its prize again). Only ever by hand.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class FishermanCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private FishermanCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("fisherman").executes(FishermanCommands::place)
                .then(Commands.literal("remove").executes(FishermanCommands::remove))
                .then(Commands.literal("collection")
                    .then(Commands.argument("player", EntityArgument.player()).executes(FishermanCommands::showCollection)
                        .then(Commands.literal("reset").executes(FishermanCommands::resetCollection))))));
    }

    // every Fisherman in a loaded part of any world
    private static List<Fisherman> all(CommandSourceStack source) {
        List<Fisherman> found = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            found.addAll(level.getEntities(ModEntities.FISHERMAN.get(), m -> true));
        }
        return found;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int removed = 0;
        for (Fisherman old : all(context.getSource())) {
            old.leave();
            removed++;
        }
        ServerLevel level = player.level();
        Fisherman fisherman = new Fisherman(ModEntities.FISHERMAN.get(), level);
        fisherman.placeAt(player.position(), player.getYRot());
        level.addFreshEntity(fisherman);
        int moved = removed;
        context.getSource().sendSuccess(() -> Component.literal(moved > 0 ? "The Fisherman moved here." : "The Fisherman will stand here.")
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        List<Fisherman> found = all(context.getSource());
        if (found.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Fisherman here (he has to be in a loaded area)."));
            return 0;
        }
        for (Fisherman f : found) f.leave();
        context.getSource().sendSuccess(() -> Component.literal("The Fisherman is gone.").withStyle(ChatFormatting.YELLOW), true);
        return found.size();
    }

    private static int showCollection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        var c = com.alexh.capitalism.fishing.CatchCollection.of(target);
        StringBuilder sb = new StringBuilder();
        for (var kind : com.alexh.capitalism.fishing.RareCatch.values()) {
            var e = c.get(kind);
            if (e != null) sb.append("\n  ").append(kind.title).append(" x").append(e.count()).append(e.heaviest() > 0 ? ", heaviest " + e.heaviest() + " lb" : "");
        }
        String text = target.getGameProfile().name() + ": " + c.count() + " / " + com.alexh.capitalism.fishing.CatchCollection.total() + " caught"
            + (c.claimed() ? " (prize claimed)" : "") + sb;
        context.getSource().sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.AQUA), false);
        return c.count();
    }

    private static int resetCollection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        com.alexh.capitalism.fishing.CatchCollection.reset(target);
        context.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().name() + "'s Fisherman's Collection was reset.")
            .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
}