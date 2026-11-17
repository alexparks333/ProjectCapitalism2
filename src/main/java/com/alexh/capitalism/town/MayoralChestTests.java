package com.alexh.capitalism.town;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModBlocks;
import com.alexh.capitalism.economy.ContainerLocks;
import com.alexh.capitalism.economy.ContainerOwner;
import com.alexh.capitalism.economy.GoldLeaderboard;
import com.alexh.capitalism.economy.GoldLedger;
import com.alexh.capitalism.gametest.GameTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.material.PushReaction;

// The Mayoral Chest (Town.md > The Town Hall): only the Mayor and the Vice Mayor open it (whoever holds the office now;
// not the Treasurer, not anyone else); it counts in no one's net worth; hoppers neither take from it nor put into it;
// nobody breaks it in survival; explosions and pistons can't move it.
public final class MayoralChestTests {

    static {
        GameTests.register("mayoral_chest", MayoralChestTests::mayoralChest);
    }

    private MayoralChestTests() {
    }

    public static void init() {
    }

    private static void mayoralChest(GameTests.Checks c) {
        ServerLevel level = c.level();
        TownHall hall = TownHall.get(level.getServer());
        for (TownHall.Office office : TownHall.Office.values()) hall.remove(office);
        ServerPlayer mayor = c.player(), vice = c.player(), treasurer = c.player(), citizen = c.player();
        hall.appoint(TownHall.Office.MAYOR, mayor);
        hall.appoint(TownHall.Office.VICE_MAYOR, vice);
        hall.appoint(TownHall.Office.TREASURER, treasurer);

        BlockPos pos = BlockPos.containing(c.at(2, 2, 2));
        level.setBlock(pos, ModBlocks.MAYORAL_CHEST.get().defaultBlockState(), 3);
        MayoralChestBlockEntity chest = (MayoralChestBlockEntity) level.getBlockEntity(pos);
        c.check(chest != null && chest.getContainerSize() == 54, "the Mayoral Chest holds 54 stacks");
        if (chest == null) return;

        // ── who opens it ──
        c.check(MayoralChestBlockEntity.mayOpen(mayor) && MayoralChestBlockEntity.mayOpen(vice), "the Mayor and the Vice Mayor open it");
        c.check(!MayoralChestBlockEntity.mayOpen(treasurer) && !MayoralChestBlockEntity.mayOpen(citizen), "the Treasurer and anyone else can't");
        c.check(chest.stillValid(mayor) && !chest.stillValid(citizen), "...and the menu stays open only for an official");
        hall.appoint(TownHall.Office.MAYOR, citizen);
        c.check(MayoralChestBlockEntity.mayOpen(citizen) && !MayoralChestBlockEntity.mayOpen(mayor), "a new Mayor gets it; the old one loses it");
        hall.remove(TownHall.Office.VICE_MAYOR);
        c.check(!MayoralChestBlockEntity.mayOpen(vice), "a removed Vice Mayor loses it");

        // ── it counts for no one ──
        chest.setItem(0, new ItemStack(Items.GOLD_BLOCK, 64));
        GoldLedger ledger = GoldLedger.get(level.getServer());
        GoldLeaderboard.recount(level.getServer(), ledger);
        long before = ledger.netWorth(citizen.getUUID());
        // even if it somehow got an owner (an old world, a stray place event), the ledger ignores it
        chest.setData(ModAttachments.CONTAINER_OWNER, new ContainerOwner(citizen.getUUID(), "mod-test", true));
        ledger.setContainer(GlobalPos.of(level.dimension(), pos), citizen.getUUID(), 999_999);
        GoldLeaderboard.recount(level.getServer(), ledger);
        c.check(ContainerLocks.ownerOf(chest) == null && ledger.netWorth(citizen.getUUID()) == before,
            "a chest of gold blocks counts in no one's net worth (" + ledger.netWorth(citizen.getUUID()) + " = " + before + ")");

        // ── hoppers ──
        c.check(chest.getSlotsForFace(Direction.DOWN).length == 0 && chest.getSlotsForFace(Direction.UP).length == 0, "no slot faces a hopper");
        BlockPos below = pos.below();
        level.setBlock(below, Blocks.HOPPER.defaultBlockState(), 3);
        HopperBlockEntity under = (HopperBlockEntity) level.getBlockEntity(below);
        boolean pulled = under != null && HopperBlockEntity.suckInItems(level, under);
        c.check(!pulled && chest.getItem(0).getCount() == 64 && under != null && under.isEmpty(), "a hopper under it takes nothing out");
        BlockPos above = pos.above();
        level.setBlock(above, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN), 3);
        HopperBlockEntity over = (HopperBlockEntity) level.getBlockEntity(above);
        if (over != null) {
            over.setItem(0, new ItemStack(Items.DIRT, 8));
            for (int i = 0; i < 20; i++) HopperBlockEntity.pushItemsTick(level, above, level.getBlockState(above), over);
        }
        int dirt = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) if (chest.getItem(i).is(Items.DIRT)) dirt += chest.getItem(i).getCount();
        c.check(over != null && dirt == 0 && over.getItem(0).getCount() == 8, "a hopper above it puts nothing in");

        // ── nothing breaks or moves it ──
        mayor.setPos(c.at(2, 3, 3));
        boolean broke = mayor.gameMode.destroyBlock(pos);
        c.check(!broke && level.getBlockState(pos).is(ModBlocks.MAYORAL_CHEST.get()) && chest.getItem(0).getCount() == 64,
            "it can't be broken in survival (not even by an official)");
        var state = level.getBlockState(pos);
        c.check(state.getPistonPushReaction() == PushReaction.IMMOVEABLE && state.getBlock().getExplosionResistance() >= 3_600_000F,
            "pistons and explosions can't move it");

        // tidy up (the hall's offices too)
        chest.clearContent();
        level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        for (TownHall.Office office : TownHall.Office.values()) hall.remove(office);
    }
}
