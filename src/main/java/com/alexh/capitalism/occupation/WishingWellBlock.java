package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

// The Wishing Well (Jobs.md > The Wishing Well): a round stone well, 3 x 3 blocks and 3 high, with two posts, a crank
// and a bucket on a rope, and a shingled roof. Right-click it with a Wishing Star (Essential or Fun) and your wish is
// granted: one more job slot of that kind (up to JobData.MAX_SLOTS), then /pick to fill it.
//  - It's made of 21 parts (one block each, PART): the 9 of the stone ring (the middle one the core, which draws the
//    whole well: client/WishingWellRenderer), the 2 posts and the crank axle above, and the 9 of the roof. Each part's
//    collision is worked out from the well's real shape (shape()), so you bump into the round wall, stand on the floor
//    inside it under the water, walk round the posts, and the roof is over your head.
//  - Placed from the middle, needing the room for all of it. Survival players can't break it (strength like bedrock);
//    an admin in Creative breaking any part takes the whole well away.
public class WishingWellBlock extends Block implements EntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    // the parts: {right, up, front} from the core, as you face the well's front
    static final int[][] PARTS = {
        {0, 0, 0}, {-1, 0, -1}, {-1, 0, 0}, {-1, 0, 1}, {0, 0, -1}, {0, 0, 1}, {1, 0, -1}, {1, 0, 0}, {1, 0, 1},
        {-1, 1, 0}, {0, 1, 0}, {1, 1, 0},
        {-1, 2, -1}, {-1, 2, 0}, {-1, 2, 1}, {0, 2, -1}, {0, 2, 0}, {0, 2, 1}, {1, 2, -1}, {1, 2, 0}, {1, 2, 1}};
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, PARTS.length - 1);

    // the well, in pixels from the core block's middle (x right, y up, z front): its stone ring, the floor inside, the
    // posts, the axle, the roof (ridge along x)
    static final float RING_OUT = 22.0F;
    static final float RING_IN = 16.5F;
    static final float RING_TOP = 14.0F;
    static final float FLOOR = 3.0F;
    static final float POST_X = 20.0F;
    static final float POST_TOP = 44.0F;
    static final float AXLE_Y = 28.5F;
    static final float RIDGE = 47.0F;
    static final float ROOF_DROP = 16.0F / 28.0F;   // how far the roof falls per pixel out from the ridge
    static final float ROOF_HALF_X = 29.0F;
    static final float ROOF_HALF_Z = 29.0F;

    private static final Map<Direction, VoxelShape[]> SHAPES = new EnumMap<>(Direction.class);

    static {
        VoxelShape[] north = new VoxelShape[PARTS.length];
        for (int i = 0; i < PARTS.length; i++) north[i] = shape(PARTS[i][0], PARTS[i][1], PARTS[i][2]);
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape[] turned = new VoxelShape[PARTS.length];
            for (int i = 0; i < PARTS.length; i++) turned[i] = turn(north[i], facing);
            SHAPES.put(facing, turned);
        }
    }

    public WishingWellBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    // ── where the parts are (facing north: right = east, front = north) ──

    private static Direction right(Direction facing) {
        return facing.getClockWise();
    }

    public static BlockPos partPos(BlockPos core, Direction facing, int part) {
        int[] p = PARTS[part];
        return core.relative(right(facing), p[0]).above(p[1]).relative(facing, p[2]);
    }

    public static BlockPos corePos(BlockPos pos, BlockState state) {
        int[] p = PARTS[state.getValue(PART)];
        Direction facing = state.getValue(FACING);
        return pos.relative(right(facing), -p[0]).below(p[1]).relative(facing, -p[2]);
    }

    // ── placing and breaking ──

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos core = context.getClickedPos();
        Level level = context.getLevel();
        if (core.getY() + 2 > level.getMaxY()) return null;
        for (int i = 0; i < PARTS.length; i++) {
            if (!level.getBlockState(partPos(core, facing, i)).canBeReplaced(context)) return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        Direction facing = state.getValue(FACING);
        for (int i = 1; i < PARTS.length; i++) level.setBlock(partPos(pos, facing, i), state.setValue(PART, i), Block.UPDATE_ALL);
    }

    // breaking any part (only an admin in Creative can) takes the whole well
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos core = corePos(pos, state);
            Direction facing = state.getValue(FACING);
            for (int i = 0; i < PARTS.length; i++) {
                BlockPos p = partPos(core, facing, i);
                if (!p.equals(pos) && level.getBlockState(p).is(this)) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ── shapes ──

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(FACING))[state.getValue(PART)];
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    // One part's shape, facing north, from the well's real shape: checked in 2 x 2 pixel columns across the block, each
    // column as tall as what's there (the ring wall, the floor inside it, a post, the axle, the roof), runs of equal
    // columns joined into one box.
    private static VoxelShape shape(int right, int up, int front) {
        VoxelShape out = Shapes.empty();
        for (int px = 0; px < 16; px += 2) {
            int runStart = 0;
            float[] run = null;
            for (int pz = 0; pz <= 16; pz += 2) {
                float[] span = pz < 16 ? column(right, up, front, px + 1, pz + 1) : null;
                boolean same = run != null && span != null && run[0] == span[0] && run[1] == span[1];
                if (!same) {
                    if (run != null && run[1] > run[0]) {
                        out = Shapes.or(out, Block.box(px, run[0], runStart, px + 2, run[1], pz));
                    }
                    run = span;
                    runStart = pz;
                }
            }
        }
        return out;
    }

    // what's solid in this block's pixel column (px, pz from its north-west corner; front = north = -z), as {from, to}
    // in the block's own pixels (0..16), or null
    private static float[] column(int right, int up, int front, float px, float pz) {
        // from the core's middle, in pixels: x to the right, z to the front
        float x = right * 16 + px - 8;
        float z = front * 16 + (16 - pz) - 8;
        float y0 = up * 16;
        float lo = Float.MAX_VALUE;
        float hi = -Float.MAX_VALUE;
        float d = (float) Math.sqrt(x * x + z * z);
        // the stone ring, and the floor inside it
        if (d <= RING_OUT) {
            float top = d >= RING_IN ? RING_TOP : FLOOR;
            lo = Math.min(lo, 0);
            hi = Math.max(hi, top);
        }
        // the posts (4 x 4) up to the roof
        if (Math.abs(Math.abs(x) - POST_X) <= 2.0F && Math.abs(z) <= 2.0F) {
            lo = Math.min(lo, 0);
            hi = Math.max(hi, POST_TOP);
        }
        // the axle between them
        if (Math.abs(x) <= POST_X && Math.abs(z) <= 1.5F) {
            lo = Math.min(lo, AXLE_Y - 1.5F);
            hi = Math.max(hi, AXLE_Y + 1.5F);
        }
        // the roof: falling away from the ridge, 2.5 thick
        if (Math.abs(x) <= ROOF_HALF_X && Math.abs(z) <= ROOF_HALF_Z) {
            float roofTop = RIDGE - Math.abs(z) * ROOF_DROP;
            lo = Math.min(lo, roofTop - 2.5F);
            hi = Math.max(hi, roofTop);
        }
        // only the part of it inside this block, in its own pixels
        float from = Math.max(lo, y0) - y0;
        float to = Math.min(hi, y0 + 16) - y0;
        if (to <= from) return null;
        return new float[] {Math.round(from), Math.max(Math.round(from) + 1, Math.round(to))};
    }

    private static VoxelShape turn(VoxelShape north, Direction facing) {
        VoxelShape[] out = {Shapes.empty()};
        north.forAllBoxes((x1, y1, z1, x2, y2, z2) -> out[0] = Shapes.or(out[0], switch (facing) {
            case EAST -> Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2);
            case SOUTH -> Shapes.box(1 - x2, y1, 1 - z2, 1 - x1, y2, 1 - z1);
            case WEST -> Shapes.box(z1, y1, 1 - x2, z2, y2, 1 - x1);
            default -> Shapes.box(x1, y1, z1, x2, y2, z2);
        }));
        return out[0].optimize();
    }

    // ── wishing ──

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        // a Changing Star: dropped in the well, your jobs can change, and the job screen opens to pick which one goes
        if (stack.getItem() instanceof ChangingStar) {
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
            JobData jobs = sp.getData(ModAttachments.JOBS.get());
            if (!jobs.changing()) {
                if (!player.isCreative()) stack.shrink(1);
                ChangingStar.startChanging(sp);
                BlockPos core = corePos(pos, state);
                ServerLevel server = (ServerLevel) level;
                server.sendParticles(ParticleTypes.END_ROD, core.getX() + 0.5, core.getY() + 0.95, core.getZ() + 0.5, 40, 0.5, 0.6, 0.5, 0.06);
                server.sendParticles(ParticleTypes.SPLASH, core.getX() + 0.5, core.getY() + 0.65, core.getZ() + 0.5, 30, 0.4, 0.1, 0.4, 0.1);
                level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 1.4F);
                level.playSound(null, core, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 0.6F, 1.4F);
            }
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(sp, new com.alexh.capitalism.network.OpenJobPickPacket());
            return InteractionResult.SUCCESS;
        }
        if (!(stack.getItem() instanceof WishingStarItem star)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        BlockPos core = corePos(pos, state);
        JobData data = sp.getData(ModAttachments.JOBS.get());
        Job.Category kind = star.category;
        if (!data.addSlot(kind)) {
            sp.sendSystemMessage(Component.literal("The well can't grant you another " + kind.title + " job.").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        if (!player.isCreative()) stack.shrink(1);
        sp.setData(ModAttachments.JOBS.get(), data);
        JobEvents.sync(sp);
        // the wish: the star sinks into the water in a burst of light
        ServerLevel server = (ServerLevel) level;
        double cx = core.getX() + 0.5, cy = core.getY() + 0.65, cz = core.getZ() + 0.5;
        server.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.3, cz, 40, 0.5, 0.6, 0.5, 0.06);
        server.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 20, 0.6, 0.2, 0.6, 0.0);
        server.sendParticles(ParticleTypes.SPLASH, cx, cy, cz, 30, 0.4, 0.1, 0.4, 0.1);
        level.playSound(null, core, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.2F, 1.0F);
        level.playSound(null, core, SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 0.6F, 1.4F);
        level.playSound(null, core, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.3F);
        sp.sendSystemMessage(Component.literal("✨ Your wish is granted! ").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
            .append(Component.literal("You can hold " + data.slots(kind) + " " + kind.title + " jobs now. Type /pick to choose one.")
                .withStyle(ChatFormatting.LIGHT_PURPLE)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.literal("The water glitters... Bring a Wishing Star and make a wish.").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }

    // glints rising off the water, now and then a drip
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != 0) return;
        if (random.nextFloat() < 0.3F) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = random.nextDouble() * 0.85;
            level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 0.58, pos.getZ() + 0.5 + Math.sin(a) * r,
                0.0, 0.015, 0.0);
        }
        if (random.nextFloat() < 0.1F) {
            level.addParticle(ParticleTypes.DRIPPING_WATER, pos.getX() + 0.5, pos.getY() + 0.68, pos.getZ() + 0.5, 0, 0, 0);
        }
    }

    // ── the core draws the well ──

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new WishingWellBlockEntity(pos, state) : null;
    }

    public static class WishingWellBlockEntity extends BlockEntity {
        public WishingWellBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.WISHING_WELL.get(), pos, state);
        }
    }
}
