package com.alexh.capitalism.deathtax;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.bounty.Standings;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.event.Purge;
import com.alexh.capitalism.network.DeathTaxPopupPacket;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

// The death tax and Purgatory (Insurance.md).
//  - Dying: they owe a share of their gold (what /balance counts: carried + Ender Chest, measured as they die, before
//    anything drops), rounded down to whole nuggets: 25%, less with insurance (deathtax/Insurance.rateFor). Nothing is
//    taken from them: it's a debt, kept through death and logouts; dying again adds to it.
//  - In The Purge (event/Purge), when another player kills them outside the town, what they owe from that death is
//    owed to their killer instead of the bank.
//  - /pay death pays it all (/pay death <gold> pays part), with /pay's money handling (GoldMoney.take: ingots first,
//    change back). The bank's share leaves the economy; a killer's share goes to the killer (at their next login if
//    they're away). A part payment is shared between the debts by size.
//  - While they owe anything they're In Purgatory: they can't break blocks or hurt anything (melee, projectiles,
//    potions, their pets), but can walk, eat, drink, open things, trade, use shops and the Banker, and be hurt. Others
//    see a skull by their name. Players in creative or spectator aren't held back (they can't die anyway).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class DeathTax {

    static final int RED = 0xFF5050;
    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private DeathTax() {
    }

    public static DeathTaxData data(Player player) {
        return player.getData(ModAttachments.DEATH_TAX.get());
    }

    // saves it (and sends it to them), and keeps In Purgatory (seen by everyone: the skull) in step
    public static void save(ServerPlayer player, DeathTaxData data) {
        player.setData(ModAttachments.DEATH_TAX.get(), data);
        refresh(player);
    }

    public static void refresh(ServerPlayer player) {
        boolean in = data(player).owed() > 0;
        if (player.getData(ModAttachments.IN_PURGATORY.get()) != in) {
            player.setData(ModAttachments.IN_PURGATORY.get(), in);
            player.refreshDisplayName();
            player.refreshTabListName();
        }
    }

    public static long owed(Player player) {
        return data(player).owed();
    }

    // held back by Purgatory right now (works on the client for the local player too: their data is synced to them)
    public static boolean restricted(@Nullable Player player) {
        return player != null && !player.isCreative() && !player.isSpectator() && data(player).owed() > 0;
    }

    // The gold the tax is a share of: ALL their gold, their net worth exactly as the Gold Leaderboard counts it (the same
    // code: recounted now): carried (with the cursor, shulker boxes, the bag on their back), the Ender Chest, every
    // container they own (Shopfront earnings too; ones in unloaded chunks at their last count). Raw Gold isn't gold here.
    public static long taxableGold(ServerPlayer player) {
        com.alexh.capitalism.economy.GoldLedger ledger = com.alexh.capitalism.economy.GoldLedger.get(player.level().getServer());
        com.alexh.capitalism.economy.GoldLeaderboard.recount(player.level().getServer(), ledger);
        return ledger.netWorth(player.getUUID());
    }

    // whole nuggets, rounded down
    // with this much gold or less (10 gold) to your name, no death tax at all
    public static final long PITY_NUGGETS = 10L * com.alexh.capitalism.economy.GoldMoney.INGOT;

    // the most one death can cost: 75 gold
    public static final long MAX_TAX_NUGGETS = 75L * com.alexh.capitalism.economy.GoldMoney.INGOT;

    public static long tax(long gold, int percent) {
        if (gold <= 0 || percent <= 0) return 0;
        return BigInteger.valueOf(gold).multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100)).longValueExact();
    }

    // ── dying ──

    // last, and only if they really die (nothing cancelled it); before the death drops (those come after this event)
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        died(player, event.getSource());
    }

    public static void died(ServerPlayer player, DamageSource source) {
        MinecraftServer server = player.level().getServer();
        Insurance.Rate rate = Insurance.rateFor(player);
        long gold = taxableGold(player);
        long tax = tax(gold, rate.percent());
        // never more than 75 gold in one death
        boolean capped = tax > MAX_TAX_NUGGETS;
        if (capped) tax = MAX_TAX_NUGGETS;
        ServerPlayer killer = Purge.killerFor(player, source);
        if (killer != null) countKill(server, killer);
        // killed by another player outside The Purge: plain PvP never brings the death tax or Purgatory
        boolean pvp = killer == null && byOtherPlayer(player, source);
        if (pvp) tax = 0;
        // 10 gold or less to your name: Death takes pity, no tax
        boolean pity = !pvp && gold > 0 && gold <= PITY_NUGGETS;
        if (pity) tax = 0;
        // a Phoenix Feather in the offhand (only from the Traveling Merchant): no death tax this time, and it burns away
        boolean phoenix = !pvp && tax > 0 && player.getOffhandItem().is(com.alexh.capitalism.core.ModItems.PHOENIX_FEATHER.get());
        if (phoenix) {
            tax = 0;
            player.getOffhandItem().shrink(1);
            player.level().sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, player.getX(), player.getY() + 1, player.getZ(), 30, 0.4, 0.6, 0.4, 0.05);
            player.sendSystemMessage(skull().append(Component.literal("Your Phoenix Feather bursts into flame: Death takes no tax this time.").withStyle(ChatFormatting.GOLD)));
        }
        DeathTaxData d = data(player);
        d.lastGold = gold;
        d.lastRate = rate.percent();
        d.lastTax = tax;
        d.lastWhy = rate.why();
        if (tax > 0) {
            d.add(killer == null ? null : killer.getUUID(), killer == null ? "Death" : killer.getGameProfile().name(), tax);
            MutableComponent text = skull().append(Component.literal("Death takes its tax: " + rate.percent() + "% of all your gold (" + GoldMoney.format(gold)
                + ": on you, in your Ender Chest and your containers; " + rate.why() + ") = " + GoldMoney.format(tax)
                + (capped ? " (the most Death ever takes)" : "")).withStyle(ChatFormatting.WHITE));
            if (killer != null) text.append(Component.literal(", owed to " + killer.getGameProfile().name() + " (The Purge)").withStyle(ChatFormatting.RED));
            text.append(Component.literal(". You owe " + GoldMoney.format(d.owed()) + " in all: /pay death").withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(text);
            if (killer != null) {
                killer.sendSystemMessage(skull().append(Component.literal(player.getGameProfile().name() + " owes you " + GoldMoney.format(tax)
                    + " in death tax. It's yours when they pay.").withStyle(ChatFormatting.WHITE)));
            }
        } else if (pvp) {
            player.sendSystemMessage(skull().append(Component.literal("Killed by a player outside The Purge: no death tax.").withStyle(ChatFormatting.GRAY)));
        } else if (phoenix) {
            // (said above)
        } else if (pity) {
            player.sendSystemMessage(skull().append(Component.literal("Death takes pity on you: with 10 gold or less to your name, no death tax.").withStyle(ChatFormatting.GRAY)));
        } else if (gold > 0) {
            player.sendSystemMessage(skull().append(Component.literal("No death tax this time (" + rate.why() + ").").withStyle(ChatFormatting.GRAY)));
        } else {
            // always say something, so a death never looks like the tax didn't run
            player.sendSystemMessage(skull().append(Component.literal("Death checks everything you own (your pockets, Ender Chest and containers): no gold, so no death tax.").withStyle(ChatFormatting.GRAY)));
        }
        com.mojang.logging.LogUtils.getLogger().info("[DeathTax] {} died: gold {}, rate {}% ({}), tax {}, owed {}{}", player.getGameProfile().name(), gold,
            rate.percent(), rate.why(), tax, d.owed(), killer != null ? ", owed to " + killer.getGameProfile().name() : "");
        save(player, d);
    }

    // another player (their arrow, their pet, or the last player to hurt them) was behind this death
    static boolean byOtherPlayer(ServerPlayer victim, DamageSource source) {
        net.minecraft.world.entity.Entity e = source.getEntity();
        net.minecraft.world.entity.player.Player behind = e instanceof net.minecraft.world.entity.player.Player p ? p
            : e instanceof net.minecraft.world.entity.TamableAnimal pet && pet.getOwner() instanceof net.minecraft.world.entity.player.Player owner ? owner
            : victim.getKillCredit() instanceof net.minecraft.world.entity.player.Player p ? p : null;
        return behind != null && !behind.getUUID().equals(victim.getUUID());
    }

    private static void countKill(MinecraftServer server, ServerPlayer killer) {
        DeathTaxLedger ledger = DeathTaxLedger.get(server);
        int kills = ledger.purgeKills.merge(killer.getUUID(), 1, Integer::sum);
        ledger.names.put(killer.getUUID(), killer.getGameProfile().name());
        ledger.setDirty();
        Standings.get(server).set(Standings.Category.PURGE_KILLS, killer.getUUID(), killer.getGameProfile().name(), kills, 0, server.overworld().getGameTime());
        Purge.countKill(server, killer);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        refresh(player);
        if (event.isEndConquered()) return;
        DeathTaxData d = data(player);
        if (d.owed() <= 0) return;
        PacketDistributor.sendToPlayer(player, new DeathTaxPopupPacket(d.lastGold, Math.max(0, d.lastRate), d.lastWhy, d.lastTax, d.owed(), breakdown(d)));
        player.level().playSound(null, player.blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MinecraftServer server = player.level().getServer();
        // death tax paid to them while they were away
        DeathTaxLedger ledger = DeathTaxLedger.get(server);
        Long waiting = ledger.owed.remove(player.getUUID());
        if (waiting != null && waiting > 0) {
            ledger.setDirty();
            GoldMoney.give(player, waiting);
            player.sendSystemMessage(skull().append(Component.literal("While you were away, Death collected " + GoldMoney.format(waiting)
                + " of Purge death tax for you.").withStyle(ChatFormatting.GOLD)));
        }
        refresh(player);
        long owed = owed(player);
        if (owed > 0) player.sendSystemMessage(skull().append(Component.literal("You're In Purgatory: you owe Death " + GoldMoney.format(owed)
            + ". No mining or fighting until it's paid: /pay death").withStyle(ChatFormatting.RED)));
    }

    // ── paying ──

    // /pay death (gold null: all of it) or /pay death <gold>
    public static int pay(ServerPlayer player, @Nullable Integer gold) {
        MinecraftServer server = player.level().getServer();
        DeathTaxData d = data(player);
        long owed = d.owed();
        if (owed <= 0) {
            player.sendSystemMessage(skull().append(Component.literal("You don't owe Death anything.").withStyle(ChatFormatting.GRAY)));
            return 0;
        }
        // Death collects from all your gold within reach (GoldCollector: inventory, Ender Chest and your containers, your
        // gold on the ground; never Raw Gold: smelt it first), as much as it can find
        long wanted = gold == null ? owed : Math.min(GoldMoney.fromGold(gold), owed);
        GoldCollector.Result collected = GoldCollector.collect(player, wanted);
        long pay = collected.taken();
        String where = collected.unloaded() ? "some of your gold is in unloaded chests" : "that's all the gold Death could find";
        if (collected.rawLeft()) where += ". Death won't take Raw Gold: smelt it into ingots in a furnace, then /pay death";
        if (pay <= 0) {
            player.sendSystemMessage(skull().append(Component.literal("Death found no gold of yours to take (you owe " + GoldMoney.format(owed) + "): " + where + ".")
                .withStyle(ChatFormatting.RED)));
            return 0;
        }
        if (pay < wanted) {
            player.sendSystemMessage(skull().append(Component.literal("Paid " + GoldMoney.format(pay) + " of " + GoldMoney.format(wanted) + ": "
                + GoldMoney.format(owed - pay) + " left; " + where + ".").withStyle(ChatFormatting.RED)));
        }
        long[] shares = shares(d.debts, pay);
        MutableComponent text = skull().append(Component.literal("You paid " + GoldMoney.format(pay) + ":").withStyle(ChatFormatting.WHITE));
        String from = player.getGameProfile().name();
        for (int i = 0; i < d.debts.size(); i++) {
            DeathTaxData.Debt debt = d.debts.get(i);
            long share = shares[i];
            if (share <= 0) continue;
            debt.nuggets -= share;
            text.append(Component.literal("\n  " + GoldMoney.format(share) + " to " + (debt.creditor == null ? "Death (the bank)" : debt.name)
                + (debt.nuggets > 0 ? " (" + GoldMoney.format(debt.nuggets) + " still owed)" : "")).withStyle(ChatFormatting.GRAY));
            if (debt.creditor != null) deliver(server, debt.creditor, debt.name, share, from);
        }
        d.debts.removeIf(debt -> debt.nuggets <= 0);
        save(player, d);
        player.sendSystemMessage(text);
        if (d.owed() <= 0) {
            player.sendSystemMessage(skull().append(Component.literal("Death is paid in full. You're free of Purgatory!").withStyle(ChatFormatting.GREEN)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.8F, 1.3F);
        } else {
            player.sendSystemMessage(skull().append(Component.literal("You still owe " + GoldMoney.format(d.owed()) + ".").withStyle(ChatFormatting.RED)));
            player.level().playSound(null, player.blockPosition(), SoundEvents.CHAIN_BREAK, SoundSource.PLAYERS, 0.8F, 0.9F);
        }
        return 1;
    }

    // How a payment is shared between the debts: by size, rounded down, the nuggets left over one each to the oldest
    // debts first. Paying everything pays each debt exactly; the shares always add up to the payment.
    static long[] shares(List<DeathTaxData.Debt> debts, long pay) {
        long[] out = new long[debts.size()];
        long total = 0;
        for (DeathTaxData.Debt d : debts) total += d.nuggets;
        if (total <= 0 || pay <= 0) return out;
        pay = Math.min(pay, total);
        long given = 0;
        for (int i = 0; i < out.length; i++) {
            out[i] = BigInteger.valueOf(pay).multiply(BigInteger.valueOf(debts.get(i).nuggets)).divide(BigInteger.valueOf(total)).longValueExact();
            given += out[i];
        }
        for (int i = 0; i < out.length && given < pay; i++) {
            if (out[i] < debts.get(i).nuggets) {
                out[i]++;
                given++;
            }
        }
        return out;
    }

    // a Purge killer's share: into their inventory (dropped at them if it's full), or kept for their next login
    static void deliver(MinecraftServer server, java.util.UUID creditor, String name, long nuggets, String from) {
        DeathTaxLedger ledger = DeathTaxLedger.get(server);
        long taken = ledger.goldTaken.merge(creditor, nuggets, Long::sum);
        ledger.names.put(creditor, name);
        ServerPlayer online = server.getPlayerList().getPlayer(creditor);
        if (online != null) {
            GoldMoney.give(online, nuggets);
            online.sendSystemMessage(skull().append(Component.literal(from + " paid their death tax: " + GoldMoney.format(nuggets) + " for you.").withStyle(ChatFormatting.GOLD)));
            online.level().playSound(null, online.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.7F);
        } else {
            ledger.owed.merge(creditor, nuggets, Long::sum);
        }
        ledger.setDirty();
        Standings.get(server).set(Standings.Category.GOLD_TAKEN, creditor, name, GoldMoney.toGold(taken), 0, server.overworld().getGameTime());
    }

    // (dev: client/PhotoShoot) owe this
    public static void devOwe(ServerPlayer player, java.util.@Nullable UUID creditor, String name, long nuggets) {
        DeathTaxData d = data(player);
        d.add(creditor, name, nuggets);
        if (d.lastRate < 0) {
            d.lastRate = Insurance.baseRate();
            d.lastWhy = "uninsured";
        }
        save(player, d);
    }

    static List<String> breakdown(DeathTaxData d) {
        List<String> out = new ArrayList<>();
        for (DeathTaxData.Debt debt : d.debts) out.add((debt.creditor == null ? "Death (the bank)" : debt.name + " (The Purge)") + ": " + GoldMoney.format(debt.nuggets));
        return out;
    }

    // ── Purgatory: no mining, no hurting anything ──

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        if (!restricted(event.getPlayer())) return;
        event.setCanceled(true);
        warn(event.getPlayer(), "You're In Purgatory: no mining until you pay Death (/pay death).");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (restricted(event.getEntity())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!restricted(event.getEntity())) return;
        event.setCanceled(true);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START) warn(event.getEntity(), "You're In Purgatory: no mining until you pay Death (/pay death).");
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttack(AttackEntityEvent event) {
        if (!restricted(event.getEntity())) return;
        event.setCanceled(true);
        warn(event.getEntity(), "You're In Purgatory: you can't hurt anything until you pay Death (/pay death).");
    }

    // projectiles, thrown tridents, potions, TNT they lit, their pets: whatever they're behind does no damage
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Player behind = behind(event.getSource().getEntity());
        if (behind != null && behind != event.getEntity() && restricted(behind)) event.setCanceled(true);
    }

    private static @Nullable Player behind(@Nullable Entity attacker) {
        if (attacker instanceof Player player) return player;
        if (attacker instanceof TamableAnimal pet && pet.getOwner() instanceof Player owner) return owner;
        return null;
    }

    private static void warn(Player player, String text) {
        if (player instanceof ServerPlayer sp) sp.sendOverlayMessage(Component.literal("☠ " + text).withStyle(s -> s.withColor(TextColor.fromRgb(RED))));
    }

    // the skull by their name, for everyone (In Purgatory is synced to all who can see them)
    @SubscribeEvent
    public static void onNameFormat(PlayerEvent.NameFormat event) {
        if (event.getEntity().getData(ModAttachments.IN_PURGATORY.get())) {
            event.setDisplayname(Component.literal("☠ ").withStyle(ChatFormatting.GRAY).append(event.getDisplayname()));
        }
    }

    // ── commands: /pay death [gold], /deathtax ──

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // (a literal: "/pay death" is always the death tax, matched before any player name; a player called Death is
        // paid with "/pay Death <gold>", the capital D, since literals match exactly)
        event.getDispatcher().register(Commands.literal("pay")
            .then(Commands.literal("death")
                .executes(c -> pay(c.getSource().getPlayerOrException(), null))
                .then(Commands.argument("gold", IntegerArgumentType.integer(1))
                    .executes(c -> pay(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "gold"))))));
        event.getDispatcher().register(Commands.literal("deathtax")
            .executes(c -> show(c.getSource(), c.getSource().getPlayerOrException()))
            .then(Commands.argument("player", EntityArgument.player()).requires(Commands.hasPermission(OPS))
                .executes(c -> show(c.getSource(), EntityArgument.getPlayer(c, "player")))
                .then(Commands.literal("forgive").executes(DeathTax::forgive))));
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        DeathTaxData d = data(player);
        MutableComponent text = skull().append(Component.literal(player.getGameProfile().name() + (d.owed() > 0 ? " owes " + GoldMoney.format(d.owed()) : " owes nothing"))
            .withStyle(ChatFormatting.WHITE));
        for (String line : breakdown(d)) text.append(Component.literal("\n  " + line).withStyle(ChatFormatting.GRAY));
        if (d.lastRate >= 0) text.append(Component.literal("\n  Last death: " + d.lastRate + "% of " + GoldMoney.format(d.lastGold) + " (" + d.lastWhy + ") = "
            + GoldMoney.format(d.lastTax)).withStyle(ChatFormatting.DARK_GRAY));
        source.sendSuccess(() -> text, false);
        return 1;
    }

    // (ops) wipes a player's death tax debts: the only way besides paying. Creditors get nothing for it.
    private static int forgive(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(c, "player");
        DeathTaxData d = data(player);
        long was = d.owed();
        d.debts.clear();
        save(player, d);
        player.sendSystemMessage(skull().append(Component.literal("An admin forgave your death tax. You're free of Purgatory.").withStyle(ChatFormatting.GREEN)));
        c.getSource().sendSuccess(() -> Component.literal("Forgave " + player.getGameProfile().name() + "'s death tax (" + GoldMoney.format(was) + ")."), true);
        return 1;
    }

    static MutableComponent skull() {
        return Component.literal("☠ ").withStyle(s -> s.withColor(TextColor.fromRgb(RED)).withBold(true));
    }
}
