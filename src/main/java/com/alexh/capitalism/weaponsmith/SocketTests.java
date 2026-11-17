package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.gametest.GameTests;
import com.alexh.capitalism.weaponsmith.Socketing.Gem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

// Molten Socket gems (Sockets.md): where each fits, and what the newer ones do (SocketEffects).
public final class SocketTests {

    static {
        GameTests.register("socket_gems", SocketTests::gems);
        GameTests.register("socket_axe", SocketTests::axe);
    }

    private SocketTests() {
    }

    // Loaded by GameTests.init (runs the static block above)
    public static void init() {
    }

    static ItemStack socketed(Item item, Gem gem) {
        ItemStack stack = new ItemStack(item);
        stack.set(ModComponents.SOCKET.get(), gem.id);
        return stack;
    }

    private static void gems(GameTests.Checks c) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD), pick = new ItemStack(Items.DIAMOND_PICKAXE), axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack helmet = new ItemStack(Items.IRON_HELMET), boots = new ItemStack(Items.IRON_BOOTS), chest = new ItemStack(Items.IRON_CHESTPLATE);
        c.check(Socketing.socketable(SmithTable.TOOLSMITH, axe) && Socketing.socketable(SmithTable.TOOLSMITH, pick), "the Toolsmith Table sockets axes and pickaxes");
        // maces and tridents: Weaponsmith Table, sword gems; Weaponsmith repairs: shields, bow + bow
        ItemStack mace = new ItemStack(net.minecraft.world.item.Items.MACE);
        ItemStack trident = new ItemStack(net.minecraft.world.item.Items.TRIDENT);
        c.check(Socketing.socketable(SmithTable.WEAPONSMITH, mace) && Socketing.socketable(SmithTable.WEAPONSMITH, trident)
            && Gem.QUARTZ.fits(mace) && Gem.QUARTZ.fits(trident), "maces and tridents take sword gems at the Weaponsmith Table");
        ItemStack bow = new ItemStack(net.minecraft.world.item.Items.BOW);
        bow.setDamageValue(200);
        ItemStack shield = new ItemStack(net.minecraft.world.item.Items.SHIELD);
        shield.setDamageValue(100);
        c.check(DentForging.isRepair(SmithTable.WEAPONSMITH, bow, new ItemStack(net.minecraft.world.item.Items.BOW))
            && !DentForging.isRepair(SmithTable.WEAPONSMITH, bow, new ItemStack(net.minecraft.world.item.Items.STRING)), "a bow mends with another bow");
        c.check(DentForging.isRepair(SmithTable.WEAPONSMITH, shield, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS))
            && DentForging.isRepair(SmithTable.ARMORSMITH, shield, new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS))
            && !SmithTable.TOOLSMITH.repairs(shield), "shields: Weaponsmith or Armorsmith Table, with planks");
        // shovels and axes go dull too
        ItemStack shovel = new ItemStack(net.minecraft.world.item.Items.IRON_SHOVEL);
        shovel.set(com.alexh.capitalism.core.ModComponents.PICKAXE_WEAR.get(), Wear.pickaxeMax(shovel));
        c.check(Wear.isDullTool(shovel) && Wear.pickaxeMax(shovel) == Wear.pickaxeMax(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE)), "an iron shovel goes dull like an iron pickaxe");
        c.check(Gem.DIAMOND.fits(axe) && !Gem.DIAMOND.fits(sword) && Gem.FLINT.fits(axe) && !Gem.FLINT.fits(pick), "Diamond and Flint: axes only");
        c.check(Gem.BLAZE.fits(axe) && Gem.BLAZE.fits(sword) && Gem.BLAZE.fits(pick) && Gem.BLAZE.fits(chest), "Blaze Powder: everything");
        c.check(Gem.PRISMARINE.fits(sword) && Gem.PRISMARINE.fits(pick) && Gem.PRISMARINE.fits(boots) && !Gem.PRISMARINE.fits(axe), "Prismarine: sword, pickaxe, armor");
        c.check(Gem.QUARTZ.fits(pick) && Gem.QUARTZ.fits(sword) && !Gem.QUARTZ.fits(helmet), "Quartz: sword and pickaxe");
        c.check(Gem.ICE.fits(sword) && Gem.ICE.fits(helmet) && !Gem.ICE.fits(pick), "Ice: sword and armor");
        c.check(Gem.AMETHYST.fits(sword) && Gem.AMETHYST.fits(chest) && !Gem.AMETHYST.fits(pick), "Amethyst: sword and armor");
        c.check(Gem.GLOWSTONE.fits(sword) && Gem.GHAST_TEAR.fits(sword) && !Gem.GLOWSTONE.fits(helmet) && !Gem.GHAST_TEAR.fits(pick), "Glowstone, Ghast Tear: swords only");
        c.check(Gem.SLIME.fits(boots) && !Gem.SLIME.fits(helmet) && !Gem.SLIME.fits(sword), "Slime Ball: boots only");
        c.check(Gem.RABBIT_FOOT.fits(helmet) && Gem.RABBIT_FOOT.fits(boots) && !Gem.RABBIT_FOOT.fits(sword), "Rabbit's Foot: any armor");
        c.check(Gem.BONE.fits(chest) && Gem.BONE.fits(sword) && !Gem.BONE.fits(pick), "Bone: sword and armor");
        c.check(!Gem.LAPIS.fits(axe) && !Gem.REDSTONE.fits(axe) && !Gem.EMERALD.fits(axe), "the old any-item gems don't go in axes");
        c.check(Gem.byItem(new ItemStack(Items.BLUE_ICE)) == Gem.ICE && Gem.byItem(new ItemStack(Items.RABBIT_FOOT)) == Gem.RABBIT_FOOT, "gems are found by their item");
        for (Gem gem : Gem.values()) {
            for (ItemStack item : List.of(sword, pick, axe, helmet, boots)) {
                if (gem.fits(item) && Socketing.effect(gem, item).isEmpty()) c.fail(gem.title + " in " + item.getHoverName().getString() + ": no tooltip");
            }
        }
        c.check(Socketing.gemsFor(axe).equals("Blaze Powder, Diamond, Flint"), "an axe's empty socket lists its gems (got " + Socketing.gemsFor(axe) + ")");

        // Bone armor: 5% less projectile damage per piece
        c.near(SocketEffects.projectileTaken(4), 0.8, 1e-6, "4 Bone pieces: 80% of projectile damage gets through");
        c.check(SocketEffects.NETHER_MOBS.contains(net.minecraft.world.entity.EntityTypes.PIGLIN) && !SocketEffects.NETHER_MOBS.contains(net.minecraft.world.entity.EntityTypes.ZOMBIE),
            "Ghast Tear: Piglins are Nether mobs, Zombies aren't");
        c.check(SocketEffects.isStone(Blocks.DEEPSLATE.defaultBlockState()) && SocketEffects.isStone(Blocks.STONE.defaultBlockState())
            && !SocketEffects.isStone(Blocks.DIRT.defaultBlockState()), "Quartz pickaxe: stone and deepslate count, dirt doesn't");

        // Rabbit's Foot: +1 Luck per piece, Amethyst: +5% knockback resistance per piece
        ServerPlayer lucky = c.player();
        lucky.setItemSlot(EquipmentSlot.HEAD, socketed(Items.IRON_HELMET, Gem.RABBIT_FOOT));
        lucky.setItemSlot(EquipmentSlot.FEET, socketed(Items.IRON_BOOTS, Gem.RABBIT_FOOT));
        lucky.setItemSlot(EquipmentSlot.CHEST, socketed(Items.IRON_CHESTPLATE, Gem.AMETHYST));
        lucky.doTick();   // the real entity tick: picks up the worn armor's attribute modifiers
        c.near(lucky.getAttributeValue(Attributes.LUCK), 2.0, 1e-6, "2 Rabbit's Foot pieces: +2 Luck");
        c.near(lucky.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.05, 1e-6, "1 Amethyst piece: +5% knockback resistance");

        // Blaze Powder sword / pickaxe: a light while held (not in an axe: that one makes charcoal)
        ServerPlayer torch = c.player();
        torch.setItemSlot(EquipmentSlot.MAINHAND, socketed(Items.IRON_PICKAXE, Gem.BLAZE));
        c.check(SocketEffects.holdsLight(torch), "a Blaze Powder pickaxe in hand lights the way");
        torch.setItemSlot(EquipmentSlot.MAINHAND, socketed(Items.IRON_AXE, Gem.BLAZE));
        c.check(!SocketEffects.holdsLight(torch), "a Blaze Powder axe doesn't light (it makes charcoal)");

        // Amethyst sword: a kill heals half a heart
        ServerPlayer healer = c.player();
        healer.setHealth(10f);
        healer.setItemSlot(EquipmentSlot.MAINHAND, socketed(Items.IRON_SWORD, Gem.AMETHYST));
        var zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(c.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (zombie != null) {
            zombie.setPos(healer.position());
            c.level().addFreshEntity(zombie);
            zombie.setHealth(1f);
            zombie.hurtServer(c.level(), c.level().damageSources().playerAttack(healer), 5f);
            c.check(healer.getHealth() == 11f, "Amethyst sword: the kill healed half a heart (health " + healer.getHealth() + ")");
            zombie.discard();
        }
    }

    // Diamond axe: a log splits into 8 planks; Blaze Powder axe: Charcoal
    private static void axe(GameTests.Checks c) {
        c.check(SocketEffects.planksFor(Blocks.OAK_LOG) == Items.OAK_PLANKS && SocketEffects.planksFor(Blocks.STRIPPED_SPRUCE_WOOD) == Items.SPRUCE_PLANKS
            && SocketEffects.planksFor(Blocks.CRIMSON_STEM) == Items.CRIMSON_PLANKS && SocketEffects.planksFor(Blocks.BAMBOO_BLOCK) == Items.BAMBOO_PLANKS
            && SocketEffects.planksFor(Blocks.CHERRY_LOG) == Items.CHERRY_PLANKS, "every log, wood, stem and bamboo block has its planks");
        ServerLevel level = c.level();
        c.check(chop(c, level, socketed(Items.IRON_AXE, Gem.DIAMOND), Items.OAK_PLANKS) == 8, "Diamond axe: an oak log drops 8 Oak Planks");
        c.check(chop(c, level, socketed(Items.IRON_AXE, Gem.BLAZE), Items.CHARCOAL) == 1, "Blaze Powder axe: an oak log drops Charcoal");
        c.check(chop(c, level, new ItemStack(Items.IRON_AXE), Items.OAK_LOG) == 1, "a plain axe: just the log");
    }

    // Break an oak log with this axe; how many of `wanted` dropped
    private static int chop(GameTests.Checks c, ServerLevel level, ItemStack axe, Item wanted) {
        BlockPos log = BlockPos.containing(c.at(2, 2, 2));
        AABB around = new AABB(log).inflate(2);
        level.getEntitiesOfClass(ItemEntity.class, around).forEach(ItemEntity::discard);
        level.setBlock(log, Blocks.OAK_LOG.defaultBlockState(), 3);
        ServerPlayer player = c.player();
        player.setItemSlot(EquipmentSlot.MAINHAND, axe);
        player.gameMode.destroyBlock(log);
        int n = 0;
        for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, around)) {
            if (drop.getItem().is(wanted)) n += drop.getItem().getCount();
            drop.discard();
        }
        return n;
    }
}
