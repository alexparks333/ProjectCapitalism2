package com.alexh.capitalism.realestate;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.GoldMoney;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /properties (also /property), RealEstate.md:
//   create <name>              (ops) a new lot from the Survey Stake's two corners
//   delete <name>              (ops)
//   list                       every lot and what it's up to
//   <name>                     one lot: its size, owner, tenant, prices
//   <name> price <gold>        (its owner, an agent) the asking price on its For Sale sign
//   <name> rent <gold>         (its owner, an agent) the rent a day on its For Lease sign
//   <name> sell <player>       (its owner, an agent) hand it over: the price is settled between them
//   <name> lease <player>      (its owner, an agent) let them build on it; they pay the rent every Minecraft day
//   <name> evict               (its owner) end the lease
@EventBusSubscriber(modid = Capitalism.MODID)
public final class PropertyCommands {

    private static final net.minecraft.server.permissions.PermissionCheck OPS =
        new net.minecraft.server.permissions.PermissionCheck.Require(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);

    private PropertyCommands() {
    }

    private static final SuggestionProvider<CommandSourceStack> NAMES = (context, builder) ->
        SharedSuggestionProvider.suggest(Properties.get(context.getSource().getServer()).all().stream().map(p -> p.name), builder);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher(), "properties");
        register(event.getDispatcher(), "property");
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(root)
            .then(Commands.literal("create").requires(Commands.hasPermission(OPS))
                .then(Commands.argument("name", StringArgumentType.word()).executes(PropertyCommands::create)))
            .then(Commands.literal("delete").requires(Commands.hasPermission(OPS))
                .then(Commands.argument("name", StringArgumentType.word()).suggests(NAMES).executes(PropertyCommands::delete)))
            .then(Commands.literal("list").executes(PropertyCommands::list))
            .then(Commands.argument("name", StringArgumentType.word()).suggests(NAMES)
                .executes(PropertyCommands::info)
                .then(Commands.literal("price").then(Commands.argument("gold", IntegerArgumentType.integer(0)).executes(c -> price(c, Kind.SALE))))
                .then(Commands.literal("rent").then(Commands.argument("gold", IntegerArgumentType.integer(0)).executes(c -> price(c, Kind.RENT))))
                .then(Commands.literal("sell").then(Commands.argument("player", EntityArgument.player()).executes(PropertyCommands::sell)))
                .then(Commands.literal("lease").then(Commands.argument("player", EntityArgument.player()).executes(PropertyCommands::lease)))
                .then(Commands.literal("evict").executes(PropertyCommands::evict)));
        dispatcher.register(command);
    }

    private enum Kind { SALE, RENT }

    // ── messages ──

    private static MutableComponent tag() {
        return Component.literal("[Real Estate] ").withStyle(ChatFormatting.DARK_GREEN);
    }

    private static int ok(CommandSourceStack source, String text) {
        source.sendSuccess(() -> tag().append(Component.literal(text).withStyle(ChatFormatting.WHITE)), false);
        return 1;
    }

    private static int no(CommandSourceStack source, String text) {
        source.sendFailure(tag().append(Component.literal(text).withStyle(ChatFormatting.RED)));
        return 0;
    }

    private static Properties data(CommandContext<CommandSourceStack> c) {
        return Properties.get(c.getSource().getServer());
    }

    // ── the commands ──

    private static int create(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = c.getSource().getPlayerOrException();
        BlockPos[] corners = SurveyStakeItem.corners(player);
        if (corners[0] == null || corners[1] == null) return no(c.getSource(), "Mark both corners with the Survey Stake first (left-click one, right-click the other).");
        String name = StringArgumentType.getString(c, "name");
        String wrong = data(c).create(name, corners[0], corners[1]);
        if (wrong != null) return no(c.getSource(), wrong);
        Property p = data(c).byName(name);
        corners[0] = corners[1] = null;
        return ok(c.getSource(), "Made lot " + name + " (" + p.width() + " x " + p.length() + "). Put a City Chest on it with what the city wants.");
    }

    private static int delete(CommandContext<CommandSourceStack> c) {
        String name = StringArgumentType.getString(c, "name");
        return data(c).delete(name) ? ok(c.getSource(), "Deleted lot " + name + ".") : no(c.getSource(), "There's no lot called " + name + ".");
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        var all = data(c).all();
        if (all.isEmpty()) return ok(c.getSource(), "There are no lots yet.");
        ok(c.getSource(), all.size() + " lot" + (all.size() == 1 ? "" : "s") + ":");
        for (Property p : all) {
            c.getSource().sendSuccess(() -> Component.literal("  " + p.name + " (" + p.width() + " x " + p.length() + "): ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(Properties.status(p)).withStyle(ChatFormatting.GRAY)), false);
        }
        return all.size();
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        Property p = data(c).byName(StringArgumentType.getString(c, "name"));
        if (p == null) return no(c.getSource(), "There's no lot called " + StringArgumentType.getString(c, "name") + ".");
        return ok(c.getSource(), p.name + ": " + p.width() + " x " + p.length() + " at " + p.minX + ", " + p.minZ + " to " + p.maxX + ", "
            + p.maxZ + "; " + Properties.status(p) + ".");
    }

    private static int price(CommandContext<CommandSourceStack> c, Kind kind) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Property p = data(c).byName(StringArgumentType.getString(c, "name"));
        if (p == null) return no(c.getSource(), "There's no lot called " + StringArgumentType.getString(c, "name") + ".");
        long nuggets = GoldMoney.fromGold(IntegerArgumentType.getInteger(c, "gold"));
        ServerPlayer player = c.getSource().getPlayerOrException();
        if (!p.ownedBy(player) || !Properties.isAgent(player)) return no(c.getSource(), "Only the Real Estate Agent who owns " + p.name + " can price it.");
        if (kind == Kind.SALE) {
            data(c).setSalePrice(p, nuggets);
            return ok(c.getSource(), p.name + " is for sale for " + GoldMoney.format(nuggets) + " (on its For Sale sign).");
        }
        data(c).setRent(p, nuggets);
        return ok(c.getSource(), p.name + " is for lease for " + GoldMoney.format(nuggets) + " a day (on its For Lease sign).");
    }

    private static int sell(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer agent = c.getSource().getPlayerOrException();
        ServerPlayer buyer = EntityArgument.getPlayer(c, "player");
        Property p = data(c).byName(StringArgumentType.getString(c, "name"));
        if (p == null) return no(c.getSource(), "There's no lot called " + StringArgumentType.getString(c, "name") + ".");
        String wrong = data(c).sell(agent, p, buyer);
        if (wrong != null) return no(c.getSource(), wrong);
        buyer.sendSystemMessage(tag().append(Component.literal("You now own " + p.name + ": you can build on it.").withStyle(ChatFormatting.WHITE)));
        return ok(c.getSource(), "Sold " + p.name + " to " + buyer.getName().getString() + ".");
    }

    private static int lease(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer agent = c.getSource().getPlayerOrException();
        ServerPlayer tenant = EntityArgument.getPlayer(c, "player");
        Property p = data(c).byName(StringArgumentType.getString(c, "name"));
        if (p == null) return no(c.getSource(), "There's no lot called " + StringArgumentType.getString(c, "name") + ".");
        String wrong = data(c).lease(agent, p, tenant, Properties.today(c.getSource().getServer()));
        if (wrong != null) return no(c.getSource(), wrong);
        tenant.sendSystemMessage(tag().append(Component.literal("You're leasing " + p.name + " from " + agent.getName().getString()
            + ": you can build on it. Rent: " + GoldMoney.format(p.rent()) + " every Minecraft day, from your gold. Miss it and the lease ends.").withStyle(ChatFormatting.WHITE)));
        return ok(c.getSource(), "Leased " + p.name + " to " + tenant.getName().getString() + " for " + GoldMoney.format(p.rent()) + " a day.");
    }

    private static int evict(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer owner = c.getSource().getPlayerOrException();
        Property p = data(c).byName(StringArgumentType.getString(c, "name"));
        if (p == null) return no(c.getSource(), "There's no lot called " + StringArgumentType.getString(c, "name") + ".");
        String tenant = p.tenantName();
        String wrong = data(c).evict(owner, p);
        if (wrong != null) return no(c.getSource(), wrong);
        return ok(c.getSource(), "Ended " + tenant + "'s lease on " + p.name + ".");
    }
}
