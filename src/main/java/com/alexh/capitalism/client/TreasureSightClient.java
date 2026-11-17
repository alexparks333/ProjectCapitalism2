package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Treasure Sight's glow: each treasure chest the server tells us about gets a small glowing cyan "orb" in its middle
// (a client-only block display: a sea lantern shrunk to 40%, full bright, with a cyan glow outline). Glowing
// outlines draw through walls, so you see a glowing cyan spot where the chest is, even through the ground. Plus a
// few soft sparkles when you can see the chest. Only the person with the effect has these (the server never knows
// about them), and display entities have no hitbox or collision, so they never get in the way.
// History: an invisible shulker (players could stand on it and got kicked for flying), then a full-size glass
// block (a big white X-ray frame), then a client-only light block (barely visible).
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class TreasureSightClient {

    private static final int GLOW_COLOR = 0x3CE0FF;
    private static final float ORB_SIZE = 0.4F;

    private static final Map<BlockPos, Entity> ORBS = new HashMap<>();
    // Client-only entities get negative ids so they never clash with real ones from the server.
    private static int nextId = -1_000_000;
    private static ClientLevel orbLevel;
    private static boolean broken;

    private TreasureSightClient() {
    }

    public static void apply(List<BlockPos> chests) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (level != orbLevel) {
            ORBS.clear();   // old world: those entities are gone with it
            orbLevel = level;
        }
        ORBS.entrySet().removeIf(entry -> {
            if (chests.contains(entry.getKey())) return false;
            level.removeEntity(entry.getValue().getId(), Entity.RemovalReason.DISCARDED);
            return true;
        });
        for (BlockPos chest : chests) {
            if (ORBS.containsKey(chest) || broken) continue;
            Display.BlockDisplay orb = new Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, level) {
                @Override
                public boolean isCurrentlyGlowing() {
                    return true;
                }
            };
            if (!setUp(orb)) {
                broken = true;
                return;
            }
            orb.setId(nextId--);
            orb.snapTo(chest.getX(), chest.getY(), chest.getZ());   // a block display's corner sits on its position
            orb.setOldPosAndRot();
            level.addEntity(orb);
            ORBS.put(chest.immutable(), orb);
        }
    }

    // A block display's look is normally synced from the server, so its setters are private: set them by reflection.
    private static boolean setUp(Display.BlockDisplay orb) {
        try {
            call(Display.BlockDisplay.class, orb, "setBlockState", BlockState.class, Blocks.SEA_LANTERN.defaultBlockState());
            float offset = (1 - ORB_SIZE) / 2;   // centered in the chest's block
            call(Display.class, orb, "setTransformation", Transformation.class, new Transformation(
                new Vector3f(offset, offset, offset), new Quaternionf(), new Vector3f(ORB_SIZE, ORB_SIZE, ORB_SIZE), new Quaternionf()));
            call(Display.class, orb, "setGlowColorOverride", int.class, GLOW_COLOR);
            call(Display.class, orb, "setBrightnessOverride", Brightness.class, Brightness.FULL_BRIGHT);
            return true;
        } catch (ReflectiveOperationException | RuntimeException failed) {
            LogUtils.getLogger().warn("[{}] Treasure Sight can't show its glow: {}", Capitalism.MODID, failed.toString());
            return false;
        }
    }

    private static <T> void call(Class<?> owner, Object target, String name, Class<T> type, T value) throws ReflectiveOperationException {
        Method method = owner.getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    // A few soft sparkles rising from each chest.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level != orbLevel || ORBS.isEmpty() || level.getGameTime() % 8 != 0) return;
        RandomSource random = level.getRandom();
        for (BlockPos chest : ORBS.keySet()) {
            BlockPos above = chest.above();
            boolean water = level.getFluidState(above).is(Fluids.WATER);
            level.addParticle(water ? ParticleTypes.GLOW : ParticleTypes.END_ROD,
                chest.getX() + 0.2 + random.nextDouble() * 0.6, chest.getY() + 1.0, chest.getZ() + 0.2 + random.nextDouble() * 0.6,
                0, water ? 0.02 : 0.01, 0);
        }
    }
}
