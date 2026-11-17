package com.alexh.capitalism.fishing;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// A player's Fisherman's Collection (Fishing.md > The Collection; the Fisherman's Collection tab): every rare catch
// they have fished up themselves, how many and the heaviest. Recorded only when they land one (RareCatchModifier.land:
// the player holding the rod); buying, being given, picking up or spawning one doesn't count, and selling or mounting
// one never takes it off. All 15 caught: the Fisherman gives one Essential Wishing Star, once ever (`claimed`; separate
// from the Blood Merchant's star). Kept through death; synced to its player (ModAttachments.CATCH_COLLECTION).
public class CatchCollection {

    public record Entry(int count, float heaviest) {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("count").forGetter(Entry::count),
            Codec.FLOAT.optionalFieldOf("heaviest", 0.0F).forGetter(Entry::heaviest)
        ).apply(i, Entry::new));
    }

    public static final MapCodec<CatchCollection> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.unboundedMap(Codec.STRING, Entry.CODEC).optionalFieldOf("caught", Map.of()).forGetter(CatchCollection::byId),
        Codec.BOOL.optionalFieldOf("claimed", false).forGetter(c -> c.claimed)
    ).apply(i, CatchCollection::new));

    public static final StreamCodec<ByteBuf, CatchCollection> STREAM_CODEC = StreamCodec.of(
        (buf, c) -> {
            ByteBufCodecs.VAR_INT.encode(buf, c.caught.size());
            for (var e : c.caught.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buf, e.getKey().id);
                ByteBufCodecs.VAR_INT.encode(buf, e.getValue().count());
                ByteBufCodecs.FLOAT.encode(buf, e.getValue().heaviest());
            }
            ByteBufCodecs.BOOL.encode(buf, c.claimed);
        },
        buf -> {
            CatchCollection c = new CatchCollection();
            int n = ByteBufCodecs.VAR_INT.decode(buf);
            for (int i = 0; i < n; i++) {
                RareCatch kind = RareCatch.byId(ByteBufCodecs.STRING_UTF8.decode(buf));
                int count = ByteBufCodecs.VAR_INT.decode(buf);
                float heaviest = ByteBufCodecs.FLOAT.decode(buf);
                if (kind != null) c.caught.put(kind, new Entry(count, heaviest));
            }
            c.claimed = ByteBufCodecs.BOOL.decode(buf);
            return c;
        });

    private final Map<RareCatch, Entry> caught = new EnumMap<>(RareCatch.class);
    private boolean claimed;

    public CatchCollection() {
    }

    private CatchCollection(Map<String, Entry> byId, boolean claimed) {
        byId.forEach((id, e) -> {
            RareCatch kind = RareCatch.byId(id);
            if (kind != null) this.caught.put(kind, e);
        });
        this.claimed = claimed;
    }

    private Map<String, Entry> byId() {
        Map<String, Entry> out = new HashMap<>();
        this.caught.forEach((k, e) -> out.put(k.id, e));
        return out;
    }

    public static int total() {
        return RareCatch.values().length;
    }

    public Entry get(RareCatch kind) {
        return this.caught.get(kind);
    }

    public boolean has(RareCatch kind) {
        return this.caught.containsKey(kind);
    }

    public int count() {
        return this.caught.size();
    }

    public boolean complete() {
        return this.caught.size() >= total();
    }

    public boolean claimed() {
        return this.claimed;
    }

    // ── on the server ──

    public static CatchCollection of(ServerPlayer player) {
        return player.getData(ModAttachments.CATCH_COLLECTION.get());
    }

    // `player` fished one up (weight 0 for the bottle and the chest)
    public static void record(ServerPlayer player, RareCatch kind, float weight) {
        CatchCollection c = of(player);
        Entry old = c.caught.get(kind);
        c.caught.put(kind, new Entry(old == null ? 1 : old.count() + 1, Math.max(weight, old == null ? 0 : old.heaviest())));
        player.setData(ModAttachments.CATCH_COLLECTION.get(), c);   // saves and sends it to them
        if (old == null) {
            player.sendSystemMessage(Component.literal("🎣 New for your collection: " + kind.title + " (" + c.count() + " / " + total() + ")")
                .withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    // the prize: all caught, not claimed yet -> one Essential Wishing Star, a fanfare, everyone told. False if not.
    public static boolean claim(ServerPlayer player) {
        CatchCollection c = of(player);
        if (!c.complete() || c.claimed) return false;
        c.claimed = true;
        player.setData(ModAttachments.CATCH_COLLECTION.get(), c);
        ItemStack star = new ItemStack(ModItems.ESSENTIAL_WISHING_STAR.get());
        if (!player.getInventory().add(star)) player.drop(star, false, net.minecraft.util.Prediction.SERVER_ONLY);
        var level = player.level();
        level.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0, player.getZ(), 60, 0.6, 0.8, 0.6, 0.3);
        level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.5, player.getZ(), 30, 0.5, 0.6, 0.5, 0.15);
        player.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("🎣 " + player.getGameProfile().name()
            + " completed the Fisherman's Collection!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        return true;
    }

    // (admin) forget it all, the prize included (only by /npc fisherman collection <player> reset)
    public static void reset(ServerPlayer player) {
        player.setData(ModAttachments.CATCH_COLLECTION.get(), new CatchCollection());
    }
}
