package com.alexh.capitalism.enchanter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// Moving XP through the Transfer Tube (TransferTubeBlock): the XP of whoever is in the booth the arrow points away from
// goes to whoever is in the other booth. Each booth must hold exactly one player OR one Apprentice (Apprentice: it
// stores XP). It's XP points that move, not levels. A player gives ALL their XP (levels and the bar); an Apprentice
// takes only what fits and the rest stays with the player. An Apprentice gives everything it holds.
public final class XpTransfer {

    private static final double TELL_RANGE = 8.0;

    private XpTransfer() {
    }

    // Who's in a booth: a player or an Apprentice
    private record Occupant(Entity entity, @Nullable ServerPlayer player, @Nullable Apprentice apprentice) {
        String name() {
            return this.apprentice != null ? this.apprentice.title() : this.entity.getName().getString();
        }

        long points() {
            return this.apprentice != null ? this.apprentice.storedXp() : totalPoints(this.player);
        }

        // How much more it can take
        long room() {
            return this.apprentice != null ? this.apprentice.room() : Long.MAX_VALUE;
        }

        int level() {
            return levelFor(this.points());
        }

        void take(long amount) {
            if (this.apprentice != null) {
                this.apprentice.setStoredXp(this.apprentice.storedXp() - amount);
                return;
            }
            long left = totalPoints(this.player) - amount;
            this.player.setExperienceLevels(0);
            this.player.setExperiencePoints(0);
            this.player.totalExperience = 0;
            if (left > 0) this.player.giveExperiencePoints((int) Math.min(Integer.MAX_VALUE, left));
        }

        void give(long amount) {
            if (this.apprentice != null) this.apprentice.setStoredXp(this.apprentice.storedXp() + amount);
            else this.player.giveExperiencePoints((int) Math.min(Integer.MAX_VALUE, amount));
        }

        // "Steve (level 25 -> 18)", or an Apprentice's "Iron Apprentice (0 -> 469 / 550 XP)"
        String change(long after) {
            if (this.apprentice != null) return this.name() + " (" + this.points() + " -> " + after + " / " + this.apprentice.kind().capacity() + " XP)";
            return this.name() + " (level " + this.level() + " -> " + levelFor(after) + ")";
        }
    }

    public static void run(ServerLevel level, BlockPos middleBottom, Direction facing, boolean toRight) {
        Occupant left = inBooth(level, TransferTubeBlock.partPos(middleBottom, facing, TransferTubeBlock.Part.LEFT_BOTTOM));
        Occupant right = inBooth(level, TransferTubeBlock.partPos(middleBottom, facing, TransferTubeBlock.Part.RIGHT_BOTTOM));
        if (left == null || right == null) {
            stop(level, middleBottom, "Transfer stopped: one player or Apprentice has to stand in each booth.");
            return;
        }
        Occupant from = toRight ? left : right;
        Occupant to = toRight ? right : left;
        long amount = Math.min(from.points(), to.room());
        if (from.points() <= 0) {
            stop(level, middleBottom, "Transfer stopped: " + from.name() + " has no XP.");
            return;
        }
        if (amount <= 0) {
            stop(level, middleBottom, "Transfer stopped: the " + to.name() + " is full.");
            return;
        }
        long fromBefore = from.points();
        long toBefore = to.points();
        String fromText = from.change(fromBefore - amount);
        String toText = to.change(toBefore + amount);
        from.take(amount);
        to.give(amount);

        // a stream of enchanting glyphs from one booth to the other
        Vec3 a = from.entity().position().add(0, 1.0, 0);
        Vec3 b = to.entity().position().add(0, 1.0, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3 p = a.lerp(b, i / 12.0);
            level.sendParticles(ParticleTypes.ENCHANT, p.x, p.y, p.z, 6, 0.15, 0.3, 0.15, 0.4);
        }
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, b.x, b.y, b.z, 20, 0.3, 0.6, 0.3, 0.2);
        level.playSound(null, middleBottom, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.9F, 1.0F);
        level.playSound(null, to.entity().blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 0.8F);
        tell(level, middleBottom, fromText + " gave " + amount + " XP to " + toText + ".");
    }

    // Said when the button is pressed: who would give how much, and the levels both end up at (if both booths are
    // filled right now; it's whoever is in them 2 seconds later that counts)
    public static void preview(ServerLevel level, BlockPos middleBottom, Direction facing, boolean toRight) {
        Occupant left = inBooth(level, TransferTubeBlock.partPos(middleBottom, facing, TransferTubeBlock.Part.LEFT_BOTTOM));
        Occupant right = inBooth(level, TransferTubeBlock.partPos(middleBottom, facing, TransferTubeBlock.Part.RIGHT_BOTTOM));
        if (left == null || right == null) {
            tell(level, middleBottom, "Transfer in 2 seconds... (one player or Apprentice in each booth)");
            return;
        }
        Occupant from = toRight ? left : right;
        Occupant to = toRight ? right : left;
        long amount = Math.min(from.points(), to.room());
        tell(level, middleBottom, "In 2 s: " + from.change(from.points() - amount) + " gives " + amount + " XP to "
            + to.change(to.points() + amount));
    }

    private static void stop(ServerLevel level, BlockPos middleBottom, String why) {
        tell(level, middleBottom, why);
        level.playSound(null, middleBottom, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 0.7F, 1.4F);
    }

    // The level this many points makes
    public static int levelFor(long points) {
        int level = 0;
        while (pointsForLevel(level + 1) <= points) level++;
        return level;
    }

    // The one player or Apprentice standing in this booth (feet inside its column), or null if it's empty or crowded
    private static @Nullable Occupant inBooth(ServerLevel level, BlockPos bottom) {
        AABB box = new AABB(bottom.getX(), bottom.getY(), bottom.getZ(), bottom.getX() + 1, bottom.getY() + 2, bottom.getZ() + 1);
        List<Occupant> found = new ArrayList<>();
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, box, player -> !player.isSpectator()
            && player.isAlive() && box.contains(player.position().add(0, 0.01, 0)))) {
            found.add(new Occupant(player, player, null));
        }
        for (Apprentice apprentice : level.getEntitiesOfClass(Apprentice.class, box, apprentice -> apprentice.isAlive()
            && box.contains(apprentice.position().add(0, 0.01, 0)))) {
            found.add(new Occupant(apprentice, null, apprentice));
        }
        return found.size() == 1 ? found.getFirst() : null;
    }

    // All a player's XP in points: their levels plus the bar
    public static long totalPoints(Player player) {
        int level = player.experienceLevel;
        long points = pointsForLevel(level);
        return points + Math.round(player.experienceProgress * player.getXpNeededForNextLevel());
    }

    // Points from 0 to the start of this level (vanilla's formula)
    public static long pointsForLevel(int level) {
        long l = level;
        if (l <= 16) return l * l + 6 * l;
        if (l <= 31) return Math.round(2.5 * l * l - 40.5 * l + 360);
        return Math.round(4.5 * l * l - 162.5 * l + 2220);
    }

    // A line over the hotbar for everyone near the tube
    public static void tell(ServerLevel level, BlockPos pos, String text) {
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(Vec3.atCenterOf(pos)) <= TELL_RANGE * TELL_RANGE) {
                player.sendOverlayMessage(Component.literal(text));
            }
        }
    }
}
