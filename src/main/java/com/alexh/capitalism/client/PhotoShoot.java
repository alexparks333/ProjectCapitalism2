package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.boats.FairyBoat;
import com.alexh.capitalism.boats.Submersible;
import com.alexh.capitalism.core.ModEntities;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// DEV ONLY: a scripted photo shoot, to see how things really look (gradlew runPhoto: runs/photo, its own world).
// Does nothing unless the game is started with -Dcapitalism.photo. Builds a scene high in the sky: a walled underwater
// basin with a sandy floor rising to a slope, seagrass, kelp, prismarine pillars and a sunken wreck; then takes
// screenshots (runs/photo/screenshots/photo_*.png) from a list of shots, each with its own set-up, camera and wait;
// then quits. The Submersible's flashlight at midnight (on, off, looking about, seen from outside), the River Queen
// and the Fairy Blossom by day and night (the Fairy Blossom mid-flutter too).
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class PhotoShoot {

    private static final String MODE = System.getProperty("capitalism.photo");
    // the scene: the basin's corner, its water's top
    private static final int OX = 0, OY = 140, OZ = 0;
    private static final int TOP = 156;
    // the decorations' square of grass: its top
    private static final int DY = 151;

    private record Shot(String name, Consumer<Ctx> setUp, int ticks) {
    }

    private static final class Ctx {
        Minecraft mc;
        MinecraftServer server;
        ServerLevel level;
        ServerPlayer player;
        Submersible sub;
        Entity steamboat;
        FairyBoat fairy;
        // show the HUD (the clock) in this shot
        boolean hud;
    }

    private static final Ctx CTX = new Ctx();
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    private static int titleTicks;
    private static int tick;
    private static int shot = -1;
    private static int waitLeft;
    private static boolean built;

    private PhotoShoot() {
    }

    static {
        // the Submersible at midnight, its flashlight on, from the dome
        SHOTS.add(new Shot("sub_on_ahead", c -> {
            night(c);
            ride(c, c.sub);
            setLamp(c, true);
            look(c, -90.0F, 18.0F, CameraType.FIRST_PERSON);
        }, 140));
        SHOTS.add(new Shot("sub_on_down", c -> look(c, -90.0F, 38.0F, CameraType.FIRST_PERSON), 40));
        SHOTS.add(new Shot("sub_on_left", c -> look(c, -125.0F, 22.0F, CameraType.FIRST_PERSON), 40));
        SHOTS.add(new Shot("sub_on_right_up", c -> look(c, -55.0F, 5.0F, CameraType.FIRST_PERSON), 40));
        SHOTS.add(new Shot("sub_off_ahead", c -> {
            setLamp(c, false);
            look(c, -90.0F, 18.0F, CameraType.FIRST_PERSON);
        }, 40));
        SHOTS.add(new Shot("sub_on_third_back", c -> {
            setLamp(c, true);
            look(c, -90.0F, 18.0F, CameraType.THIRD_PERSON_BACK);
        }, 40));
        SHOTS.add(new Shot("sub_on_third_front", c -> look(c, -90.0F, 18.0F, CameraType.THIRD_PERSON_FRONT), 40));
        // from outside, under the water: nobody aboard, the lamp aimed ahead and down, seen from the side and behind
        SHOTS.add(new Shot("sub_beam_side", c -> {
            dismount(c);
            aim(c, 0.0F, 22.0F);
            stand(c, 7.0, TOP - 5.0, 11.0, 7.0, TOP - 5.0, 0.0);
        }, 50));
        SHOTS.add(new Shot("sub_beam_behind", c -> stand(c, -7.0, TOP - 3.0, 3.0, 8.0, TOP - 6.0, 0.0), 40));
        SHOTS.add(new Shot("sub_beam_left", c -> {
            aim(c, -40.0F, 25.0F);
            stand(c, -6.0, TOP - 3.0, 4.0, 8.0, TOP - 7.0, -6.0);
        }, 40));
        // the River Queen and the Fairy Blossom, from a vantage point on the water's edge
        SHOTS.add(new Shot("steamboat_side_day", c -> {
            dismount(c);
            day(c);
            stand(c, 6.5, TOP + 2.0, -20.5, 6.5, TOP + 2.0, -12.5);
        }, 80));
        SHOTS.add(new Shot("steamboat_bow_day", c -> stand(c, 13.5, TOP + 3.0, -18.0, 6.5, TOP + 2.0, -12.5), 40));
        SHOTS.add(new Shot("steamboat_stern_day", c -> stand(c, -2.0, TOP + 3.0, -18.0, 4.0, TOP + 2.0, -12.5), 40));
        SHOTS.add(new Shot("steamboat_bow_night", c -> {
            night(c);
            stand(c, 13.5, TOP + 3.0, -18.0, 6.5, TOP + 2.0, -12.5);
        }, 40));
        SHOTS.add(new Shot("fairy_side_day", c -> {
            day(c);
            stand(c, 6.5, TOP + 0.8, 16.5, 6.5, TOP + 1.0, 12.5);
        }, 40));
        SHOTS.add(new Shot("fairy_quarter_day", c -> stand(c, 10.0, TOP + 1.5, 16.0, 6.5, TOP + 1.0, 12.5), 40));
        SHOTS.add(new Shot("fairy_flutter_day", c -> {
            c.server.execute(() -> c.fairy.flutter());
            stand(c, 10.5, TOP + 2.5, 17.0, 6.5, TOP + 3.2, 12.5);
        }, 22));
        SHOTS.add(new Shot("fairy_night", c -> {
            night(c);
            stand(c, 10.0, TOP + 1.5, 16.0, 6.5, TOP + 1.0, 12.5);
        }, 70));
        // the seasonal decorations on a square of grass (all put up by hand), by day and night, and the clock
        SHOTS.add(new Shot("decor_day", c -> {
            day(c);
            c.server.execute(() -> com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.UP));
            stand(c, 43.0, DY + 3.0, 0.0, 60.0, DY + 3.0, 0.0);
        }, 220));
        SHOTS.add(new Shot("decor_tree", c -> stand(c, 50.0, DY + 1.0, 4.0, 60.0, DY + 4.5, 0.0), 40));
        SHOTS.add(new Shot("decor_scarecrow", c -> stand(c, 50.5, DY + 0.5, -4.0, 54.5, DY + 1.6, -7.5), 40));
        SHOTS.add(new Shot("decor_cauldron", c -> stand(c, 52.5, DY + 0.3, 9.5, 56.5, DY + 0.4, 7.5), 40));
        SHOTS.add(new Shot("decor_pumpkins", c -> stand(c, 62.0, DY + 1.8, -10.5, 67.5, DY + 0.5, -5.5), 40));
        SHOTS.add(new Shot("decor_leaves", c -> stand(c, 62.0, DY + 1.8, 12.0, 67.5, DY + 0.3, 7.5), 40));
        SHOTS.add(new Shot("decor_presents", c -> stand(c, 51.5, DY + 1.2, 5.0, 56.5, DY + 0.4, 2.5), 40));
        SHOTS.add(new Shot("decor_candy", c -> stand(c, 56.0, DY + 1.5, 2.5, 48.5, DY + 2.2, 2.5), 40));
        SHOTS.add(new Shot("decor_candy_close", c -> stand(c, 53.0, DY + 0.6, 4.5, 50.5, DY + 0.6, 3.5), 40));
        SHOTS.add(new Shot("decor_lights_day", c -> stand(c, 60.0, DY + 0.5, -6.0, 60.0, DY + 3.5, -13.0), 40));
        SHOTS.add(new Shot("decor_night", c -> {
            night(c);
            stand(c, 43.0, DY + 3.0, 0.0, 60.0, DY + 3.0, 0.0);
        }, 60));
        SHOTS.add(new Shot("decor_lights_night", c -> stand(c, 60.0, DY + 0.5, -6.0, 60.0, DY + 3.5, -13.0), 40));
        SHOTS.add(new Shot("decor_candy_night", c -> stand(c, 56.0, DY + 1.5, 2.5, 48.5, DY + 2.2, 2.5), 40));
        SHOTS.add(new Shot("decor_tree_night", c -> stand(c, 50.0, DY + 1.0, 4.0, 60.0, DY + 4.5, 0.0), 40));
        // the other seasons' decorations on a second square (x 77 to 100): fall and Halloween, summer, spring
        SHOTS.add(new Shot("season_fall", c -> {
            day(c);
            stand(c, 77.0, DY + 2.5, -7.0, 88.0, DY + 1.5, -7.0);
        }, 60));
        SHOTS.add(new Shot("season_graves", c -> stand(c, 80.0, DY + 1.4, -9.0, 85.0, DY + 0.5, -9.0), 40));
        SHOTS.add(new Shot("season_trees", c -> stand(c, 80.0, DY + 2.0, -9.0, 92.0, DY + 3.0, -9.0), 40));
        SHOTS.add(new Shot("season_summer", c -> stand(c, 79.0, DY + 2.2, 5.0, 86.0, DY + 0.5, 5.0), 40));
        SHOTS.add(new Shot("season_spring", c -> stand(c, 79.0, DY + 2.0, 13.0, 86.0, DY + 0.5, 13.0), 40));
        SHOTS.add(new Shot("season_fog_night", c -> {
            night(c);
            stand(c, 80.0, DY + 1.8, -6.0, 87.0, DY + 0.2, -3.0);
        }, 60));
        SHOTS.add(new Shot("season_leaves_close", c -> {
            day(c);
            stand(c, 86.0, DY + 1.6, -9.0, 91.0, DY, -7.0);
        }, 40));
        // the calendar for real (not forced up): Fall 20, so the fall and Halloween things are up, summer's and spring's away
        SHOTS.add(new Shot("season_fall_by_calendar", c -> {
            c.server.execute(() -> {
                com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.AUTO);
                com.alexh.capitalism.calendar.Calendar.setClock(c.level,
                    com.alexh.capitalism.calendar.Calendar.startOf(1, com.alexh.capitalism.calendar.Calendar.Season.FALL, 20) + 6000L);
            });
            stand(c, 77.0, DY + 3.0, 0.0, 90.0, DY + 1.5, 0.0);
        }, 260));
        SHOTS.add(new Shot("season_back_up", c ->
            c.server.execute(() -> com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.UP)), 200));
        // winter: snow falling over the plains (a rain biome), frost on the grass, icicles under the roof
        SHOTS.add(new Shot("winter_snow", c -> {
            day(c);
            command(c, "weather rain");
            c.server.execute(() -> com.alexh.capitalism.calendar.Calendar.setClock(c.level,
                com.alexh.capitalism.calendar.Calendar.startOf(1, com.alexh.capitalism.calendar.Calendar.Season.WINTER, 10) + 6000L));
            stand(c, 44.0, DY + 3.0, 0.0, 60.0, DY + 2.0, 0.0);
        }, 80));
        SHOTS.add(new Shot("winter_frost", c -> stand(c, 49.0, DY + 1.8, -3.0, 53.0, DY, -7.5), 40));
        SHOTS.add(new Shot("winter_icicles", c -> stand(c, 54.0, DY + 0.9, 8.5, 54.0, DY + 2.4, 12.5), 40));
        // performance (-Pphoto=perf, with a Flight Recorder recording, build.gradle): ~10 seconds each, frame times measured
        // (onFrame): nothing up; both decoration squares up; 40 hurt husks with health bars; then the Enchanter's shop
        SHOTS.add(new Shot("perf_base", c -> {
            day(c);
            c.hud = true;
            unlockFps(c);
            c.server.execute(() -> com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.DOWN));
            stand(c, 45.0, DY + 4.0, 0.0, 70.0, DY + 1.0, 0.0);
        }, 260));
        SHOTS.add(new Shot("perf_decor", c ->
            c.server.execute(() -> com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.UP)), 260));
        // the same, but looking away from them (they should cost nothing out of sight)
        SHOTS.add(new Shot("perf_away", c -> stand(c, 45.0, DY + 4.0, 0.0, 45.5, DY + 60.0, 0.2), 260));
        SHOTS.add(new Shot("perf_back", c -> stand(c, 45.0, DY + 4.0, 0.0, 70.0, DY + 1.0, 0.0), 30));
        SHOTS.add(new Shot("perf_mobs", c -> {
            command(c, "difficulty easy");
            for (int i = 0; i < 40; i++) {
                command(c, "summon husk " + (46 + (i % 8) * 1.5) + " " + DY + " " + (-7 + (i / 8) * 3.5) + " {NoAI:1b,PersistenceRequired:1b,Tags:[\"perf\"]}");
            }
        }, 260));
        // the husks alone, decorations packed away (to tell their cost apart)
        SHOTS.add(new Shot("perf_mobs_only", c ->
            c.server.execute(() -> com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.DOWN)), 260));
        SHOTS.add(new Shot("perf_enchant", c -> c.server.execute(() -> {
            c.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
            c.player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_BLOCK, 3));
            com.alexh.capitalism.enchanter.NpcEnchanting.open(c.player, c.player);
        }), 30));
        SHOTS.add(new Shot("perf_enchant_item", c -> c.server.execute(() -> {
            if (c.player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu menu) {
                menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
                c.player.getInventory().setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                menu.slotsChanged(menu.getSlot(0).container);
                c.player.experienceLevel = 40;
            }
        }), 20));
        // the Wishing Well (-Pphoto=well): from the front, the side, by night, standing at its rim; and the /pick screen
        SHOTS.add(new Shot("well_front", c -> {
            day(c);
            c.server.execute(() -> {
                BlockPos core = new BlockPos(60, DY, -6);
                var block = com.alexh.capitalism.core.ModBlocks.WISHING_WELL.get();
                var state = block.defaultBlockState().setValue(com.alexh.capitalism.occupation.WishingWellBlock.FACING, net.minecraft.core.Direction.WEST);
                for (int i = 0; i < 21; i++) c.level.setBlock(com.alexh.capitalism.occupation.WishingWellBlock.partPos(core,
                    net.minecraft.core.Direction.WEST, i), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                c.level.setBlock(core, state, 3);
                block.setPlacedBy(c.level, core, state, null, net.minecraft.world.item.ItemStack.EMPTY);
            });
            stand(c, 53.5, DY + 1.5, -6.0, 60.5, DY + 1.5, -5.5);
        }, 60));
        SHOTS.add(new Shot("well_side", c -> stand(c, 60.5, DY + 2.5, -13.5, 60.5, DY + 1.0, -5.5), 40));
        SHOTS.add(new Shot("well_rim", c -> stand(c, 58.5, DY + 0.2, -6.0, 60.5, DY + 0.6, -5.5), 40));
        SHOTS.add(new Shot("well_night", c -> {
            night(c);
            stand(c, 53.5, DY + 1.5, -6.0, 60.5, DY + 1.5, -5.5);
        }, 40));
        SHOTS.add(new Shot("well_pick", c -> {
            day(c);
            c.hud = true;
            c.mc.execute(JobPickScreen::open);
        }, 30));
        // the Mailbox (-Pphoto=mail): two side by side (yours with mail: flag up, the count over it; someone else's,
        // empty), then your mail and their mail slot
        SHOTS.add(new Shot("mail_boxes", c -> {
            day(c);
            c.server.execute(() -> {
                var block = com.alexh.capitalism.core.ModBlocks.MAILBOX.get();
                var state = block.defaultBlockState().setValue(com.alexh.capitalism.mail.MailboxBlock.FACING, net.minecraft.core.Direction.WEST);
                BlockPos mine = new BlockPos(56, DY, -12);
                BlockPos theirs = new BlockPos(56, DY, -10);
                c.level.setBlock(mine, state, 3);
                c.level.setBlock(theirs, state, 3);
                c.level.setBlock(new BlockPos(56, DY, -15), com.alexh.capitalism.core.ModBlocks.TOWN_MAILBOX.get().defaultBlockState()
                    .setValue(com.alexh.capitalism.mail.MailboxBlock.FACING, net.minecraft.core.Direction.WEST), 3);
                if (c.level.getBlockEntity(mine) instanceof com.alexh.capitalism.mail.MailboxBlockEntity box) {
                    box.setOwner(c.player.getUUID(), c.player.getGameProfile().name());
                    box.deliver(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER, 3));
                    box.deliver(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 5));
                    box.deliver(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK));
                    box.deliver(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 4));
                    box.deliver(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 2));
                }
                if (c.level.getBlockEntity(theirs) instanceof com.alexh.capitalism.mail.MailboxBlockEntity box) {
                    box.setOwner(java.util.UUID.randomUUID(), "Sam");
                }
            });
            stand(c, 51.5, DY + 1.0, -9.5, 56.5, DY + 0.9, -11.0);
        }, 60));
        SHOTS.add(new Shot("mail_side", c -> stand(c, 54.0, DY + 1.6, -15.5, 56.5, DY + 0.9, -11.0), 40));
        SHOTS.add(new Shot("mail_town", c -> stand(c, 53.2, DY + 1.3, -16.2, 56.5, DY + 1.0, -14.5), 40));
        SHOTS.add(new Shot("mail_owner", c -> {
            c.hud = true;
            c.server.execute(() -> {
                if (c.level.getBlockEntity(new BlockPos(56, DY, -12)) instanceof com.alexh.capitalism.mail.MailboxBlockEntity box) box.open(c.player);
            });
        }, 30));
        SHOTS.add(new Shot("mail_sender", c -> {
            c.hud = true;
            c.server.execute(() -> {
                c.player.closeContainer();
                if (c.level.getBlockEntity(new BlockPos(56, DY, -10)) instanceof com.alexh.capitalism.mail.MailboxBlockEntity box) box.open(c.player);
                if (c.player.containerMenu instanceof com.alexh.capitalism.mail.MailboxMenu menu) menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAPER, 2));
            });
        }, 30));
        // the shop signs (-Pphoto=signs): all three on a dark oak wall, each over a double door
        SHOTS.add(new Shot("signs_wall", c -> {
            day(c);
            c.server.execute(() -> {
                var wall = net.minecraft.world.level.block.Blocks.DARK_OAK_LOG.defaultBlockState();
                for (int z = -6; z <= 6; z++) for (int y = 0; y < 5; y++) c.level.setBlock(new BlockPos(62, DY + y, z), wall, 3);
                var dir = net.minecraft.core.Direction.WEST;
                java.util.List<net.minecraft.world.level.block.Block> signs = java.util.List.of(com.alexh.capitalism.core.ModBlocks.TOWN_HALL_SIGN.get(),
                    com.alexh.capitalism.core.ModBlocks.BANK_SIGN.get(), com.alexh.capitalism.core.ModBlocks.BOAT_SHOP_SIGN.get());
                for (int i = 0; i < 3; i++) {
                    int z = -5 + i * 4;
                    var state = signs.get(i).defaultBlockState().setValue(com.alexh.capitalism.decor.ShopSignBlock.FACING, dir);
                    for (int y = 0; y < 4; y++) for (int dz = 0; dz < 2; dz++) c.level.setBlock(new BlockPos(61, DY + y, z + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    c.level.setBlock(new BlockPos(61, DY + 2, z), state, 3);
                    c.level.setBlock(new BlockPos(61, DY + 2, z + 1), state.setValue(com.alexh.capitalism.decor.ShopSignBlock.RIGHT, true), 3);
                    var door = net.minecraft.world.level.block.Blocks.SPRUCE_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, dir);
                    for (int dz = 0; dz < 2; dz++) {
                        var d = door.setValue(net.minecraft.world.level.block.DoorBlock.HINGE, dz == 0 ? net.minecraft.world.level.block.state.properties.DoorHingeSide.RIGHT
                            : net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT);
                        c.level.setBlock(new BlockPos(61, DY, z + dz), d, 3);
                        c.level.setBlock(new BlockPos(61, DY + 1, z + dz), d.setValue(net.minecraft.world.level.block.DoorBlock.HALF,
                            net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
                    }
                }
            });
            stand(c, 52.5, DY + 2.0, -0.5, 61.0, DY + 2.0, -0.5);
        }, 60));
        SHOTS.add(new Shot("signs_close", c -> stand(c, 58.0, DY + 2.3, -4.0, 61.0, DY + 2.5, -4.0), 30));
        // the Blood Moon (-Pphoto=bloodmoon): the red night sky and moon, then the Crimson Revenant (held still) in its
        // poses: standing, raising its sword to slam, calling its thralls
        SHOTS.add(new Shot("bm_sky", c -> {
            command(c, "event bloodmoon start");
            night(c);
            command(c, "effect give @a night_vision 99999 0 true");
            stand(c, 52.5, DY + 1.6, 0.5, 52.5, DY + 60.0, 0.6);
        }, 120));
        SHOTS.add(new Shot("bm_horizon", c -> stand(c, 52.5, DY + 1.6, 0.5, 80.0, DY + 6.0, 0.5), 40));
        SHOTS.add(new Shot("bm_revenant", c -> {
            c.server.execute(() -> {
                var revenant = com.alexh.capitalism.core.ModEntities.CRIMSON_REVENANT.get().create(c.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (revenant == null) return;
                revenant.snapTo(60.5, DY, 0.5, 90.0F, 0.0F);
                revenant.setYHeadRot(90.0F);
                revenant.setYBodyRot(90.0F);
                revenant.setNoAi(true);
                c.level.addFreshEntity(revenant);
                com.alexh.capitalism.event.BloodMoon.adopt(c.server, revenant);
            });
            stand(c, 52.0, DY + 2.2, 0.5, 60.5, DY + 2.6, 0.5);
        }, 60));
        SHOTS.add(new Shot("bm_revenant_side", c -> stand(c, 55.0, DY + 1.5, -5.0, 60.5, DY + 2.6, 0.5), 30));
        SHOTS.add(new Shot("bm_hitbox", c -> {
            hitboxes(true);
            stand(c, 53.0, DY + 2.2, 4.0, 60.5, DY + 2.2, 0.5);
        }, 10));
        SHOTS.add(new Shot("bm_slam", c -> {
            hitboxes(false);
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.SLAM_WINDUP);
            stand(c, 56.0, DY + 6.0, 9.0, 60.5, DY + 0.5, 0.5);
        }, 14));
        SHOTS.add(new Shot("bm_slam_wave", c -> {
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.SLAM_WINDUP);
            stand(c, 56.0, DY + 6.0, 9.0, 60.5, DY + 0.5, 0.5);
        }, 25));
        SHOTS.add(new Shot("bm_summon", c -> {
            c.server.execute(() -> {
                for (var r : c.level.getEntitiesOfClass(com.alexh.capitalism.event.CrimsonRevenant.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                    r.setAction(com.alexh.capitalism.event.CrimsonRevenant.SUMMON);
                }
            });
            stand(c, 53.0, DY + 1.8, 4.0, 60.5, DY + 3.0, 0.5);
        }, 40));
        SHOTS.add(new Shot("bm_sweep", c -> {
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.SWEEP_WINDUP);
            stand(c, 56.0, DY + 6.0, 9.0, 60.5, DY + 0.5, 0.5);
        }, 8));
        SHOTS.add(new Shot("bm_sweep_arc", c -> {
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.SWEEP_WINDUP);
            stand(c, 56.0, DY + 6.0, 9.0, 60.5, DY + 0.5, 0.5);
        }, 19));
        SHOTS.add(new Shot("bm_line", c -> {
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.BLOOD_LINE);
            stand(c, 57.0, DY + 6.0, 9.0, 52.5, DY + 0.5, 0.5);
        }, 30));
        SHOTS.add(new Shot("bm_geyser", c -> {
            pool(c, true);
            fight(c, com.alexh.capitalism.event.CrimsonRevenant.GEYSER);
            stand(c, 50.0, DY + 7.0, 11.0, 57.5, DY + 0.5, 0.5);
        }, 30));
        SHOTS.add(new Shot("bm_geyser_burst", c -> stand(c, 50.0, DY + 7.0, 11.0, 57.5, DY + 0.5, 0.5), 9));
        SHOTS.add(new Shot("bm_geyser_dry", c -> pool(c, false), 10));
        SHOTS.add(new Shot("bm_hit", c -> {
            bossAction(c, 0);
            c.server.execute(() -> {
                for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.BloodMoonBoss.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                    b.hurtServer(c.level, c.level.damageSources().generic(), 1.0F);
                }
            });
            stand(c, 52.0, DY + 2.2, 0.5, 60.5, DY + 2.6, 0.5);
        }, 3));
        // Gorefang: standing, crouched to pounce, howling, charging
        SHOTS.add(new Shot("bm_wolf", c -> {
            spawnBoss(c, com.alexh.capitalism.core.ModEntities.GOREFANG.get());
            stand(c, 53.0, DY + 2.0, -4.0, 60.5, DY + 1.6, 0.5);
        }, 60));
        SHOTS.add(new Shot("bm_wolf_front", c -> stand(c, 51.5, DY + 2.0, 0.5, 60.5, DY + 1.8, 0.5), 20));
        SHOTS.add(new Shot("bm_wolf_hitbox", c -> {
            hitboxes(true);
            stand(c, 53.0, DY + 2.2, -4.0, 60.5, DY + 1.6, 0.5);
        }, 10));
        SHOTS.add(new Shot("bm_wolf_crouch", c -> {
            hitboxes(false);
            fight(c, com.alexh.capitalism.event.Gorefang.POUNCE_CROUCH);
            stand(c, 57.0, DY + 6.0, 9.0, 55.5, DY + 0.5, 0.5);
        }, 10));
        SHOTS.add(new Shot("bm_wolf_howl", c -> {
            fight(c, com.alexh.capitalism.event.Gorefang.HOWL);
            stand(c, 50.0, DY + 7.0, 12.0, 60.5, DY + 0.5, 0.5);
        }, 24));
        SHOTS.add(new Shot("bm_wolf_charge", c -> {
            fight(c, com.alexh.capitalism.event.Gorefang.CHARGE_WINDUP);
            stand(c, 57.0, DY + 6.0, 9.0, 52.5, DY + 0.5, 0.5);
        }, 12));
        SHOTS.add(new Shot("bm_wolf_dazed", c -> {
            fight(c, com.alexh.capitalism.event.Gorefang.DAZED);
            stand(c, 55.0, DY + 3.0, 4.0, 60.5, DY + 1.6, 0.5);
        }, 10));
        SHOTS.add(new Shot("bm_wolf_bite", c -> {
            c.server.execute(() -> {
                for (var g : c.level.getEntitiesOfClass(com.alexh.capitalism.event.Gorefang.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10)))
                    for (var v : c.level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                        g.setAction(0);
                        g.doHurtTarget(c.level, v);
                    }
            });
            stand(c, 49.5, DY + 1.9, 0.5, 60.5, DY + 1.4, 0.5);
        }, 2));
        SHOTS.add(new Shot("bm_wolf_riptide", c -> {
            pool(c, true);
            fight(c, com.alexh.capitalism.event.Gorefang.RIPTIDE);
            stand(c, 56.0, DY + 6.0, 9.0, 58.5, DY + 0.5, 0.5);
        }, 8));
        SHOTS.add(new Shot("bm_wolf_riptide_surge", c -> stand(c, 56.0, DY + 6.0, 9.0, 56.5, DY + 0.5, 0.5), 7));
        SHOTS.add(new Shot("bm_wolf_dry", c -> pool(c, false), 10));
        // the Sanguine Witch: floating, raising blood rain, flinging bolts, draining
        SHOTS.add(new Shot("bm_witch", c -> {
            spawnBoss(c, com.alexh.capitalism.core.ModEntities.SANGUINE_WITCH.get());
            stand(c, 53.0, DY + 2.2, -3.0, 60.5, DY + 2.2, 0.5);
        }, 60));
        SHOTS.add(new Shot("bm_witch_front", c -> stand(c, 52.0, DY + 2.4, 0.5, 60.5, DY + 2.6, 0.5), 20));
        SHOTS.add(new Shot("bm_witch_rain", c -> {
            bossAction(c, com.alexh.capitalism.event.SanguineWitch.RAIN);
            stand(c, 52.0, DY + 2.4, 0.5, 60.5, DY + 3.0, 0.5);
        }, 20));
        SHOTS.add(new Shot("bm_witch_bolts", c -> {
            bossAction(c, com.alexh.capitalism.event.SanguineWitch.BOLTS);
            stand(c, 53.0, DY + 2.2, -3.0, 60.5, DY + 2.4, 0.5);
        }, 10));
        SHOTS.add(new Shot("bm_witch_drain", c -> {
            bossAction(c, com.alexh.capitalism.event.SanguineWitch.DRAIN);
            stand(c, 53.0, DY + 2.2, -3.0, 60.5, DY + 2.4, 0.5);
        }, 20));
        SHOTS.add(new Shot("bm_witch_nova", c -> {
            bossAction(c, com.alexh.capitalism.event.SanguineWitch.NOVA);
            stand(c, 53.0, DY + 2.4, 0.5, 60.5, DY + 2.8, 0.5);
        }, 27));
        // the Blood Nova's wave rolling out along the ground (one ring sent ~1.3 s before the shot, a second behind it)
        SHOTS.add(new Shot("bm_witch_ring", c -> {
            witchRing(c);
            stand(c, 48.0, DY + 3.5, 8.0, 60.5, DY + 0.5, 0.5);
        }, 26));
        // Blood Rain: clouds gathering, the pale circle and its countdown ring (storms land in 30 / 38 ticks)
        SHOTS.add(new Shot("bm_witch_rain_cloud", c -> {
            witch(c, w -> w.photoRain(54.5, 3.0, 55.5, -3.5, 30));
            stand(c, 47.0, DY + 4.0, 0.0, 55.0, DY + 1.5, 0.0);
        }, 18));
        // ...and the downpour hitting the first spot (the second still counting down)
        SHOTS.add(new Shot("bm_witch_rain_pour", c -> {
            witch(c, w -> w.photoRain(54.5, 3.0, 55.5, -3.5, 30));
            stand(c, 47.0, DY + 4.0, 0.0, 55.0, DY + 1.5, 0.0);
        }, 28));
        // Drain: the violet siphon on a villager standing in for a player, past its warning
        SHOTS.add(new Shot("bm_witch_drain2", c -> {
            c.server.execute(() -> {
                var victim = net.minecraft.world.entity.EntityTypes.VILLAGER.create(c.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (victim == null) return;
                victim.snapTo(53.5, DY, 0.5, -90.0F, 0.0F);
                victim.setNoAi(true);
                victim.addTag("photo_drain_victim");
                c.level.addFreshEntity(victim);
                for (var w : c.level.getEntitiesOfClass(com.alexh.capitalism.event.SanguineWitch.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) w.photoDrain(victim);
            });
            stand(c, 57.0, DY + 2.5, 7.5, 57.0, DY + 1.3, 0.5);
        }, 40));
        SHOTS.add(new Shot("bm_witch_drain_warn", c -> {
            c.server.execute(() -> {
                for (var w : c.level.getEntitiesOfClass(com.alexh.capitalism.event.SanguineWitch.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                    var victims = c.level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10));
                    w.photoDrain(victims.isEmpty() ? null : victims.get(0));
                }
            });
            stand(c, 57.0, DY + 2.5, 7.5, 57.0, DY + 1.3, 0.5);
        }, 12));
        SHOTS.add(new Shot("bm_witch_spears", c -> {
            fight(c, com.alexh.capitalism.event.SanguineWitch.SPEARS);
            stand(c, 57.0, DY + 7.0, 10.0, 53.0, DY + 1.0, 0.5);
        }, 30));
        SHOTS.add(new Shot("bm_witch_spears_fall", c -> stand(c, 57.0, DY + 7.0, 10.0, 53.0, DY + 3.0, 0.5), 9));
        SHOTS.add(new Shot("bm_witch_bubble", c -> {
            fight(c, com.alexh.capitalism.event.SanguineWitch.BUBBLE);
            stand(c, 54.0, DY + 3.0, 6.0, 60.5, DY + 2.0, 0.5);
        }, 20));
        SHOTS.add(new Shot("bm_witch_maelstrom", c -> {
            pool(c, true);
            fight(c, com.alexh.capitalism.event.SanguineWitch.MAELSTROM);
            stand(c, 49.0, DY + 8.0, 9.0, 52.5, DY, 0.5);
        }, 40));
        SHOTS.add(new Shot("bm_witch_dry", c -> pool(c, false), 10));
        SHOTS.add(new Shot("bm_witch_blink", c -> {
            fight(c, com.alexh.capitalism.event.SanguineWitch.BLINK);
            stand(c, 57.0, DY + 5.0, 10.0, 58.0, DY + 1.0, 0.5);
        }, 8));
        SHOTS.add(new Shot("bm_witch_back", c -> {
            c.server.execute(() -> {
                for (var w : c.level.getEntitiesOfClass(com.alexh.capitalism.event.SanguineWitch.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) w.photoDrain(null);
                for (var v : c.level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) v.discard();
            });
            bossAction(c, 0);
            stand(c, 65.5, DY + 3.0, 4.0, 60.5, DY + 2.6, 0.5);
        }, 20));
        // mobs: two husks (they don't burn by day), one a Boss, hit: health bars, damage numbers, level marks
        SHOTS.add(new Shot("mob_spawn", c -> {
            day(c);
            command(c, "difficulty easy");
            command(c, "summon husk 52 " + DY + " -1.5 {NoAI:1b,PersistenceRequired:1b,Tags:[\"photo_a\"]}");
            command(c, "summon husk 52 " + DY + " 1.5 {NoAI:1b,PersistenceRequired:1b,Tags:[\"photo_b\"]}");
            c.server.execute(() -> {
                for (var e : c.level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(40, DY - 2, -10, 60, DY + 5, 10),
                        e -> e.entityTags().contains("photo_b"))) {
                    if (e instanceof net.minecraft.world.entity.Mob mob) com.alexh.capitalism.core.TougherMobs.setLevel(mob, com.alexh.capitalism.core.TougherMobs.Level.BOSS);
                }
            });
            stand(c, 46.5, DY, 0.0, 52.0, DY + 1.6, 0.0);
        }, 40));
        SHOTS.add(new Shot("mob_hit", c -> {
            command(c, "damage @e[tag=photo_a,limit=1] 7");
            command(c, "damage @e[tag=photo_b,limit=1] 18");
        }, 8));
        SHOTS.add(new Shot("mob_after", c -> { }, 40));
        // the Blood Merchant's Halloween stall (-Pphoto=spooky): its three decorations by day and night (the scarecrow's
        // head turned to the camera, an epitaph carved), the Ghost Sheet worn, the Spooky tab
        SHOTS.add(new Shot("spooky_row_day", c -> {
            day(c);
            c.server.execute(() -> spookyScene(c.level));
            stand(c, 113.5, DY + 1.0, 70.5, 118.5, DY + 1.0, 70.5);
        }, 80));
        SHOTS.add(new Shot("spooky_side", c -> stand(c, 115.0, DY + 1.6, 66.0, 118.5, DY + 1.2, 70.5), 30));
        SHOTS.add(new Shot("spooky_tombstone", c -> stand(c, 116.2, DY + 0.0, 70.5, 118.5, DY + 0.5, 70.5), 30));
        SHOTS.add(new Shot("spooky_tombstone_short", c -> stand(c, 116.2, DY + 0.3, 66.5, 118.5, DY + 0.5, 66.5), 30));
        SHOTS.add(new Shot("spooky_row_night", c -> {
            night(c);
            stand(c, 113.5, DY + 1.0, 70.5, 118.5, DY + 1.0, 70.5);
        }, 60));
        SHOTS.add(new Shot("spooky_sheet", c -> {
            day(c);
            c.server.execute(() -> c.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,
                new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.GHOST_SHEET.get())));
            stand(c, 112.5, DY, 62.5, 106.5, DY + 1.0, 62.5);
            look(c, 90.0F, 10.0F, CameraType.THIRD_PERSON_FRONT);
        }, 30));
        // nodding and looking aside: the hood goes with the head
        SHOTS.add(new Shot("spooky_sheet_nod", c -> look(c, 90.0F, 45.0F, CameraType.THIRD_PERSON_FRONT), 20));
        SHOTS.add(new Shot("spooky_tab", c -> {
            night(c);
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.UP);
                var m = new com.alexh.capitalism.town.BloodMerchant(com.alexh.capitalism.core.ModEntities.BLOOD_MERCHANT.get(), c.level);
                m.snapTo(110.5, DY, 76.5, 180.0F, 0.0F);
                c.level.addFreshEntity(m);
                c.player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) -> new com.alexh.capitalism.town.BloodMerchantMenu(id, inv,
                    com.alexh.capitalism.town.BloodMerchantMenu.liveData(c.player), m), net.minecraft.network.chat.Component.literal("The Blood Merchant")));
            });
        }, 20));
        SHOTS.add(new Shot("spooky_tab_open", c -> c.mc.execute(() -> {
            if (c.mc.gui.screen() instanceof BloodMerchantScreen s) s.showTab(2);
        }), 20));
        // the Investment Chest (-Pphoto=invest, Economy.md > The Investment Chest): from the front and the side (its sign),
        // open (its screen with gold in and returns waiting), the lid up, the item in a slot
        SHOTS.add(new Shot("invest_chest", c -> {
            day(c);
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            c.hud = false;
            c.server.execute(() -> {
                c.player.closeContainer();
                for (int x = 104; x <= 118; x++) for (int z = 70; z <= 84; z++) {
                    c.level.setBlock(new BlockPos(x, DY - 1, z), Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                    for (int y = DY; y < DY + 4; y++) c.level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
                c.level.setBlock(new BlockPos(112, DY, 76), com.alexh.capitalism.core.ModBlocks.INVESTMENT_CHEST.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.WEST), 3);
            });
            stand(c, 109.0, DY + 1.4, 76.5, 112.5, DY + 1.0, 76.5);
        }, 50));
        SHOTS.add(new Shot("invest_side", c -> stand(c, 109.4, DY + 1.6, 73.6, 112.5, DY + 1.2, 76.5), 25));
        SHOTS.add(new Shot("invest_sign", c -> stand(c, 110.2, DY + 2.0, 76.5, 112.5, DY + 1.8, 76.5), 25));
        SHOTS.add(new Shot("invest_menu", c -> {
            c.hud = true;
            c.server.execute(() -> {
                var bank = com.alexh.capitalism.economy.InvestmentBank.get(c.server);
                var account = bank.account(c.player.getUUID());
                account.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 64));
                account.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 36));
                c.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 30));
                // a morning's returns already waiting
                bank.payEveryone(c.server, 0);
                account.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_INGOT, 36));
                bank.payEveryone(c.server, 1);
                if (c.level.getBlockEntity(new BlockPos(112, DY, 76)) instanceof com.alexh.capitalism.economy.InvestmentChestBlockEntity be) {
                    com.alexh.capitalism.economy.InvestmentChestBlock.open(c.player, be);
                }
            });
        }, 30));
        SHOTS.add(new Shot("invest_lid", c -> {
            c.hud = false;
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            stand(c, 110.0, DY + 1.8, 74.4, 112.5, DY + 0.6, 76.5);
        }, 40));
        SHOTS.add(new Shot("invest_icon", c -> {
            c.hud = true;
            c.server.execute(() -> {
                c.player.closeContainer();
                c.player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.INVESTMENT_CHEST.get()));
            });
            c.mc.execute(() -> c.mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.mc.player)));
        }, 25));
        // the admin panel (-Pphoto=admin, core/AdminPanel): each tab, a search
        SHOTS.add(new Shot("admin_settings", c -> {
            c.hud = true;
            c.server.execute(() -> com.alexh.capitalism.core.AdminPanel.open(c.player, true));
        }, 25));
        SHOTS.add(new Shot("admin_banker", c -> c.mc.execute(() -> {
            if (c.mc.gui.screen() instanceof AdminPanelScreen s) s.showTab(1, "");
        }), 15));
        SHOTS.add(new Shot("admin_banker_search", c -> c.mc.execute(() -> {
            if (c.mc.gui.screen() instanceof AdminPanelScreen s) s.showTab(1, "emerald");
        }), 15));
        SHOTS.add(new Shot("admin_commands", c -> c.mc.execute(() -> {
            if (c.mc.gui.screen() instanceof AdminPanelScreen s) s.showTab(2, "");
        }), 15));
        SHOTS.add(new Shot("admin_invest", c -> c.mc.execute(() -> {
            AdminPanelScreen.showSection("economy");
            AdminPanelScreen.photoChange("economy.investmentDailyPercent", "8");
            AdminPanelScreen.photoChange("economy.leaderboardEnabled", "false");
            if (c.mc.gui.screen() instanceof AdminPanelScreen s) s.showTab(0, "");
        }), 15));
        // the Blood Moon decorations (-Pphoto=blooddecor): all eleven in a row on their own square (bloodDecorScene), by
        // day and night, close up in threes, the coffin opened, from the side
        SHOTS.add(new Shot("blood_row_day", c -> {
            day(c);
            c.server.execute(() -> {
                com.alexh.capitalism.decor.Decorations.get(c.server).setMode(com.alexh.capitalism.decor.Decorations.Mode.DOWN);
                bloodDecorScene(c.level);
            });
            stand(c, 101.0, DY + 4.0, 0.0, 118.0, DY + 1.0, 0.0);
        }, 100));
        // each close up (bloodClose: its index in the row, how far back and how high the camera stands, what it looks at)
        bloodClose("lantern", 0, 2.8, 0.4, 0.5);
        bloodClose("candelabra", 1, 2.8, 0.5, 0.65);
        bloodClose("throne", 2, 4.0, 1.0, 1.3);
        bloodClose("coffin", 3, 3.4, 2.0, 0.2);
        bloodClose("fountain", 4, 4.4, 1.6, 0.5);
        bloodClose("banner", 5, 4.6, 1.2, 1.6);
        bloodClose("rug", 6, 3.4, 2.0, 0.0);
        bloodClose("sword", 7, 4.0, 1.0, 1.4);
        bloodClose("skull", 8, 2.6, 0.9, 1.5);
        bloodClose("staff", 9, 3.0, 0.5, 0.8);
        bloodClose("gargoyle", 10, 2.6, 0.3, 0.6);
        SHOTS.add(new Shot("blood_side", c -> stand(c, 110.0, DY + 3.0, -19.0, 118.0, DY + 1.0, -8.0), 40));
        SHOTS.add(new Shot("blood_banner_back", c -> stand(c, 122.5, DY + 1.0, 1.5, 118.5, DY + 1.6, 0.5), 40));
        // sitting on the throne (seen from in front of it)
        SHOTS.add(new Shot("blood_throne_sit", c -> {
            stand(c, 116.5, DY, -8.5, 118.5, DY + 1.0, -8.5);
            c.server.execute(() -> com.alexh.capitalism.decor.DecorSeat.sit(c.level, new BlockPos(118, DY, -9), c.player,
                net.minecraft.world.phys.Vec3.atBottomCenterOf(new BlockPos(118, DY, -9)).add(-0.06, 0.66, 0.0), net.minecraft.core.Direction.WEST));
            look(c, 90.0F, 15.0F, CameraType.THIRD_PERSON_FRONT);
        }, 40));
        SHOTS.add(new Shot("blood_throne_sit_side", c -> look(c, 150.0F, 10.0F, CameraType.THIRD_PERSON_FRONT), 30));
        SHOTS.add(new Shot("blood_coffin_open", c -> {
            c.server.execute(() -> c.player.stopRiding());
            c.server.execute(() -> {
                BlockPos head = new BlockPos(119, DY, -6);
                var state = c.level.getBlockState(head);
                if (state.getBlock() instanceof com.alexh.capitalism.decor.BloodBedBlock) {
                    c.level.setBlock(head, state.setValue(com.alexh.capitalism.decor.BloodBedBlock.OPEN, true), 3);
                }
            });
            stand(c, 115.5, DY + 2.6, -8.0, 119.0, DY + 0.2, -5.5);
        }, 60));
        // the decorations as items (BloodDecorItemRenderer): in the inventory and hotbar, in hand, in item frames, on the ground
        SHOTS.add(new Shot("blood_items_inventory", c -> {
            day(c);
            c.hud = true;
            command(c, "gamemode survival @a");
            c.server.execute(() -> {
                bloodDecorScene(c.level);
                c.player.getInventory().clearContent();
                var items = bloodItems();
                for (int i = 0; i < items.size(); i++) c.player.getInventory().setItem(i < 9 ? i : i, new net.minecraft.world.item.ItemStack(items.get(i)));
                c.player.getInventory().setSelectedSlot(0);
            });
            stand(c, 104.0, DY, 10.0, 104.0, DY + 1.6, 14.0);
        }, 30));
        SHOTS.add(new Shot("blood_items_screen", c -> c.mc.execute(() -> c.mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.mc.player))), 30));
        // the Blood Merchant's Curios tab with these in it (each drawn at 2x on its card)
        SHOTS.add(new Shot("blood_items_curios", c -> {
            night(c);
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                var m = new com.alexh.capitalism.town.BloodMerchant(com.alexh.capitalism.core.ModEntities.BLOOD_MERCHANT.get(), c.level);
                m.snapTo(104.5, DY, 14.5, 180.0F, 0.0F);
                c.level.addFreshEntity(m);
                c.player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) -> new com.alexh.capitalism.town.BloodMerchantMenu(id, inv,
                    com.alexh.capitalism.town.BloodMerchantMenu.liveData(c.player), m), net.minecraft.network.chat.Component.literal("The Blood Merchant")));
            });
        }, 30));
        SHOTS.add(new Shot("blood_items_hand_lantern", c -> {
            day(c);
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            c.server.execute(() -> c.player.getInventory().setSelectedSlot(0));
        }, 20));
        SHOTS.add(new Shot("blood_items_hand_throne", c -> c.server.execute(() -> c.player.getInventory().setSelectedSlot(2)), 20));
        SHOTS.add(new Shot("blood_items_hand_fountain", c -> c.server.execute(() -> c.player.getInventory().setSelectedSlot(4)), 20));
        SHOTS.add(new Shot("blood_items_third", c -> {
            c.server.execute(() -> c.player.getInventory().setSelectedSlot(7));
            look(c, 30.0F, 15.0F, CameraType.THIRD_PERSON_FRONT);
        }, 20));
        SHOTS.add(new Shot("blood_items_frames", c -> {
            c.hud = false;
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                var wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                for (int z = 4; z <= 16; z++) for (int y = 0; y < 3; y++) c.level.setBlock(new BlockPos(110, DY + y, z), wall, 3);
                var items = bloodItems();
                for (int i = 0; i < 12; i++) {
                    var frame = new net.minecraft.world.entity.decoration.ItemFrame(c.level, new BlockPos(109, DY + (i < 6 ? 1 : 2), 5 + (i % 6) * 2), net.minecraft.core.Direction.WEST);
                    frame.setItem(new net.minecraft.world.item.ItemStack(items.get(i < 6 ? i * 2 : 8 + (i - 6) * 2)), false);
                    c.level.addFreshEntity(frame);
                }
                for (int i = 0; i < 5; i++) {
                    var drop = new net.minecraft.world.entity.item.ItemEntity(c.level, 106.5, DY + 0.1, 6.5 + i * 2.0,
                        new net.minecraft.world.item.ItemStack(items.get(i * 4 + 1)), 0, 0, 0);
                    drop.setNeverPickUp();
                    drop.setUnlimitedLifetime();
                    c.level.addFreshEntity(drop);
                }
            });
            stand(c, 103.0, DY + 1.2, 10.5, 110.0, DY + 1.4, 10.5);
        }, 40));
        SHOTS.add(new Shot("blood_items_frames_close", c -> stand(c, 106.6, DY + 0.8, 8.0, 110.0, DY + 1.6, 8.0), 30));
        // the second ten: by day, close up, in their states
        SHOTS.add(new Shot("blood_row2_day", c -> stand(c, 101.0, DY + 4.0, 35.5, 118.0, DY + 1.0, 35.5), 60));
        bloodClose("clock", 11, 3.6, 1.2, 1.3);
        bloodClose("clock_face", 11, 1.6, 1.4, 1.95);
        bloodClose("chandelier", 12, 3.0, 0.6, 2.3);
        bloodClose("organ", 13, 4.6, 1.4, 1.4);
        bloodClose("bookshelf", 14, 3.4, 0.8, 1.0);
        bloodClose("portrait", 15, 2.6, 0.9, 1.9);
        bloodClose("maiden", 16, 3.2, 0.8, 1.0);
        bloodClose("altar", 17, 3.2, 1.0, 0.9);
        bloodClose("roost", 18, 3.2, 0.9, 1.3);
        bloodClose("bed", 19, 4.0, 1.8, 1.0);
        bloodClose("fireplace", 20, 4.0, 1.2, 1.1);
        SHOTS.add(new Shot("blood_states", c -> c.server.execute(() -> {
            BlockPos organ = new BlockPos(118, DY, (int) Math.floor(bloodZ(13)));
            if (c.level.getBlockEntity(organ) instanceof com.alexh.capitalism.decor.BloodDecorBlockEntity be) be.toggleOrgan(c.level, organ, c.level.getBlockState(organ));
            BlockPos shelf = new BlockPos(118, DY, (int) Math.floor(bloodZ(14)));
            com.alexh.capitalism.decor.BloodDoorBlock.swing(c.level, shelf, c.level.getBlockState(shelf), true);
            BlockPos maiden = new BlockPos(118, DY, (int) Math.floor(bloodZ(16)));
            c.level.setBlock(maiden, c.level.getBlockState(maiden).setValue(com.alexh.capitalism.decor.BloodDecorBlock.LIT, true), 3);
            BlockPos fire = new BlockPos(118, DY, (int) Math.floor(bloodZ(20)));
            if (c.level.getBlockEntity(fire) instanceof com.alexh.capitalism.decor.BloodDecorBlockEntity fb) fb.addFuel(36000);
            com.alexh.capitalism.decor.BloodDecorBlock.setLit(c.level, fire, c.level.getBlockState(fire), true);
            for (int x = 118; x <= 119; x++) {
                BlockPos bed = new BlockPos(x, DY, (int) Math.floor(bloodZ(19)));
                c.level.setBlock(bed, c.level.getBlockState(bed).setValue(com.alexh.capitalism.decor.BloodBedBlock.OCCUPIED, true), 3);
            }
        }), 10));
        bloodClose("organ_playing", 13, 3.6, 1.2, 1.2);
        SHOTS.add(new Shot("blood_bookshelf_open", c -> stand(c, 115.0, DY + 1.2, bloodZ(14) + 2.2, 118.5, DY + 1.0, bloodZ(14)), 30));
        bloodClose("maiden_open", 16, 2.8, 0.8, 1.0);
        bloodClose("bed_curtains", 19, 4.0, 1.8, 1.0);
        bloodClose("fireplace_lit", 20, 4.0, 1.2, 1.1);
        SHOTS.add(new Shot("blood_altar_vision", c -> {
            c.server.execute(() -> com.alexh.capitalism.decor.BloodAltar.offer(c.level, new BlockPos(118, DY, (int) Math.floor(bloodZ(17))), c.player,
                new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.BLOOD_SHARD.get(), 40)));
            stand(c, 114.0, DY + 1.0, bloodZ(17), 118.5, DY + 4.0, bloodZ(17));
        }, 80));
        SHOTS.add(new Shot("blood_row_night", c -> {
            night(c);
            stand(c, 101.0, DY + 4.0, 0.0, 118.0, DY + 1.0, 0.0);
        }, 60));
        bloodNight("lantern", 0, 2.8, 0.4, 0.5);
        bloodNight("candelabra", 1, 2.8, 0.5, 0.65);
        bloodNight("fountain", 4, 4.4, 1.6, 0.5);
        bloodNight("banner", 5, 4.6, 1.2, 1.6);
        bloodNight("sword", 7, 4.0, 1.0, 1.4);
        bloodNight("staff", 9, 3.0, 0.5, 0.8);
        bloodNight("gargoyle", 10, 2.6, 0.3, 0.6);
        SHOTS.add(new Shot("blood_row2_night", c -> stand(c, 101.0, DY + 4.0, 35.5, 118.0, DY + 1.0, 35.5), 40));
        bloodNight("chandelier", 12, 3.0, 0.6, 2.3);
        bloodNight("portrait", 15, 2.6, 0.9, 1.9);
        bloodNight("roost", 18, 4.2, 1.4, 1.6);
        bloodNight("fireplace", 20, 4.0, 1.2, 1.1);
        bloodNight("altar", 17, 3.2, 1.0, 0.9);
        bloodNight("organ", 13, 4.6, 1.4, 1.4);
        // lying in the open coffin at night
        SHOTS.add(new Shot("blood_coffin_sleep", c -> {
            stand(c, 117.5, DY, -5.5, 119.0, DY, -5.5);
            c.server.execute(() -> {
                BlockPos head = new BlockPos(119, DY, -6);
                var state = c.level.getBlockState(head);
                if (state.getBlock() instanceof com.alexh.capitalism.decor.BloodBedBlock bed) {
                    c.player.startSleepInBed(bed, state, bed.getBedRule(c.level, head), head)
                        .ifLeft(p -> LOG.info("[photo] couldn't sleep: {}", p.message() == null ? "?" : p.message().getString()));
                }
            });
            look(c, 0.0F, 40.0F, CameraType.THIRD_PERSON_BACK);
        }, 40));
        SHOTS.add(new Shot("blood_moon_night", c -> {
            c.server.execute(() -> c.player.stopSleeping());
            command(c, "event bloodmoon start");
            stand(c, 104.0, DY + 2.5, -3.0, 118.0, DY + 1.5, 2.0);
        }, 80));
        // the banner up close in a dark stone room, from low and to the side (-Pphoto=bloodbanner): where it used to
        // flicker in stripes
        SHOTS.add(new Shot("bannertest_room", c -> {
            night(c);
            c.server.execute(() -> {
                bloodDecorScene(c.level);
                var stone = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                for (int x = 113; x <= 123; x++) for (int z = -5; z <= 5; z++) for (int y = DY - 1; y <= DY + 5; y++) {
                    boolean shell = x == 113 || x == 123 || Math.abs(z) == 5 || y == DY - 1 || y == DY + 5;
                    BlockPos p = new BlockPos(x, y, z);
                    if (shell) c.level.setBlock(p, stone, 2);
                    else if (!(c.level.getBlockState(p).getBlock() instanceof com.alexh.capitalism.decor.BloodDecorKind.Holder)) c.level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                }
            });
            stand(c, 116.2, DY, 0.6, 118.5, DY + 2.4, 0.5);
        }, 80));
        SHOTS.add(new Shot("bannertest_low_left", c -> stand(c, 116.6, DY, 2.0, 118.5, DY + 2.3, 0.4), 30));
        SHOTS.add(new Shot("bannertest_low_right", c -> stand(c, 116.6, DY, -1.0, 118.5, DY + 2.3, 0.6), 30));
        SHOTS.add(new Shot("bannertest_side", c -> stand(c, 118.3, DY, 2.8, 118.5, DY + 2.0, 0.5), 30));
        SHOTS.add(new Shot("bannertest_back", c -> stand(c, 120.6, DY, -0.6, 118.5, DY + 2.3, 0.5), 30));
        SHOTS.add(new Shot("bannertest_far", c -> stand(c, 114.5, DY + 0.8, 0.5, 118.5, DY + 1.8, 0.5), 30));
        SHOTS.add(new Shot("clock_hud", c -> {
            day(c);
            c.hud = true;
            // Winter 25 (the Winter Feast), 8:40 am
            c.server.execute(() -> com.alexh.capitalism.calendar.Calendar.setClock(c.level,
                com.alexh.capitalism.calendar.Calendar.startOf(1, com.alexh.capitalism.calendar.Calendar.Season.WINTER, 25) + 2700L));
            stand(c, 43.0, DY + 3.0, 0.0, 60.0, DY + 3.0, 0.0);
        }, 40));
    }

    // the Blood Merchant (town/BloodMerchant): out at night, his two tabs, gone by day (-Pphoto=merchant)
    static {
        SHOTS.add(new Shot("merch_front", c -> {
            night(c);
            c.server.execute(() -> {
                var m = new com.alexh.capitalism.town.BloodMerchant(com.alexh.capitalism.core.ModEntities.BLOOD_MERCHANT.get(), c.level);
                m.snapTo(56.5, DY, 4.5, 180.0F, 0.0F);
                c.level.addFreshEntity(m);
                c.player.getInventory().add(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.BLOOD_SHARD.get(), 64));
                c.player.getInventory().add(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.GOREFANG_CREST.get()));
                c.player.getInventory().add(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.BLOOD_MOON_CREST.get()));
            });
            stand(c, 56.5, DY + 1.6, 1.5, 56.5, DY + 1.5, 4.5);
        }, 60));
        SHOTS.add(new Shot("merch_side", c -> stand(c, 59.0, DY + 1.8, 2.5, 56.5, DY + 1.2, 4.5), 20));
        SHOTS.add(new Shot("merch_close", c -> stand(c, 56.5, DY + 1.75, 3.0, 56.5, DY + 1.7, 4.5), 20));
        SHOTS.add(new Shot("merch_curios", c -> {
            stand(c, 56.5, DY + 1.6, 1.5, 56.5, DY + 1.5, 4.5);
            c.server.execute(() -> {
                for (var m : c.level.getEntitiesOfClass(com.alexh.capitalism.town.BloodMerchant.class, new net.minecraft.world.phys.AABB(50, DY - 2, -2, 64, DY + 4, 10))) {
                    c.player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) -> new com.alexh.capitalism.town.BloodMerchantMenu(id, inv,
                        com.alexh.capitalism.town.BloodMerchantMenu.liveData(c.player), m), net.minecraft.network.chat.Component.literal("The Blood Merchant")));
                }
            });
        }, 15));
        SHOTS.add(new Shot("merch_mend", c -> {
            c.server.execute(() -> {
                if (c.player.containerMenu instanceof com.alexh.capitalism.town.BloodMerchantMenu menu) {
                    menu.slots.get(0).set(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.REVENANT_CREST.get()));
                    menu.slots.get(1).set(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.SANGUINE_CREST.get()));
                    menu.broadcastChanges();
                }
            });
            if (Minecraft.getInstance().gui.screen() instanceof BloodMerchantScreen screen) screen.showTab(1);
        }, 15));
        SHOTS.add(new Shot("merch_day", c -> {
            c.server.execute(() -> c.player.closeContainer());
            day(c);
            stand(c, 56.5, DY + 1.6, 1.5, 56.5, DY + 1.5, 4.5);
        }, 50));
    }

    // the Traveling Merchant (town/TravelingMerchant) and his wagon, and his shop (-Pphoto=traveler)
    static {
        SHOTS.add(new Shot("trav_front", c -> {
            day(c);
            c.server.execute(() -> {
                com.alexh.capitalism.town.TravelingMerchantShop.arrive(c.server);
                var m = new com.alexh.capitalism.town.TravelingMerchant(com.alexh.capitalism.core.ModEntities.TRAVELING_MERCHANT.get(), c.level);
                m.placeAt(new net.minecraft.world.phys.Vec3(70.5, DY, 6.5), 180.0F);
                c.level.addFreshEntity(m);
                c.player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_BLOCK, 40));
            });
            stand(c, 69.0, DY + 2.2, -1.0, 69.5, DY + 1.2, 6.5);
        }, 60));
        SHOTS.add(new Shot("trav_wagon", c -> stand(c, 64.0, DY + 3.0, 1.0, 69.5, DY + 1.0, 7.0), 20));
        SHOTS.add(new Shot("trav_back", c -> stand(c, 66.0, DY + 2.5, 13.5, 69.0, DY + 1.0, 6.5), 20));
        SHOTS.add(new Shot("trav_close", c -> stand(c, 70.5, DY + 1.7, 4.0, 70.5, DY + 1.5, 6.5), 20));
        SHOTS.add(new Shot("trav_goods", c -> {
            c.server.execute(() -> {
                c.level.setBlockAndUpdate(new net.minecraft.core.BlockPos(66, DY, 2), com.alexh.capitalism.core.ModBlocks.TRAVELERS_SNOW_GLOBE.get().defaultBlockState());
                c.level.setBlockAndUpdate(new net.minecraft.core.BlockPos(67, DY, 2), com.alexh.capitalism.core.ModBlocks.TRAVELERS_LANTERN.get().defaultBlockState());
                c.level.setBlockAndUpdate(new net.minecraft.core.BlockPos(68, DY, 2), com.alexh.capitalism.core.ModBlocks.TRAVELERS_LANTERN.get().defaultBlockState()
                    .setValue(com.alexh.capitalism.town.TravelerBlocks.Lantern.LIT, true));
                var inv = c.player.getInventory();
                inv.setItem(0, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.BOTTOMLESS_WATERSKIN.get()));
                inv.setItem(1, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.HAND_WARMER.get()));
                inv.setItem(2, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.SMITHS_CHISEL.get()));
                inv.setItem(3, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.PHOENIX_FEATHER.get()));
                inv.setItem(4, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.TRAVELERS_SNOW_GLOBE.get()));
                inv.setItem(5, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.TRAVELERS_LANTERN.get()));
            });
            stand(c, 67.0, DY + 1.0, 0.3, 67.0, DY + 0.3, 2.5);
        }, 30));
        // the cosmetic hats, worn (third person, from the front)
        String[] hats = {"chefs_hat", "welders_mask", "wizards_hat", "farmers_hat", "travelers_pipe"};
        for (String hat : hats) {
            SHOTS.add(new Shot("trav_hat_" + hat, c -> {
                c.server.execute(() -> c.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, hat)))));
                look(c, 200.0F, 10.0F, CameraType.THIRD_PERSON_FRONT);
            }, 20));
        }
        SHOTS.add(new Shot("trav_hat_done", c -> {
            c.server.execute(() -> c.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY));
            look(c, 0.0F, 0.0F, CameraType.FIRST_PERSON);
        }, 5));
        SHOTS.add(new Shot("trav_shop", c -> {
            stand(c, 70.5, DY + 1.6, 4.0, 70.5, DY + 1.5, 6.5);
            c.server.execute(() -> {
                for (var m : c.level.getEntitiesOfClass(com.alexh.capitalism.town.TravelingMerchant.class, new net.minecraft.world.phys.AABB(60, DY - 2, 0, 80, DY + 4, 12))) {
                    com.alexh.capitalism.town.TravelingMerchantMenu.open(c.player, m);
                }
            });
        }, 20));
    }

    // fishing (Fishing.md, -Pphoto=fish): every rare catch in item frames, a pirate chest opened (its loot, the hat worn),
    // the Fisherman and his two tabs, the plaques 1, 2 and 3 wide on a wall
    private static final int FX = 200;

    private static java.util.List<net.minecraft.world.item.ItemStack> fishItems() {
        var out = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        var random = net.minecraft.util.RandomSource.create(5L);
        for (var rare : com.alexh.capitalism.fishing.RareCatch.values()) {
            out.add(com.alexh.capitalism.fishing.RareCatchModifier.make(rare, "Alex", "Summer 9, Year 2", random));
        }
        out.add(com.alexh.capitalism.fishing.RareFindItems.MessageInABottle.note(random));
        out.add(new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.PIRATE_HAT.get()));
        out.add(com.alexh.capitalism.fishing.FishMarket.plaqueFor(out.get(com.alexh.capitalism.fishing.RareCatch.MAKO_SHARK.ordinal())));
        return out;
    }

    private static void hangPlaque(ServerLevel level, BlockPos pos, com.alexh.capitalism.fishing.RareCatch rare, String catcher) {
        var stack = com.alexh.capitalism.fishing.RareCatchModifier.make(rare, catcher, "Autumn 14, Year 1", net.minecraft.util.RandomSource.create(rare.ordinal()));
        var plaque = com.alexh.capitalism.fishing.FishMarket.plaqueFor(stack);
        var state = com.alexh.capitalism.core.ModBlocks.FISH_PLAQUE.get().defaultBlockState()
            .setValue(com.alexh.capitalism.fishing.FishPlaqueBlock.FACING, net.minecraft.core.Direction.WEST);
        level.setBlock(pos, state, 3);
        if (level.getBlockEntity(pos) instanceof com.alexh.capitalism.fishing.FishPlaqueBlockEntity be) {
            be.set(rare, stack.get(com.alexh.capitalism.core.ModComponents.CATCH_INFO.get()));
            level.sendBlockUpdated(pos, state, state, 3);
        }
        state.getBlock().setPlacedBy(level, pos, state, null, plaque);
    }

    static {
        SHOTS.add(new Shot("fish_items_frames", c -> {
            day(c);
            c.hud = false;
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                var wall = Blocks.SPRUCE_PLANKS.defaultBlockState();
                // a floor under the whole fishing scene (the player is in survival for some of it)
                for (int x = FX - 4; x <= FX + 11; x++) for (int z = -3; z <= 56; z++) c.level.setBlock(new BlockPos(x, DY - 1, z), Blocks.SMOOTH_SANDSTONE.defaultBlockState(), 2);
                for (int z = -1; z <= 9; z++) for (int y = 0; y < 3; y++) c.level.setBlock(new BlockPos(FX + 10, DY + y, z), wall, 3);
                var items = fishItems();
                for (int i = 0; i < items.size(); i++) {
                    var frame = new net.minecraft.world.entity.decoration.ItemFrame(c.level, new BlockPos(FX + 9, DY + (i < 9 ? 2 : 1), i % 9), net.minecraft.core.Direction.WEST);
                    frame.setItem(items.get(i), false);
                    c.level.addFreshEntity(frame);
                }
            });
            stand(c, FX + 4.2, DY + 0.1, 4.5, FX + 10.0, DY + 1.9, 4.5);
        }, 40));
        SHOTS.add(new Shot("fish_items_frames_left", c -> stand(c, FX + 6.6, DY + 0.2, 2.0, FX + 10.0, DY + 1.9, 2.0), 25));
        SHOTS.add(new Shot("fish_items_frames_right", c -> stand(c, FX + 6.6, DY + 0.2, 6.5, FX + 10.0, DY + 1.9, 6.5), 25));
        // a Sunken Pirate Chest cracked open: what came out, and the hat on
        SHOTS.add(new Shot("fish_pirate_loot", c -> {
            c.hud = true;
            command(c, "gamemode survival @a");
            c.server.execute(() -> {
                c.player.getInventory().clearContent();
                c.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
                    new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.RARE_CATCHES.get(com.alexh.capitalism.fishing.RareCatch.SUNKEN_PIRATE_CHEST).get(), 1));
                // a chest with the hat in it (the first seed that has one)
                for (long seed = 1; seed < 500; seed++) {
                    var loot = com.alexh.capitalism.fishing.RareFindItems.PirateChest.loot(net.minecraft.util.RandomSource.create(seed));
                    if (loot.stream().anyMatch(s -> s.is(com.alexh.capitalism.core.ModItems.PIRATE_HAT.get()))
                        && loot.stream().anyMatch(s -> s.is(net.minecraft.world.item.Items.EMERALD) || s.is(net.minecraft.world.item.Items.DIAMOND))) {
                        c.player.getMainHandItem().shrink(1);
                        for (var s : loot) c.player.getInventory().add(s);
                        break;
                    }
                }
            });
            stand(c, FX + 2.5, DY, 14.5, FX + 2.5, DY + 1.6, 18.5);
        }, 30));
        SHOTS.add(new Shot("fish_pirate_loot_screen", c -> c.mc.execute(() -> c.mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.mc.player))), 30));
        SHOTS.add(new Shot("fish_pirate_hat_worn", c -> {
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            c.hud = false;
            c.server.execute(() -> {
                var inv = c.player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (inv.getItem(i).is(com.alexh.capitalism.core.ModItems.PIRATE_HAT.get())) {
                        c.player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, inv.removeItemNoUpdate(i));
                        break;
                    }
                }
                c.player.getInventory().setSelectedSlot(1);
            });
            look(c, 200.0F, 10.0F, CameraType.THIRD_PERSON_FRONT);
        }, 25));
        SHOTS.add(new Shot("fish_pirate_hat_side", c -> {
            command(c, "execute as @a at @s run tp @s ~ ~ ~ 110 5");
            look(c, 110.0F, 5.0F, CameraType.THIRD_PERSON_FRONT);
        }, 20));
        SHOTS.add(new Shot("fish_pirate_hat_back", c -> {
            command(c, "execute as @a at @s run tp @s ~ ~ ~ 20 15");
            look(c, 20.0F, 15.0F, CameraType.THIRD_PERSON_BACK);
        }, 20));
        // the Fisherman on his dock
        SHOTS.add(new Shot("fish_npc_front", c -> {
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                var planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
                for (int x = FX - 2; x <= FX + 2; x++) for (int z = 26; z <= 31; z++) c.level.setBlock(new BlockPos(x, DY - 1, z), planks, 3);
                var f = new com.alexh.capitalism.town.Fisherman(ModEntities.FISHERMAN.get(), c.level);
                f.placeAt(new net.minecraft.world.phys.Vec3(FX + 0.5, DY, 30.5), 180.0F);
                c.level.addFreshEntity(f);
                c.player.getInventory().clearContent();
                for (var s : fishItems()) c.player.getInventory().add(s);
            });
            look(c, 0.0F, 0.0F, CameraType.FIRST_PERSON);
            stand(c, FX + 0.5, DY, 27.6, FX + 0.5, DY + 1.5, 30.5);
        }, 60));
        SHOTS.add(new Shot("fish_npc_side", c -> stand(c, FX + 2.6, DY + 0.2, 28.4, FX + 0.5, DY + 1.3, 30.5), 25));
        SHOTS.add(new Shot("fish_npc_close", c -> stand(c, FX + 0.5, DY + 0.15, 29.2, FX + 0.5, DY + 1.65, 30.5), 25));
        SHOTS.add(new Shot("fish_npc_back", c -> stand(c, FX + 1.2, DY + 0.2, 32.6, FX + 0.5, DY + 1.3, 30.5), 25));
        SHOTS.add(new Shot("fish_shop_sell", c -> {
            c.hud = true;
            stand(c, FX + 0.5, DY, 27.6, FX + 0.5, DY + 1.5, 30.5);
            c.server.execute(() -> {
                for (var f : c.level.getEntitiesOfClass(com.alexh.capitalism.town.Fisherman.class, new net.minecraft.world.phys.AABB(FX - 5, DY - 2, 24, FX + 5, DY + 4, 36))) {
                    f.setCustomer(c.player);
                    c.player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) -> new com.alexh.capitalism.town.FishermanMenu(id, inv, f),
                        net.minecraft.network.chat.Component.literal("The Fisherman")));
                }
            });
        }, 15));
        SHOTS.add(new Shot("fish_shop_sell_filled", c -> c.server.execute(() -> {
            if (c.player.containerMenu instanceof com.alexh.capitalism.town.FishermanMenu menu) {
                var items = fishItems();
                int[] pick = {0, 1, 4, 6, 9, 11, 15};
                for (int i = 0; i < pick.length; i++) menu.slots.get(i).set(items.get(pick[i]));
                menu.broadcastChanges();
            }
        }), 15));
        SHOTS.add(new Shot("fish_shop_mount", c -> {
            c.server.execute(() -> {
                if (c.player.containerMenu instanceof com.alexh.capitalism.town.FishermanMenu menu) {
                    menu.slots.get(com.alexh.capitalism.town.FishermanMenu.MOUNT_SLOT).set(fishItems().get(com.alexh.capitalism.fishing.RareCatch.OARFISH.ordinal()));
                    menu.broadcastChanges();
                }
            });
            if (Minecraft.getInstance().gui.screen() instanceof FishermanScreen screen) screen.showTab(1);
        }, 15));
        // the Collection tab: partly filled, then complete (the Claim button), then claimed
        SHOTS.add(new Shot("fish_collection_partial", c -> {
            c.server.execute(() -> {
                com.alexh.capitalism.fishing.CatchCollection.reset(c.player);
                var r = com.alexh.capitalism.fishing.RareCatch.values();
                int[] pick = {0, 1, 3, 5, 7, 9, 12};
                for (int i : pick) com.alexh.capitalism.fishing.CatchCollection.record(c.player, r[i], r[i].weighed() ? (r[i].minLb + r[i].maxLb) / 2 : 0);
                com.alexh.capitalism.fishing.CatchCollection.record(c.player, r[1], r[1].maxLb * 0.9F);
            });
            if (Minecraft.getInstance().gui.screen() instanceof FishermanScreen screen) screen.showTab(2);
        }, 20));
        SHOTS.add(new Shot("fish_collection_complete", c -> c.server.execute(() -> {
            for (var r : com.alexh.capitalism.fishing.RareCatch.values()) {
                if (!com.alexh.capitalism.fishing.CatchCollection.of(c.player).has(r)) com.alexh.capitalism.fishing.CatchCollection.record(c.player, r, r.weighed() ? r.maxLb * 0.8F : 0);
            }
        }), 20));
        SHOTS.add(new Shot("fish_collection_claimed", c -> c.server.execute(() -> com.alexh.capitalism.fishing.CatchCollection.claim(c.player)), 30));
        // the plaques on a wall (facing west): the Oarfish 3 wide (centred), the Mako and the Kraken Tentacle 2, the rest 1
        SHOTS.add(new Shot("fish_plaques", c -> {
            c.server.execute(() -> c.player.closeContainer());
            c.hud = false;
            c.server.execute(() -> {
                var wall = Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState();
                for (int z = 38; z <= 54; z++) for (int y = 0; y < 5; y++) c.level.setBlock(new BlockPos(FX + 10, DY + y, z), wall, 3);
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 3, 40), com.alexh.capitalism.fishing.RareCatch.OARFISH, "Alex");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 3, 43), com.alexh.capitalism.fishing.RareCatch.MAKO_SHARK, "Captain Rook");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 3, 46), com.alexh.capitalism.fishing.RareCatch.KRAKEN_TENTACLE, "Mira");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 3, 49), com.alexh.capitalism.fishing.RareCatch.SWORDFISH, "Alex");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 39), com.alexh.capitalism.fishing.RareCatch.GOLDEN_KOI, "Jo");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 41), com.alexh.capitalism.fishing.RareCatch.ANGLERFISH, "Mira");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 43), com.alexh.capitalism.fishing.RareCatch.GOLDEN_LEVIATHAN_SCALE, "Alex");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 45), com.alexh.capitalism.fishing.RareCatch.GHOST_KOI, "Sam");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 47), com.alexh.capitalism.fishing.RareCatch.PUFFERFISH_KING, "Jo");
                hangPlaque(c.level, new BlockPos(FX + 9, DY + 2, 49), com.alexh.capitalism.fishing.RareCatch.OCEAN_SUNFISH, "Sam");
            });
            stand(c, FX + 2.0, DY + 0.6, 45.0, FX + 10.0, DY + 2.0, 45.0);
        }, 50));
        SHOTS.add(new Shot("fish_plaque_oarfish", c -> stand(c, FX + 6.4, DY + 1.2, 40.5, FX + 9.5, DY + 2.5, 40.5), 25));
        SHOTS.add(new Shot("fish_plaque_mako", c -> stand(c, FX + 7.2, DY + 1.2, 44.0, FX + 9.5, DY + 2.5, 44.0), 25));
        SHOTS.add(new Shot("fish_plaque_kraken", c -> stand(c, FX + 7.2, DY + 1.2, 47.0, FX + 9.5, DY + 2.5, 47.0), 25));
        SHOTS.add(new Shot("fish_plaque_small", c -> stand(c, FX + 7.4, DY + 0.0, 42.0, FX + 9.5, DY + 1.5, 42.0), 25));
        SHOTS.add(new Shot("fish_plaque_nameplate", c -> stand(c, FX + 8.3, DY + 0.0, 39.5, FX + 9.6, DY + 1.3, 39.5), 25));
        SHOTS.add(new Shot("fish_plaque_angle", c -> stand(c, FX + 6.0, DY + 1.5, 36.0, FX + 9.5, DY + 2.0, 44.0), 25));
        // from the side and from above: the catches stand out of their boards in 3D
        SHOTS.add(new Shot("fish_plaque_side_mako", c -> stand(c, FX + 6.4, DY + 1.4, 41.2, FX + 9.3, DY + 2.5, 44.2), 25));
        SHOTS.add(new Shot("fish_plaque_side_small", c -> stand(c, FX + 7.0, DY + 0.4, 37.6, FX + 9.3, DY + 1.5, 41.5), 25));
        SHOTS.add(new Shot("fish_plaque_above", c -> stand(c, FX + 6.6, DY + 3.4, 44.0, FX + 9.3, DY + 2.2, 44.0), 25));
        SHOTS.add(new Shot("fish_plaque_scale", c -> stand(c, FX + 7.3, DY + 0.6, 42.4, FX + 9.5, DY + 1.7, 43.5), 25));
        SHOTS.add(new Shot("fish_plaque_puffer", c -> stand(c, FX + 7.4, DY + 0.5, 46.6, FX + 9.5, DY + 1.6, 47.5), 25));
        SHOTS.add(new Shot("fish_plaque_ghost", c -> stand(c, FX + 7.6, DY + 0.5, 44.8, FX + 9.5, DY + 1.6, 45.5), 25));
        SHOTS.add(new Shot("fish_plaque_kraken_front", c -> stand(c, FX + 5.8, DY + 2.2, 47.0, FX + 9.5, DY + 2.5, 47.0), 25));
        SHOTS.add(new Shot("fish_plaque_scale_night", c -> {
            night(c);
            stand(c, FX + 7.0, DY + 0.8, 43.5, FX + 9.5, DY + 1.6, 43.5);
        }, 40));
        // at night, each tier and width straight on and from below (the nameplates must read): name, block z of the
        // plaque's middle, its row (1 or 2 above the floor), how far back to stand
        Object[][] night = {
            {"oarfish", 40.5, 3, 4.4}, {"mako", 44.0, 3, 3.4}, {"kraken", 47.0, 3, 3.4}, {"swordfish", 50.0, 3, 3.4},
            {"koi", 39.5, 2, 2.6}, {"angler", 41.5, 2, 2.6}, {"scale", 43.5, 2, 2.6}, {"ghost", 45.5, 2, 2.6}, {"puffer", 47.5, 2, 2.6},
            {"sunfish", 50.0, 2, 3.4}};
        for (Object[] p : night) {
            String name = (String) p[0];
            double z = (Double) p[1], back = (Double) p[3];
            int row = (Integer) p[2];
            SHOTS.add(new Shot("fish_night_" + name, c -> {
                night(c);
                stand(c, FX + 9.9 - back, DY + row + 0.4 - 1.62, z, FX + 9.9, DY + row + 0.4, z);
            }, 30));
            SHOTS.add(new Shot("fish_night_" + name + "_below", c -> stand(c, FX + 9.9 - back, DY + (row - 2) * 0.6, z, FX + 9.9, DY + row + 0.3, z), 25));
        }
        SHOTS.add(new Shot("fish_plaque_kraken_side", c -> stand(c, FX + 6.2, DY + 2.2, 51.0, FX + 9.0, DY + 2.4, 47.0), 25));
    }

    // the General Guide (-Pphoto=guide): the Rules, each Wiki tab (its longest entry), Crafting; logs any entry too long
    static {
        String[][] views = {{"START", null, "0"}, {"START", null, "2"}, {"START", null, "3"}, {"START", null, "5"}, {"RULES", null, "0"}, {"WIKI", "GOLD", "9"}, {"WIKI", "GOLD", "3"}, {"WIKI", "INSURANCE", "4"}, {"WIKI", "INSURANCE", "2"}, {"WIKI", "TOWN", "4"}, {"WIKI", "EVENTS", "0"},
            {"WIKI", "JOBS", "0"}, {"WIKI", "JOBS", "3"}, {"WIKI", "WELLNESS", "4"}, {"WIKI", "WELLNESS", "5"}, {"CRAFTING", "BLOCKS", "0"}, {"CRAFTING", "ITEMS", "2"}};
        for (String[] v : views) {
            SHOTS.add(new Shot("guide_" + v[0].toLowerCase() + (v[1] == null ? "" : "_" + v[1].toLowerCase()) + "_" + v[2], c -> c.mc.execute(() -> {
                c.hud = true;
                GeneralGuideScreen screen = c.mc.gui.screen() instanceof GeneralGuideScreen open ? open : new GeneralGuideScreen(null);
                if (c.mc.gui.screen() != screen) c.mc.gui.setScreen(screen);
                screen.show(v[0], v[1], Integer.parseInt(v[2]));
                List<String> over = screen.overflows();
                LOG.info("[photo] guide entries too long: {}", over.isEmpty() ? "none" : over);
            }), 15));
        }
    }

    // the death tax (Insurance.md, -Pphoto=insurance): the respawn popup, the Purgatory tag, the insurance terms (top, end)
    static {
        SHOTS.add(new Shot("ins_popup", c -> {
            day(c);
            c.hud = true;
            c.server.execute(() -> {
                var d = com.alexh.capitalism.deathtax.DeathTax.data(c.player);
                com.alexh.capitalism.deathtax.DeathTax.devOwe(c.player, null, "Death", 110);
                com.alexh.capitalism.deathtax.DeathTax.devOwe(c.player, java.util.UUID.randomUUID(), "Captain Rook", 45);
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(c.player, new com.alexh.capitalism.network.DeathTaxPopupPacket(
                    441, 25, "uninsured", 110, d.owed(), java.util.List.of("Death (the bank): 12 gold 2 nuggets", "Captain Rook (The Purge): 5 gold")));
            });
        }, 40));
        SHOTS.add(new Shot("ins_hud", c -> c.mc.execute(() -> c.mc.gui.setScreen(null)), 160));
        SHOTS.add(new Shot("ins_guide", c -> c.mc.execute(() -> InsuranceGuideScreen.open(java.util.List.of(1, 20, 3, 15, 7, 10, 20, 5, 25, 30))), 20));
        SHOTS.add(new Shot("ins_guide_end", c -> {
            if (Minecraft.getInstance().gui.screen() instanceof InsuranceGuideScreen screen) screen.scrollToEnd();
        }, 15));
    }

    // the Bounty Clerk (town/BountyClerk, Bounties.md, -Pphoto=bounty): him and his notice board, then his three tabs
    private static final int BX = 300;

    static {
        SHOTS.add(new Shot("bounty_front", c -> {
            day(c);
            c.hud = false;
            command(c, "gamemode creative @a");
            c.server.execute(() -> {
                for (int x = BX - 6; x <= BX + 6; x++) for (int z = -6; z <= 8; z++) {
                    c.level.setBlock(new BlockPos(x, DY - 1, z), ((x + z) & 1) == 0 ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.POLISHED_ANDESITE.defaultBlockState(), 2);
                }
                var clerk = new com.alexh.capitalism.town.BountyClerk(ModEntities.BOUNTY_CLERK.get(), c.level);
                clerk.placeAt(new net.minecraft.world.phys.Vec3(BX + 0.5, DY, 4.5), 180.0F);
                c.level.addFreshEntity(clerk);
            });
            stand(c, BX + 0.5, DY, 1.4, BX + 0.5, DY + 1.5, 4.5);
        }, 60));
        SHOTS.add(new Shot("bounty_left", c -> stand(c, BX - 2.4, DY + 0.2, 2.0, BX - 0.3, DY + 1.3, 4.6), 25));
        SHOTS.add(new Shot("bounty_right", c -> stand(c, BX + 3.0, DY + 0.3, 2.2, BX + 0.3, DY + 1.3, 4.5), 25));
        SHOTS.add(new Shot("bounty_close", c -> stand(c, BX + 0.5, DY + 0.15, 3.0, BX + 0.5, DY + 1.65, 4.5), 25));
        SHOTS.add(new Shot("bounty_board", c -> stand(c, BX + 1.6, DY + 0.3, 2.6, BX + 1.3, DY + 1.3, 4.9), 25));
        // his screen: today's offer
        SHOTS.add(new Shot("bounty_today", c -> {
            c.hud = true;
            stand(c, BX + 0.5, DY, 1.4, BX + 0.5, DY + 1.5, 4.5);
            c.server.execute(() -> {
                com.alexh.capitalism.bounty.Bounties.reset(c.player);
                com.alexh.capitalism.economy.GoldMoney.give(c.player, 9 * 40);
                com.alexh.capitalism.bounty.Bounties.setPosted(c.server, "kill_zombies", 6);
                for (var clerk : c.level.getEntitiesOfClass(com.alexh.capitalism.town.BountyClerk.class, new net.minecraft.world.phys.AABB(BX - 5, DY - 2, 0, BX + 5, DY + 4, 9))) {
                    clerk.setCustomer(c.player);
                    c.player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inv, who) -> new com.alexh.capitalism.town.BountyMenu(id, inv, clerk),
                        net.minecraft.network.chat.Component.literal("The Bounty Clerk")));
                }
            });
        }, 20));
        // one taken, some way along; then their task
        SHOTS.add(new Shot("bounty_tasks", c -> {
            c.server.execute(() -> {
                com.alexh.capitalism.bounty.Bounties.setPosted(c.server, "mine_diamonds", 5);
                com.alexh.capitalism.bounty.Bounties.take(c.player);
                com.alexh.capitalism.bounty.Bounties.count(c.player, t -> t.id.equals("mine_diamonds"), 2);
            });
            if (Minecraft.getInstance().gui.screen() instanceof BountyClerkScreen screen) screen.showTab(1);
        }, 25));        SHOTS.add(new Shot("bounty_today_taken", c -> {
            if (Minecraft.getInstance().gui.screen() instanceof BountyClerkScreen screen) screen.showTab(0);
        }, 15));
        SHOTS.add(new Shot("bounty_ledger", c -> {
            c.server.execute(() -> {
                String[] names = {"Mira", "Captain Rook", "Jo", "Sam", "Alexandra", "Theo", "Wren", "Bjorn", "Ivy", "Quill", "Hazel", "Otto", "Pip"};
                int[] counts = {41, 37, 37, 33, 30, 29, 27, 26, 25, 24, 24, 24, 12};
                for (int i = 0; i < names.length; i++) com.alexh.capitalism.bounty.Bounties.devStanding(c.server, names[i], counts[i], 9L * counts[i] * 8);
                com.alexh.capitalism.bounty.Bounties.devSetCompleted(c.player, 23, 9L * 187);
            });
            if (Minecraft.getInstance().gui.screen() instanceof BountyClerkScreen screen) screen.showTab(2);
        }, 15));
        SHOTS.add(new Shot("bounty_ledger_prize", c -> c.server.execute(() -> {
            com.alexh.capitalism.bounty.Bounties.devSetCompleted(c.player, 50, 9L * 412);
            com.alexh.capitalism.bounty.Bounties.devReserve(c.player);
        }), 15));
        // the Ledger's second board: the cravings satisfied (Cravings.md)
        SHOTS.add(new Shot("bounty_ledger_cravings", c -> {
            c.server.execute(() -> {
                String[] names = {"Hazel", "Otto", "Mira", "Jo", "Pip", "Wren"};
                int[] counts = {48, 41, 33, 20, 9, 4};
                for (int i = 0; i < names.length; i++) com.alexh.capitalism.bounty.Standings.get(c.server).set(com.alexh.capitalism.bounty.Standings.Category.CRAVINGS,
                    java.util.UUID.nameUUIDFromBytes(names[i].getBytes()), names[i], counts[i], 0, c.level.getGameTime());
                com.alexh.capitalism.bounty.Bounties.syncBoard(c.server);
            });
            if (Minecraft.getInstance().gui.screen() instanceof BountyClerkScreen screen) {
                screen.showTab(2);
                screen.showBoard(1);
            }
        }, 15));
        // the Wellness screen's Tasks page: the bounty, today's cravings (the drink already satisfied)
        SHOTS.add(new Shot("bounty_wellness_setup", c -> {
            c.server.execute(() -> {
                c.player.closeContainer();
                String rib = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(com.alexh.capitalism.core.ModItems.SEARED_CUTS.get(com.alexh.capitalism.farmer.MeatCut.BEEF_RIB).get()).toString();
                String moonshine = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(com.alexh.capitalism.core.ModItems.MINERS_MOONSHINE.get()).toString();
                com.alexh.capitalism.craving.Cravings.set(c.player, rib, com.alexh.capitalism.farmer.Sear.Doneness.MEDIUM_RARE.ordinal(), moonshine);
                com.alexh.capitalism.craving.Cravings.consumed(c.player, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.MINERS_MOONSHINE.get()));
            });
        }, 15));
        SHOTS.add(new Shot("bounty_wellness_tasks", c -> {
            c.mc.execute(() -> {
                WellnessScreen screen = new WellnessScreen();
                c.mc.gui.setScreen(screen);
                screen.showPage(2);
            });
        }, 25));
        // the Mayoral Chest (Town.md > The Town Hall): shut, from the front and the side; open (the lid up, the velvet);
        // its menu; the item in a slot
        SHOTS.add(new Shot("bounty_mayoral_chest", c -> {
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            c.hud = false;
            c.server.execute(() -> {
                c.player.closeContainer();
                c.level.setBlock(new BlockPos(BX + 4, DY, 0), com.alexh.capitalism.core.ModBlocks.MAYORAL_CHEST.get().defaultBlockState()
                    .setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
                c.level.setBlock(new BlockPos(BX + 2, DY, 0), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.ChestBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
            });
            stand(c, BX + 3.5, DY + 1.4, 2.6, BX + 4.0, DY + 0.4, 0.5);
        }, 40));
        SHOTS.add(new Shot("bounty_mayoral_chest_side", c -> stand(c, BX + 6.2, DY + 1.3, 2.0, BX + 4.5, DY + 0.4, 0.5), 25));
        SHOTS.add(new Shot("bounty_mayoral_chest_open", c -> {
            c.server.execute(() -> {
                if (c.level.getBlockEntity(new BlockPos(BX + 4, DY, 0)) instanceof com.alexh.capitalism.town.MayoralChestBlockEntity chest) {
                    chest.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_BLOCK, 64));
                    chest.setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 32));
                    chest.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITABLE_BOOK));
                    chest.setItem(53, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
                    c.player.openMenu(chest);
                }
            });
            stand(c, BX + 4.5, DY + 1.9, 2.4, BX + 4.5, DY + 0.5, 0.5);
        }, 40));
        SHOTS.add(new Shot("bounty_mayoral_chest_menu", c -> c.hud = true, 15));
        SHOTS.add(new Shot("bounty_mayoral_chest_lid", c -> {
            c.hud = false;
            c.mc.execute(() -> c.mc.gui.setScreen(null));
            // (the screen goes but the server keeps the menu open, so the lid stays up)
            stand(c, BX + 6.0, DY + 1.8, 2.4, BX + 4.0, DY + 0.3, 0.5);
        }, 40));
        SHOTS.add(new Shot("bounty_mayoral_chest_item", c -> {
            c.hud = true;
            c.server.execute(() -> {
                c.player.closeContainer();
                c.player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(com.alexh.capitalism.core.ModItems.MAYORAL_CHEST.get()));
                c.player.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST));
            });
        }, 15));
        SHOTS.add(new Shot("bounty_mayoral_chest_icon", c -> c.mc.execute(() -> c.mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(c.mc.player))), 25));
        SHOTS.add(new Shot("bounty_offer_heroic", c -> {
            c.server.execute(() -> {
                com.alexh.capitalism.calendar.Calendar.setClock(c.level, com.alexh.capitalism.calendar.Calendar.clock(c.level) + com.alexh.capitalism.calendar.Calendar.DAY_TICKS);
                com.alexh.capitalism.bounty.Bounties.setPosted(c.server, "slay_dragon", 1);
            });
            if (Minecraft.getInstance().gui.screen() instanceof BountyClerkScreen screen) screen.showTab(0);
        }, 15));
    }

    static {
        // the boats are done: their shots only with -Dcapitalism.photo=boats
        if (MODE == null || !MODE.contains("boats")) {
            SHOTS.removeIf(s -> s.name().startsWith("sub_") || s.name().startsWith("steamboat") || s.name().startsWith("fairy"));
        }
        // just the mob shots with -Pphoto=mobs (quick), and only then
        if (MODE != null && MODE.contains("mobs")) SHOTS.removeIf(s -> !s.name().startsWith("mob_"));
        else if (MODE != null && MODE.contains("perf")) SHOTS.removeIf(s -> !s.name().startsWith("perf_"));
        else if (MODE != null && MODE.contains("well")) SHOTS.removeIf(s -> !s.name().startsWith("well_"));
        else if (MODE != null && MODE.contains("mail")) SHOTS.removeIf(s -> !s.name().startsWith("mail_"));
        else if (MODE != null && MODE.contains("signs")) SHOTS.removeIf(s -> !s.name().startsWith("signs_"));
        else if (MODE != null && MODE.contains("bloodmoon")) SHOTS.removeIf(s -> !s.name().startsWith("bm_"));
        else if (MODE != null && MODE.contains("blooditems")) SHOTS.removeIf(s -> !s.name().startsWith("blood_items_"));
        else if (MODE != null && MODE.contains("blooddecor")) SHOTS.removeIf(s -> !s.name().startsWith("blood_"));
        else if (MODE != null && MODE.contains("spooky")) SHOTS.removeIf(s -> !s.name().startsWith("spooky_"));
        else if (MODE != null && MODE.contains("invest")) SHOTS.removeIf(s -> !s.name().startsWith("invest_"));
        else if (MODE != null && MODE.contains("admin")) SHOTS.removeIf(s -> !s.name().startsWith("admin_"));
        else if (MODE != null && MODE.contains("bloodbanner")) SHOTS.removeIf(s -> !s.name().startsWith("bannertest_"));
        else if (MODE != null && MODE.contains("traveler")) SHOTS.removeIf(s -> !s.name().startsWith("trav_"));
        else if (MODE != null && MODE.contains("merchant")) SHOTS.removeIf(s -> !s.name().startsWith("merch_"));
        else if (MODE != null && MODE.contains("fish")) SHOTS.removeIf(s -> !s.name().startsWith("fish_"));
        else if (MODE != null && MODE.contains("bounty")) SHOTS.removeIf(s -> !s.name().startsWith("bounty_"));
        else if (MODE != null && MODE.contains("insurance")) SHOTS.removeIf(s -> !s.name().startsWith("ins_"));
        else if (MODE != null && MODE.contains("guide")) SHOTS.removeIf(s -> !s.name().startsWith("guide_"));
        else SHOTS.removeIf(s -> s.name().startsWith("fish_") || s.name().startsWith("bounty_") || s.name().startsWith("ins_") || s.name().startsWith("guide_") || s.name().startsWith("mob_") || s.name().startsWith("perf_") || s.name().startsWith("well_") || s.name().startsWith("mail_") || s.name().startsWith("signs_") || s.name().startsWith("bm_") || s.name().startsWith("merch_") || s.name().startsWith("trav_") || s.name().startsWith("blood_") || s.name().startsWith("bannertest_"));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        if (MODE == null) return;
        Minecraft mc = Minecraft.getInstance();
        // at the title screen: open its world (saves/photo) ourselves
        if (mc.level == null) {
            titleTicks++;
            if (titleTicks == 40 && mc.gui.screen() instanceof net.minecraft.client.gui.screens.TitleScreen) {
                // a fresh flat world each time (any old one is deleted first: runs/photo/saves/photo_world)
                java.io.File old = new java.io.File(mc.gameDirectory, "saves/photo_world");
                if (old.exists()) {
                    try (var walk = java.nio.file.Files.walk(old.toPath())) {
                        walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                    } catch (java.io.IOException e) {
                        LOG.warn("[photo] couldn't delete the old world", e);
                    }
                }
                LOG.info("[photo] creating the world");
                mc.createWorldOpenFlows().createFreshLevel("photo_world",
                    new net.minecraft.world.level.LevelSettings("photo_world", net.minecraft.world.level.GameType.CREATIVE,
                        net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT, true,
                        net.minecraft.world.level.WorldDataConfiguration.DEFAULT),
                    new net.minecraft.world.level.levelgen.WorldOptions(1234L, false, false),
                    net.minecraft.world.level.levelgen.presets.WorldPresets::createTestWorldDimensions,
                    mc.gui.screen());
            }
            if (titleTicks % 200 == 0) LOG.info("[photo] waiting at {}", mc.gui.screen() == null ? "no screen" : mc.gui.screen().getClass().getSimpleName());
            return;
        }
        if (mc.player == null || mc.getSingleplayerServer() == null) return;
        tick++;
        Ctx c = CTX;
        c.mc = mc;
        c.server = mc.getSingleplayerServer();
        c.level = c.server.overworld();
        c.player = c.server.getPlayerList().getPlayer(mc.player.getUUID());
        if (c.player == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.gui.hud.isHidden() == c.hud) mc.gui.hud.toggle();
        if (tick == 20) {
            command(c, "gamemode creative @a");
            command(c, "difficulty peaceful");
            command(c, "tp @a " + (OX + 6) + " " + (TOP + 10) + " " + (OZ + 6));
        }
        if (tick == 60 && !built) {
            built = true;
            c.server.execute(() -> build(c));
        }
        if (tick >= 120 && shot >= 0 && shot < SHOTS.size() && SHOTS.get(shot).name().startsWith("perf_mobs") && tick % 30 == 0) {
            command(c, "damage @e[tag=perf] 1");
        }
        if (tick < 120) return;
        if (waitLeft > 0) {
            waitLeft--;
            if (waitLeft == 200 && shot >= 0 && shot < SHOTS.size() && SHOTS.get(shot).name().startsWith("perf_")) FRAMES.clear();
            if (waitLeft == 1 && shot >= 0 && shot < SHOTS.size() && SHOTS.get(shot).name().startsWith("perf_") && !SHOTS.get(shot).name().contains("enchant")) report(SHOTS.get(shot).name());
            if (waitLeft == 0 && shot >= 0 && shot < SHOTS.size()) {
                Screenshot.grab(mc.gameDirectory, "photo_" + String.format("%02d", shot) + "_" + SHOTS.get(shot).name() + ".png",
                    mc.gameRenderer.mainRenderTarget(), 1, msg -> { });
            }
            return;
        }
        shot++;
        if (shot >= SHOTS.size()) {
            if (shot == SHOTS.size() + 1) mc.stop();
            waitLeft = 20;
            return;
        }
        Shot s = SHOTS.get(shot);
        LOG.info("[photo] shot {}: {}", shot, s.name());
        s.setUp().accept(c);
        waitLeft = s.ticks();
    }

    // frame times (ms) of the current perf shot, from its 3rd second on
    private static final java.util.List<Double> FRAMES = new java.util.ArrayList<>();
    private static long lastFrame;

    @SubscribeEvent
    public static void onFrame(net.neoforged.neoforge.client.event.RenderFrameEvent.Post event) {
        if (MODE == null || !MODE.contains("perf")) return;
        long now = System.nanoTime();
        if (lastFrame != 0) FRAMES.add((now - lastFrame) / 1.0e6);
        lastFrame = now;
    }

    private static void report(String name) {
        if (FRAMES.isEmpty()) return;
        java.util.List<Double> sorted = new java.util.ArrayList<>(FRAMES);
        java.util.Collections.sort(sorted);
        double total = 0;
        for (double f : FRAMES) total += f;
        double avg = total / FRAMES.size();
        int n = sorted.size();
        double p99 = sorted.get(Math.min(n - 1, (int) (n * 0.99)));
        double max = sorted.get(n - 1);
        long over50 = FRAMES.stream().filter(f -> f > 50).count();
        LOG.info("[perf] {}: {} frames, avg {} fps ({} ms), 1% low {} fps ({} ms), worst frame {} ms, {} frames over 50 ms", name, n,
            String.format("%.0f", 1000 / avg), String.format("%.2f", avg), String.format("%.0f", 1000 / p99), String.format("%.1f", p99),
            String.format("%.1f", max), over50);
    }

    private static void unlockFps(Ctx c) {
        c.mc.options.framerateLimit().set(260);
        c.mc.options.enableVsync().set(false);
        // no AFK cap: the game drops to 30 fps when nobody has touched it for a while, which the scripted run never does
        c.mc.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);
    }

    // show or hide the entity hitboxes (F3+B)
    private static void hitboxes(boolean on) {
        var debug = net.minecraft.client.Minecraft.getInstance().debugEntries;
        if (debug.isCurrentlyEnabled(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES) != on) {
            debug.toggleStatus(net.minecraft.client.gui.components.debug.DebugScreenEntries.ENTITY_HITBOXES);
        }
    }

    // (Blood Moon shots) the boss in the scene does this move (held: its AI is off, so the pose stays)
    private static void bossAction(Ctx c, int action) {
        c.server.execute(() -> {
            for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.BloodMoonBoss.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                b.setAction(action);
            }
        });
    }

    // (Blood Moon shots) the boss makes a move for real (photoAi: its move logic runs though it's held still), at a
    // villager standing in for a player 8 blocks in front of it
    private static void fight(Ctx c, int action) {
        c.server.execute(() -> {
            var box = new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10);
            var victims = c.level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, box);
            net.minecraft.world.entity.LivingEntity victim;
            if (victims.isEmpty()) {
                var v = net.minecraft.world.entity.EntityTypes.VILLAGER.create(c.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (v == null) return;
                v.snapTo(52.5, DY, 0.5, -90.0F, 0.0F);
                v.setNoAi(true);
                c.level.addFreshEntity(v);
                victim = v;
            } else victim = victims.get(0);
            // (it takes the hits: Resistance V, so it lives through every shot)
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.RESISTANCE, 100000, 4, false, false));
            victim.setHealth(victim.getMaxHealth());
            for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.BloodMoonBoss.class, box)) {
                b.photoAi = true;
                b.setTarget(victim);
                b.setAction(action);
            }
        });
    }

    // (Blood Moon shots) a pool of water two deep under the boss and the villager (the bosses' water moves), and back
    private static final java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> POOL = new java.util.HashMap<>();

    private static void pool(Ctx c, boolean water) {
        c.server.execute(() -> {
            if (water && POOL.isEmpty()) {
                for (int x = 46; x <= 66; x++) for (int z = -8; z <= 8; z++) for (int y = DY - 2; y < DY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    POOL.put(p, c.level.getBlockState(p));
                    c.level.setBlock(p, net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), 3);
                }
            } else if (!water) {
                POOL.forEach((p, s) -> c.level.setBlock(p, s, 3));
                POOL.clear();
            }
        });
    }

    // (Blood Moon shots) something done to the witch in the scene
    private static void witch(Ctx c, java.util.function.Consumer<com.alexh.capitalism.event.SanguineWitch> what) {
        c.server.execute(() -> {
            for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.SanguineWitch.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) what.accept(b);
        });
    }

    // (Blood Moon shots) the witch sends a nova ring out
    private static void witchRing(Ctx c) {
        c.server.execute(() -> {
            for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.SanguineWitch.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) {
                b.photoNova();
            }
        });
    }

    // (Blood Moon shots) swap the boss in the scene for another, held still, adopted as tonight's
    private static void spawnBoss(Ctx c, net.minecraft.world.entity.EntityType<? extends com.alexh.capitalism.event.BloodMoonBoss> type) {
        c.server.execute(() -> {
            for (var b : c.level.getEntitiesOfClass(com.alexh.capitalism.event.BloodMoonBoss.class, new net.minecraft.world.phys.AABB(50, DY - 2, -10, 70, DY + 8, 10))) b.discard();
            var boss = type.create(c.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (boss == null) return;
            boss.snapTo(60.5, DY, 0.5, 90.0F, 0.0F);
            boss.setYHeadRot(90.0F);
            boss.setYBodyRot(90.0F);
            boss.setNoAi(true);
            c.level.addFreshEntity(boss);
            com.alexh.capitalism.event.BloodMoon.adopt(c.server, boss);
        });
    }

    private static void command(Ctx c, String cmd) {
        c.server.execute(() -> c.server.getCommands().performPrefixedCommand(c.server.createCommandSourceStack().withSuppressedOutput(), cmd));
    }

    private static void night(Ctx c) {
        command(c, "time set 18000");
        command(c, "weather clear 100000");
    }

    private static void day(Ctx c) {
        command(c, "time set 6000");
    }

    private static void ride(Ctx c, Entity boat) {
        c.server.execute(() -> c.player.startRiding(boat, true, true));
    }

    private static void dismount(Ctx c) {
        c.server.execute(() -> c.player.stopRiding());
    }

    // the lamp aimed (no rider): on the server (its light) and on this side (the beam drawn)
    private static void aim(Ctx c, float yaw, float pitch) {
        c.server.execute(() -> c.sub.aimLamp(yaw, pitch));
        if (c.mc.level.getEntity(c.sub.getId()) instanceof Submersible here) here.aimLamp(yaw, pitch);
    }

    private static void setLamp(Ctx c, boolean on) {
        c.server.execute(() -> {
            if (c.sub.flashlightOn() != on) c.sub.toggleFlashlight();
        });
    }

    // the camera: the player's look, first or third person
    private static void look(Ctx c, float yaw, float pitch, CameraType camera) {
        c.mc.options.setCameraType(camera);
        c.mc.player.setYRot(yaw);
        c.mc.player.setXRot(pitch);
        c.mc.player.setYHeadRot(yaw);
        c.mc.player.yRotO = yaw;
        c.mc.player.xRotO = pitch;
    }

    // standing (flying) at a point, looking at another, first person
    private static void stand(Ctx c, double x, double y, double z, double tx, double ty, double tz) {
        double dx = tx - x, dy = ty - (y + 1.62), dz = tz - z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        c.mc.player.getAbilities().flying = true;
        command(c, String.format(java.util.Locale.ROOT, "tp @a %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
        c.mc.player.setPos(x, y, z);
        look(c, yaw, pitch, CameraType.FIRST_PERSON);
    }

    // â”€â”€ the scene â”€â”€

    private static void build(Ctx c) {
        ServerLevel level = c.level;
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int x = -26; x <= 42; x++) {
            for (int z = -30; z <= 30; z++) {
                boolean edge = x == -26 || x == 42 || Math.abs(z) == 30;
                int floor = OY + (int) Math.floor(1.2 * Math.sin(x * 0.35) * Math.cos(z * 0.27) + 1.2) + Math.max(0, (x - 20) / 3);
                for (int y = OY - 1; y <= TOP + 1; y++) {
                    BlockPos p = new BlockPos(OX + x, y, OZ + z);
                    BlockState s;
                    if (edge) s = y <= TOP ? stone : Blocks.AIR.defaultBlockState();
                    else if (y < floor) s = stone;
                    else if (y == floor) s = (x * 7 + z * 3) % 5 == 0 ? Blocks.GRAVEL.defaultBlockState() : Blocks.SAND.defaultBlockState();
                    else if (y <= TOP) s = water;
                    else s = Blocks.AIR.defaultBlockState();
                    level.setBlock(p, s, 2);
                }
                if (edge) continue;
                // seagrass here and there, kelp in clumps
                long h = (x * 73856093L) ^ (z * 19349663L);
                BlockPos above = new BlockPos(OX + x, floor + 1, OZ + z);
                if (Math.floorMod(h, 7) == 0) level.setBlock(above, Blocks.SEAGRASS.defaultBlockState(), 2);
                if (Math.floorMod(h, 23) == 0) {
                    int tall = 4 + (int) Math.floorMod(h, 6);
                    for (int k = 0; k < tall && floor + 1 + k < TOP - 1; k++)
                        level.setBlock(above.above(k), (k == tall - 1 ? Blocks.KELP : Blocks.KELP_PLANT).defaultBlockState(), 2);
                }
            }
        }
        // prismarine pillars at 8 and 16 blocks, a sunken wreck further off
        for (int[] p : new int[][]{{10, -5}, {18, 6}, {14, 9}}) {
            for (int y = OY; y < OY + 8; y++) level.setBlock(new BlockPos(OX + p[0], y, OZ + p[1]), Blocks.PRISMARINE_BRICKS.defaultBlockState(), 2);
        }
        for (int x = 26; x <= 33; x++) for (int z = -2; z <= 2; z++) for (int y = OY + 3; y <= OY + 5; y++) {
            boolean shell = Math.abs(z) == 2 || y == OY + 3 || x == 26 || x == 33;
            if (shell && !(x == 29 && y == OY + 5)) level.setBlock(new BlockPos(OX + x, y, OZ + z), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 2);
        }
        // the boats
        c.sub = spawn(level, ModEntities.SUBMERSIBLE.get(), 0.5, TOP + 0.5, 0.5, -90.0F);
        c.steamboat = spawn(level, ModEntities.STEAMBOAT.get(), 6.5, TOP + 0.5, -12.5, -90.0F);
        c.fairy = spawn(level, ModEntities.FAIRY_BOAT.get(), 6.5, TOP + 0.5, 12.5, -90.0F);
        decorations(level);
        for (Entity e : level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(OX - 40, OY - 10, OZ - 40, OX + 60, TOP + 20, OZ + 40),
            e -> e instanceof net.minecraft.world.entity.Mob)) e.discard();
    }

    // a square of grass at x 46 to 74, the decorations on it facing west (the camera), fence posts with hooks and
    // string lights between them
    private static void decorations(ServerLevel level) {
        for (int x = 44; x <= 76; x++) for (int z = -16; z <= 16; z++) {
            level.setBlock(new BlockPos(x, DY - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = DY; y < DY + 12; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        net.minecraft.core.Direction west = net.minecraft.core.Direction.WEST;
        java.util.function.BiConsumer<BlockPos, net.minecraft.world.level.block.Block> put = (p, b) -> level.setBlock(p,
            b.defaultBlockState().setValue(com.alexh.capitalism.decor.SeasonalDecorBlock.FACING, west).setValue(com.alexh.capitalism.decor.SeasonalDecorBlock.UP, true), 3);
        put.accept(new BlockPos(60, DY, 0), com.alexh.capitalism.core.ModBlocks.CHRISTMAS_TREE.get());
        put.accept(new BlockPos(54, DY, -7), com.alexh.capitalism.core.ModBlocks.SPOOKY_SCARECROW.get());
        put.accept(new BlockPos(56, DY, 7), com.alexh.capitalism.core.ModBlocks.WITCHS_CAULDRON.get());
        put.accept(new BlockPos(67, DY, -6), com.alexh.capitalism.core.ModBlocks.PUMPKIN_PILE.get());
        put.accept(new BlockPos(67, DY, 7), com.alexh.capitalism.core.ModBlocks.LEAF_PILE.get());
        put.accept(new BlockPos(56, DY, 2), com.alexh.capitalism.core.ModBlocks.PRESENT_PILE.get());
        // two candy cane poles with lights between them, and a bunch of candy canes
        BlockPos poleA = new BlockPos(48, DY, -4);
        BlockPos poleB = new BlockPos(48, DY, 9);
        put.accept(poleA, com.alexh.capitalism.core.ModBlocks.CANDY_CANE_POLE.get());
        put.accept(poleB, com.alexh.capitalism.core.ModBlocks.CANDY_CANE_POLE.get());
        if (level.getBlockEntity(poleA) instanceof com.alexh.capitalism.decor.SeasonalDecorBlockEntity pa) pa.addStrand(poleB);
        put.accept(new BlockPos(50, DY, 3), com.alexh.capitalism.core.ModBlocks.CANDY_CANES.get());
        // frost over a patch of grass, icicles under a little stone roof
        for (int x = 50; x <= 55; x++) for (int z = -10; z <= -5; z++) put.accept(new BlockPos(x, DY, z), com.alexh.capitalism.core.ModBlocks.CHRISTMAS_FROST.get());
        for (int x = 51; x <= 57; x++) {
            level.setBlock(new BlockPos(x, DY + 3, 12), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            put.accept(new BlockPos(x, DY + 2, 12), com.alexh.capitalism.core.ModBlocks.ICICLES.get());
        }
        // the other seasons, on a second square
        for (int x = 77; x <= 100; x++) for (int z = -16; z <= 18; z++) {
            level.setBlock(new BlockPos(x, DY - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = DY; y < DY + 12; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        put.accept(new BlockPos(85, DY, -12), com.alexh.capitalism.core.ModBlocks.GRAVESTONE_ROUND.get());
        put.accept(new BlockPos(85, DY, -10), com.alexh.capitalism.core.ModBlocks.GRAVESTONE_CROSS.get());
        put.accept(new BlockPos(85, DY, -8), com.alexh.capitalism.core.ModBlocks.GRAVESTONE_TOMB.get());
        put.accept(new BlockPos(84, DY, -5), com.alexh.capitalism.core.ModBlocks.BONE_PILE.get());
        put.accept(new BlockPos(87, DY, -3), com.alexh.capitalism.core.ModBlocks.GROUND_FOG.get());
        // a carpet of fallen leaves round the trees (ragged at its edges)
        for (int x = 88; x <= 97; x++) for (int z = -15; z <= -1; z++) {
            if (Math.floorMod(x * 7 + z * 13, 9) == 0 || (x == 89 && z == -8)) continue;
            if ((x - 92) * (x - 92) + (z + 8) * (z + 8) * 0.35 > 22) continue;
            put.accept(new BlockPos(x, DY, z), com.alexh.capitalism.core.ModBlocks.FALLEN_LEAVES.get());
        }
        put.accept(new BlockPos(92, DY, -12), com.alexh.capitalism.core.ModBlocks.AUTUMN_MAPLE.get());
        put.accept(new BlockPos(93, DY, -5), com.alexh.capitalism.core.ModBlocks.AUTUMN_BIRCH.get());
        put.accept(new BlockPos(89, DY, -8), com.alexh.capitalism.core.ModBlocks.LEAF_PILE.get());
        put.accept(new BlockPos(84, DY, 3), com.alexh.capitalism.core.ModBlocks.BEACH_TOWEL.get());
        put.accept(new BlockPos(86, DY, 4), com.alexh.capitalism.core.ModBlocks.BEACH_UMBRELLA.get());
        put.accept(new BlockPos(83, DY, 6), com.alexh.capitalism.core.ModBlocks.BEACH_BALL.get());
        put.accept(new BlockPos(85, DY, 7), com.alexh.capitalism.core.ModBlocks.BEACH_CHAIR.get());
        put.accept(new BlockPos(84, DY, 11), com.alexh.capitalism.core.ModBlocks.EGG_BASKET.get());
        put.accept(new BlockPos(87, DY, 12), com.alexh.capitalism.core.ModBlocks.GIANT_EGG.get());
        put.accept(new BlockPos(84, DY, 14), com.alexh.capitalism.core.ModBlocks.SPRING_BUNNY.get());
        put.accept(new BlockPos(85, DY, 16), com.alexh.capitalism.core.ModBlocks.TULIP_BED.get());
        put.accept(new BlockPos(86, DY, 16), com.alexh.capitalism.core.ModBlocks.TULIP_BED.get());
        int[][] posts = {{50, -13}, {58, -14}, {66, -13}, {72, -11}};
        BlockPos prev = null;
        for (int[] p : posts) {
            for (int y = DY; y < DY + 3; y++) level.setBlock(new BlockPos(p[0], y, p[1]), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            BlockPos hook = new BlockPos(p[0], DY + 3, p[1]);
            put.accept(hook, com.alexh.capitalism.core.ModBlocks.STRING_LIGHT_HOOK.get());
            if (prev != null && level.getBlockEntity(prev) instanceof com.alexh.capitalism.decor.SeasonalDecorBlockEntity from) from.addStrand(hook);
            prev = hook;
        }
    }

    // (Blood Moon decorations) a close up of the one at `index` in the row: the camera `back` blocks in front of it (and
    // a little to its side), `up` above the ground, looking at `at` blocks above its foot
    private static void bloodClose(String name, int index, double back, double up, double at) {
        double z = bloodZ(index);
        SHOTS.add(new Shot("blood_close_" + name, c -> stand(c, 118.5 - back, DY + up, z + back * 0.35, 118.5, DY + at, z), 30));
    }

    private static void bloodNight(String name, int index, double back, double up, double at) {
        double z = bloodZ(index);
        SHOTS.add(new Shot("blood_night_" + name, c -> stand(c, 118.5 - back, DY + up, z + back * 0.35, 118.5, DY + at, z), 30));
    }

    // the Blood Moon decorations (-Pphoto=blooddecor): a square of grass at x 104 to 130, the eleven in a row at x 118
    // facing west (the camera), three blocks apart (z -15 to 15; the second ten from z 22: bloodZ); the skull trophy, the
    // portrait and the fireplace against little blackstone walls, the chandelier under a dark oak ceiling, the bookshelf
    // door in a wall
    static java.util.List<net.minecraft.world.item.Item> bloodItems() {
        java.util.List<net.minecraft.world.item.Item> out = new java.util.ArrayList<>();
        for (var kind : com.alexh.capitalism.decor.BloodDecorKind.values()) {
            out.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath(com.alexh.capitalism.Capitalism.MODID, kind.id)));
        }
        return out;
    }

    static double bloodZ(int index) {
        return (index < 11 ? -15 + index * 3 : 22 + (index - 11) * 3) + 0.5;
    }

    // the Halloween stall's three decorations in a row facing west (toward the camera): lantern, tombstone (carved), scarecrow
    private static void spookyScene(ServerLevel level) {
        for (int x = 108; x <= 124; x++) for (int z = 62; z <= 80; z++) {
            level.setBlock(new BlockPos(x, DY - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = DY; y < DY + 5; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        var west = net.minecraft.core.Direction.WEST;
        var blocks = java.util.List.of(com.alexh.capitalism.core.ModBlocks.GRINNING_JACK_O_LANTERN.get(),
            com.alexh.capitalism.core.ModBlocks.HAUNTED_TOMBSTONE.get(), com.alexh.capitalism.core.ModBlocks.POSSESSED_SCARECROW.get());
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos at = new BlockPos(118, DY, 68 + i * 2);
            level.setBlock(at, blocks.get(i).defaultBlockState().setValue(com.alexh.capitalism.decor.BloodDecorBlock.FACING, west), 3);
            var kind = ((com.alexh.capitalism.decor.BloodDecorKind.Holder) blocks.get(i)).kind();
            if (kind.multiBlock()) com.alexh.capitalism.decor.BloodDecorBlock.placeParts(level, at, west, kind, at);
            if (level.getBlockEntity(at) instanceof com.alexh.capitalism.decor.BloodDecorBlockEntity be && kind == com.alexh.capitalism.decor.BloodDecorKind.HAUNTED_TOMBSTONE) {
                be.setEpitaph("Here lies Old Bob, who fed the pigs");
            }
        }
        // a second tombstone with a short name, like most players carve
        BlockPos second = new BlockPos(118, DY, 66);
        level.setBlock(second, com.alexh.capitalism.core.ModBlocks.HAUNTED_TOMBSTONE.get().defaultBlockState().setValue(com.alexh.capitalism.decor.BloodDecorBlock.FACING, west), 3);
        if (level.getBlockEntity(second) instanceof com.alexh.capitalism.decor.BloodDecorBlockEntity be) be.setEpitaph("Justin");
    }

    private static void bloodDecorScene(ServerLevel level) {
        for (int x = 102; x <= 132; x++) for (int z = -20; z <= 54; z++) {
            level.setBlock(new BlockPos(x, DY - 1, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
            for (int y = DY; y < DY + 6; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
        }
        java.util.List<net.minecraft.world.level.block.Block> row = java.util.List.of(com.alexh.capitalism.core.ModBlocks.BLOOD_MOON_LANTERN.get(),
            com.alexh.capitalism.core.ModBlocks.CRIMSON_CANDELABRA.get(), com.alexh.capitalism.core.ModBlocks.CRIMSON_THRONE.get(),
            com.alexh.capitalism.core.ModBlocks.VAMPIRE_COFFIN.get(), com.alexh.capitalism.core.ModBlocks.BLOOD_FOUNTAIN.get(),
            com.alexh.capitalism.core.ModBlocks.BLOOD_MOON_BANNER.get(), com.alexh.capitalism.core.ModBlocks.CRIMSON_RUG.get(),
            com.alexh.capitalism.core.ModBlocks.REVENANT_GREATSWORD_STATUE.get(), com.alexh.capitalism.core.ModBlocks.GOREFANG_SKULL_TROPHY.get(),
            com.alexh.capitalism.core.ModBlocks.SANGUINE_STAFF_PEDESTAL.get(), com.alexh.capitalism.core.ModBlocks.BAT_GARGOYLE.get(),
            com.alexh.capitalism.core.ModBlocks.GRANDFATHER_CLOCK.get(), com.alexh.capitalism.core.ModBlocks.CRIMSON_CHANDELIER.get(),
            com.alexh.capitalism.core.ModBlocks.BLOOD_PIPE_ORGAN.get(), com.alexh.capitalism.core.ModBlocks.SECRET_BOOKSHELF_DOOR.get(),
            com.alexh.capitalism.core.ModBlocks.HAUNTED_PORTRAIT.get(), com.alexh.capitalism.core.ModBlocks.IRON_MAIDEN.get(),
            com.alexh.capitalism.core.ModBlocks.BLOOD_MOON_ALTAR.get(), com.alexh.capitalism.core.ModBlocks.BAT_ROOST.get(),
            com.alexh.capitalism.core.ModBlocks.CRIMSON_CANOPY_BED.get(), com.alexh.capitalism.core.ModBlocks.BLOOD_CRYSTAL_FIREPLACE.get());
        var wall = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        var west = net.minecraft.core.Direction.WEST;
        for (int i = 0; i < row.size(); i++) {
            int z = (int) Math.floor(bloodZ(i));
            var block = row.get(i);
            if (block instanceof com.alexh.capitalism.decor.BloodBedBlock) {
                // a bed: its foot toward the camera, its head behind
                var bed = block.defaultBlockState().setValue(com.alexh.capitalism.decor.BloodBedBlock.FACING, net.minecraft.core.Direction.EAST);
                level.setBlock(new BlockPos(118, DY, z), bed, 3);
                level.setBlock(new BlockPos(119, DY, z), bed.setValue(com.alexh.capitalism.decor.BloodBedBlock.PART,
                    net.minecraft.world.level.block.state.properties.BedPart.HEAD), 3);
                var bedKind = ((com.alexh.capitalism.decor.BloodDecorKind.Holder) block).kind();
                if (bedKind.tall > 1) com.alexh.capitalism.decor.BloodDecorBlock.placeParts(level, new BlockPos(118, DY, z), west, bedKind, new BlockPos(118, DY, z));
                continue;
            }
            if (block instanceof com.alexh.capitalism.decor.BloodDoorBlock) {
                // in a wall two blocks either side
                for (int y = 0; y < 3; y++) for (int dz = -1; dz <= 1; dz++) if (dz != 0 || y == 2) level.setBlock(new BlockPos(118, DY + y, z + dz), wall, 3);
                var door = block.defaultBlockState().setValue(com.alexh.capitalism.decor.BloodDoorBlock.FACING, west);
                level.setBlock(new BlockPos(118, DY, z), door, 3);
                level.setBlock(new BlockPos(118, DY + 1, z), door.setValue(com.alexh.capitalism.decor.BloodDoorBlock.HALF,
                    net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 3);
                continue;
            }
            var state = block.defaultBlockState().setValue(com.alexh.capitalism.decor.BloodDecorBlock.FACING, west);
            var kind = ((com.alexh.capitalism.decor.BloodDecorKind.Holder) block).kind();
            if (kind.wallMounted || kind == com.alexh.capitalism.decor.BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE) {
                int w = kind == com.alexh.capitalism.decor.BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE ? 2 : 1;
                for (int y = 0; y < 4; y++) for (int dz = -w; dz <= w; dz++) level.setBlock(new BlockPos(119, DY + y, z + dz), wall, 3);
                level.setBlock(new BlockPos(118, DY + (kind.wallMounted ? 1 : 0), z), state, 3);
                if (kind.multiBlock()) com.alexh.capitalism.decor.BloodDecorBlock.placeParts(level, new BlockPos(118, DY, z), west, kind, new BlockPos(118, DY, z));
            } else if (kind.ceiling) {
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlock(new BlockPos(118 + dx, DY + 3, z + dz), Blocks.DARK_OAK_PLANKS.defaultBlockState(), 3);
                level.setBlock(new BlockPos(118, DY + 3, z).below(), state, 3);
            } else {
                level.setBlock(new BlockPos(118, DY, z), state, 3);
                if (kind.multiBlock()) com.alexh.capitalism.decor.BloodDecorBlock.placeParts(level, new BlockPos(118, DY, z), west, kind, new BlockPos(118, DY, z));
            }
        }
    }

    private static <T extends Entity> T spawn(ServerLevel level, EntityType<T> type, double x, double y, double z, float yaw) {
        T e = type.create(level, EntitySpawnReason.COMMAND);
        if (e == null) return null;
        e.snapTo(OX + x, y, OZ + z, yaw, 0.0F);
        level.addFreshEntity(e);
        return e;
    }
}
