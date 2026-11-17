package com.alexh.capitalism.player;

import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import com.alexh.capitalism.weaponsmith.Socketing;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

// Body heat, like Rust's: a hidden -5 to 100 (shown as degrees) that slides toward what the world around you is doing to it (Wellness.md >
// Temperature). Worked out once a second on the server (WellnessEvents), saved, shown as the ball in the bottom right
// (client/TemperatureHud).
//
// Zones: Freezing below 32%, Cold 32-50%, Normal 50-80% (70% is just right), Hot (burning) above 80%.
//  - Freezing: frosty hearts, and you lose half a heart every few seconds, faster the colder you are (every 15s at
//    the edge, every 3s at 0%); never below 1 heart.
//  - Hot: you sweat (the water bar drips) and thirst drains x3 to x5, the hotter the faster.
//
// What pulls it (the target it slides toward):
//  - Cold: rain falling on you (25%), water at night (20%), water in a snowy place (-5), a snowy place at night or while it
//    snows (-5), powder snow (0%, fast). Water in a snowy place chills you twice as fast. Never in the Nether.
//  - Wet: wearing any leather into water soaks it. Wet counts as still being in that water (cold at night / in snowy
//    places, cooling somewhere hot) until you dry off: 5 seconds by a fire / fireplace (or in the Nether).
//  - Warmth against the cold: a fire, lit campfire, lit furnace / smoker / blast furnace, lava or magma within 4 blocks,
//    or a lit Blood Crystal Fireplace within 10 (it warms a whole base): back to 70%. A fire never makes you hot on
//    its own. Leather: each piece keeps a quarter of the cold off (all 4: none gets through), but not when it's wet;
//    in powder snow each piece softens it by 5%.
//  - Heat: the Nether (95%), a desert or badlands in the daytime with no rain (88%). Water (or being
//    wet) cools you there (65%). A fire nearby when you're already somewhere hot: burning (100%). Leather there: each
//    piece adds a quarter of the way to 100% (all 4: burning).
//  - Molten Sockets (weaponsmith/Socketing, any armor piece): Ice = you can't get hot, but the cold bites 10% harder;
//    Blaze Powder = the cold can't reach you, but somewhere hot you burn (100%). Both: neither.
//  - Cook meals: Campfire Chili (Warmed Up, 5 min): the cold can't reach you. Frosty Melon Sorbet (Cooled Off,
//    5 min): you can't get hot.
//  - Skill books: Winter Survival (trait) / Hot Pepper (lesson): the cold can't touch you. Ice In The Pants (lesson) / Used
//    To Hell (trait): never above 70%.
//  - Otherwise: 70%.
//
// How fast it moves (per second):
//  - Cooling: Hot -> down 3 (out of burning in a few seconds), Normal -> down 1.33 (70% to Cold in 15s), Cold and
//    below -> down 0.15 (Cold to Freezing in 2 minutes). Powder snow: down 1.5 all the way.
//  - Warming: by a heat source 2 (semi-fast out of Freezing, through Cold, back to Normal), without one 1 in the cold
//    zones and 1.5 up to 70%. Above 70% (heading for Hot): 0.67 (15s to Hot), then 0.3 deeper into burning.
public final class BodyTemperature {

    public static final float NORMAL = 70f;
    public static final float FREEZING_BELOW = 32f;
    public static final float COLD_BELOW = 50f;
    public static final float HOT_ABOVE = 80f;

    // targets
    public static final float RAIN_TARGET = 25f, COLD_WATER_TARGET = 20f, SNOWY_TARGET = -5f, SNOWY_WATER_TARGET = -5f, POWDER_SNOW_TARGET = 0f;
    // the coldest body heat can get (snowy nights and snowy water)
    public static final float MIN_HEAT = -5f;
    public static final float NETHER_TARGET = 95f, DESERT_SUN_TARGET = 88f, WATER_COOLED_TARGET = 65f, BURNING_TARGET = 100f;
    // powder snow: each leather piece softens it this much
    static final float LEATHER_POWDER_SOFTENS = 5f;
    // an Ice socket: the cold bites this much harder
    public static final float ICE_EXTRA_COLD = 10f;
    // water in a snowy place chills you this many times faster
    public static final float SNOWY_WATER_CHILL = 2f;
    public static final int HEAT_RANGE = 4;
    public static final int FIREPLACE_RANGE = 10;
    // wet leather dries after this many seconds by a fire
    public static final int DRY_SECONDS = 5;

    // Zones (synced as the ball)
    public static final int ZONE_FREEZING = 0, ZONE_COLD = 1, ZONE_NORMAL = 2, ZONE_HOT = 3;

    // Why: what's acting on you right now (a bitmask, sent for the hover)
    public static final int R_RAIN = 1, R_COLD_WATER = 2, R_SNOWY = 4, R_POWDER_SNOW = 8, R_FIRE = 16, R_FIREPLACE = 32,
        R_LEATHER = 64, R_NETHER = 128, R_SUN = 256, R_WATER_COOLING = 512, R_WINTER_SURVIVAL = 1024, R_WET = 2048,
        R_ICE = 4096, R_BLAZE = 8192, R_NO_HEAT = 16384, R_HOT_PEPPER = 32768, R_LEATHER_HOT = 65536, R_WARMED_UP = 131072, R_COOLED_OFF = 262144, R_HAND_WARMER = 524288;

    // Freezing: never below this much health
    public static final float MIN_HEALTH = 2f;

    private BodyTemperature() {
    }

    public static int zone(float heat) {
        if (heat < FREEZING_BELOW) return ZONE_FREEZING;
        if (heat < COLD_BELOW) return ZONE_COLD;
        if (heat <= HOT_ABOVE) return ZONE_NORMAL;
        return ZONE_HOT;
    }

    // Seconds between half-hearts lost while Freezing: 15 at the edge (32%), 3 at 0%
    public static int frostInterval(float heat) {
        return Math.max(3, Math.round(3f + 12f * Math.max(0f, heat) / FREEZING_BELOW));
    }

    // Thirst drain while Hot: x3 the moment you cross 80, up to x5 at 100 (normal: x1). Sweat shows on the water bar
    // (client/ThirstHud).
    public static final float HOT_THIRST_MIN = 3f, HOT_THIRST_MAX = 5f;
    public static float thirstMultiplier(float heat) {
        return heat <= HOT_ABOVE ? 1f : HOT_THIRST_MIN + (HOT_THIRST_MAX - HOT_THIRST_MIN) * (Math.min(100f, heat) - HOT_ABOVE) / 20f;
    }

    // Only places that look hot: desert and badlands (not savanna or anything else that is merely warm)
    public static boolean hotBiome(Level level, BlockPos pos) {
        var biome = level.getBiome(pos);
        return biome.is(net.minecraft.world.level.biome.Biomes.DESERT) || biome.is(net.minecraft.tags.BiomeTags.IS_BADLANDS);
    }

    // Leather against the cold: each piece keeps a quarter of it off (4 pieces: 70%). Wet leather does nothing; in
    // powder snow each piece only softens it by 5%.
    public static float leatherCold(float cold, int pieces, boolean wet, boolean powder) {
        if (pieces <= 0 || wet) return cold;
        if (powder) return Math.min(NORMAL, cold + LEATHER_POWDER_SOFTENS * pieces);
        return cold + (NORMAL - cold) * pieces / 4f;
    }

    // Leather somewhere hot: each piece adds a quarter of the way to burning (4 pieces: 100%)
    public static float leatherHot(float hot, int pieces) {
        if (pieces <= 0) return hot;
        return hot + (BURNING_TARGET - hot) * pieces / 4f;
    }

    // What the world is doing to you right now
    // chill: how many times faster than normal you cool down (water in a snowy place: x2)
    public record Climate(float target, boolean heated, boolean fast, int reasons, float chill) {
    }

    public static Climate climate(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        WellbeingData data = player.getData(ModAttachments.WELLBEING.get());
        boolean nether = level.dimension() == Level.NETHER;
        boolean wet = player.isInWater() || data.wet;   // wet leather: like still being in the water
        int reasons = data.wet ? R_WET : 0;
        int leather = leather(player);
        boolean ice = Socketing.armorWith(player, Socketing.Gem.ICE) > 0;
        boolean blaze = Socketing.armorWith(player, Socketing.Gem.BLAZE) > 0;

        // ── cold ──
        float cold = NORMAL;
        boolean fast = false;
        boolean powder = false;
        float chill = 1f;
        if (!nether) {
            boolean snowyPlace = level.getBiome(pos).value().coldEnoughToSnow(pos, level.getSeaLevel());
            if (player.isInPowderSnow || player.getTicksFrozen() > 0) {
                cold = POWDER_SNOW_TARGET;
                fast = true;
                powder = true;
                reasons |= R_POWDER_SNOW;
            }
            if (wet && (level.isDarkOutside() || snowyPlace)) {
                cold = Math.min(cold, snowyPlace ? SNOWY_WATER_TARGET : COLD_WATER_TARGET);
                if (snowyPlace) chill = SNOWY_WATER_CHILL;
                reasons |= R_COLD_WATER;
            }
            if (snowyPlace && (level.isDarkOutside() || level.isRaining())) {
                cold = Math.min(cold, SNOWY_TARGET);
                reasons |= R_SNOWY;
            }
            if (level.isRainingAt(BlockPos.containing(player.getEyePosition()))) {
                cold = Math.min(cold, RAIN_TARGET);
                reasons |= R_RAIN;
            }
        }

        // ── heat ──
        float hot = NORMAL;
        if (nether) {
            hot = NETHER_TARGET;
            reasons |= R_NETHER;
        } else if (!level.isRaining() && !level.isDarkOutside() && hotBiome(level, pos)) {
            hot = DESERT_SUN_TARGET;
            reasons |= R_SUN;
        }

        boolean isCold = cold < NORMAL;
        boolean isHot = hot > NORMAL;
        // only look for fires when it matters (it's a block scan)
        int source = isCold || isHot || data.wet || data.bodyHeat < NORMAL ? heatSource(player) : 0;
        reasons |= source;
        boolean heated = source != 0;

        if (isCold) {
            if (ice) {
                cold = Math.max(MIN_HEAT, cold - ICE_EXTRA_COLD);
                reasons |= R_ICE;
            }
            if (Skills.has(player, Skill.WINTER_SURVIVAL)) {
                cold = NORMAL;
                reasons |= R_WINTER_SURVIVAL;
            } else if (Skills.has(player, Skill.HOT_PEPPER)) {
                cold = NORMAL;
                reasons |= R_HOT_PEPPER;
            } else if (player.hasEffect(com.alexh.capitalism.core.ModEffects.WARMED_UP)) {
                cold = NORMAL;
                reasons |= R_WARMED_UP;
            } else if (blaze) {
                cold = NORMAL;
                reasons |= R_BLAZE;
            } else if (heated) {
                cold = NORMAL;
            } else if (player.getOffhandItem().is(com.alexh.capitalism.core.ModItems.HAND_WARMER.get())) {
                // (only burns down when nothing else is keeping you warm)
                cold = NORMAL;
                reasons |= R_HAND_WARMER;
            } else if (leather > 0 && !wet) {
                cold = leatherCold(cold, leather, false, powder);
                reasons |= R_LEATHER;
            }
        }
        if (isHot) {
            boolean noHeat = Skills.has(player, Skill.ICE_IN_THE_PANTS) || Skills.has(player, Skill.USED_TO_HELL);
            if (noHeat) {
                hot = NORMAL;
                reasons |= R_NO_HEAT;
            } else if (player.hasEffect(com.alexh.capitalism.core.ModEffects.COOLED_OFF)) {
                hot = NORMAL;
                reasons |= R_COOLED_OFF;
            } else if (ice) {
                hot = NORMAL;
                reasons |= R_ICE;
            } else if (!nether && wet) {
                hot = WATER_COOLED_TARGET;
                reasons |= R_WATER_COOLING;
            } else if (blaze || heated) {
                hot = BURNING_TARGET;
                if (blaze) reasons |= R_BLAZE;
            } else if (leather > 0) {
                hot = leatherHot(hot, leather);
                reasons |= R_LEATHER_HOT;
            }
        }

        float target = cold < NORMAL ? cold : hot;
        // Used To Hell / Ice In The Pants / Frosty Melon Sorbet: never above 70%, whatever else is going on
        if (target > NORMAL && (Skills.has(player, Skill.USED_TO_HELL) || Skills.has(player, Skill.ICE_IN_THE_PANTS)
            || player.hasEffect(com.alexh.capitalism.core.ModEffects.COOLED_OFF))) target = NORMAL;
        return new Climate(target, heated, fast && cold < NORMAL, reasons, cold < NORMAL ? chill : 1f);
    }

    // One second: body heat moves toward the target
    public static float step(float heat, float target, boolean heated, boolean fast) {
        return step(heat, target, heated, fast, 1f);
    }

    public static float step(float heat, float target, boolean heated, boolean fast, float chill) {
        if (target < heat) {
            float rate = (fast ? 1.5f : heat > HOT_ABOVE ? 3f : heat > COLD_BELOW ? 4f / 3f : 0.15f) * chill;
            return Math.max(target, heat - rate);
        }
        if (target > heat) {
            float rate = heat < COLD_BELOW ? (heated ? 2f : 1f)
                : heat < NORMAL ? (heated ? 2f : 1.5f)
                : heat < HOT_ABOVE ? 2f / 3f : 0.3f;
            return Math.min(target, heat + rate);
        }
        return heat;
    }

    public static int leather(ServerPlayer player) {
        int n = 0;
        if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.LEATHER_HELMET)) n++;
        if (player.getItemBySlot(EquipmentSlot.CHEST).is(Items.LEATHER_CHESTPLATE)) n++;
        if (player.getItemBySlot(EquipmentSlot.LEGS).is(Items.LEATHER_LEGGINGS)) n++;
        if (player.getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS)) n++;
        return n;
    }

    // R_FIRE: a fire, lit campfire, lit furnace / smoker / blast furnace, lava or magma within HEAT_RANGE (2 up / down);
    // R_FIREPLACE: a lit Blood Crystal Fireplace within FIREPLACE_RANGE. 0 = neither.
    public static int heatSource(ServerPlayer player) {
        Level level = player.level();
        BlockPos pos = player.blockPosition();
        int r = HEAT_RANGE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -2, -r), pos.offset(r, 2, r))) {
            BlockState state = level.getBlockState(p);
            if (state.is(BlockTags.FIRE) || state.is(Blocks.LAVA) || state.is(Blocks.MAGMA_BLOCK)) return R_FIRE;
            if ((state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof AbstractFurnaceBlock)
                && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) return R_FIRE;
        }
        int f = FIREPLACE_RANGE;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-f, -f, -f), pos.offset(f, f, f))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof com.alexh.capitalism.decor.BloodDecorBlock fire
                && fire.kind() == com.alexh.capitalism.decor.BloodDecorKind.BLOOD_CRYSTAL_FIREPLACE
                && state.getValue(BlockStateProperties.LIT)) return R_FIREPLACE;
        }
        return 0;
    }

    // Back to just right: 70%, dry (respawning, the Crimson Bed, A Fresh Start)
    public static void reset(WellbeingData data) {
        data.bodyHeat = NORMAL;
        data.frostSeconds = 0;
        data.wet = false;
        data.drySeconds = 0;
    }

    // Once a second (WellnessEvents): move the body heat, soak / dry leather, and Freezing costs health.
    // Creative / spectator: always 70%, dry.
    public static void tick(ServerPlayer player, WellbeingData data) {
        if (player.isCreative() || player.isSpectator()) {
            reset(data);
            data.heatReasons = 0;
            return;
        }
        Climate climate = climate(player);
        data.bodyHeat = step(data.bodyHeat, climate.target(), climate.heated(), climate.fast(), climate.chill());
        data.heatReasons = climate.reasons();
        // the Hand Warmer burns down a second at a time while it keeps the cold off
        if ((climate.reasons() & R_HAND_WARMER) != 0) {
            var warmer = player.getOffhandItem();
            warmer.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            if (warmer.isEmpty()) player.sendOverlayMessage(Component.literal("Your Hand Warmer has burnt out.").withStyle(ChatFormatting.GRAY));
        }

        // leather into water: soaked until it dries by a fire
        if (player.isInWater()) {
            if (!data.wet && leather(player) > 0) {
                data.wet = true;
                player.sendOverlayMessage(Component.literal("Your leather is soaked! Dry off by a fire.").withStyle(ChatFormatting.AQUA));
            }
            data.drySeconds = 0;
        } else if (data.wet) {
            if (climate.heated() || player.level().dimension() == Level.NETHER) {
                if (++data.drySeconds >= DRY_SECONDS) {
                    data.wet = false;
                    data.drySeconds = 0;
                    player.sendOverlayMessage(Component.literal("You're dry again.").withStyle(ChatFormatting.GOLD));
                }
            } else {
                data.drySeconds = 0;
            }
        }

        if (zone(data.bodyHeat) == ZONE_FREEZING) {
            if (++data.frostSeconds >= frostInterval(data.bodyHeat)) {
                data.frostSeconds = 0;
                if (player.getHealth() > MIN_HEALTH) player.hurtServer(player.level(), player.damageSources().freeze(), 1.0F);
            }
        } else {
            data.frostSeconds = 0;
        }
    }
}
