package com.alexh.capitalism.bounty;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.occupation.Job;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// Where the players' Shopfronts are and whose (and whether the owner is a Cook), so the Bounty Clerk only offers the
// town tasks that can be done ("buy from a player's Shopfront", "sell from your Shopfront", "buy a meal from a Cook's
// Shopfront"). Noted when a Shopfront is placed and when something's bought from it; one that's gone is dropped when
// its spot is next checked while loaded. Saved with the world.
public class ShopIndex extends SavedData {

    record Shop(GlobalPos pos, UUID owner, boolean cook) {
        static final Codec<Shop> CODEC = RecordCodecBuilder.create(i -> i.group(
            GlobalPos.CODEC.fieldOf("pos").forGetter(Shop::pos),
            UUIDUtil.CODEC.fieldOf("owner").forGetter(Shop::owner),
            Codec.BOOL.optionalFieldOf("cook", false).forGetter(Shop::cook)
        ).apply(i, Shop::new));
    }

    private static final Codec<ShopIndex> CODEC = Shop.CODEC.listOf().xmap(ShopIndex::new, s -> new ArrayList<>(s.shops.values()));
    static final SavedDataType<ShopIndex> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "shop_index"), ShopIndex::new, CODEC);

    private final Map<GlobalPos, Shop> shops = new HashMap<>();

    public ShopIndex() {
    }

    private ShopIndex(List<Shop> shops) {
        shops.forEach(s -> this.shops.put(s.pos(), s));
    }

    static ShopIndex get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    // a Shopfront at `pos` owned by `owner` (placed, or something bought from it)
    public static void note(ServerLevel level, net.minecraft.core.BlockPos pos, ServerPlayer owner) {
        boolean cook = owner.getData(ModAttachments.JOBS.get()).has(Job.COOK);
        ShopIndex index = get(level.getServer());
        index.shops.put(GlobalPos.of(level.dimension(), pos.immutable()), new Shop(GlobalPos.of(level.dimension(), pos.immutable()), owner.getUUID(), cook));
        index.setDirty();
    }

    public static void note(ServerLevel level, net.minecraft.core.BlockPos pos, UUID owner, boolean cook) {
        ShopIndex index = get(level.getServer());
        index.shops.put(GlobalPos.of(level.dimension(), pos.immutable()), new Shop(GlobalPos.of(level.dimension(), pos.immutable()), owner, cook));
        index.setDirty();
    }

    // the owner of the Shopfront here is a Cook (as last noted)
    public static boolean cookAt(ServerLevel level, net.minecraft.core.BlockPos pos) {
        Shop s = get(level.getServer()).shops.get(GlobalPos.of(level.dimension(), pos));
        return s != null && s.cook();
    }

    // drop the ones whose spot is loaded and no longer a Shopfront
    private static List<Shop> live(MinecraftServer server) {
        ShopIndex index = get(server);
        List<Shop> out = new ArrayList<>();
        var it = index.shops.values().iterator();
        while (it.hasNext()) {
            Shop s = it.next();
            ServerLevel level = server.getLevel(s.pos().dimension());
            if (level != null && level.isLoaded(s.pos().pos())
                && !(level.getBlockEntity(s.pos().pos()) instanceof com.alexh.capitalism.economy.ShopfrontBlockEntity)) {
                it.remove();
                index.setDirty();
                continue;
            }
            out.add(s);
        }
        return out;
    }

    // someone else's Shopfront to buy from
    public static boolean anyOther(ServerPlayer player) {
        return live(player.level().getServer()).stream().anyMatch(s -> !s.owner().equals(player.getUUID()));
    }

    // a Shopfront of their own
    public static boolean owns(ServerPlayer player) {
        return live(player.level().getServer()).stream().anyMatch(s -> s.owner().equals(player.getUUID()));
    }

    // another player's Shopfront who's a Cook
    public static boolean anyCook(ServerPlayer player) {
        return live(player.level().getServer()).stream().anyMatch(s -> s.cook() && !s.owner().equals(player.getUUID()));
    }

    // (tests) forget them all
    public static void clear(MinecraftServer server) {
        get(server).shops.clear();
        get(server).setDirty();
    }
}
