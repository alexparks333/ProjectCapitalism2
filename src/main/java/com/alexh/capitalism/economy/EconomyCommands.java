package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// /balance               -> how much gold you're carrying, plus all your storage (net worth, like the leaderboard)
// /price                 -> what the Banker pays for the held item, per unit
// /pay <player> <amount> -> hand gold from your inventory to an online player (like Project Capitalism 1)
@EventBusSubscriber(modid = Capitalism.MODID)
public class EconomyCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("balance").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            long carried = GoldMoney.carried(player);
            // everything else: Ender Chest, every container you own, Shopfront earnings (the same count as the leaderboard)
            GoldLedger ledger = GoldLedger.get(player.level().getServer());
            GoldLeaderboard.recount(player.level().getServer(), ledger);
            long storage = Math.max(0, ledger.netWorth(player.getUUID()) - carried);
            player.sendSystemMessage(tag("Bank").append(text("You're carrying ")).append(gold(GoldMoney.format(carried)))
                .append(text(", plus ")).append(gold(GoldMoney.format(storage))).append(text(" in all your storage.")));
            return 1;
        }));

        // /price: what the Banker pays for the item in your hand, per unit
        event.getDispatcher().register(Commands.literal("price").executes(context -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
            player.sendSystemMessage(BankerEvents.priceMessage(player.getMainHandItem()));
            return 1;
        }));

        // /banker (operators): a Banker villager right here, facing you. It stays put, can't be hurt (even by an operator), never despawns,
        // and buys like any mob named "Banker" (BankerEvents). Nothing can kill a Banker; /banker remove takes the nearest away.
        event.getDispatcher().register(Commands.literal("banker")
            .requires(Commands.hasPermission(new net.minecraft.server.permissions.PermissionCheck.Require(
                net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)))
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                var level = player.level();
                var villager = net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (villager == null) return 0;
                float yaw = player.getYRot() + 180.0F;
                villager.snapTo(player.getX(), player.getY(), player.getZ(), yaw, 0.0F);
                villager.setYHeadRot(yaw);
                villager.setYBodyRot(yaw);
                villager.setCustomName(Component.literal("Banker").withStyle(ChatFormatting.GOLD));
                villager.setCustomNameVisible(true);
                villager.setNoAi(true);
                villager.setPermanentlyInvulnerable(true);
                villager.setSilent(true);
                villager.setPersistenceRequired();
                level.addFreshEntity(villager);
                player.sendSystemMessage(tag("Bank").append(text("A Banker is open for business. Right-click it holding an item to sell one, shift + right-click to sell the stack. /price shows what an item is worth.")));
                return 1;
            })
            .then(Commands.literal("remove").executes(context -> {
                // the nearest Banker within 6 blocks (they can't be killed, so this is how one goes)
                ServerPlayer player = context.getSource().getPlayerOrException();
                var nearest = player.level().getEntities(player, player.getBoundingBox().inflate(6.0), BankerEvents::isBanker).stream()
                    .min(java.util.Comparator.comparingDouble(player::distanceToSqr)).orElse(null);
                if (nearest == null) {
                    player.sendSystemMessage(tag("Bank").append(error("No Banker within 6 blocks.")));
                    return 0;
                }
                nearest.discard();
                player.sendSystemMessage(tag("Bank").append(text("The Banker has closed up shop.")));
                return 1;
            })));

        event.getDispatcher().register(Commands.literal("pay")
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                    .executes(context -> {
                        ServerPlayer payer = context.getSource().getPlayerOrException();
                        ServerPlayer target = EntityArgument.getPlayer(context, "player");
                        int amount = IntegerArgumentType.getInteger(context, "amount");
                        return pay(payer, target, amount);
                    }))));
    }

    private static int pay(ServerPlayer payer, ServerPlayer target, int amount) {
        if (target == payer) {
            payer.sendSystemMessage(tag("Pay").append(error("You can't pay yourself.")));
            return 0;
        }
        long cost = GoldMoney.fromGold(amount);
        long carried = GoldMoney.carried(payer);
        if (carried < cost) {
            payer.sendSystemMessage(tag("Pay").append(error("You don't have enough gold. You have "))
                .append(gold(GoldMoney.format(carried))).append(error(".")));
            return 0;
        }
        if (!GoldMoney.take(payer, cost)) return 0;
        GoldMoney.give(target, cost);

        payer.sendSystemMessage(tag("Pay").append(text("You paid ")).append(gold(amount + " gold"))
            .append(text(" to ")).append(name(target)).append(text(".")));
        target.sendSystemMessage(tag("Pay").append(name(payer)).append(text(" paid you "))
            .append(gold(amount + " gold")).append(text(".")));
        return 1;
    }

    static MutableComponent tag(String label) {
        return Component.literal("[" + label + "] ").withStyle(ChatFormatting.GOLD);
    }

    static MutableComponent text(String s) {
        return Component.literal(s).withStyle(ChatFormatting.WHITE);
    }

    static MutableComponent gold(String s) {
        return Component.literal(s).withStyle(ChatFormatting.YELLOW);
    }

    static MutableComponent error(String s) {
        return Component.literal(s).withStyle(ChatFormatting.RED);
    }

    private static MutableComponent name(ServerPlayer player) {
        return Component.literal(player.getGameProfile().name()).withStyle(ChatFormatting.YELLOW);
    }
}
