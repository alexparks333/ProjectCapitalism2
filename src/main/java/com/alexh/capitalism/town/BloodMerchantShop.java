package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModItems;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

// What the Blood Merchant sells (Town.md > The Blood Merchant): one-of-a-kind Blood Moon decorations (decor/, Decorations.md)
// for Blood Shards (the bosses' trophies also take that boss's piece of the crest), and an Essential Wishing Star for a
// whole Blood Moon Crest, once per player (who's bought one is saved with the world: STAR_BUYERS). Mending the crest
// (the three pieces joined) costs MEND_SHARDS. Prices are his own (Blood Shards), not NpcPricing's (that's gold).
public final class BloodMerchantShop {

    public static final int MEND_SHARDS = 50;

    // what: the item's id (looked up when needed); shards: its price; piece: the crest piece it also takes (or null);
    // star: the Essential Wishing Star (paid with a Blood Moon Crest, once per player)
    public record Offer(String id, int shards, @Nullable Supplier<Item> piece, boolean star) {
        public Item item() {
            return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Capitalism.MODID, this.id));
        }

        // there (the decorations are a separate part of the mod: an id that isn't registered is left out)
        public boolean exists() {
            return this.item() != Items.AIR;
        }
    }

    public static final List<Offer> OFFERS = List.of(
        new Offer("bloodstone_bricks", 1, null, false),
        new Offer("blood_crystal_block", 1, null, false),
        new Offer("crimson_rug", 50, null, false),
        new Offer("blood_moon_lantern", 60, null, false),
        new Offer("blood_moon_banner", 75, null, false),
        new Offer("crimson_candelabra", 90, null, false),
        new Offer("bat_roost", 100, null, false),
        new Offer("bat_gargoyle", 125, null, false),
        new Offer("haunted_portrait", 150, null, false),
        new Offer("vampire_coffin", 200, null, false),
        new Offer("crimson_chandelier", 200, null, false),
        new Offer("iron_maiden", 225, null, false),
        new Offer("crimson_throne", 250, null, false),
        new Offer("grandfather_clock", 250, null, false),
        new Offer("secret_bookshelf_door", 275, null, false),
        new Offer("crimson_canopy_bed", 275, null, false),
        new Offer("blood_pipe_organ", 300, null, false),
        new Offer("blood_moon_altar", 300, null, false),
        new Offer("blood_crystal_fireplace", 325, null, false),
        new Offer("blood_fountain", 350, null, false),
        new Offer("revenant_greatsword_statue", 300, ModItems.REVENANT_CREST::get, false),
        new Offer("sanguine_staff_pedestal", 300, ModItems.SANGUINE_CREST::get, false),
        new Offer("gorefang_skull_trophy", 300, ModItems.GOREFANG_CREST::get, false),
        new Offer("essential_wishing_star", 0, ModItems.BLOOD_MOON_CREST::get, true));

    private BloodMerchantShop() {
    }

    // ── paying ──

    public static int count(Player player, Item item) {
        int n = 0;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(item)) n += stack.getCount();
        }
        return n;
    }

    public static void take(Player player, Item item, int n) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize() && n > 0; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(item)) continue;
            int k = Math.min(n, stack.getCount());
            stack.shrink(k);
            n -= k;
        }
    }

    public static int shards(Player player) {
        return count(player, ModItems.BLOOD_SHARD.get());
    }

    // ── who's had their star ──

    public static final class StarBuyers extends SavedData {
        private static final Codec<StarBuyers> CODEC = Codec.STRING.listOf().xmap(StarBuyers::new, s -> new ArrayList<>(s.players));
        public static final SavedDataType<StarBuyers> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(Capitalism.MODID, "blood_merchant_stars"), StarBuyers::new, CODEC);

        private final Set<String> players = new HashSet<>();

        public StarBuyers() {
        }

        private StarBuyers(List<String> players) {
            this.players.addAll(players);
        }

        public static StarBuyers get(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }

        public boolean has(UUID player) {
            return this.players.contains(player.toString());
        }

        public void add(UUID player) {
            if (this.players.add(player.toString())) this.setDirty();
        }
    }
}
