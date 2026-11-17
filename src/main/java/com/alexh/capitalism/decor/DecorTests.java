package com.alexh.capitalism.decor;

import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.calendar.Calendar.Season;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

// The seasonal decorations (Decorations.md): each holiday's go up on its days and are packed away the rest of the year
// (and stay where they are), they change one by one, /decorations up/down/auto, packed away there's nothing to bump into
// and no light, string lights between hooks.
public final class DecorTests {

    static {
        GameTests.register("decorations", DecorTests::decorations);
    }

    private DecorTests() {
    }

    public static void init() {
    }

    private static void decorations(GameTests.Checks c) {
        ServerLevel level = c.level();
        // which are up when, by the calendar
        Calendar.setDate(level, 1, Season.WINTER, 12);
        c.check(Decorations.shouldBeUp(level, DecorKind.CHRISTMAS_TREE) && Decorations.shouldBeUp(level, DecorKind.STRING_LIGHTS)
            && !Decorations.shouldBeUp(level, DecorKind.SCARECROW) && !Decorations.shouldBeUp(level, DecorKind.PUMPKIN_PILE),
            "in winter the Christmas decorations are up, the fall ones packed away");
        Calendar.setDate(level, 1, Season.FALL, 5);
        c.check(Decorations.shouldBeUp(level, DecorKind.PUMPKIN_PILE) && Decorations.shouldBeUp(level, DecorKind.LEAF_PILE)
            && !Decorations.shouldBeUp(level, DecorKind.SCARECROW) && !Decorations.shouldBeUp(level, DecorKind.CHRISTMAS_TREE),
            "early fall: the harvest decorations, not Halloween's yet");
        Calendar.setDate(level, 1, Season.FALL, 17);
        boolean day17 = !Decorations.shouldBeUp(level, DecorKind.SCARECROW) && !Decorations.shouldBeUp(level, DecorKind.GROUND_FOG)
            && Decorations.shouldBeUp(level, DecorKind.AUTUMN_MAPLE) && Decorations.shouldBeUp(level, DecorKind.FALLEN_LEAVES);
        Calendar.setDate(level, 1, Season.FALL, 18);
        c.check(day17 && Decorations.shouldBeUp(level, DecorKind.SCARECROW) && Decorations.shouldBeUp(level, DecorKind.AUTUMN_MAPLE),
            "Halloween's go up on Fall 18; the fall ones are up all fall");
        Calendar.setDate(level, 1, Season.FALL, 20);
        c.check(Decorations.shouldBeUp(level, DecorKind.SCARECROW) && Decorations.shouldBeUp(level, DecorKind.WITCHS_CAULDRON)
            && Decorations.shouldBeUp(level, DecorKind.PUMPKIN_PILE), "late fall: Halloween's too");
        c.check(Decorations.shouldBeUp(level, DecorKind.GRAVESTONE_TOMB) && Decorations.shouldBeUp(level, DecorKind.GROUND_FOG)
            && Decorations.shouldBeUp(level, DecorKind.BONE_PILE) && Decorations.shouldBeUp(level, DecorKind.AUTUMN_MAPLE),
            "...gravestones, bones and fog, beside the autumn trees");
        Calendar.setDate(level, 1, Season.SUMMER, 10);
        boolean onlyBeach = true;
        for (DecorKind k : DecorKind.values()) onlyBeach &= Decorations.shouldBeUp(level, k) == (k.holiday == DecorKind.Holiday.SUMMER);
        c.check(onlyBeach && Decorations.shouldBeUp(level, DecorKind.BEACH_UMBRELLA), "in summer only the beach things are up");
        Calendar.setDate(level, 1, Season.SPRING, 10);
        boolean onlySpring = true;
        for (DecorKind k : DecorKind.values()) onlySpring &= Decorations.shouldBeUp(level, k) == (k.holiday == DecorKind.Holiday.SPRING);
        c.check(onlySpring && Decorations.shouldBeUp(level, DecorKind.GIANT_EGG), "in spring only the spring things are up");
        // every fall decoration, placed and ticked on Fall 20 (harvest and Halloween alike), is up
        Calendar.setDate(level, 1, Season.FALL, 20);
        StringBuilder notUp = new StringBuilder();
        int fx = 0;
        for (DecorKind k : DecorKind.values()) {
            if (k.holiday != DecorKind.Holiday.HARVEST && k.holiday != DecorKind.Holiday.HALLOWEEN) continue;
            if (k == DecorKind.CHRISTMAS_FROST || k == DecorKind.ICICLES) continue;
            net.minecraft.world.level.block.Block block = null;
            for (var e : net.minecraft.core.registries.BuiltInRegistries.BLOCK) if (e instanceof SeasonalDecorBlock d && d.kind() == k) block = e;
            if (block == null) { notUp.append(k).append(" (no block) "); continue; }
            BlockPos p = BlockPos.containing(c.at(fx++ % 5, 6, 5));
            level.setBlock(p, block.defaultBlockState(), 3);
            step(level, p);
            for (int i = 0; i < SeasonalDecorBlockEntity.STAGGER + 2; i++) step(level, p);
            if (!level.getBlockState(p).getValue(SeasonalDecorBlock.UP)) notUp.append(k).append(' ');
            level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        c.check(notUp.isEmpty(), "on Fall 20 every fall decoration goes up, trees too" + (notUp.isEmpty() ? "" : " (not: " + notUp + ")"));
        // fog, a towel and a tulip bed are walked through; a gravestone and a beach chair aren't
        c.check(DecorKind.GROUND_FOG.passable() && DecorKind.BEACH_TOWEL.passable() && DecorKind.TULIP_BED.passable()
            && !DecorKind.GRAVESTONE_ROUND.passable() && !DecorKind.BEACH_CHAIR.passable(), "fog, towels and tulip beds can be walked through");
        Calendar.setDate(level, 1, Season.SUMMER, 10);

        // a tree and a scarecrow placed in summer: packed away. Then it's winter: the tree goes up, the scarecrow doesn't
        BlockPos treePos = BlockPos.containing(c.at(1, 2, 1));
        BlockPos crowPos = BlockPos.containing(c.at(3, 2, 1));
        level.setBlock(treePos, ModBlocks.CHRISTMAS_TREE.get().defaultBlockState(), 3);
        level.setBlock(crowPos, ModBlocks.SPOOKY_SCARECROW.get().defaultBlockState(), 3);
        BlockState packed = level.getBlockState(treePos);
        c.check(!packed.getValue(SeasonalDecorBlock.UP) && packed.getCollisionShape(level, treePos, CollisionContext.empty()).isEmpty()
            && packed.getLightEmission() == 0, "packed away: nothing to bump into, no light");
        // Winter 4: still packed away (Christmas decorations are up Winter 5 to 28)
        Calendar.setDate(level, 1, Season.WINTER, 4);
        step(level, treePos);
        boolean earlyDown = !level.getBlockState(treePos).getValue(SeasonalDecorBlock.UP);
        Calendar.setDate(level, 1, Season.WINTER, 5);
        step(level, treePos);
        step(level, crowPos);
        c.check(earlyDown, "on Winter 4 the tree is still packed away; it goes up on Winter 5");
        BlockState tree = level.getBlockState(treePos);
        c.check(tree.getValue(SeasonalDecorBlock.UP) && !tree.getCollisionShape(level, treePos, CollisionContext.empty()).isEmpty()
            && tree.getLightEmission() == 12, "in winter the tree puts itself up: solid, lit (" + tree.getLightEmission() + ")");
        c.check(!level.getBlockState(crowPos).getValue(SeasonalDecorBlock.UP), "...the scarecrow stays packed away");
        // spring: the tree is packed away again, and it's still there
        Calendar.setDate(level, 2, Season.SPRING, 3);
        step(level, treePos);
        c.check(level.getBlockState(treePos).is(ModBlocks.CHRISTMAS_TREE.get()) && !level.getBlockState(treePos).getValue(SeasonalDecorBlock.UP),
            "in spring the tree packs itself away, staying where it is");
        // by hand: /decorations up and down
        Decorations.get(level.getServer()).setMode(Decorations.Mode.UP);
        step(level, crowPos);
        boolean upByHand = level.getBlockState(crowPos).getValue(SeasonalDecorBlock.UP);
        Decorations.get(level.getServer()).setMode(Decorations.Mode.DOWN);
        step(level, crowPos);
        boolean downByHand = !level.getBlockState(crowPos).getValue(SeasonalDecorBlock.UP);
        Decorations.get(level.getServer()).setMode(Decorations.Mode.AUTO);
        c.check(upByHand && downByHand, "/decorations up puts everything up, /decorations down packs it away");
        // one by one: different spots change at different moments, within a few seconds
        java.util.Set<Integer> delays = new java.util.HashSet<>();
        boolean inRange = true;
        for (int i = 0; i < 40; i++) {
            int d = SeasonalDecorBlockEntity.delayFor(treePos.offset(i * 3, 0, i * 7));
            delays.add(d);
            inRange &= d >= 1 && d <= SeasonalDecorBlockEntity.STAGGER;
        }
        c.check(inRange && delays.size() > 20, "they go up one by one round the town (" + delays.size() + " different moments of 40)");
        // string lights: hook to hook, once each, not to itself
        BlockPos a = BlockPos.containing(c.at(1, 3, 3));
        BlockPos b = BlockPos.containing(c.at(4, 3, 3));
        level.setBlock(a, ModBlocks.STRING_LIGHT_HOOK.get().defaultBlockState(), 3);
        level.setBlock(b, ModBlocks.STRING_LIGHT_HOOK.get().defaultBlockState(), 3);
        if (level.getBlockEntity(a) instanceof SeasonalDecorBlockEntity hook) {
            boolean first = hook.addStrand(b);
            boolean again = hook.addStrand(b);
            boolean self = hook.addStrand(a);
            c.check(first && !again && !self && hook.strands().equals(java.util.List.of(b)), "string lights run hook to hook, once each, never to itself");
            hook.clearStrands();
            c.check(hook.strands().isEmpty(), "...and come down again");
        } else {
            c.fail("no hook block entity");
        }
        // candy canes: Christmas; a Candy Cane Pole holds lights from near its top, like a hook
        c.check(DecorKind.CANDY_CANE_POLE.holiday == DecorKind.Holiday.CHRISTMAS && DecorKind.CANDY_CANES.holiday == DecorKind.Holiday.CHRISTMAS,
            "candy cane poles and candy canes are Christmas decorations");
        c.check(DecorKind.CANDY_CANE_POLE.isHook() && DecorKind.CANDY_CANE_POLE.hookHeight() > 3.5F && DecorKind.STRING_LIGHTS.isHook()
            && !DecorKind.CANDY_CANES.isHook(), "string lights hang from a hook or the top of a Candy Cane Pole");
        // Ctrl-click a holder: every strand on it comes down, its own and those strung to it
        BlockPos pole = BlockPos.containing(c.at(4, 2, 4));
        level.setBlock(pole, ModBlocks.CANDY_CANE_POLE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(a) instanceof SeasonalDecorBlockEntity ha && level.getBlockEntity(b) instanceof SeasonalDecorBlockEntity hb
            && level.getBlockEntity(pole) instanceof SeasonalDecorBlockEntity hp) {
            ha.addStrand(b);
            hb.addStrand(pole);
            hp.addStrand(a);
            int removed = SeasonalDecorBlockEntity.clearAllAt(level, b);
            c.check(removed == 2 && ha.strands().isEmpty() && hb.strands().isEmpty() && hp.strands().equals(java.util.List.of(a)),
                "clearing a holder takes down its own strand and the one strung to it, and leaves the rest (" + removed + " down)");
        } else {
            c.fail("no holder block entities");
        }
        // Christmas Frost lies on a block; Icicles hang under one; neither is solid; they fall off when that block goes
        BlockPos floor = BlockPos.containing(c.at(1, 2, 4));
        BlockPos ceiling = floor.above(3);
        level.setBlock(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(ceiling, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        BlockState frost = ModBlocks.CHRISTMAS_FROST.get().defaultBlockState().setValue(SeasonalDecorBlock.UP, true);
        BlockState icicles = ModBlocks.ICICLES.get().defaultBlockState().setValue(SeasonalDecorBlock.UP, true);
        c.check(frost.canSurvive(level, floor.above()) && !frost.canSurvive(level, floor.above(2)),
            "Christmas Frost lies on top of a block, not in the air");
        c.check(icicles.canSurvive(level, ceiling.below()) && !icicles.canSurvive(level, ceiling.below(2)),
            "Icicles hang from the underside of a block, not in the air");
        level.setBlock(floor.above(), frost, 3);
        level.setBlock(ceiling.below(), icicles, 3);
        c.check(level.getBlockState(floor.above()).getCollisionShape(level, floor.above(), CollisionContext.empty()).isEmpty()
            && level.getBlockState(ceiling.below()).getCollisionShape(level, ceiling.below(), CollisionContext.empty()).isEmpty()
            && level.getBlockState(floor.above()).getRenderShape() == net.minecraft.world.level.block.RenderShape.MODEL,
            "they're drawn as block models and nothing to bump into");
        level.setBlock(ceiling, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        c.check(!level.getBlockState(ceiling.below()).is(ModBlocks.ICICLES.get()), "Icicles fall when the block they hang from goes");
        for (BlockPos p : new BlockPos[]{floor, floor.above(), ceiling.below()}) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        // one spot, one decoration per holiday: a pumpkin pile placed over packed-away frost keeps the frost; in winter the
        // frost is back (the pile kept), in fall the pile (the frost kept)
        BlockPos ground = BlockPos.containing(c.at(3, 2, 0));
        BlockPos spot = ground.above();
        level.setBlock(ground, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        Calendar.setDate(level, 1, Season.SUMMER, 10);
        level.setBlock(spot, ModBlocks.CHRISTMAS_FROST.get().defaultBlockState(), 3);
        SeasonalDecorBlockEntity.keepFor(level, spot, DecorKind.PUMPKIN_PILE);
        level.setBlock(spot, ModBlocks.PUMPKIN_PILE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(spot) instanceof SeasonalDecorBlockEntity pile) pile.takeKept();
        boolean kept = level.getBlockEntity(spot) instanceof SeasonalDecorBlockEntity d1 && d1.stash().size() == 1
            && d1.stash().get(0).kind() == DecorKind.CHRISTMAS_FROST;
        c.check(kept, "a pumpkin pile placed over packed-away frost keeps the frost there");
        Calendar.setDate(level, 1, Season.WINTER, 10);
        step(level, spot);
        BlockState winter = level.getBlockState(spot);
        boolean pileKept = level.getBlockEntity(spot) instanceof SeasonalDecorBlockEntity d2 && d2.stash().size() == 1
            && d2.stash().get(0).kind() == DecorKind.PUMPKIN_PILE;
        c.check(winter.is(ModBlocks.CHRISTMAS_FROST.get()) && winter.getValue(SeasonalDecorBlock.UP) && pileKept,
            "in winter the frost is back, up, and the pumpkin pile is kept");
        Calendar.setDate(level, 2, Season.FALL, 5);
        step(level, spot);
        BlockState fall = level.getBlockState(spot);
        boolean frostKept = level.getBlockEntity(spot) instanceof SeasonalDecorBlockEntity d3 && d3.stash().size() == 1
            && d3.stash().get(0).kind() == DecorKind.CHRISTMAS_FROST;
        c.check(fall.is(ModBlocks.PUMPKIN_PILE.get()) && fall.getValue(SeasonalDecorBlock.UP) && frostKept,
            "in fall the pumpkin pile is back, up, and the frost is kept");
        // nobody breaks them
        c.check(fall.getDestroySpeed(level, spot) < 0 && !Decorations.canRemove(c.player()),
            "decorations can't be broken (only an operator in Creative can take one away)");
        if (level.getBlockEntity(spot) instanceof SeasonalDecorBlockEntity d4) d4.stash().size();
        level.setBlock(spot, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ground, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos p : new BlockPos[]{treePos, crowPos, a, b, pole}) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
    }

    // its next check, then the moment it changes
    private static void step(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SeasonalDecorBlockEntity decor)) return;
        decor.dueNow();
        SeasonalDecorBlockEntity.serverTick(level, pos, level.getBlockState(pos), decor);
    }
}
