package com.alexh.capitalism.farmer;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.occupation.Job;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

// Automated checks for the Farmer (Farmer.md), run by the game test server (gametest/GameTests,
// `gradlew runGameTestServer`): what a harvest gives a Farmer, anyone else and nobody (water, pistons...), Fertilizer
// (only Farmers, its harvests, the extra crops), the Scythes' patches, the Decomposter (its recipes, how it looks),
// Meat Slabs (who gets them, how often, which), the Butcher's Table (what a clean or botched cut gives), and meat's
// freshness (shelf lives, lean, rotting, the Fridge and Freezer).
public final class FarmerTests {

    static {
        GameTests.register("farmer_harvests", FarmerTests::harvests);
        GameTests.register("farmer_fertilizer", FarmerTests::fertilizer);
        GameTests.register("farmer_fertilized_farmland", FarmerTests::fertilizedFarmland);
        GameTests.register("farmer_scythe", FarmerTests::scythe);
        GameTests.register("farmer_diamond_scythe", FarmerTests::diamondScythe);
        GameTests.register("farmer_decomposter", FarmerTests::decomposter);
        GameTests.register("farmer_meat_slabs", FarmerTests::meatSlabs);
        GameTests.register("farmer_butchers_table", FarmerTests::butchersTable);
        GameTests.register("farmer_freshness", FarmerTests::freshness);
        GameTests.register("cook_dry_aging_and_grill", FarmerTests::agingAndGrill);
    }

    private FarmerTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    private static ServerPlayer farmer(GameTests.Checks c) {
        ServerPlayer player = c.player();
        player.getData(ModAttachments.JOBS.get()).add(Job.FARMER);
        return player;
    }

    // A ripe crop on farmland at this spot of the test box
    private static BlockPos plant(GameTests.Checks c, int x, int z, BlockState crop) {
        BlockPos at = c.helper.absolutePos(new BlockPos(x, 2, z));
        c.level().setBlockAndUpdate(at.below(), Blocks.FARMLAND.defaultBlockState());
        c.level().setBlockAndUpdate(at, crop);
        return at;
    }

    private static BlockState ripe(net.minecraft.world.level.block.Block crop) {
        return ((CropBlock) crop).getStateForAge(((CropBlock) crop).getMaxAge());
    }

    // The dropped items of this item around a spot, picked up (removed)
    private static int collect(ServerLevel level, BlockPos at, Item item) {
        int count = 0;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2.5))) {
            if (drop.getItem().is(item)) {
                count += drop.getItem().getCount();
                drop.discard();
            }
        }
        return count;
    }

    private static void clearDrops(ServerLevel level, BlockPos at) {
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(2.5))) drop.discard();
    }

    private static void harvests(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer other = c.player();
        ServerPlayer farmer = farmer(c);

        BlockPos wheat = plant(c, 2, 2, ripe(Blocks.WHEAT));
        other.gameMode.destroyBlock(wheat);
        c.check(collect(level, wheat, Items.WHEAT) == 1 && collect(level, wheat, Items.WHEAT_SEEDS) == 1, "anyone else: ripe wheat gives one wheat and one seed");
        BlockPos carrots = plant(c, 2, 2, ripe(Blocks.CARROTS));
        other.gameMode.destroyBlock(carrots);
        c.check(collect(level, carrots, Items.CARROT) == 2, "anyone else: ripe carrots give two carrots (they plant themselves)");
        BlockPos unripe = plant(c, 2, 2, Blocks.WHEAT.defaultBlockState());
        other.gameMode.destroyBlock(unripe);
        c.check(collect(level, unripe, Items.WHEAT_SEEDS) == 1 && collect(level, unripe, Items.WHEAT) == 0, "an unripe crop just gives its seed back");
        BlockPos noOne = plant(c, 2, 2, ripe(Blocks.POTATOES));
        level.destroyBlock(noOne, true);
        c.check(collect(level, noOne, Items.POTATO) == 2, "nobody (water, pistons...): two potatoes, like anyone else");
        clearDrops(level, noOne);

        // a Farmer gets the full harvest: over many, a lot more than the two carrots anyone else gets
        int total = 0;
        for (int i = 0; i < 20; i++) {
            BlockPos at = plant(c, 2, 2, ripe(Blocks.CARROTS));
            farmer.gameMode.destroyBlock(at);
            total += collect(level, at, Items.CARROT);
            clearDrops(level, at);
        }
        c.check(total > 20 * 2, "a Farmer gets the full harvest (" + total + " carrots from 20, others get 40)");

        // picking a sweet berry bush
        BlockPos bush = c.helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(bush.below(), Blocks.GRASS_BLOCK.defaultBlockState());
        level.setBlockAndUpdate(bush, Blocks.SWEET_BERRY_BUSH.defaultBlockState().setValue(SweetBerryBushBlock.AGE, 3));
        level.getBlockState(bush).useWithoutItem(level, other, new BlockHitResult(Vec3.atCenterOf(bush), Direction.UP, bush, false));
        c.check(collect(level, bush, Items.SWEET_BERRIES) == 1, "anyone else picking a ripe bush: one berry");
        level.setBlockAndUpdate(bush, Blocks.AIR.defaultBlockState());
        clearDrops(level, bush);
    }

    private static void fertilizer(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer other = c.player();
        ServerPlayer farmer = farmer(c);
        BlockPos crop = plant(c, 2, 2, ripe(Blocks.CARROTS));
        BlockPos soil = crop.below();

        c.check(!use(other, new ItemStack(ModItems.FERTILIZER.get()), crop) && FertilizedSoil.harvestsLeft(level, soil) == 0, "only a Farmer can use Fertilizer");
        ItemStack bag = new ItemStack(ModItems.FERTILIZER.get(), 3);
        c.check(use(farmer, bag, crop) && FertilizedSoil.harvestsLeft(level, soil) == FertilizerItem.HARVESTS && bag.getCount() == 2,
            "a Farmer fertilizes the farmland under a crop (2 harvests, one used)");
        c.check(!use(farmer, bag, soil) && bag.getCount() == 2, "it won't go on farmland that's already fertilized");
        other.gameMode.destroyBlock(crop);
        c.check(FertilizedSoil.harvestsLeft(level, soil) == FertilizerItem.HARVESTS && collect(level, crop, Items.CARROT) == 2,
            "anyone else's harvest doesn't use it up (and gets the small harvest)");
        level.setBlockAndUpdate(crop, ripe(Blocks.CARROTS));
        farmer.gameMode.destroyBlock(crop);
        clearDrops(level, crop);
        c.check(FertilizedSoil.harvestsLeft(level, soil) == 1, "a Farmer's harvest uses one up");
        level.setBlockAndUpdate(crop, ripe(Blocks.CARROTS));
        farmer.gameMode.destroyBlock(crop);
        clearDrops(level, crop);
        c.check(FertilizedSoil.harvestsLeft(level, soil) == 0, "after 2 harvests it's used up");
        ItemStack superBag = new ItemStack(ModItems.SUPER_FERTILIZER.get());
        c.check(use(farmer, superBag, soil) && FertilizedSoil.harvestsLeft(level, soil) == FertilizerItem.SUPER_HARVESTS, "Super Fertilizer lasts 3 harvests");

        // one fertilizer at a time, of any kind: nothing tops it up or goes with it
        c.check(!use(farmer, new ItemStack(ModItems.FERTILIZER.get()), soil) && FertilizedSoil.harvestsLeft(level, soil) == FertilizerItem.SUPER_HARVESTS,
            "Yield Fertilizer won't go on Super Yield");
        ItemStack fastBag = new ItemStack(ModItems.FAST_FERTILIZER.get(), 3);
        c.check(!use(farmer, fastBag, soil) && FertilizedSoil.fastLeft(level, soil) == 0, "Fast Fertilizer won't go with Super Yield");
        FertilizedSoil.set(level, soil, 0);
        c.check(!use(other, fastBag, soil), "only a Farmer can use Fast Fertilizer");
        c.check(use(farmer, fastBag, soil) && FertilizedSoil.fastLeft(level, soil) == FertilizerItem.FAST_HARVESTS, "Fast Fertilizer lasts 2 harvests");
        c.check(!use(farmer, fastBag, soil) && fastBag.getCount() == 2, "Fast Fertilizer won't go on twice");
        c.check(!use(farmer, new ItemStack(ModItems.SUPER_FERTILIZER.get()), soil) && FertilizedSoil.harvestsLeft(level, soil) == 0,
            "Super Yield won't go with Fast Fertilizer");
        c.check(!use(farmer, new ItemStack(ModItems.FERTILIZER.get()), soil) && FertilizedSoil.harvestsLeft(level, soil) == 0,
            "Yield Fertilizer won't go with Fast Fertilizer");
        level.setBlockAndUpdate(crop, ripe(Blocks.CARROTS));
        other.gameMode.destroyBlock(crop);
        clearDrops(level, crop);
        c.check(FertilizedSoil.fastLeft(level, soil) == 1, "anyone's harvest uses up Fast Fertilizer");

        // Fast Fertilizer: a stage of growth is followed by another 75% of the time; plain farmland never
        int fastStages = 0;
        int plainStages = 0;
        for (int i = 0; i < 40; i++) {
            level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            FertilizerItem.grow(level, crop, level.getRandom());
            fastStages += ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(crop));
        }
        FertilizedSoil.setFast(level, soil, 0);
        for (int i = 0; i < 10; i++) {
            level.setBlockAndUpdate(crop, Blocks.WHEAT.defaultBlockState());
            FertilizerItem.grow(level, crop, level.getRandom());
            plainStages += ((CropBlock) Blocks.WHEAT).getAge(level.getBlockState(crop));
        }
        c.check(fastStages >= 20 && fastStages <= 38 && plainStages == 0,
            "on Fast Fertilizer a crop grows an extra stage about 75% of the time (" + fastStages + " of 40), never without it");
        level.setBlockAndUpdate(crop, Blocks.AIR.defaultBlockState());

        // the extra crops: wheat always gives one wheat; fertilized it gives 1.5 to 1.9 (one or two)
        FertilizedSoil.set(level, soil, 0);
        int plain = 0;
        int fed = 0;
        for (int i = 0; i < 30; i++) {
            BlockPos at = plant(c, 2, 2, ripe(Blocks.WHEAT));
            farmer.gameMode.destroyBlock(at);
            plain += collect(level, at, Items.WHEAT);
            clearDrops(level, at);
            at = plant(c, 2, 2, ripe(Blocks.WHEAT));
            FertilizedSoil.set(level, at.below(), FertilizerItem.HARVESTS);
            farmer.gameMode.destroyBlock(at);
            fed += collect(level, at, Items.WHEAT);
            clearDrops(level, at);
            FertilizedSoil.set(level, at.below(), 0);
        }
        c.check(plain == 30 && fed >= 30 * 1.3 && fed <= 30 * 2, "fertilized farmland gives 50% to 90% more (" + fed + " wheat from 30, " + plain + " without)");
        FertilizedSoil.set(level, soil, 0);
    }

    // Right-click this spot with the item, the way the game does; true if it was used
    private static boolean use(ServerPlayer player, ItemStack stack, BlockPos at) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false));
        return stack.useOn(context).consumesAction();
    }

    private static void scythe(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer farmer = farmer(c);
        // a 5x4 field; the player faces north (-z) and cuts the middle of the south row
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 3; z++) plant(c, x, z, ripe(Blocks.WHEAT));
        }
        BlockPos unripe = plant(c, 3, 2, Blocks.WHEAT.defaultBlockState());
        farmer.snapTo(c.at(2.5, 2, 4.5).x, c.at(2.5, 2, 4.5).y, c.at(2.5, 2, 4.5).z, 180.0F, 0.0F);
        c.check(farmer.getDirection() == Direction.NORTH, "the player faces north");
        ItemStack scythe = new ItemStack(ModItems.SCYTHE.get());
        farmer.setItemInHand(InteractionHand.MAIN_HAND, scythe);
        farmer.gameMode.destroyBlock(c.helper.absolutePos(new BlockPos(2, 2, 3)));
        int cut = 0;
        for (int x = 0; x <= 4; x++) {
            for (int z = 1; z <= 3; z++) {
                if (level.getBlockState(c.helper.absolutePos(new BlockPos(x, 2, z))).isAir()) cut++;
            }
        }
        c.check(cut == 14 && level.getBlockState(unripe).is(Blocks.WHEAT), "it cuts the ripe crops 5 wide and 3 deep (" + cut + " of 14), leaving the unripe one");
        boolean beyond = true;
        for (int x = 0; x <= 4; x++) beyond &= !level.getBlockState(c.helper.absolutePos(new BlockPos(x, 2, 0))).isAir();
        c.check(beyond, "the row beyond the patch is left standing");
        c.check(scythe.getDamageValue() == 1, "one swing wears it by one");
        c.check(collect(level, c.helper.absolutePos(new BlockPos(2, 2, 2)), Items.WHEAT) == 14, "every crop it cut gives a Farmer's harvest");
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                BlockPos at = c.helper.absolutePos(new BlockPos(x, 2, z));
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(at.below(), Blocks.AIR.defaultBlockState());
            }
        }
        clearDrops(level, c.helper.absolutePos(new BlockPos(2, 2, 2)));
    }

    private static void diamondScythe(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer farmer = farmer(c);
        // a 5x4 field; the player faces north (-z) and cuts the middle of the south row
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 3; z++) plant(c, x, z, ripe(Blocks.WHEAT));
        }
        farmer.snapTo(c.at(2.5, 2, 4.5).x, c.at(2.5, 2, 4.5).y, c.at(2.5, 2, 4.5).z, 180.0F, 0.0F);
        ItemStack scythe = new ItemStack(ModItems.DIAMOND_SCYTHE.get());
        farmer.setItemInHand(InteractionHand.MAIN_HAND, scythe);
        farmer.gameMode.destroyBlock(c.helper.absolutePos(new BlockPos(2, 2, 3)));
        int cut = 0;
        for (int x = 0; x <= 4; x++) {
            for (int z = 1; z <= 3; z++) {
                if (level.getBlockState(c.helper.absolutePos(new BlockPos(x, 2, z))).isAir()) cut++;
            }
        }
        c.check(cut == 15, "the Diamond Scythe cuts 5 wide and 3 deep (" + cut + " of 15)");
        boolean beyond = true;
        for (int x = 0; x <= 4; x++) beyond &= !level.getBlockState(c.helper.absolutePos(new BlockPos(x, 2, 0))).isAir();
        c.check(beyond, "the row beyond its patch is left standing");
        c.check(scythe.getDamageValue() == 1 && scythe.getMaxDamage() == ScytheItem.DIAMOND_USES, "one swing wears it by one, of 1600");
        for (int x = 0; x <= 4; x++) {
            for (int z = 0; z <= 4; z++) {
                BlockPos at = c.helper.absolutePos(new BlockPos(x, 2, z));
                level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(at.below(), Blocks.AIR.defaultBlockState());
            }
        }
        clearDrops(level, c.helper.absolutePos(new BlockPos(2, 2, 2)));
    }

    private static void fertilizedFarmland(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer farmer = farmer(c);
        BlockPos crop = plant(c, 2, 2, Blocks.WHEAT.defaultBlockState());
        BlockPos soil = crop.below();
        level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState().setValue(FarmlandBlock.MOISTURE, 7));
        use(farmer, new ItemStack(ModItems.FERTILIZER.get()), crop);
        BlockState state = level.getBlockState(soil);
        c.check(state.is(ModBlocks.FERTILIZED_FARMLAND.get()) && state.getValue(FertilizedFarmlandBlock.FERTILIZER) == FertilizedFarmlandBlock.Look.YIELD
            && state.getValue(FarmlandBlock.MOISTURE) == 7, "fertilized farmland shows it (Yield), and stays wet");
        c.check(level.getBlockState(crop).is(Blocks.WHEAT), "the crop on it stays");
        FertilizedSoil.set(level, soil, 0);
        c.check(level.getBlockState(soil).is(Blocks.FARMLAND) && level.getBlockState(soil).getValue(FarmlandBlock.MOISTURE) == 7, "used up: plain (wet) farmland again");
        use(farmer, new ItemStack(ModItems.FAST_FERTILIZER.get()), crop);
        c.check(level.getBlockState(soil).getValue(FertilizedFarmlandBlock.FERTILIZER) == FertilizedFarmlandBlock.Look.FAST, "Fast Fertilizer shows yellow and blue");
        FertilizedSoil.setFast(level, soil, 0);
        FertilizedSoil.set(level, soil, FertilizerItem.SUPER_HARVESTS, true);
        c.check(level.getBlockState(soil).getValue(FertilizedFarmlandBlock.FERTILIZER) == FertilizedFarmlandBlock.Look.SUPER_YIELD, "Super Yield shows silver");
        // trampled: dirt, and hoed again it's plain farmland with nothing on it
        level.setBlockAndUpdate(crop, Blocks.AIR.defaultBlockState());
        ((FarmlandBlock) level.getBlockState(soil).getBlock()).turnToBaseBlock(farmer, level.getBlockState(soil), level, soil);
        c.check(level.getBlockState(soil).is(Blocks.DIRT), "trampled, it turns to dirt");
        level.setBlockAndUpdate(soil, Blocks.FARMLAND.defaultBlockState());
        c.check(FertilizedSoil.harvestsLeft(level, soil) == 0 && level.getBlockState(soil).is(Blocks.FARMLAND), "hoed again, its fertilizer is gone");
        use(farmer, new ItemStack(ModItems.FAST_FERTILIZER.get()), soil);
        c.check(FertilizedSoil.harvestsLeft(level, soil) == 0 && FertilizedSoil.fastLeft(level, soil) == FertilizerItem.FAST_HARVESTS,
            "nothing old comes back when it's fertilized again");
        level.setBlockAndUpdate(soil, Blocks.AIR.defaultBlockState());
    }

    private static net.minecraft.world.entity.LivingEntity mob(GameTests.Checks c, net.minecraft.world.entity.EntityType<?> type) {
        net.minecraft.world.entity.LivingEntity mob = (net.minecraft.world.entity.LivingEntity) type.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        Vec3 at = c.helper.absoluteVec(new Vec3(2.5, 2, 2.5));
        mob.snapTo(at.x, at.y, at.z, 0, 0);
        return mob;
    }

    // How many of these kills drop a slab, and how many of those are this cut
    private static int[] slabs(GameTests.Checks c, net.minecraft.world.entity.player.Player killer, net.minecraft.world.entity.LivingEntity mob, int tries, MeatCut which) {
        int slabs = 0;
        int those = 0;
        for (int i = 0; i < tries; i++) {
            ItemStack slab = MeatCut.slabDrop(killer, mob, c.level().getRandom());
            if (slab == null) continue;
            slabs++;
            if (slab.is(which.slab())) those++;
        }
        return new int[]{slabs, those};
    }

    private static void meatSlabs(GameTests.Checks c) {
        ServerPlayer farmer = farmer(c);
        ServerPlayer other = c.player();
        var cow = mob(c, net.minecraft.world.entity.EntityTypes.COW);
        int tries = 6000;
        farmer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        other.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        int[] got = slabs(c, farmer, cow, tries, MeatCut.BEEF_RIB);
        c.near(got[0] / (double) tries, MeatCut.SLAB_CHANCE, 0.025, "a Farmer's diamond sword: share of cows dropping a slab");
        c.near(got[1] / (double) got[0], 0.10, 0.03, "a tenth of beef slabs are Beef Ribs");
        c.check(slabs(c, other, cow, 400, MeatCut.BEEF_RIB)[0] == 0, "no slabs for anyone else");
        // a Bone in the sword's Molten Socket: +15% (30% in all)
        ItemStack boneSword = new ItemStack(Items.DIAMOND_SWORD);
        boneSword.set(com.alexh.capitalism.core.ModComponents.SOCKET.get(), com.alexh.capitalism.weaponsmith.Socketing.Gem.BONE.id);
        farmer.setItemInHand(InteractionHand.MAIN_HAND, boneSword);
        c.near(slabs(c, farmer, cow, tries, MeatCut.BEEF_RIB)[0] / (double) tries, MeatCut.SLAB_CHANCE + MeatCut.BONE_CHANCE, 0.03,
            "a Bone-socketed sword: share of cows dropping a slab");
        c.check(com.alexh.capitalism.weaponsmith.Socketing.Gem.BONE.fits(new ItemStack(Items.DIAMOND_SWORD))
            && !com.alexh.capitalism.weaponsmith.Socketing.Gem.BONE.fits(new ItemStack(Items.DIAMOND_PICKAXE)), "a Bone only fits in a sword");
        farmer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        c.check(slabs(c, farmer, cow, 400, MeatCut.BEEF_RIB)[0] == 0, "no slabs with an iron sword");
        farmer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.NETHERITE_AXE));
        c.check(slabs(c, farmer, cow, 400, MeatCut.BEEF_RIB)[0] > 30, "a netherite axe works too");
        ((net.minecraft.world.entity.AgeableMob) cow).setAge(-24000);
        c.check(slabs(c, farmer, cow, 400, MeatCut.BEEF_RIB)[0] == 0, "no slabs from a calf");
        c.check(slabs(c, farmer, mob(c, net.minecraft.world.entity.EntityTypes.ZOMBIE), 400, MeatCut.BEEF_RIB)[0] == 0
            && slabs(c, farmer, mob(c, net.minecraft.world.entity.EntityTypes.GOAT), 400, MeatCut.BEEF_RIB)[0] == 0, "no slabs from a zombie or a goat");
        boolean pork = true;
        var hoglin = mob(c, net.minecraft.world.entity.EntityTypes.HOGLIN);
        for (int i = 0; i < 400; i++) {
            ItemStack slab = MeatCut.slabDrop(farmer, hoglin, c.level().getRandom());
            if (slab != null && MeatCut.ofSlab(slab).animal != MeatCut.Animal.PIG) pork = false;
        }
        c.check(pork, "a hoglin drops pork slabs");
        // Luck: more slabs, and more of the rare ones
        farmer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(2);
        got = slabs(c, farmer, mob(c, net.minecraft.world.entity.EntityTypes.COW), tries, MeatCut.BEEF_RIB);
        c.near(got[0] / (double) tries, MeatCut.chance(2), 0.025, "2 Luck: share of cows dropping a slab");
        c.near(got[1] / (double) got[0], 20 / 120.0, 0.03, "2 Luck: more of them are Beef Ribs");
        farmer.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.LUCK).setBaseValue(0);
        ItemStack rib = new ItemStack(MeatCut.BEEF_RIB.cut());
        c.check(rib.has(net.minecraft.core.component.DataComponents.FOOD)
            && rib.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM,
                net.minecraft.resources.Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, "raw_meat_or_fish"))),
            "a fine cut is raw meat (food, and goes in Training Feed)");
    }

    private static void butchersTable(GameTests.Checks c) {
        ServerLevel level = c.level();
        ServerPlayer farmer = farmer(c);
        BlockPos at = c.helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(at, ModBlocks.BUTCHERS_TABLE.get().defaultBlockState());
        ButchersTableBlockEntity table = (ButchersTableBlockEntity) level.getBlockEntity(at);
        ButchersTableMenu menu = new ButchersTableMenu(0, farmer.getInventory(), table.knife(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, at));
        c.check(!menu.getSlot(0).mayPlace(new ItemStack(Items.BEEF)), "only Meat Slabs go in");
        c.check(!menu.clickMenuButton(farmer, ButchersTableMenu.SUCCESS), "nothing to finish before it's started");
        long now = level.getGameTime();
        // slabs don't stack: one at a time, all 100% lean
        java.util.function.Supplier<ItemStack> rib = () -> {
            ItemStack slab = Freshness.newSlab(MeatCut.BEEF_RIB, now, level.getRandom());
            slab.set(com.alexh.capitalism.core.ModComponents.LEAN.get(), 100);
            return slab;
        };
        c.check(new ItemStack(MeatCut.BEEF_RIB.slab()).getMaxStackSize() == 1, "slabs don't stack");
        menu.getSlot(0).set(rib.get());
        c.check(!menu.clickMenuButton(farmer, ButchersTableMenu.CUT) && menu.slab().getCount() == 1, "no cutting without a Butcher Knife");
        c.check(!menu.getSlot(1).mayPlace(new ItemStack(Items.IRON_SWORD)), "only Butcher Knives go in the knife slot");
        ItemStack knife = new ItemStack(ModItems.IRON_BUTCHER_KNIFE.get());
        knife.set(com.alexh.capitalism.core.ModComponents.KNIFE_CUTS.get(), ButcherKnifeItem.IRON_CUTS - 4);
        menu.getSlot(1).set(knife);
        menu.clickMenuButton(farmer, ButchersTableMenu.CUT);
        c.check(menu.slab().isEmpty() && menu.cutting() == MeatCut.BEEF_RIB, "Start Cutting takes the slab");
        menu.getSlot(0).set(rib.get());
        c.check(ButcherKnifeItem.cutsLeft(menu.knife()) == 3, "and one cut of the knife's sharpness");
        menu.removed(farmer);
        menu = new ButchersTableMenu(0, farmer.getInventory(), table.knife(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, at));
        c.check(ButcherKnifeItem.cutsLeft(menu.knife()) == 3 && farmer.getInventory().countItem(ModItems.IRON_BUTCHER_KNIFE.get()) == 0,
            "the knife stays in the table when you walk away");
        c.check(farmer.getInventory().countItem(Items.BEEF) == MeatCut.BEEF_RIB.yield, "(walking off mid-cut: plain meat)");
        menu.getSlot(0).set(rib.get());
        menu.clickMenuButton(farmer, ButchersTableMenu.CUT);
        menu.getSlot(0).set(rib.get());
        c.check(!menu.clickMenuButton(farmer, ButchersTableMenu.CUT), "one slab at a time");
        menu.clickMenuButton(farmer, ButchersTableMenu.SUCCESS);
        c.check(farmer.getInventory().countItem(MeatCut.BEEF_RIB.cut()) == MeatCut.BEEF_RIB.yield && menu.cutting() == null,
            "a clean cut gives 5 Raw Beef Rib (a 100% lean slab)");
        int pieces = 0;
        for (ItemStack stack : farmer.getInventory().getNonEquipmentItems()) if (stack.is(MeatCut.BEEF_RIB.cut())) pieces++;
        c.check(pieces == MeatCut.BEEF_RIB.yield && new ItemStack(MeatCut.BEEF_RIB.cut()).getMaxStackSize() == 1, "cuts don't stack: 5 pieces, each on its own");
        long cutRots = -1;
        for (ItemStack stack : farmer.getInventory().getNonEquipmentItems()) {
            if (stack.is(MeatCut.BEEF_RIB.cut())) cutRots = Freshness.rotsAt(stack);
        }
        c.check(cutRots == Freshness.cutRotsAt(now, Freshness.shelfLife(MeatCut.BEEF_RIB)),
            "the cuts get the slab's time left plus a quarter (" + cutRots + ")");
        menu.clickMenuButton(farmer, ButchersTableMenu.CUT);
        menu.getSlot(0).set(rib.get());
        menu.clickMenuButton(farmer, ButchersTableMenu.FAILED);
        c.check(farmer.getInventory().countItem(Items.BEEF) == 2 * MeatCut.BEEF_RIB.yield, "a botched cut gives 5 plain beef");
        menu.clickMenuButton(farmer, ButchersTableMenu.CUT);
        c.check(menu.knife().isEmpty(), "the knife's last cut spends it (it breaks)");
        menu.removed(farmer);
        c.check(farmer.getInventory().countItem(Items.BEEF) == 3 * MeatCut.BEEF_RIB.yield
            && farmer.getInventory().countItem(MeatCut.BEEF_RIB.cut()) == MeatCut.BEEF_RIB.yield, "walking off mid-cut counts as botched");
        ItemStack diamond = new ItemStack(ModItems.DIAMOND_BUTCHER_KNIFE.get());
        ButcherKnifeItem.useCut(diamond);
        c.check(ButcherKnifeItem.cutsLeft(diamond) == ButcherKnifeItem.DIAMOND_CUTS - 1, "a Diamond Butcher Knife has 10 cuts");
        ButcherKnifeItem.sharpen(diamond);
        c.check(ButcherKnifeItem.cutsLeft(diamond) == ButcherKnifeItem.DIAMOND_CUTS, "sharpening puts it back to full");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
    }

    private static void freshness(GameTests.Checks c) {
        ServerLevel level = c.level();
        long now = level.getGameTime();
        c.check(Freshness.shelfLife(MeatCut.PORK_SHOULDER) == 15 * Freshness.MINUTE && Freshness.shelfLife(MeatCut.PORK_HAM) == 20 * Freshness.MINUTE
            && Freshness.shelfLife(MeatCut.BEEF_RIB) == 25 * Freshness.MINUTE && Freshness.shelfLife(MeatCut.CHICKEN_BREAST) == 10 * Freshness.MINUTE,
            "shelf lives: 15 / 20 / 25 minutes by rarity, chicken 10");
        boolean leanOk = true;
        for (int i = 0; i < 200; i++) {
            int lean = Freshness.lean(Freshness.newSlab(MeatCut.BEEF_CHUCK, now, level.getRandom()));
            leanOk &= lean >= 60 && lean <= 100;
        }
        c.check(leanOk, "a slab rolls 60-100% lean");
        ItemStack chuck = Freshness.newSlab(MeatCut.BEEF_CHUCK, now, level.getRandom());
        chuck.set(com.alexh.capitalism.core.ModComponents.LEAN.get(), 85);
        int y85 = Freshness.yield(MeatCut.BEEF_CHUCK, chuck);
        chuck.set(com.alexh.capitalism.core.ModComponents.LEAN.get(), 84);
        int y84 = Freshness.yield(MeatCut.BEEF_CHUCK, chuck);
        chuck.set(com.alexh.capitalism.core.ModComponents.LEAN.get(), 69);
        int y69 = Freshness.yield(MeatCut.BEEF_CHUCK, chuck);
        c.check(y85 == 3 && y84 == 2 && y69 == 1, "lean: 85% gives it all, under 85% one less, under 70% two less (" + y85 + ", " + y84 + ", " + y69 + ")");
        c.check(Freshness.cutRotsAt(now, 1000) == now + 1250, "cuts get a quarter more time");

        // rotting
        ItemStack old = Freshness.newSlab(MeatCut.BEEF_RIB, now, level.getRandom());
        old.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), Math.max(0, now - 1));
        ItemStack rotted = Freshness.update(old, now, level.getRandom());
        c.check(rotted != null && rotted.is(ModItems.ROTTEN_MEAT.get()) && rotted.getCount() == MeatCut.BEEF_RIB.yield, "a slab past its time is Rotten Meat (as much as its cuts)");
        ItemStack cuts = new ItemStack(MeatCut.PORK_LOIN.cut(), 4);
        cuts.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), Math.max(0, now - 1));
        ItemStack rottedCuts = Freshness.update(cuts, now, level.getRandom());
        c.check(rottedCuts != null && rottedCuts.is(ModItems.ROTTEN_MEAT.get()) && rottedCuts.getCount() == 4, "4 cuts past their time are 4 Rotten Meat");
        ItemStack fresh = Freshness.newSlab(MeatCut.BEEF_RIB, now, level.getRandom());
        c.check(Freshness.update(fresh, now, level.getRandom()) == null, "a fresh slab stays as it is");
        ItemStack undated = new ItemStack(MeatCut.MUTTON_RACK.cut(), 2);
        Freshness.update(undated, now, level.getRandom());
        c.check(Freshness.rotsAt(undated) == now + Math.round(Freshness.shelfLife(MeatCut.MUTTON_RACK) * Freshness.CUT_BONUS), "meat with no timer gets one");
        c.check(Freshness.update(new ItemStack(Items.BEEF), now, level.getRandom()) == null, "plain meat doesn't rot");

        // the Fridge: 2x while it burns coal
        BlockPos at = c.helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(at, ModBlocks.FRIDGE.get().defaultBlockState());
        ColdStorageBlockEntity fridge = (ColdStorageBlockEntity) level.getBlockEntity(at);
        ItemStack kept = Freshness.newSlab(MeatCut.BEEF_RIB, now, level.getRandom());
        kept.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 1000);
        fridge.setItem(0, kept);
        fridge.setItem(ColdStorageBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        fridge.update(now, level);
        fridge.update(now + 600, level);
        c.check(Freshness.rotsAt(fridge.getItem(0)) == now + 1300, "a running Fridge: 600 ticks only take 300 off (" + (Freshness.rotsAt(fridge.getItem(0)) - now) + " left of 1000)");
        c.check(fridge.getItem(ColdStorageBlockEntity.FUEL).getCount() == 1 && fridge.burnLeft() == ColdStorageBlockEntity.FRIDGE_FUEL - 600, "it burns one coal at a time");
        Chill chill = fridge.getItem(0).get(com.alexh.capitalism.core.ModComponents.CHILL.get());
        c.check(chill != null && chill.slower() == 2 && chill.leftAtNormalRate(now + 600) == 700 && chill.leftAtNormalRate(now + 800) == 600,
            "meat in a running Fridge shows it's chilled: its time left goes down half as fast");
        c.check(!fridge.canPlaceItem(ColdStorageBlockEntity.FUEL, new ItemStack(Items.ICE)), "a Fridge doesn't take ice");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());

        // the Freezer: 6x on snowballs (short) or ice (long); without fuel meat rots as normal
        level.setBlockAndUpdate(at, ModBlocks.FREEZER.get().defaultBlockState());
        ColdStorageBlockEntity freezer = (ColdStorageBlockEntity) level.getBlockEntity(at);
        ItemStack frozen = new ItemStack(MeatCut.BEEF_ROUND.cut(), 1);
        frozen.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 1000);
        freezer.setItem(0, frozen);
        freezer.setItem(ColdStorageBlockEntity.FUEL, new ItemStack(Items.SNOWBALL, 1));
        freezer.update(now, level);
        freezer.update(now + 600, level);
        c.check(Freshness.rotsAt(freezer.getItem(0)) == now + 1500, "a running Freezer: 600 ticks only take 100 off (" + (Freshness.rotsAt(freezer.getItem(0)) - now) + " left of 1000)");
        c.check(ColdStorageBlockEntity.Kind.FREEZER.fuel(new ItemStack(Items.ICE)) == 10 * ColdStorageBlockEntity.Kind.FREEZER.fuel(new ItemStack(Items.SNOWBALL)),
            "an Ice block runs it 10 times as long as a Snowball");
        freezer.update(now + 2200, level);
        c.check(freezer.getItem(0).is(ModItems.ROTTEN_MEAT.get()) && freezer.getItem(0).getCount() == 1, "out of fuel, meat rots in it as normal");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());

        // Rotten Meat stacks to 64, whatever it rotted from
        ItemStack rottenSlab = Freshness.rotted(Freshness.newSlab(MeatCut.BEEF_CHUCK, now, level.getRandom()));
        ItemStack rottenCuts = Freshness.rotted(new ItemStack(MeatCut.PORK_LOIN.cut(), 2));
        c.check(rottenSlab.getMaxStackSize() == 64 && ItemStack.isSameItemSameComponents(rottenSlab, rottenCuts), "Rotten Meat stacks to 64, from slabs and cuts alike");

        // a slab left in the Butcher's Table rots there too
        BlockPos tableAt = c.helper.absolutePos(new BlockPos(1, 2, 1));
        level.setBlockAndUpdate(tableAt, ModBlocks.BUTCHERS_TABLE.get().defaultBlockState());
        ServerPlayer cook = c.player();
        ButchersTableMenu tableMenu = new ButchersTableMenu(0, cook.getInventory(),
            ((ButchersTableBlockEntity) level.getBlockEntity(tableAt)).knife(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, tableAt));
        ItemStack leftIn = Freshness.newSlab(MeatCut.BEEF_ROUND, now, level.getRandom());
        leftIn.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), Math.max(0, now - 1));
        tableMenu.getSlot(0).set(leftIn);
        net.minecraft.world.inventory.AbstractContainerMenu before = cook.containerMenu;
        cook.containerMenu = tableMenu;
        FreshnessEvents.update(cook);
        cook.containerMenu = before;
        c.check(tableMenu.getSlot(0).getItem().is(ModItems.ROTTEN_MEAT.get()), "a slab left in the Butcher's Table rots there");
        level.setBlockAndUpdate(tableAt, Blocks.AIR.defaultBlockState());

        // ...and in an item frame
        net.minecraft.world.entity.decoration.ItemFrame frame = new net.minecraft.world.entity.decoration.ItemFrame(level, tableAt, net.minecraft.core.Direction.UP);
        ItemStack framed = new ItemStack(MeatCut.BEEF_RIB.cut(), 1);
        framed.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), Math.max(0, now - 1));
        frame.setItem(framed, false);
        frame.tickCount = 20;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(frame));
        c.check(frame.getItem().is(ModItems.ROTTEN_MEAT.get()), "meat in an item frame rots too");

        // a placed slab on ice rots like in a Freezer; off the ice it carries on as normal
        BlockPos ice = c.helper.absolutePos(new BlockPos(3, 1, 3));
        BlockPos placed = ice.above();
        level.setBlockAndUpdate(ice, Blocks.PACKED_ICE.defaultBlockState());
        level.setBlockAndUpdate(placed, ModBlocks.MEAT_SLABS.get(MeatCut.PORK_LOIN).get().defaultBlockState());
        MeatSlabBlockEntity slabEntity = (MeatSlabBlockEntity) level.getBlockEntity(placed);
        ItemStack loin = Freshness.newSlab(MeatCut.PORK_LOIN, now, level.getRandom());
        loin.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 1000);
        slabEntity.applyComponentsFromItemStack(loin);
        slabEntity.update(level, placed, level.getBlockState(placed), now);
        c.check(slabEntity.onIce() && slabEntity.left(now + 600) == 900, "a slab on ice: 600 ticks only take 100 off (" + slabEntity.left(now + 600) + " left of 1000)");
        level.setBlockAndUpdate(ice, Blocks.STONE.defaultBlockState());
        slabEntity.update(level, placed, level.getBlockState(placed), now + 600);
        c.check(!slabEntity.onIce() && slabEntity.left(now + 700) == 800, "off the ice it rots as normal again (" + slabEntity.left(now + 700) + ")");
        slabEntity.update(level, placed, level.getBlockState(placed), now + 2000);
        c.check(level.getBlockState(placed).getValue(MeatSlabBlock.ROTTEN), "a placed slab goes rotten when its time is up");
        level.setBlockAndUpdate(placed, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(ice, Blocks.AIR.defaultBlockState());

        c.check(DecomposterBlockEntity.find(java.util.List.of(new ItemStack(Items.BONE_MEAL), new ItemStack(ModItems.ROTTEN_MEAT.get()), new ItemStack(Items.DIRT)))
            == DecomposterBlockEntity.Recipe.FERTILIZER, "Rotten Meat does for Rotten Flesh in the Decomposter");
    }

    // Dry-aging in the Fridge (not the Freezer), and the Grill: its heat, and what a sear makes
    private static void agingAndGrill(GameTests.Checks c) {
        ServerLevel level = c.level();
        long now = level.getGameTime();
        BlockPos at = c.helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(at, ModBlocks.FRIDGE.get().defaultBlockState());
        ColdStorageBlockEntity fridge = (ColdStorageBlockEntity) level.getBlockEntity(at);
        ItemStack rib = new ItemStack(MeatCut.BEEF_RIB.cut());
        rib.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 60 * Freshness.MINUTE);
        fridge.setItem(0, rib);
        fridge.setItem(ColdStorageBlockEntity.FUEL, new ItemStack(Items.COAL, 4));
        fridge.update(now, level);
        fridge.update(now + Freshness.DRY_AGED / 2, level);
        c.check(Freshness.agedLevel(fridge.getItem(0)) == 0 && Freshness.aging(fridge.getItem(0)) == Freshness.DRY_AGED / 2, "half way to Dry-Aged");
        fridge.update(now + Freshness.DRY_AGED, level);
        c.check(Freshness.agedLevel(fridge.getItem(0)) == 1, "a cut in a running Fridge is Dry-Aged in time");
        fridge.update(now + Freshness.PRIME_AGED, level);
        c.check(Freshness.agedLevel(fridge.getItem(0)) == 2, "...and Prime Dry-Aged after longer");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(at, ModBlocks.FREEZER.get().defaultBlockState());
        ColdStorageBlockEntity freezer = (ColdStorageBlockEntity) level.getBlockEntity(at);
        ItemStack frozen = new ItemStack(MeatCut.BEEF_RIB.cut());
        frozen.set(com.alexh.capitalism.core.ModComponents.ROTS_AT.get(), now + 60 * Freshness.MINUTE);
        freezer.setItem(0, frozen);
        freezer.setItem(ColdStorageBlockEntity.FUEL, new ItemStack(Items.ICE, 4));
        freezer.update(now, level);
        freezer.update(now + Freshness.DRY_AGED, level);
        c.check(Freshness.aging(freezer.getItem(0)) == 0, "a Freezer doesn't dry-age");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());

        // the Grill's heat: a coal is a few sears
        level.setBlockAndUpdate(at, ModBlocks.GRILL.get().defaultBlockState());
        GrillBlockEntity grill = (GrillBlockEntity) level.getBlockEntity(at);
        c.check(!grill.hasHeat(), "a cold Grill has no heat");
        grill.setItem(GrillBlockEntity.FUEL, new ItemStack(Items.COAL, 1));
        boolean all = true;
        for (int i = 0; i < GrillBlockEntity.COAL_SEARS; i++) all &= grill.useHeat();
        c.check(all && !grill.hasHeat() && grill.getItem(GrillBlockEntity.FUEL).isEmpty(), "one coal is " + GrillBlockEntity.COAL_SEARS + " sears");
        c.check(!grill.canPlaceItem(GrillBlockEntity.CUT, new ItemStack(Items.BEEF)) && grill.canPlaceItem(GrillBlockEntity.CUT, new ItemStack(MeatCut.BEEF_RIB.cut())),
            "only fine cuts go on the Grill");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());

        // up to three steaks at once: Sear! puts every cut on (heat allowing), each comes off on its own
        level.setBlockAndUpdate(at, ModBlocks.GRILL.get().defaultBlockState());
        GrillBlockEntity busy = (GrillBlockEntity) level.getBlockEntity(at);
        ServerPlayer cook = c.player();
        GrillMenu grillMenu = new GrillMenu(0, cook.getInventory(), busy, new net.minecraft.world.inventory.SimpleContainerData(1),
            net.minecraft.world.inventory.ContainerLevelAccess.create(level, at));
        grillMenu.getSlot(0).set(new ItemStack(MeatCut.BEEF_RIB.cut()));
        grillMenu.getSlot(2).set(new ItemStack(MeatCut.PORK_LOIN.cut()));
        busy.setItem(GrillBlockEntity.FUEL, new ItemStack(Items.COAL, 1));
        grillMenu.clickMenuButton(cook, GrillMenu.SEAR);
        c.check(grillMenu.cut(0).isEmpty() && grillMenu.cut(2).isEmpty() && busy.charges() == GrillBlockEntity.COAL_SEARS - 2,
            "Sear! puts both cuts on the grill, one sear of heat each");
        grillMenu.clickMenuButton(cook, GrillMenu.RESULT + 0 * 256 + 135);
        grillMenu.clickMenuButton(cook, GrillMenu.RESULT + 2 * 256 + 165);
        boolean ribMR = false;
        boolean loinWD = false;
        for (ItemStack stack : cook.getInventory().getNonEquipmentItems()) {
            Sear got = stack.get(com.alexh.capitalism.core.ModComponents.SEAR.get());
            if (got == null) continue;
            ribMR |= stack.is(ModItems.SEARED_CUTS.get(MeatCut.BEEF_RIB).get()) && got.done() == Sear.Doneness.MEDIUM_RARE;
            loinWD |= stack.is(ModItems.SEARED_CUTS.get(MeatCut.PORK_LOIN).get()) && got.done() == Sear.Doneness.WELL_DONE;
        }
        c.check(ribMR && loinWD, "each steak comes off at its own doneness");
        grillMenu.getSlot(1).set(new ItemStack(MeatCut.BEEF_ROUND.cut()));
        grillMenu.clickMenuButton(cook, GrillMenu.SEAR);
        int burntBefore = cook.getInventory().countItem(ModItems.BURNT_FOOD.get());
        grillMenu.removed(cook);
        c.check(cook.getInventory().countItem(ModItems.BURNT_FOOD.get()) == burntBefore + 1, "walking away burns what's still on");
        level.setBlockAndUpdate(at, Blocks.AIR.defaultBlockState());

        // what a sear makes
        ItemStack perfect = GrillMenu.seared(MeatCut.BEEF_RIB, 135, 0);
        Sear sear = perfect.get(com.alexh.capitalism.core.ModComponents.SEAR.get());
        c.check(perfect.is(ModItems.SEARED_CUTS.get(MeatCut.BEEF_RIB).get()) && sear != null && sear.done() == Sear.Doneness.MEDIUM_RARE
            && sear.perfect(MeatCut.BEEF_RIB), "pulled at 135°F: Medium Rare, perfectly cooked");
        c.check(Sear.Doneness.of(125) == Sear.Doneness.RARE && Sear.Doneness.of(145) == Sear.Doneness.MEDIUM && Sear.Doneness.of(155) == Sear.Doneness.MEDIUM_WELL
            && Sear.Doneness.of(170) == Sear.Doneness.WELL_DONE, "the doneness bands");
        ItemStack wellDone = GrillMenu.seared(MeatCut.BEEF_RIB, 165, 0);
        ItemStack aged = GrillMenu.seared(MeatCut.BEEF_RIB, 135, 2);
        int nPerfect = perfect.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();
        int nWell = wellDone.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();
        int nAged = aged.get(net.minecraft.core.component.DataComponents.FOOD).nutrition();
        c.check(nPerfect > nWell && nAged > nPerfect, "Medium Rare fills more than Well Done, Prime aging more still (" + nWell + " / " + nPerfect + " / " + nAged + ")");
        c.check(!aged.get(net.minecraft.core.component.DataComponents.CONSUMABLE).onConsumeEffects().isEmpty() && aged.get(com.alexh.capitalism.core.ModComponents.SEAR.get()).strengthens(MeatCut.BEEF_RIB)
            && perfect.get(net.minecraft.core.component.DataComponents.CONSUMABLE).onConsumeEffects().isEmpty(),
            "a Prime Dry-Aged steak at Medium Rare gives Strength; an ordinary one doesn't");
        c.check(com.alexh.capitalism.block.KitchenRecipes.isGourmet(perfect), "a seared steak is Gourmet (gold drumsticks)");
        c.check(GrillMenu.seared(MeatCut.BEEF_RIB, 180, 0).is(ModItems.BURNT_FOOD.get()), "past Well Done it's Burnt Food");
        ItemStack pinkChicken = GrillMenu.seared(MeatCut.CHICKEN_BREAST, 140, 0);
        c.check(pinkChicken.get(com.alexh.capitalism.core.ModComponents.SEAR.get()).unsafe(MeatCut.CHICKEN_BREAST)
            && !pinkChicken.get(net.minecraft.core.component.DataComponents.CONSUMABLE).onConsumeEffects().isEmpty(), "chicken that isn't Well Done is undercooked (food poisoning)");
        ItemStack doneChicken = GrillMenu.seared(MeatCut.CHICKEN_BREAST, 165, 0);
        c.check(!doneChicken.get(com.alexh.capitalism.core.ModComponents.SEAR.get()).unsafe(MeatCut.CHICKEN_BREAST), "Well Done chicken is safe");
    }

    private static void decomposter(GameTests.Checks c) {
        ServerLevel level = c.level();
        BlockPos at = c.helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlockAndUpdate(at, ModBlocks.DECOMPOSTER.get().defaultBlockState());
        DecomposterBlockEntity decomposter = (DecomposterBlockEntity) level.getBlockEntity(at);
        decomposter.setItem(0, new ItemStack(Items.DIRT, 2));
        decomposter.setItem(1, new ItemStack(Items.BONE_MEAL, 2));
        decomposter.setItem(2, new ItemStack(Items.ROTTEN_FLESH, 2));
        c.check(!level.getBlockState(at).getValue(DecomposterBlock.FULL), "it stands empty");
        for (int i = 0; i < DecomposterBlockEntity.TICKS - 1; i++) DecomposterBlockEntity.serverTick(level, at, level.getBlockState(at), decomposter);
        c.check(level.getBlockState(at).getValue(DecomposterBlock.FULL) && level.getBlockState(at).getValue(DecomposterBlock.WORKING), "with ingredients in it, it is full and working");
        c.check(decomposter.getItem(DecomposterBlockEntity.OUTPUT).isEmpty(), "it takes its time");
        DecomposterBlockEntity.serverTick(level, at, level.getBlockState(at), decomposter);
        c.check(decomposter.getItem(DecomposterBlockEntity.OUTPUT).is(ModItems.FERTILIZER.get())
            && decomposter.getItem(DecomposterBlockEntity.OUTPUT).getCount() == DecomposterBlockEntity.Recipe.FERTILIZER.count
            && decomposter.getItem(0).getCount() == 1, "Dirt, Bone Meal and Rotten Flesh (any order) rot down into 2 Yield Fertilizer");
        decomposter.setItem(DecomposterBlockEntity.OUTPUT, ItemStack.EMPTY);
        decomposter.setItem(0, new ItemStack(Items.REDSTONE));
        decomposter.setItem(1, new ItemStack(Items.BONE_MEAL));
        decomposter.setItem(2, new ItemStack(Items.REDSTONE));
        for (int i = 0; i < DecomposterBlockEntity.TICKS; i++) DecomposterBlockEntity.serverTick(level, at, level.getBlockState(at), decomposter);
        c.check(decomposter.getItem(DecomposterBlockEntity.OUTPUT).is(ModItems.SUPER_FERTILIZER.get()) && decomposter.getItem(DecomposterBlockEntity.OUTPUT).getCount() == 1, "Bone Meal and two Redstone make 1 Super Yield Fertilizer");
        decomposter.setItem(0, new ItemStack(Items.DIRT));
        decomposter.setItem(1, new ItemStack(Items.DIRT));
        decomposter.setItem(2, new ItemStack(Items.BONE_MEAL));
        c.check(DecomposterBlockEntity.find(java.util.List.of(new ItemStack(Items.SUGAR), new ItemStack(Items.DIRT), new ItemStack(Items.BONE_MEAL))) == DecomposterBlockEntity.Recipe.FAST_FERTILIZER,
            "Bone Meal, Sugar and Dirt make Fast Fertilizer");
        c.check(DecomposterBlockEntity.find(java.util.List.of(new ItemStack(Items.DIRT), new ItemStack(Items.DIRT), new ItemStack(Items.BONE_MEAL))) == null, "anything else makes nothing");
        decomposter.clearContent();
        DecomposterBlockEntity.serverTick(level, at, level.getBlockState(at), decomposter);
        c.check(!level.getBlockState(at).getValue(DecomposterBlock.FULL) && !level.getBlockState(at).getValue(DecomposterBlock.WORKING), "emptied, it stands empty again");
        level.removeBlock(at, false);
    }
}
