package com.alexh.capitalism.decor;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.ModBlockEntities;
import com.alexh.capitalism.event.BloodMoon;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// A Blood Moon decoration's block entity (decor/BloodDecorBlock, BloodBedBlock, BloodDoorBlock), on its main block:
// client/BloodDecorRenderer draws the whole piece from it. A few do more by themselves on the server (serverTick):
//  - the Iron Maiden is a chest (27 slots, saved, dropped when it's broken) whose door swings open while anyone has it
//    open (openers, like a vanilla chest); armed, it slams shut on the first mob to step inside it (8 damage, held 3
//    seconds), then lets go, disarmed;
//  - the Blood Pipe Organ plays its melodies, note by note (Song);
//  - the Gothic Grandfather Clock gives a redstone signal at night and tolls thirteen times when a Blood Moon begins;
//    the Haunted Portrait whispers at midnight;
//  - the Blood Crystal Fireplace burns its wood down (fuel) and heals whoever stands in front of it for 5 seconds;
//  - the Crimson Chandelier's candles gutter out under a Blood Moon and come back after;
//  - gold nuggets tossed into the Blood Fountain's basin are taken (BloodDecorBlock.tossCoin).
// The bookshelf door remembers who placed it (owner): only they (or the owner of the lot it's on) can open it.
// On the client it remembers how far a lid, door or curtain has swung, so it moves smoothly.
public class BloodDecorBlockEntity extends BlockEntity implements Container, MenuProvider {

    public static final int SLOTS = 27;
    // the fireplace: how near its front you stand to be healed (blocks from its hearth), for how long first (ticks)
    public static final double HEARTH_RANGE = 3.0;
    public static final int WARM_UP = 100;
    // the iron maiden: what its spikes do, how long it holds its victim (ticks)
    public static final float MAIDEN_DAMAGE = 8.0F;
    public static final int MAIDEN_HOLD = 60;

    // client: how far open (0 shut to 1 open) and when it was last moved (game time + partial ticks)
    public float lid = -1.0F;
    public float lidTime;
    // client: the box it's drawn in (client/BloodDecorRenderer.getRenderBoundingBox)
    public AABB renderBox;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    // the organ: which melody is next, which is playing and how far in (ticks), or -1
    private int song;
    private int songTick = -1;
    private int playing;
    // the clock and portrait: the hour last seen (-1: not yet), bell strokes still to toll and when the next is due;
    // whether a Blood Moon was on last time the clock looked
    private int lastHour = -1;
    private int tolls;
    private int tollIn;
    private boolean moonWasUp;
    // the fireplace: wood left (ticks of burning); how long each player has stood before it (not saved)
    private int fuel;
    private final Map<UUID, Integer> warming = new HashMap<>();
    // the iron maiden: who it holds and for how much longer
    private @Nullable UUID victim;
    private int holding;
    // the bookshelf door: who placed it
    private @Nullable UUID owner;

    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            if (state.hasProperty(BloodDecorBlock.OPEN)) level.setBlock(pos, state.setValue(BloodDecorBlock.OPEN, true), 3);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.playSound(null, pos, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.8F, 0.5F);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            if (state.hasProperty(BloodDecorBlock.OPEN)) level.setBlock(pos, state.setValue(BloodDecorBlock.OPEN, false), 3);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.55F);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
        }

        @Override
        public boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof ChestMenu menu && menu.getContainer() == BloodDecorBlockEntity.this;
        }
    };

    public BloodDecorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLOOD_DECOR.get(), pos, state);
    }

    @Nullable BloodDecorKind kind() {
        return this.getBlockState().getBlock() instanceof BloodDecorKind.Holder h ? h.kind() : null;
    }

    // the middle of a piece's footprint, on the ground (its main block at `pos`)
    public static Vec3 centreOf(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        Vec3 c = Vec3.atBottomCenterOf(pos);
        if (!(state.getBlock() instanceof BloodDecorBlock block)) return c;
        Direction front = state.getValue(BloodDecorBlock.FACING), back = front.getOpposite(), right = front.getCounterClockWise();
        double w = (block.kind().wide - 1) * 0.5, d = (block.kind().deep - 1) * 0.5;
        return c.add(right.getStepX() * w + back.getStepX() * d, 0, right.getStepZ() * w + back.getStepZ() * d);
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, BloodDecorBlockEntity be) {
        BloodDecorKind kind = be.kind();
        if (kind == null) return;
        switch (kind) {
            case BLOOD_PIPE_ORGAN -> be.playOrgan(level, pos, state);
            case GRANDFATHER_CLOCK -> be.keepClock(level, pos, state);
            case HAUNTED_PORTRAIT -> be.watchMidnight(level, pos);
            case BLOOD_CRYSTAL_FIREPLACE -> be.burn(level, pos, state);
            case CRIMSON_CHANDELIER -> {
                if (level.getGameTime() % 20 == Math.floorMod(pos.asLong(), 20)) gutter(level, pos, state);
            }
            case IRON_MAIDEN -> be.watchTrap(level, pos, state);
            case BLOOD_FOUNTAIN -> {
                if (level.getGameTime() % 10 == Math.floorMod(pos.asLong(), 10)) takeCoins(level, pos);
            }
            default -> {
            }
        }
    }

    // ── the fireplace ──

    public int fuel() {
        return this.fuel;
    }

    public void addFuel(int ticks) {
        this.fuel = Math.min(BloodDecorBlock.MAX_FUEL, this.fuel + ticks);
        this.setChanged();
    }

    // burning: the wood goes down (out when it's gone); whoever stands before it for 5 seconds is healed (Regeneration I,
    // kept topped up while they stay)
    private void burn(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getValue(BloodDecorBlock.LIT)) {
            this.warming.clear();
            return;
        }
        if (--this.fuel <= 0) {
            this.fuel = 0;
            BloodDecorBlock.setLit(level, pos, state, false);
            this.setChanged();
            return;
        }
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        if (level.getGameTime() % 200 == 0) this.setChanged();
        this.warmTick(level, pos, state);
    }

    // one second at the fire: who's in front of it gets a second nearer their healing
    public void warmTick(ServerLevel level, BlockPos pos, BlockState state) {
        Direction front = state.getValue(BloodDecorBlock.FACING);
        Vec3 hearth = centreOf(level, pos).add(0, 0.5, 0);
        Map<UUID, Integer> seen = new HashMap<>();
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(BlockPos.containing(hearth)).inflate(HEARTH_RANGE + 1))) {
            if (!inFrontOfHearth(hearth, front, p.position())) continue;
            int t = this.warming.getOrDefault(p.getUUID(), 0) + 20;
            seen.put(p.getUUID(), t);
            if (t >= WARM_UP) p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, true));
        }
        this.warming.clear();
        this.warming.putAll(seen);
    }

    // standing in front of the fireplace's hearth (`front`: the way it faces), within HEARTH_RANGE of it?
    public static boolean inFrontOfHearth(Vec3 hearth, Direction front, Vec3 at) {
        Vec3 d = at.subtract(hearth);
        double ahead = d.x * front.getStepX() + d.z * front.getStepZ();
        return ahead > 0.2 && Math.sqrt(d.x * d.x + d.z * d.z) <= HEARTH_RANGE && Math.abs(d.y) < 2.0;
    }

    // ── the chandelier ──

    // under a Blood Moon its candles gutter out (TRIPPED); when it's over they come back
    static void gutter(ServerLevel level, BlockPos pos, BlockState state) {
        boolean moon = BloodMoon.isActive();
        if (moon && state.getValue(BloodDecorBlock.LIT) && !state.getValue(BloodDecorBlock.TRIPPED)) {
            level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, false).setValue(BloodDecorBlock.TRIPPED, true), 3);
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.6F);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 24, 0.45, 0.2, 0.45, 0.01);
        } else if (!moon && state.getValue(BloodDecorBlock.TRIPPED)) {
            level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, true).setValue(BloodDecorBlock.TRIPPED, false), 3);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.3F, 1.6F);
        }
    }

    // ── the fountain ──

    // gold nuggets lying in the basin: taken (BloodDecorBlock.tossCoin)
    static void takeCoins(ServerLevel level, BlockPos pos) {
        Vec3 c = centreOf(level, pos);
        AABB basin = new AABB(c.x - 0.95, pos.getY(), c.z - 0.95, c.x + 0.95, pos.getY() + 0.8, c.z + 0.95);
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, basin, e -> e.getItem().is(Items.GOLD_NUGGET))) {
            int n = e.getItem().getCount();
            e.discard();
            BloodDecorBlock.tossCoin(level, pos, n);
        }
    }

    // ── the clock and the portrait ──

    // the clock: its redstone signal follows the night (LIT); a Blood Moon beginning sets it tolling thirteen times
    private void keepClock(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.tolls > 0 && --this.tollIn <= 0) {
            level.playSound(null, pos, SoundEvents.BELL_BLOCK, SoundSource.BLOCKS, 2.0F, 0.42F);
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 2.0F, 0.5F);
            level.sendParticles(new DustParticleOptions(0xC01020, 0.8F), pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 6, 0.3, 0.2, 0.3, 0.0);
            this.tolls--;
            this.tollIn = 30;
        }
        if (level.getGameTime() % 20 != Math.floorMod(pos.asLong(), 20)) return;
        boolean night = isNight(level);
        if (state.getValue(BloodDecorBlock.LIT) != night) level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, night), 3);
        boolean moon = BloodMoon.isActive();
        if (moon && !this.moonWasUp) {
            this.tolls = 13;
            this.tollIn = 1;
        }
        this.moonWasUp = moon;
    }

    // 6 pm to 6 am (the clock's signal)
    public static boolean isNight(Level level) {
        return !Calendar.isDay(Calendar.clock(level));
    }

    public int tollsLeft() {
        return this.tolls;
    }

    // the portrait: it whispers at midnight
    private void watchMidnight(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 != 0) return;
        int hour = Calendar.minuteOfDay(Calendar.clock(level)) / 60;
        if (this.lastHour >= 0 && hour != this.lastHour && hour == 0) {
            level.playSound(null, pos, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), SoundSource.BLOCKS, 0.6F, 1.4F);
            level.playSound(null, pos, SoundEvents.VEX_AMBIENT, SoundSource.BLOCKS, 0.25F, 0.5F);
            level.sendParticles(new DustParticleOptions(0xC01020, 0.6F), pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 12, 0.3, 0.4, 0.3, 0.0);
        }
        this.lastHour = hour;
    }

    // ── the iron maiden's trap ──

    // armed (LIT): the first mob (never a player) to step into it gets the spikes: the door slams (TRIPPED), 8 damage,
    // held inside for 3 seconds; then it lets go and is disarmed
    private void watchTrap(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getValue(BloodDecorBlock.TRIPPED)) {
            Entity held = this.victim == null ? null : level.getEntity(this.victim);
            if (held instanceof Mob mob && mob.isAlive() && --this.holding > 0) {
                Vec3 c = Vec3.atBottomCenterOf(pos).add(0, 0.1, 0);
                mob.teleportTo(c.x, c.y, c.z);
                mob.setDeltaMovement(Vec3.ZERO);
                return;
            }
            this.victim = null;
            this.holding = 0;
            level.setBlock(pos, state.setValue(BloodDecorBlock.TRIPPED, false).setValue(BloodDecorBlock.LIT, false), 3);
            level.playSound(null, pos, SoundEvents.IRON_DOOR_OPEN, SoundSource.BLOCKS, 0.8F, 0.7F);
            return;
        }
        if (!state.getValue(BloodDecorBlock.LIT)) return;
        AABB hollow = new AABB(pos).inflate(-0.15, 0, -0.15).expandTowards(0, 1.0, 0);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, hollow, Entity::isAlive)) {
            spring(level, pos, state, mob);
            return;
        }
    }

    // the door slams on `mob`
    public void spring(ServerLevel level, BlockPos pos, BlockState state, Mob mob) {
        this.victim = mob.getUUID();
        this.holding = MAIDEN_HOLD;
        level.setBlock(pos, state.setValue(BloodDecorBlock.TRIPPED, true).setValue(BloodDecorBlock.LIT, false), 3);
        mob.hurtServer(level, level.damageSources().cactus(), MAIDEN_DAMAGE);
        mob.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, MAIDEN_HOLD, 10, false, false));
        level.playSound(null, pos, SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.4F, 0.5F);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.5F, 0.6F);
        level.sendParticles(new DustParticleOptions(0x9A0A14, 1.2F), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.3, 0.6, 0.3, 0.05);
    }

    public @Nullable UUID victim() {
        return this.victim;
    }

    // ── the bookshelf door's owner ──

    public @Nullable UUID owner() {
        return this.owner;
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        this.setChanged();
    }

    // ── the organ ──

    // starts the next melody (true), or stops the one playing (false)
    public boolean toggleOrgan(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.songTick >= 0) {
            this.songTick = -1;
            level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, false), 3);
            return false;
        }
        this.playing = this.song;
        this.song = (this.song + 1) % Song.SONGS.size();
        this.songTick = 0;
        level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, true), 3);
        this.setChanged();
        return true;
    }

    public boolean isPlaying() {
        return this.songTick >= 0;
    }

    public String songTitle() {
        return Song.SONGS.get(this.playing).title;
    }

    private void playOrgan(ServerLevel level, BlockPos pos, BlockState state) {
        if (this.songTick < 0) return;
        Song s = Song.SONGS.get(this.playing);
        Vec3 c = centreOf(level, pos);
        for (Song.Note n : s.notes) {
            if (n.tick != this.songTick) continue;
            level.playSound(null, pos, n.sound, SoundSource.RECORDS, n.volume, n.pitch());
            // red notes rising from the pipes
            level.sendParticles(ParticleTypes.NOTE, c.x + (level.getRandom().nextDouble() - 0.5) * 1.6, pos.getY() + 2.6,
                c.z + (level.getRandom().nextDouble() - 0.5) * 1.6, 0, 0.25, 0.0, 0.0, 1.0);
        }
        if (++this.songTick > s.length) {
            this.songTick = -1;
            if (state.getValue(BloodDecorBlock.LIT)) level.setBlock(pos, state.setValue(BloodDecorBlock.LIT, false), 3);
        }
    }

    // A melody for the organ: notes written "fA5/4": an instrument (f flute: the pipes, b bass, h harp, c chime, B bell), a
    // note (C to B, # for sharp, its octave), how many ticks until the next; "fA5+bD2/4" plays them together; "-/4" rests.
    public static final class Song {
        record Note(int tick, SoundEvent sound, int index, float volume) {
            float pitch() {
                return (float) Math.pow(2.0, (this.index - 12) / 12.0);
            }
        }

        final String title;
        final List<Note> notes = new ArrayList<>();
        int length;

        Song(String title, String score) {
            this.title = title;
            int tick = 0;
            for (String token : score.trim().split("\\s+")) {
                int slash = token.indexOf('/');
                int dur = slash < 0 ? 4 : Integer.parseInt(token.substring(slash + 1));
                String chord = slash < 0 ? token : token.substring(0, slash);
                if (!chord.equals("-")) {
                    for (String n : chord.split("\\+")) this.notes.add(note(tick, n));
                }
                tick += dur;
            }
            this.length = tick + 10;
        }

        private static Note note(int tick, String n) {
            char inst = n.charAt(0);
            String name = n.substring(1);
            int octave = name.charAt(name.length() - 1) - '0';
            String letter = name.substring(0, name.length() - 1);
            int semis = switch (letter.charAt(0)) {
                case 'C' -> 0;
                case 'D' -> 2;
                case 'E' -> 4;
                case 'F' -> 5;
                case 'G' -> 7;
                case 'A' -> 9;
                default -> 11;
            } + (letter.endsWith("#") ? 1 : 0);
            int midi = 12 * (octave + 1) + semis;
            SoundEvent sound;
            int base;
            float volume;
            switch (inst) {
                case 'b' -> { sound = SoundEvents.NOTE_BLOCK_BASS.value(); base = 30; volume = 1.0F; }
                case 'h' -> { sound = SoundEvents.NOTE_BLOCK_HARP.value(); base = 54; volume = 0.6F; }
                case 'c' -> { sound = SoundEvents.NOTE_BLOCK_CHIME.value(); base = 78; volume = 0.5F; }
                case 'B' -> { sound = SoundEvents.NOTE_BLOCK_BELL.value(); base = 78; volume = 0.6F; }
                default -> { sound = SoundEvents.NOTE_BLOCK_FLUTE.value(); base = 66; volume = 0.9F; }
            }
            int index = Math.max(0, Math.min(24, midi - base));
            return new Note(tick, sound, index, volume);
        }

        public static final List<Song> SONGS = List.of(
            new Song("Toccata of the Blood Moon",
                "fA5+bD2/3 fG5/3 fA5/10 fG5/2 fF5/2 fE5/2 fD5/2 fC#5+bA1/6 fD5+bD2/12 "
                    + "fA4+bD2/3 fG4/3 fA4/10 fE4/3 fF4/3 fC#4+bA1/6 fD4+bD2/14 "
                    + "fA4+fD5+bD2/4 fA4+fE5/4 fA4+fF5/4 fA4+fG5/4 fA4+fE5/4 fA4+fF5/4 fA4+fD5/4 fA4+fE5/4 fC#5+bA1/8 fD5+fA5+bD2/20"),
            new Song("Nocturne for the Dead",
                "bD2+hD4/4 hF4/4 hA4/4 fD5/12 bA1+hC#4/4 hE4/4 hA4/4 fE5/12 bA#1+hD4/4 hF4/4 hA#4/4 fF5/8 fE5/4 "
                    + "bG1+hD4/4 hG4/4 hA#4/4 fD5/8 fC5/4 bA1+hC#4/4 hE4/4 hG4/4 fA4/8 fC#5/4 bD2+hD4+fD5/24"),
            new Song("The Witch's Waltz",
                "bG1+fG5/6 hD4+hG4/3 hD4+hG4/3 fA#5/6 fA5/6 bD2+fG5/6 hD4+hF#4/3 hD4+hF#4/3 fA5/12 "
                    + "bG1+fD5/6 hD4+hG4/3 hD4+hG4/3 fG5/6 fF#5/6 bD2+fA5/6 hC4+hF#4/3 hC4+hF#4/3 fG5/6 fD5/6 "
                    + "bC2+fD#5/6 hC4+hG4/3 hC4+hG4/3 fD5/6 fC5/6 bD2+fA#4/6 hD4+hF#4/3 fA4/3 fF#4/6 bG1+fG4+BG5/24"),
            new Song("Lament of the Revenant",
                "bE2+fE5/8 fG5/8 bD2+fF#5/8 fE5/8 bC2+fG5/8 fB4/8 bB1+fD#5/16 "
                    + "bA1+fC5/8 fE5/8 bB1+fD#5/8 fF#5/8 bE2+fG5/6 fF#5/2 fE5/8 fD#5/8 bE1+fE5+BE5/24")
        );
    }


    // ── the iron maiden's chest ──

    @Override
    public Component getDisplayName() {
        return Component.literal(BloodDecorKind.IRON_MAIDEN.title);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return ChestMenu.threeRows(id, inventory, this);
    }

    @Override
    public void startOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openers.incrementOpeners(user.getLivingEntity(), this.level, this.worldPosition, this.getBlockState(), user.getContainerInteractionRange());
        }
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!this.remove && !user.getLivingEntity().isSpectator() && this.level != null) {
            this.openers.decrementOpeners(user.getLivingEntity(), this.level, this.worldPosition, this.getBlockState());
        }
    }

    @Override
    public List<ContainerUser> getEntitiesWithContainerOpen() {
        return this.level == null ? List.of() : this.openers.getEntitiesWithContainerOpen(this.level, this.worldPosition);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (this.level != null) Containers.dropContents(this.level, pos, this.items);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack out = ContainerHelper.removeItem(this.items, slot, count);
        if (!out.isEmpty()) this.setChanged();
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.items.set(slot, stack);
        stack.limitSize(this.getMaxStackSize(stack));
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    // ── saving ──

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, this.items);
        this.song = input.getIntOr("song", 0);
        this.fuel = input.getIntOr("fuel", 0);
        this.owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, this.items);
        output.putInt("song", this.song);
        output.putInt("fuel", this.fuel);
        if (this.owner != null) output.store("owner", UUIDUtil.CODEC, this.owner);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }
}
