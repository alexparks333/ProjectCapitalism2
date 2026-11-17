package com.alexh.capitalism.realestate;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.economy.GoldMoney;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// Every lot of town land (RealEstate.md), saved with the world, and the rules about them: who can build where, buying
// a lot off the city, selling and leasing it, and the rent. Rent paid to someone who's offline waits for them (owed)
// and is handed over when they next log in. Lots are in the Overworld only.
public class Properties extends SavedData {

    public static final int DAY = 24000;

    private static final Codec<Properties> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Property.CODEC.listOf().fieldOf("properties").forGetter(p -> p.all),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("owed", Map.of()).forGetter(p -> p.owed)
    ).apply(inst, Properties::new));

    public static final SavedDataType<Properties> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Capitalism.MODID, "properties"), Properties::new, CODEC);

    private final List<Property> all = new ArrayList<>();
    private final Map<UUID, Long> owed = new HashMap<>();

    public Properties() {
    }

    private Properties(List<Property> all, Map<UUID, Long> owed) {
        this.all.addAll(all);
        this.owed.putAll(owed);
    }

    public static Properties get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static long today(MinecraftServer server) {
        return server.overworld().getGameTime() / DAY;
    }

    public List<Property> all() {
        return List.copyOf(this.all);
    }

    public @Nullable Property byName(String name) {
        for (Property p : this.all) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    public @Nullable Property at(BlockPos pos) {
        for (Property p : this.all) if (p.contains(pos)) return p;
        return null;
    }

    public static @Nullable Property at(Level level, BlockPos pos) {
        if (level.isClientSide() || level.dimension() != Level.OVERWORLD || level.getServer() == null) return null;
        return get(level.getServer()).at(pos);
    }

    // Can this player build at this spot (it's in a lot they own or lease)?
    public static boolean canBuild(Player player, Level level, BlockPos pos) {
        Property p = at(level, pos);
        return p != null && p.canBuild(player);
    }

    public static boolean isAgent(Player player) {
        return JobEvents.has(player, Job.REAL_ESTATE_AGENT);
    }

    // ── making and removing lots (ops) ──

    // A new lot: null if done, else what's wrong
    public @Nullable String create(String name, BlockPos a, BlockPos b) {
        if (!name.matches("[A-Za-z0-9_\\-]{1,32}")) return "A lot's name is one word: letters, numbers, - and _.";
        if (List.of("create", "delete", "list").contains(name.toLowerCase(Locale.ROOT))) return "That name is taken by a command.";
        if (this.byName(name) != null) return "There's already a lot called " + name + ".";
        Property lot = new Property(name, a.getX(), a.getZ(), b.getX(), b.getZ());
        for (Property p : this.all) if (p.overlaps(lot)) return "That overlaps " + p.name + ".";
        this.all.add(lot);
        this.setDirty();
        return null;
    }

    public boolean delete(String name) {
        Property p = this.byName(name);
        if (p == null) return false;
        this.all.remove(p);
        this.setDirty();
        return true;
    }

    public void setSalePrice(Property p, long nuggets) {
        p.salePrice = nuggets;
        this.setDirty();
    }

    public void setRent(Property p, long nuggets) {
        p.rent = nuggets;
        this.setDirty();
    }

    // ── buying, selling, leasing ──

    // A Real Estate Agent paid the City Chest everything it wanted (CityChestBlockEntity): the lot is theirs
    public @Nullable String claimFromCity(ServerPlayer agent, Property p) {
        if (!isAgent(agent)) return "Only a Real Estate Agent can unlock city land.";
        if (!p.cityOwned()) return p.name + " isn't city land any more.";
        this.giveTo(p, agent);
        this.setDirty();
        return null;
    }

    private void giveTo(Property p, Player player) {
        p.owner = player.getUUID();
        p.ownerName = player.getName().getString();
        p.salePrice = -1;
    }

    // The agent who owns it sells it to a player (they've settled the price between them): null if done, else what's wrong
    public @Nullable String sell(ServerPlayer agent, Property p, ServerPlayer buyer) {
        if (!p.ownedBy(agent) || !isAgent(agent)) return "Only the Real Estate Agent who owns " + p.name + " can sell it.";
        if (buyer == agent) return "You already own it.";
        if (p.leased()) return p.name + " is leased to " + p.tenantName + ": end the lease first (/properties " + p.name + " evict).";
        this.giveTo(p, buyer);
        p.rent = -1;
        this.setDirty();
        return null;
    }

    // ... or leases it to one: they can build on it and pay the rent every Minecraft day, from tomorrow
    public @Nullable String lease(ServerPlayer agent, Property p, ServerPlayer tenant, long today) {
        if (!p.ownedBy(agent) || !isAgent(agent)) return "Only the Real Estate Agent who owns " + p.name + " can lease it.";
        if (tenant == agent) return "You can't lease your own lot to yourself.";
        if (p.leased()) return p.name + " is already leased to " + p.tenantName + ".";
        if (p.rent < 0) return "Set the rent first: /properties " + p.name + " rent <gold a day>.";
        p.tenant = tenant.getUUID();
        p.tenantName = tenant.getName().getString();
        p.paidThrough = today;
        this.setDirty();
        return null;
    }

    public @Nullable String evict(ServerPlayer agent, Property p) {
        if (!p.ownedBy(agent)) return "Only the owner of " + p.name + " can end its lease.";
        if (!p.leased()) return p.name + " isn't leased.";
        this.endLease(p);
        return null;
    }

    private void endLease(Property p) {
        p.tenant = null;
        p.tenantName = "";
        p.paidThrough = -1;
        this.setDirty();
    }

    // ── rent: every Minecraft day, from tenants who are online (an offline one pays what's due when they're back);
    // a tenant who can't pay loses the lease ──

    public void collectRent(MinecraftServer server, long today) {
        this.collectRent(id -> server.getPlayerList().getPlayer(id), today);
    }

    // (who's online: a lookup, so the tests can stand in their own players)
    public void collectRent(java.util.function.Function<UUID, @Nullable ServerPlayer> online, long today) {
        for (Property p : this.all) {
            if (!p.leased() || p.paidThrough >= today || p.rent < 0) continue;
            ServerPlayer tenant = online.apply(p.tenant);
            if (tenant == null) continue;
            while (p.paidThrough < today) {
                if (!GoldMoney.take(tenant, p.rent)) {
                    tenant.sendSystemMessage(Component.literal("[Real Estate] You couldn't pay the rent on " + p.name
                        + " (" + GoldMoney.format(p.rent) + "): the lease has ended."));
                    ServerPlayer owner = p.owner == null ? null : online.apply(p.owner);
                    if (owner != null) owner.sendSystemMessage(Component.literal("[Real Estate] " + p.tenantName
                        + " couldn't pay the rent on " + p.name + ": the lease has ended."));
                    this.endLease(p);
                    break;
                }
                p.paidThrough++;
                this.pay(online, p.owner, p.rent);
                tenant.sendSystemMessage(Component.literal("[Real Estate] Paid " + GoldMoney.format(p.rent) + " rent on " + p.name + "."));
            }
            this.setDirty();
        }
    }

    // Gold to someone: now if they're online, else when they're next on
    private void pay(java.util.function.Function<UUID, @Nullable ServerPlayer> online, @Nullable UUID to, long nuggets) {
        if (to == null) return;
        ServerPlayer player = online.apply(to);
        if (player != null) GoldMoney.give(player, nuggets);
        else this.owed.merge(to, nuggets, Long::sum);
        this.setDirty();
    }

    // Logging in: any rent paid to them while they were away
    public void payOwed(ServerPlayer player) {
        Long due = this.owed.remove(player.getUUID());
        if (due == null || due <= 0) return;
        GoldMoney.give(player, due);
        player.sendSystemMessage(Component.literal("[Real Estate] Rent paid to you while you were away: " + GoldMoney.format(due) + "."));
        this.setDirty();
    }

    // What a lot is up to, in a few words
    public static String status(Property p) {
        if (p.cityOwned()) return "city land (bought at its City Chest)";
        String s = "owned by " + p.ownerName;
        if (p.leased()) s += ", leased to " + p.tenantName + " (" + GoldMoney.format(p.rent) + " a day)";
        else if (p.rent >= 0) s += ", for lease: " + GoldMoney.format(p.rent) + " a day";
        if (p.salePrice >= 0) s += ", for sale: " + GoldMoney.format(p.salePrice);
        return s;
    }
}
