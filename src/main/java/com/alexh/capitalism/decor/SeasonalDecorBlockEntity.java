package com.alexh.capitalism.decor;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

// A seasonal decoration's block entity (decor/SeasonalDecorBlock): putting it up and packing it away on time, one by
// one round the town; and for a String Light Hook, the hooks its strands of lights run to (strung with the String
// Lights: decor/StringLightsItem), drawn by client/DecorRenderer.
public class SeasonalDecorBlockEntity extends BlockEntity {

    // how many ticks after it's due it changes: its own moment, up to this, so the town's go up one by one
    public static final int STAGGER = 120;
    // longest strand of lights (blocks)
    public static final int MAX_STRAND = 24;

    private int countdown = -1;
    private final List<BlockPos> strands = new ArrayList<>();
    // client: the box it's drawn in (client/DecorRenderer.getRenderBoundingBox), kept until its strands change
    public net.minecraft.world.phys.AABB renderBox;
    public int renderBoxKey;

    public SeasonalDecorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEASONAL_DECOR.get(), pos, state);
    }

    public List<BlockPos> strands() {
        return List.copyOf(this.strands);
    }

    // a strand of lights from this hook to that one (true if added; one strand between any two hooks)
    public boolean addStrand(BlockPos to) {
        if (to.equals(this.worldPosition) || this.strands.contains(to)) return false;
        this.strands.add(to.immutable());
        this.changed();
        return true;
    }

    public boolean removeStrand(BlockPos to) {
        boolean removed = this.strands.remove(to);
        if (removed) this.changed();
        return removed;
    }

    // Every strand attached to the holder at pos taken down: its own, and any other holder's strand running to it
    // (holders up to MAX_STRAND blocks away, in loaded chunks). How many came down.
    public static int clearAllAt(ServerLevel level, BlockPos pos) {
        int removed = 0;
        if (level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity self) {
            removed += self.strands.size();
            self.clearStrands();
        }
        int reach = (MAX_STRAND >> 4) + 1;
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        for (int x = cx - reach; x <= cx + reach; x++) {
            for (int z = cz - reach; z <= cz + reach; z++) {
                if (!level.hasChunk(x, z)) continue;
                for (BlockEntity be : List.copyOf(level.getChunk(x, z).getBlockEntities().values())) {
                    if (be instanceof SeasonalDecorBlockEntity other && other.removeStrand(pos)) removed++;
                }
            }
        }
        return removed;
    }

    public void clearStrands() {
        this.strands.clear();
        this.changed();
    }

    private void changed() {
        this.setChanged();
        if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    // its own moment to change, from where it is (so it's the same each time)
    public static int delayFor(BlockPos pos) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        return 1 + (int) Math.floorMod(h >>> 20, STAGGER);
    }

    // checks on its next tick whether it's due to change, and changes then (the tests)
    public void dueNow() {
        this.countdown = 1;
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, SeasonalDecorBlockEntity decor) {
        if (!(state.getBlock() instanceof SeasonalDecorBlock block)) return;
        // check once a second whether it's due to change
        if (decor.countdown < 0) {
            if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
            boolean want = Decorations.shouldBeUp(level, block.kind());
            boolean swap = !want && decor.inSeason(level, pos) != null;
            if (!swap && want == state.getValue(SeasonalDecorBlock.UP)) return;
            decor.countdown = delayFor(pos);
            return;
        }
        if (--decor.countdown > 0) return;
        decor.countdown = -1;
        boolean want = Decorations.shouldBeUp(level, block.kind());
        // out of season, but a decoration kept here for another holiday is in season: that one takes the spot
        Stashed next = want ? null : decor.inSeason(level, pos);
        if (next != null) {
            swap(level, pos, state, decor, next);
            return;
        }
        if (want == state.getValue(SeasonalDecorBlock.UP)) return;
        setUp(level, pos, state, want);
    }

    // puts it up or packs it away now, with a flourish
    public static void setUp(ServerLevel level, BlockPos pos, BlockState state, boolean up) {
        level.setBlock(pos, state.setValue(SeasonalDecorBlock.UP, up), 3);
        DecorKind kind = ((SeasonalDecorBlock) state.getBlock()).kind();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double spread = kind == DecorKind.CHRISTMAS_TREE ? 2.5 : kind == DecorKind.STRING_LIGHTS ? 0.3 : 1.0;
        double height = kind == DecorKind.CHRISTMAS_TREE ? 4.0 : 0.6;
        if (up) {
            ParticleOptions sparkle = switch (kind.holiday) {
                case CHRISTMAS -> ParticleTypes.SNOWFLAKE;
                case HALLOWEEN -> ParticleTypes.WITCH;
                case HARVEST -> ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, 0xE87A20);
                case SUMMER -> ParticleTypes.WAX_ON;
                case SPRING -> ParticleTypes.CHERRY_LEAVES;
            };
            level.sendParticles(sparkle, x, y + height / 2, z, 30, spread, height / 2, spread, 0.02);
            level.sendParticles(ParticleTypes.END_ROD, x, y + height / 2, z, 8, spread, height / 2, spread, 0.01);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 1.2F);
        } else {
            level.sendParticles(ParticleTypes.POOF, x, y + height / 2, z, 20, spread, height / 2, spread, 0.02);
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0F, 0.9F);
        }
    }

    // ── one spot, one decoration per holiday ──
    // A decoration placed on a spot where a packed-away decoration of ANOTHER holiday sits goes in there, and keeps that
    // one (and any it was keeping) here, packed away: its block, the way it faced, a holder's strands. When the season
    // turns, whichever is in season takes the spot (swap); if the one showing is removed, the next kept one comes forward.

    public record Stashed(BlockState state, List<BlockPos> strands) {
        public static final com.mojang.serialization.Codec<Stashed> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(i -> i.group(
            BlockState.CODEC.fieldOf("state").forGetter(Stashed::state),
            BlockPos.CODEC.listOf().fieldOf("strands").forGetter(Stashed::strands)).apply(i, Stashed::new));

        public DecorKind kind() {
            return ((SeasonalDecorBlock) this.state.getBlock()).kind();
        }
    }

    private final List<Stashed> stash = new ArrayList<>();
    // our own change (a swap, a decoration placed over this one): the kept ones are not brought forward
    private boolean moving;
    // decorations kept at a spot while a new one is being placed over it (taken up by the new one's block entity)
    private static final java.util.Map<String, List<Stashed>> PENDING = new java.util.HashMap<>();

    private static String key(net.minecraft.world.level.Level level, BlockPos pos) {
        return level.dimension().identifier() + "@" + pos.asLong();
    }

    public List<Stashed> stash() {
        return List.copyOf(this.stash);
    }

    // a kept decoration that is in season now and can stand here, if any
    public Stashed inSeason(ServerLevel level, BlockPos pos) {
        for (Stashed s : this.stash) {
            if (s.state().getBlock() instanceof SeasonalDecorBlock && Decorations.shouldBeUp(level, s.kind()) && s.state().canSurvive(level, pos)) return s;
        }
        return null;
    }

    // A decoration is about to be placed over this packed-away one (decor/SeasonalDecorBlock.getStateForPlacement):
    // keep this one and the ones it keeps for the new one, except any of the new one's own holiday (dropped as items)
    public static void keepFor(ServerLevel level, BlockPos pos, DecorKind incoming) {
        if (!(level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity old)) return;
        BlockState here = level.getBlockState(pos);
        List<Stashed> kept = new ArrayList<>(old.stash);
        kept.add(new Stashed(here.setValue(SeasonalDecorBlock.UP, false), List.copyOf(old.strands)));
        kept.removeIf(s -> {
            boolean same = s.kind().holiday == incoming.holiday;
            if (same) net.minecraft.world.level.block.Block.popResource(level, pos, new net.minecraft.world.item.ItemStack(s.state().getBlock()));
            return same;
        });
        old.moving = true;
        PENDING.put(key(level, pos), kept);
    }

    // the new decoration's block entity takes up what was kept at its spot (decor/SeasonalDecorBlock.setPlacedBy)
    public void takeKept() {
        if (this.level == null) return;
        List<Stashed> kept = PENDING.remove(key(this.level, this.worldPosition));
        if (kept == null) return;
        this.stash.addAll(kept);
        this.changed();
    }

    // the kept decoration `next` takes the spot; the one showing is kept in its place
    static void swap(ServerLevel level, BlockPos pos, BlockState state, SeasonalDecorBlockEntity decor, Stashed next) {
        List<Stashed> rest = new ArrayList<>(decor.stash);
        rest.remove(next);
        rest.add(new Stashed(state.setValue(SeasonalDecorBlock.UP, false), List.copyOf(decor.strands)));
        if (state.getValue(SeasonalDecorBlock.UP)) {
            level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 15, 0.6, 0.4, 0.6, 0.02);
        }
        decor.moving = true;
        place(level, pos, next, rest);
        setUp(level, pos, level.getBlockState(pos), true);
    }

    private static void place(ServerLevel level, BlockPos pos, Stashed s, List<Stashed> rest) {
        level.setBlock(pos, s.state().setValue(SeasonalDecorBlock.UP, false), 3);
        if (level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity fresh) {
            fresh.strands.addAll(s.strands());
            fresh.stash.addAll(rest);
            fresh.changed();
        }
    }

    // removed some other way (an operator broke it, the block under frost went): the next kept decoration that can
    // stand here comes forward (packed away; it goes up on its own if it is in season)
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.moving || this.stash.isEmpty() || !(this.level instanceof ServerLevel server)) return;
        List<Stashed> rest = new ArrayList<>(this.stash);
        server.getServer().execute(() -> {
            if (!server.getBlockState(pos).isAir()) return;
            for (Stashed s : rest) {
                if (!s.state().canSurvive(server, pos)) continue;
                List<Stashed> others = new ArrayList<>(rest);
                others.remove(s);
                place(server, pos, s, others);
                return;
            }
        });
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("strands", BlockPos.CODEC.listOf(), List.copyOf(this.strands));
        output.store("stash", Stashed.CODEC.listOf(), List.copyOf(this.stash));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.strands.clear();
        input.read("strands", BlockPos.CODEC.listOf()).ifPresent(this.strands::addAll);
        this.stash.clear();
        input.read("stash", Stashed.CODEC.listOf()).ifPresent(this.stash::addAll);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
