package com.alexh.capitalism.bounty;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

// The Bounty Clerk's board, one for the whole server (Bounties.md), saved with the world:
//  - the task posted right now (everyone sees the same one; the moment someone takes it, a new one is posted);
//  - the last 20 posted (never posted again till they drop off);
//  - the milestone places: the first 3 players to turn in 50 bounties, in order (a place is reserved the moment they
//    reach 50, so nobody faster at clicking can take it), and whether each has claimed their Fun Wishing Star;
// (The standings are in Standings, which has a board for each thing ranked.)
public class BountyBoard extends SavedData {

    public static final int STARS = 3;

    public record Place(UUID id, String name, boolean claimed) {
        static final Codec<Place> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(Place::id),
            Codec.STRING.fieldOf("name").forGetter(Place::name),
            Codec.BOOL.optionalFieldOf("claimed", false).forGetter(Place::claimed)
        ).apply(i, Place::new));
    }

    static final Codec<BountyBoard> CODEC = RecordCodecBuilder.create(i -> i.group(
        BountyData.Open.CODEC.optionalFieldOf("posted").forGetter(b -> Optional.ofNullable(b.posted)),
        Codec.STRING.listOf().optionalFieldOf("history", List.of()).forGetter(b -> b.history),
        Place.CODEC.listOf().optionalFieldOf("places", List.of()).forGetter(b -> b.places)
    ).apply(i, (posted, history, places) -> new BountyBoard(posted.orElse(null), history, places)));

    static final SavedDataType<BountyBoard> TYPE = new SavedDataType<>(Identifier.fromNamespaceAndPath(Capitalism.MODID, "bounty_board"), BountyBoard::new, CODEC);

    BountyData.Open posted;
    final List<String> history = new ArrayList<>();
    final List<Place> places = new ArrayList<>();

    public BountyBoard() {
    }

    private BountyBoard(BountyData.Open posted, List<String> history, List<Place> places) {
        this.posted = posted;
        this.history.addAll(history);
        this.places.addAll(places);
    }

    public static BountyBoard get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public List<Place> places() {
        return List.copyOf(this.places);
    }

    public List<String> history() {
        return List.copyOf(this.history);
    }

    // a tier by its weight (Easy 40, Medium 30, Hard 20, Heroic 10)
    public static BountyTask.Tier rollTier(RandomSource random) {
        int roll = random.nextInt(100), acc = 0;
        for (BountyTask.Tier t : BountyTask.Tier.values()) {
            acc += t.weight;
            if (roll < acc) return t;
        }
        return BountyTask.Tier.EASY;
    }

    // a new task to post: a tier by weight, then any task of it not among the last 20 posted; a town task ("feed your
    // pet", the Shopfront ones) only if someone on can do it; its amount rolled, its reward to match
    BountyData.Open roll(MinecraftServer server, RandomSource random) {
        BountyTask.Tier[] tiers = BountyTask.Tier.values();
        int first = rollTier(random).ordinal();
        for (int step = 0; step < tiers.length; step++) {
            BountyTask.Tier tier = tiers[Math.floorMod(first - step, tiers.length)];
            List<BountyTask> pool = new ArrayList<>();
            for (BountyTask task : BountyTask.all()) {
                if (task.tier != tier || this.history.contains(task.id)) continue;
                if (task.conditional() && server.getPlayerList().getPlayers().stream().noneMatch(task::availableTo)) continue;
                pool.add(task);
            }
            if (!pool.isEmpty()) {
                BountyTask pick = pool.get(random.nextInt(pool.size()));
                int amount = pick.rollAmount(random);
                return new BountyData.Open(pick.id, amount, Bounties.reward(pick, amount), 0);
            }
        }
        BountyTask any = BountyTask.all().get(random.nextInt(BountyTask.all().size()));
        return new BountyData.Open(any.id, any.min, Bounties.reward(any, any.min), 0);
    }

    // post a new task (and remember it among the last 20)
    void post(BountyData.Open task) {
        this.posted = task;
        this.history.add(task.id());
        while (this.history.size() > 20) this.history.remove(0);
        this.setDirty();
    }

    // reserve a milestone place for this player, if one's left and they haven't one; their place (0, 1, 2) or -1
    int reserve(ServerPlayer player) {
        for (int i = 0; i < this.places.size(); i++) if (this.places.get(i).id().equals(player.getUUID())) return i;
        if (this.places.size() >= STARS) return -1;
        this.places.add(new Place(player.getUUID(), player.getGameProfile().name(), false));
        this.setDirty();
        return this.places.size() - 1;
    }

    int placeOf(UUID id) {
        for (int i = 0; i < this.places.size(); i++) if (this.places.get(i).id().equals(id)) return i;
        return -1;
    }

    void markClaimed(int place) {
        Place p = this.places.get(place);
        this.places.set(place, new Place(p.id(), p.name(), true));
        this.setDirty();
    }

    // (admin, by hand only) clear the milestone places
    void resetPlaces() {
        this.places.clear();
        this.setDirty();
    }
}
