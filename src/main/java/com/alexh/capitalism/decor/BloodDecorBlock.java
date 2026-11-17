package com.alexh.capitalism.decor;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.List;

// A Blood Moon decoration (decor/BloodDecorKind, Decorations.md > Blood Moon decorations): always there, drawn by
// client/BloodDecorRenderer (its block model is empty), facing the way it was placed. Anyone can break it (it drops
// itself) and put it anywhere. The big ones fill several blocks (their footprint, BloodDecorKind): placing one needs
// all of them free, and part blocks (decor/BloodPartBlock) fill the rest, each solid with its slice of the piece. The
// skull trophy and portrait hang on a wall, the chandelier from a ceiling. What each one does when you use it is
// below (useWithoutItem, useItemOn) and in BloodDecorBlockEntity (what it does by itself); BloodAltar runs the altar.
public class BloodDecorBlock extends BaseEntityBlock implements BloodDecorKind.Holder {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    // open: the iron maiden's door (while someone has its chest open); the throne: someone sits on it
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    // lit: the chandelier's candles, the fireplace's fire, the organ playing, the clock's night signal, the iron maiden
    // armed (its door standing open, its spikes waiting)
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    // tripped: the iron maiden shut on a victim; the chandelier's candles guttered out under a Blood Moon
    public static final BooleanProperty TRIPPED = BooleanProperty.create("tripped");

    // the iron maiden armed or shut on someone: only its walls are solid, so a mob can walk into it
    private static final List<AABB> HOLLOW = List.of(new AABB(12, 0, 2, 14, 32, 14), new AABB(2, 0, 2, 12, 32, 3.5),
        new AABB(2, 0, 12.5, 12, 32, 14), new AABB(2, 0, 2, 14, 1.5, 14), new AABB(2, 30, 2, 14, 32, 14));

    // burning wood in the fireplace (ticks a piece; the most it holds)
    public static final int PLANK_FUEL = 1200, LOG_FUEL = 4800, MAX_FUEL = 36000;

    private final BloodDecorKind kind;

    public BloodDecorBlock(BloodDecorKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false)
            .setValue(LIT, kind == BloodDecorKind.CRIMSON_CHANDELIER).setValue(TRIPPED, false));
    }

    @Override
    public BloodDecorKind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OPEN, LIT, TRIPPED);
    }

    // a shape made facing west (front toward -x), turned to face `to`
    public static VoxelShape turn(VoxelShape west, Direction to) {
        VoxelShape out = Shapes.empty();
        for (AABB b : west.toAabbs()) {
            AABB r = switch (to) {
                case EAST -> new AABB(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
                case NORTH -> new AABB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
                case SOUTH -> new AABB(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
                default -> b;
            };
            out = Shapes.or(out, Shapes.create(r));
        }
        return out.optimize();
    }

    // the shape of one block of the piece (cx, cy, cz: which one; BloodDecorKind.cellOf), what you bump into or what
    // you point at
    VoxelShape shapeAt(BlockState state, int cx, int cy, int cz, boolean collision) {
        Direction front = state.getValue(FACING);
        if (collision && this.kind.passable) return Shapes.empty();
        if (collision && this.kind == BloodDecorKind.IRON_MAIDEN && (state.getValue(LIT) || state.getValue(TRIPPED))) {
            return this.kind.cellShape(HOLLOW, cx, cy, cz, front, 1);
        }
        return this.kind.cellShape(cx, cy, cz, front);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapeAt(state, 0, 0, 0, false);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return this.shapeAt(state, 0, 0, 0, true);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return this.kind.passable;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // ── placing and breaking ──

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state;
        if (this.kind.wallMounted) {
            // on the face of a wall that was clicked, facing out of it (clicking a floor or ceiling: the side you look at)
            Direction face = context.getClickedFace();
            Direction out = face.getAxis().isHorizontal() ? face : context.getHorizontalDirection().getOpposite();
            state = this.defaultBlockState().setValue(FACING, out);
        } else {
            state = this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }
        if (!state.canSurvive(context.getLevel(), context.getClickedPos())) return null;
        // a big piece needs every block of its footprint free
        if (this.kind.multiBlock() && !roomFor(this.kind, context.getLevel(), context.getClickedPos(), state.getValue(FACING), context)) {
            if (context.getPlayer() != null && !context.getLevel().isClientSide()) {
                context.getPlayer().sendOverlayMessage(Component.literal("There isn't room for the " + this.kind.title + " here ("
                    + this.kind.wide + " wide, " + this.kind.deep + " deep, " + this.kind.tall + " tall).").withStyle(ChatFormatting.RED));
            }
            return null;
        }
        return state;
    }

    // is every block of a piece's footprint free (air, grass...), and inside the world?
    public static boolean roomFor(BloodDecorKind kind, Level level, BlockPos main, Direction front, @Nullable BlockPlaceContext context) {
        for (int[] c : kind.otherCells()) {
            BlockPos p = BloodDecorKind.cellPos(main, front, c[0], c[1], c[2]);
            if (p.getY() >= level.getMaxY() || !level.getWorldBorder().isWithinBounds(p)) return false;
            BlockState there = level.getBlockState(p);
            if (!(context != null ? there.canBeReplaced(context) : there.canBeReplaced())) return false;
        }
        return true;
    }

    // placed: its part blocks fill the rest of its footprint
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        if (!level.isClientSide()) placeParts(level, pos, state.getValue(FACING), this.kind, pos);
    }

    // fills a piece's footprint (from `origin`, facing `front`) with part blocks pointing at `main`; the bed's parts are
    // only its upper blocks, each pointing at the bed block under it
    public static void placeParts(Level level, BlockPos origin, Direction front, BloodDecorKind kind, BlockPos main) {
        BlockState part = com.alexh.capitalism.core.ModBlocks.BLOOD_DECOR_PART.get().defaultBlockState();
        for (int[] c : kind.otherCells()) {
            if (kind.bed && c[1] == 0) continue;
            BlockPos p = BloodDecorKind.cellPos(origin, front, c[0], c[1], c[2]);
            BlockPos to = kind.bed ? p.below() : main;
            level.setBlock(p, BloodPartBlock.pointingAt(part, p, to), 3);
        }
    }

    // gone: its part blocks go with it (they drop nothing; the item came from this one)
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        removeParts(level, pos, state.getValue(FACING), this.kind, pos);
    }

    public static void removeParts(Level level, BlockPos origin, Direction front, BloodDecorKind kind, BlockPos main) {
        for (int[] c : kind.otherCells()) {
            BlockPos p = BloodDecorKind.cellPos(origin, front, c[0], c[1], c[2]);
            BlockState there = level.getBlockState(p);
            BlockPos to = kind.bed ? p.below() : main;
            if (there.getBlock() instanceof BloodPartBlock && BloodPartBlock.mainOf(there, p).equals(to)) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    // the skull trophy and the portrait need a sturdy wall behind them, the chandelier a ceiling over it
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (this.kind.ceiling) return level.getBlockState(pos.above()).isFaceSturdy(level, pos.above(), Direction.DOWN);
        if (!this.kind.wallMounted) return true;
        Direction out = state.getValue(FACING);
        BlockPos wall = pos.relative(out.getOpposite());
        return level.getBlockState(wall).isFaceSturdy(level, wall, out);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
            : super.updateShape(state, level, ticks, pos, direction, neighbourPos, neighbourState, random);
    }

    // ── the clock's redstone: 15 at night, 0 by day ──

    @Override
    protected boolean isSignalSource(BlockState state) {
        return this.kind == BloodDecorKind.GRANDFATHER_CLOCK;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return this.kind == BloodDecorKind.GRANDFATHER_CLOCK && state.getValue(LIT) ? 15 : 0;
    }

    // ── using it ──

    // something held in the hand: shards for the altar, a bottle or a gold nugget for the fountain, wood for the fireplace
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hit) {
        switch (this.kind) {
            case BLOOD_MOON_ALTAR -> {
                if (!stack.is(com.alexh.capitalism.core.ModItems.BLOOD_SHARD.get())) break;
                if (level instanceof ServerLevel server && player instanceof ServerPlayer sp) BloodAltar.offer(server, pos, sp, stack);
                return InteractionResult.SUCCESS;
            }
            case BLOOD_FOUNTAIN -> {
                if (stack.is(Items.GLASS_BOTTLE)) {
                    if (level instanceof ServerLevel server) fillBottle(server, pos, player, hand, stack);
                    return InteractionResult.SUCCESS;
                }
                if (stack.is(Items.GOLD_NUGGET)) {
                    if (level instanceof ServerLevel server) {
                        stack.consume(1, player);
                        tossCoin(server, pos, 1);
                    }
                    return InteractionResult.SUCCESS;
                }
            }
            case BLOOD_CRYSTAL_FIREPLACE -> {
                int fuel = stack.is(ItemTags.LOGS) ? LOG_FUEL : stack.is(ItemTags.PLANKS) ? PLANK_FUEL : 0;
                if (fuel == 0) break;
                if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BloodDecorBlockEntity fire) {
                    if (fire.fuel() + fuel > MAX_FUEL) {
                        player.sendOverlayMessage(Component.literal("The fire can't take any more wood.").withStyle(ChatFormatting.GRAY));
                    } else {
                        fire.addFuel(fuel);
                        stack.consume(1, player);
                        if (!state.getValue(LIT)) setLit(server, pos, state, true);
                        else server.playSound(null, pos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 0.8F);
                        player.sendOverlayMessage(Component.literal("🔥 Fuel: " + minutes(fire.fuel())).withStyle(ChatFormatting.GOLD));
                    }
                }
                return InteractionResult.SUCCESS;
            }
            default -> {
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    static String minutes(int ticks) {
        int s = ticks / 20;
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        switch (this.kind) {
            case CRIMSON_THRONE -> {
                // sit on its cushion, facing out (sneak to get up): Lord of the Manor
                if (!level.isClientSide()) {
                    Direction front = state.getValue(FACING);
                    Vec3 seat = Vec3.atBottomCenterOf(pos).add(front.getStepX() * 0.06, 0.66, front.getStepZ() * 0.06);
                    DecorSeat.sit(level, pos, player, seat, front);
                }
                return InteractionResult.SUCCESS;
            }
            case CRIMSON_CHANDELIER -> {
                if (level instanceof ServerLevel server) {
                    if (state.getValue(TRIPPED)) {
                        player.sendOverlayMessage(Component.literal("The candles won't stay lit under the Blood Moon.").withStyle(ChatFormatting.DARK_RED));
                    } else setLit(server, pos, state, !state.getValue(LIT));
                }
                return InteractionResult.SUCCESS;
            }
            case BLOOD_CRYSTAL_FIREPLACE -> {
                // put it out, or light it again (only with wood in it)
                if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BloodDecorBlockEntity fire) {
                    if (fire.fuel() <= 0) {
                        player.sendOverlayMessage(Component.literal("The fireplace needs wood: logs or planks.").withStyle(ChatFormatting.GRAY));
                    } else {
                        setLit(server, pos, state, !state.getValue(LIT));
                        player.sendOverlayMessage(Component.literal("🔥 Fuel: " + minutes(fire.fuel())).withStyle(ChatFormatting.GOLD));
                    }
                }
                return InteractionResult.SUCCESS;
            }
            case GRANDFATHER_CLOCK -> {
                if (level instanceof ServerLevel server) player.sendSystemMessage(clockReading(server));
                return InteractionResult.SUCCESS;
            }
            case BLOOD_PIPE_ORGAN -> {
                if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BloodDecorBlockEntity organ) {
                    boolean on = organ.toggleOrgan(server, pos, state);
                    player.sendOverlayMessage(Component.literal(on ? "♫ " + organ.songTitle() : "The organ falls silent.")
                        .withStyle(ChatFormatting.DARK_RED));
                }
                return InteractionResult.SUCCESS;
            }
            case IRON_MAIDEN -> {
                // a chest: right-click opens it (its door swings open while anyone has it open); sneak + right-click arms
                // the trap, or disarms it
                if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof BloodDecorBlockEntity maiden) {
                    if (player.isSecondaryUseActive()) {
                        if (state.getValue(TRIPPED)) {
                            player.sendOverlayMessage(Component.literal("It's busy.").withStyle(ChatFormatting.DARK_RED));
                        } else {
                            boolean arm = !state.getValue(LIT);
                            server.setBlock(pos, state.setValue(LIT, arm), 3);
                            server.playSound(null, pos, arm ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 0.55F);
                            server.playSound(null, pos, SoundEvents.CHAIN_STEP, SoundSource.BLOCKS, 0.8F, 0.5F);
                            player.sendOverlayMessage(Component.literal(arm ? "The iron maiden waits, open... (sneak + right-click to disarm)"
                                : "The iron maiden is disarmed.").withStyle(ChatFormatting.DARK_RED));
                        }
                    } else if (!state.getValue(TRIPPED)) player.openMenu(maiden);
                }
                return InteractionResult.SUCCESS;
            }
            case BLOOD_MOON_ALTAR -> {
                if (level instanceof ServerLevel server) player.sendOverlayMessage(BloodAltar.status(server));
                return InteractionResult.SUCCESS;
            }
            case BLOOD_FOUNTAIN -> {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Component.literal(com.alexh.capitalism.event.BloodMoon.isActive()
                        ? "The basin runs red tonight: fill a glass bottle." : "The basin only runs red under a Blood Moon. (Toss in a gold nugget for luck.)")
                        .withStyle(ChatFormatting.DARK_RED));
                }
                return InteractionResult.SUCCESS;
            }
            default -> {
                return InteractionResult.PASS;
            }
        }
    }

    // the chandelier's candles, the fireplace's fire: lit or put out, with a flourish
    public static void setLit(ServerLevel level, BlockPos pos, BlockState state, boolean lit) {
        level.setBlock(pos, state.setValue(LIT, lit), 3);
        double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
        if (lit) {
            level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.4F, 1.4F);
            level.sendParticles(new DustParticleOptions(0xFF4030, 0.9F), x, y, z, 16, 0.4, 0.3, 0.4, 0.02);
        } else {
            level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.8F);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
            level.sendParticles(ParticleTypes.SMOKE, x, y, z, 20, 0.45, 0.25, 0.45, 0.01);
        }
    }

    // ── the fountain ──

    // a glass bottle dipped in the basin: a Bottle of Blood, but only under a Blood Moon
    public static boolean fillBottle(ServerLevel level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!com.alexh.capitalism.event.BloodMoon.isActive()) {
            player.sendOverlayMessage(Component.literal("The basin only runs red under a Blood Moon.").withStyle(ChatFormatting.DARK_RED));
            return false;
        }
        ItemStack blood = new ItemStack(com.alexh.capitalism.core.ModItems.BOTTLE_OF_BLOOD.get());
        player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(stack, player, blood));
        level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 0.7F);
        level.sendParticles(new DustParticleOptions(0xB0101C, 0.8F), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.02);
        return true;
    }

    // gold nuggets tossed into the basin (`count` of them): each one gone with a splash and a plink, and one in five
    // brings a Blood Shard bobbing back up. How many shards came up.
    public static int tossCoin(ServerLevel level, BlockPos pos, int count) {
        Vec3 c = BloodDecorBlockEntity.centreOf(level, pos);
        int shards = 0;
        for (int i = 0; i < count; i++) if (level.getRandom().nextFloat() < 0.2F) shards++;
        level.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 0.4F, 1.6F);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.5F, 1.8F);
        level.sendParticles(new DustParticleOptions(0xC01020, 0.7F), c.x, pos.getY() + 0.45, c.z, 8 + 4 * count, 0.3, 0.05, 0.3, 0.05);
        level.sendParticles(ParticleTypes.SPLASH, c.x, pos.getY() + 0.45, c.z, 6, 0.3, 0.05, 0.3, 0.1);
        if (shards > 0) {
            ItemEntity shard = new ItemEntity(level, c.x, pos.getY() + 0.7, c.z, new ItemStack(com.alexh.capitalism.core.ModItems.BLOOD_SHARD.get(), shards),
                (level.getRandom().nextDouble() - 0.5) * 0.15, 0.32, (level.getRandom().nextDouble() - 0.5) * 0.15);
            shard.setPickUpDelay(10);
            level.addFreshEntity(shard);
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        return shards;
    }

    // what the grandfather clock tells you: the time, and how long until the next Blood Moon
    public static Component clockReading(ServerLevel level) {
        String time = com.alexh.capitalism.calendar.Calendar.timeText(com.alexh.capitalism.calendar.Calendar.clock(level));
        String moon;
        if (com.alexh.capitalism.event.BloodMoon.isActive()) moon = "The Blood Moon is upon us!";
        else {
            long in = com.alexh.capitalism.event.BloodMoon.ticksUntil(level.getServer());
            if (in < 0) moon = "The moon gives no sign yet.";
            else {
                long nights = in / 24000;
                moon = nights <= 0 ? "The Blood Moon rises tonight." : nights == 1 ? "The Blood Moon rises in 1 night."
                    : "The Blood Moon rises in " + nights + " nights.";
            }
        }
        return Component.literal("🕰 It is " + time + ". ").withStyle(ChatFormatting.GOLD)
            .append(Component.literal(moon).withStyle(ChatFormatting.DARK_RED));
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloodDecorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != com.alexh.capitalism.core.ModBlockEntities.BLOOD_DECOR.get()) return null;
        if (level.isClientSide()) return (l, pos, s, be) -> this.ambient(s, l, pos, l.getRandom());
        return switch (this.kind) {
            case BLOOD_PIPE_ORGAN, GRANDFATHER_CLOCK, HAUNTED_PORTRAIT, BLOOD_CRYSTAL_FIREPLACE, CRIMSON_CHANDELIER, IRON_MAIDEN, BLOOD_FOUNTAIN ->
                (l, pos, s, be) -> BloodDecorBlockEntity.serverTick((ServerLevel) l, pos, s, (BloodDecorBlockEntity) be);
            default -> null;
        };
    }

    // a little life, every tick on the client: the fountain's mist, spray and bubbling; candle smoke; the crystal's and
    // the sword's red motes
    void ambient(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // (the middle of its footprint)
        Vec3 c = BloodDecorBlockEntity.centreOf(level, pos);
        double x = c.x;
        double y = pos.getY();
        double z = c.z;
        switch (this.kind) {
            case BLOOD_FOUNTAIN -> {
                // a low red mist drifting over the basin
                if (random.nextFloat() < 0.35F) {
                    double a = random.nextDouble() * Math.PI * 2, r = 0.35 + random.nextDouble() * 0.5;
                    level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0xE8A0A4 : 0xC85060, 1.6F + random.nextFloat()),
                        x + Math.cos(a) * r, y + 0.42, z + Math.sin(a) * r, Math.cos(a) * 0.004, 0.003, Math.sin(a) * 0.004);
                }
                // spray where the four jets land, and a fine drizzle off the top bowl
                if (random.nextFloat() < 0.5F) {
                    double a = Math.PI / 4 + random.nextInt(4) * Math.PI / 2, r = 10.8 / 16.0;
                    level.addParticle(new DustParticleOptions(0xD01C2C, 0.6F), x + Math.cos(a) * r + (random.nextDouble() - 0.5) * 0.12, y + 0.42,
                        z + Math.sin(a) * r + (random.nextDouble() - 0.5) * 0.12, 0.0, 0.03 + random.nextDouble() * 0.03, 0.0);
                }
                if (random.nextFloat() < 0.15F) {
                    level.addParticle(new DustParticleOptions(0xB0101C, 0.45F), x + (random.nextDouble() - 0.5) * 0.35, y + 1.4,
                        z + (random.nextDouble() - 0.5) * 0.35, 0.0, -0.04, 0.0);
                }
                // the moon at its top shedding sparks
                if (random.nextFloat() < 0.08F) {
                    level.addParticle(new DustParticleOptions(0xFF6050, 0.4F), x + (random.nextDouble() - 0.5) * 0.25, y + 1.65,
                        z + (random.nextDouble() - 0.5) * 0.25, 0.0, 0.015, 0.0);
                }
                // bubbling and splashing
                if (random.nextFloat() < 0.012F) {
                    level.playLocalSound(x, y + 0.5, z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.4F, 0.5F + random.nextFloat() * 0.3F, false);
                }
                if (random.nextFloat() < 0.006F) {
                    level.playLocalSound(x, y + 0.5, z, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.25F, 0.55F + random.nextFloat() * 0.15F, false);
                }
                if (random.nextFloat() < 0.003F) {
                    level.playLocalSound(x, y + 0.5, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 0.25F, 0.6F, false);
                }
            }
            case CRIMSON_CANDELABRA -> {
                if (random.nextFloat() < 0.02F) {
                    level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.15, y + 1.3, z + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.01, 0.0);
                }
            }
            case SANGUINE_STAFF_PEDESTAL -> {
                if (random.nextFloat() < 0.1F) {
                    double a = random.nextDouble() * Math.PI * 2;
                    level.addParticle(new DustParticleOptions(0xE01830, 0.5F), x + Math.cos(a) * 0.3, y + 1.3 + random.nextDouble() * 0.4,
                        z + Math.sin(a) * 0.3, 0.0, 0.012, 0.0);
                }
            }
            case REVENANT_GREATSWORD_STATUE -> {
                if (random.nextFloat() < 0.05F) {
                    level.addParticle(new DustParticleOptions(0xFF3020, 0.5F), x + (random.nextDouble() - 0.5) * 0.5, y + 0.7,
                        z + (random.nextDouble() - 0.5) * 0.5, 0.0, 0.03, 0.0);
                }
            }
            case CRIMSON_CHANDELIER -> {
                if ((state.getValue(LIT) && random.nextFloat() < 0.03F) || (state.getValue(TRIPPED) && random.nextFloat() < 0.3F)) {
                    double a = random.nextInt(8) * Math.PI / 4;
                    level.addParticle(ParticleTypes.SMOKE, x + Math.cos(a) * 0.42, y + 0.55, z + Math.sin(a) * 0.42, 0.0, 0.01, 0.0);
                }
            }
            case BLOOD_CRYSTAL_FIREPLACE -> {
                if (!state.getValue(LIT)) break;
                Direction f = state.getValue(FACING);
                // the hearth's middle: a quarter block in from its front, toward the back
                double hx = x - f.getStepX() * 0.2, hz = z - f.getStepZ() * 0.2;
                double sx = -f.getStepZ(), sz = f.getStepX();
                if (random.nextFloat() < 0.6F) {
                    double o = (random.nextDouble() - 0.5) * 0.7;
                    level.addParticle(new DustParticleOptions(random.nextBoolean() ? 0xFF2A1A : 0xC0101E, 0.9F + random.nextFloat() * 0.6F),
                        hx + sx * o, y + 0.2 + random.nextDouble() * 0.3, hz + sz * o, 0.0, 0.04, 0.0);
                }
                if (random.nextFloat() < 0.08F) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, hx + sx * (random.nextDouble() - 0.5) * 0.5, y + 0.25, hz + sz * (random.nextDouble() - 0.5) * 0.5, 0.0, 0.02, 0.0);
                }
                if (random.nextFloat() < 0.05F) {
                    level.addParticle(ParticleTypes.LARGE_SMOKE, hx, y + 1.3, hz, 0.0, 0.04, 0.0);
                }
                if (random.nextFloat() < 0.03F) {
                    level.playLocalSound(x, y + 0.5, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 0.7F + random.nextFloat() * 0.3F, false);
                }
                if (random.nextFloat() < 0.004F) level.addParticle(ParticleTypes.LAVA, hx, y + 0.3, hz, 0.0, 0.0, 0.0);
            }
            case BAT_ROOST -> {
                int m = com.alexh.capitalism.calendar.Calendar.minuteOfDay(com.alexh.capitalism.calendar.Calendar.clock(level));
                boolean night = m >= 19 * 60 || m < 6 * 60;
                if (night && random.nextFloat() < 0.006F) {
                    level.playLocalSound(x, y + 1.6, z, SoundEvents.BAT_AMBIENT, SoundSource.BLOCKS, 0.35F, 0.9F + random.nextFloat() * 0.3F, false);
                }
            }
            case BLOOD_MOON_ALTAR -> {
                if (random.nextFloat() < 0.15F) {
                    double a = random.nextDouble() * Math.PI * 2;
                    level.addParticle(new DustParticleOptions(0xE01828, 0.5F), x + Math.cos(a) * 0.3, y + 1.1 + random.nextDouble() * 0.5,
                        z + Math.sin(a) * 0.3, 0.0, 0.012, 0.0);
                }
            }
            case BLOOD_MOON_LANTERN -> {
                if (random.nextFloat() < 0.02F) {
                    level.addParticle(new DustParticleOptions(0xFF4040, 0.35F), x + (random.nextDouble() - 0.5) * 0.2, y + 0.38,
                        z + (random.nextDouble() - 0.5) * 0.2, 0.0, 0.008, 0.0);
                }
            }
            default -> {
            }
        }
    }
}
