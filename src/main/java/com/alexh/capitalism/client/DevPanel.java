package com.alexh.capitalism.client;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.librarian.Skill;
import com.alexh.capitalism.librarian.Skills;
import com.alexh.capitalism.librarian.Traits;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// A debugging panel in the bottom left (/devpanel toggles it, client only). Shows how long the last block took to mine
// (counted in ticks while you held the button on it), how fast you're moving (blocks a second, flat), the attributes
// skill books change, and your active lessons and traits with their level. GUI layer "capitalism:dev_panel".
@EventBusSubscriber(modid = Capitalism.MODID, value = Dist.CLIENT)
public final class DevPanel {

    private static boolean shown;

    // mining: the block being mined and for how many ticks; the last one finished
    private static BlockPos miningPos;
    private static String miningName = "";
    private static int miningTicks;
    private static float lastMineSeconds = -1;
    private static String lastMineName = "";
    // speed: the last few ticks' flat distance
    private static Vec3 lastPos;
    private static final double[] speeds = new double[10];
    private static int speedIndex;

    private DevPanel() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("devpanel").executes(context -> {
            shown = !shown;
            context.getSource().sendSystemMessage(Component.literal("Dev panel " + (shown ? "on" : "off") + "."));
            return 1;
        }));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null || mc.gameMode == null) {
            lastPos = null;
            return;
        }
        // speed: what you (or what you ride) moved flat this tick, averaged over half a second
        Entity mover = player.getVehicle() != null ? player.getVehicle() : player;
        Vec3 pos = mover.position();
        if (lastPos != null) {
            speeds[speedIndex] = Math.hypot(pos.x - lastPos.x, pos.z - lastPos.z) * 20.0;
            speedIndex = (speedIndex + 1) % speeds.length;
        }
        lastPos = pos;

        // mining: count ticks while the button is held on a block; when that block turns to air, that's the time
        if (mc.gameMode.isDestroying() && mc.hitResult instanceof BlockHitResult hit && mc.hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos at = hit.getBlockPos();
            if (!at.equals(miningPos)) {
                miningPos = at;
                miningTicks = 0;
                miningName = mc.level.getBlockState(at).getBlock().getName().getString();
            }
            miningTicks++;
        } else if (miningPos != null) {
            if (mc.level.getBlockState(miningPos).isAir()) {
                lastMineSeconds = (miningTicks + 1) / 20.0F;
                lastMineName = miningName;
            }
            miningPos = null;
        }
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!shown) return;
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        Font font = mc.font;

        List<String> lines = new ArrayList<>();
        lines.add("§6Dev Panel §7(/devpanel)");
        lines.add("Last block: " + (lastMineSeconds < 0 ? "-" : String.format(Locale.ROOT, "%.2fs", lastMineSeconds) + " §7" + lastMineName));
        if (miningPos != null) lines.add("§7  mining " + miningName + "... " + String.format(Locale.ROOT, "%.2fs", miningTicks / 20.0F));
        if (mc.hitResult instanceof BlockHitResult hit && mc.hitResult.getType() == HitResult.Type.BLOCK && mc.level != null) {
            BlockState state = mc.level.getBlockState(hit.getBlockPos());
            float perTick = state.getDestroyProgress(player, mc.level, hit.getBlockPos());
            lines.add("§7  looking at: " + (perTick <= 0 ? "unbreakable" : perTick >= 1 ? "instant"
                : String.format(Locale.ROOT, "%.2fs to mine", Math.ceil(1.0 / perTick) / 20.0)));
        }
        double speed = 0;
        for (double s : speeds) speed += s;
        speed /= speeds.length;
        String how = player.getVehicle() != null ? "riding" : player.isSprinting() ? "sprinting" : player.isCrouching() ? "sneaking"
            : player.isSwimming() || player.isInWater() ? "in water" : "walking";
        lines.add(String.format(Locale.ROOT, "Speed: %.2f b/s §7(%s)", speed, how));
        lines.add(attribute(player, Attributes.MOVEMENT_SPEED, "Move speed"));
        lines.add(attribute(player, Attributes.BLOCK_BREAK_SPEED, "Mining speed"));
        lines.add(attribute(player, Attributes.WATER_MOVEMENT_EFFICIENCY, "Swim"));
        lines.add(attribute(player, Attributes.OXYGEN_BONUS, "Oxygen bonus"));
        lines.add(attribute(player, Attributes.FALL_DAMAGE_MULTIPLIER, "Fall damage x"));
        lines.add(attribute(player, Attributes.SAFE_FALL_DISTANCE, "Safe fall"));
        lines.add(attribute(player, Attributes.ATTACK_DAMAGE, "Attack"));
        lines.add(attribute(player, Attributes.BLOCK_INTERACTION_RANGE, "Reach"));
        lines.add(attribute(player, Attributes.LUCK, "Luck"));
        if (player.getVehicle() instanceof net.minecraft.world.entity.LivingEntity ridden) {
            lines.add(attribute(ridden, Attributes.MOVEMENT_SPEED, "Mount speed"));
        }
        // skill books running
        StringBuilder skills = new StringBuilder();
        for (MobEffectInstance effect : player.getActiveEffects()) {
            Skill skill = Skills.lessonOf(effect);
            if (skill != null) skills.append(skills.isEmpty() ? "" : ", ").append(skill.title).append(effect.getAmplifier() > 0 ? " II" : "");
        }
        lines.add("Lessons: §a" + (skills.isEmpty() ? "none" : skills));
        StringBuilder traits = new StringBuilder();
        for (Skill skill : Traits.of(player).traits()) traits.append(traits.isEmpty() ? "" : ", ").append(skill.title);
        lines.add("Traits: §6" + (traits.isEmpty() ? "none" : traits));

        int width = 0;
        for (String line : lines) width = Math.max(width, font.width(line));
        int x = 4;
        int y = mc.getWindow().getGuiScaledHeight() - 4 - lines.size() * 10;
        graphics.fill(x - 2, y - 2, x + width + 3, y + lines.size() * 10, 0xA0000000);
        for (String line : lines) {
            graphics.text(font, line, x, y, 0xFFFFFFFF, false);
            y += 10;
        }
    }

    // "Move speed: 0.110 (base 0.100, +10%)"
    private static String attribute(net.minecraft.world.entity.LivingEntity entity, Holder<Attribute> attribute, String name) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) return name + ": -";
        double value = instance.getValue();
        double base = instance.getBaseValue();
        String change = "";
        if (Math.abs(value - base) > 1e-6) {
            change = base != 0 ? String.format(Locale.ROOT, " §a(%+.0f%%)", (value / base - 1) * 100) : String.format(Locale.ROOT, " §a(%+.2f)", value - base);
        }
        return String.format(Locale.ROOT, "%s: %.3f%s", name, value, change);
    }
}
