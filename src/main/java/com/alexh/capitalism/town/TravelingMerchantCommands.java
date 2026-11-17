package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

// Admin commands for the Traveling Merchant (Town.md), under /npc (ops):
//  - /npc travelingmerchant: he (and his wagon, on his left) stands exactly where you are, facing your way
//  - /npc travelingmerchant remove: he's gone for good (until placed again)
//  - /npc travelingmerchant arrive: his visit starts right now (fresh wares; the next one booked 8-14 days on)
//  - /npc travelingmerchant leave: today's visit ends now
//  - /npc travelingmerchant status: in town or not, and when he's next due
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TravelingMerchantCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private TravelingMerchantCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("travelingmerchant").executes(TravelingMerchantCommands::place)
                .then(Commands.literal("remove").executes(TravelingMerchantCommands::remove))
                .then(Commands.literal("arrive").executes(TravelingMerchantCommands::arrive))
                .then(Commands.literal("leave").executes(TravelingMerchantCommands::leave))
                .then(Commands.literal("status").executes(TravelingMerchantCommands::status))));
    }

    private static List<TravelingMerchant> all(MinecraftServer server) {
        List<TravelingMerchant> found = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) found.addAll(level.getEntities(ModEntities.TRAVELING_MERCHANT.get(), m -> true));
        return found;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MinecraftServer server = context.getSource().getServer();
        int removed = 0;
        for (TravelingMerchant old : all(server)) {
            old.leave();
            removed++;
        }
        ServerLevel level = player.level();
        TravelingMerchant merchant = new TravelingMerchant(ModEntities.TRAVELING_MERCHANT.get(), level);
        merchant.placeAt(player.position(), player.getYRot());
        merchant.setInvisible(!TravelingMerchantShop.inTown(server));
        level.addFreshEntity(merchant);
        int moved = removed;
        context.getSource().sendSuccess(() -> Component.literal((moved > 0 ? "The Traveling Merchant moved here" : "The Traveling Merchant will stop here")
            + (TravelingMerchantShop.inTown(server) ? " (he's in town today)." : " (next due in " + daysUntil(server) + " days; /npc travelingmerchant arrive to bring him now)."))
            .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static long daysUntil(MinecraftServer server) {
        return Math.max(0, TravelingMerchantShop.nextDay(server) - TravelingMerchantShop.today(server));
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        List<TravelingMerchant> found = all(context.getSource().getServer());
        if (found.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Traveling Merchant here (he has to be in a loaded area)."));
            return 0;
        }
        for (TravelingMerchant m : found) m.leave();
        context.getSource().sendSuccess(() -> Component.literal("The Traveling Merchant is gone.").withStyle(ChatFormatting.YELLOW), true);
        return found.size();
    }

    private static int arrive(CommandContext<CommandSourceStack> context) {
        TravelingMerchantShop.arrive(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("The Traveling Merchant is in town for today, with fresh wares.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int leave(CommandContext<CommandSourceStack> context) {
        TravelingMerchantShop.leave(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("The Traveling Merchant has left (next due in " + daysUntil(context.getSource().getServer()) + " days).")
            .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        MinecraftServer server = context.getSource().getServer();
        boolean here = TravelingMerchantShop.inTown(server);
        String placed = all(server).isEmpty() ? " He isn't placed (or not in a loaded area): /npc travelingmerchant." : "";
        context.getSource().sendSuccess(() -> Component.literal((here ? "In town today with " + TravelingMerchantShop.stock(server).size() + " wares."
            : "Away. Next due in " + daysUntil(server) + " days.") + placed), false);
        return 1;
    }
}
