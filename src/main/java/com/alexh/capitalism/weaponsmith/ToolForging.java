package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

// Tools get an Efficiency stat (mining speed, as a % of normal), rolled once like a sword's stats (SwordForging):
// when crafted (mixin/ItemMixin; Toolsmith ranges if a Toolsmith made it), when found in loot (SwordLootModifier),
// or as soon as it's in a player's inventory (below). Pickaxes, shovels and axes; hoes don't get one.
// It scales the tool's own speed only: the Efficiency enchantment and Haste still add on top in full.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class ToolForging {

    // Everyone else (and every tool that wasn't crafted): 75-105
    public static final int OTHERS_MIN = 75;
    public static final int OTHERS_MAX = 105;
    // Toolsmith: 85-110, or 125 on a 1 in GRAIL_ODDS roll
    public static final int SMITH_MIN = 85;
    public static final int SMITH_MAX = 110;
    public static final int GRAIL = 125;
    public static final int GRAIL_ODDS = 150;

    private ToolForging() {
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.AXES);
    }

    public static boolean isForged(ItemStack stack) {
        return stack.has(ModComponents.TOOL_EFFICIENCY.get());
    }

    // crafter = null for a Crafter block, loot, trades... (counts as "everyone else")
    public static void roll(ItemStack stack, @Nullable Player crafter, RandomSource random) {
        if (!isTool(stack) || isForged(stack)) return;
        // The real job only: Creative mode doesn't make you a Toolsmith here
        boolean smith = crafter != null && JobEvents.has(crafter, Job.TOOLSMITH);
        int efficiency;
        if (smith) {
            efficiency = random.nextInt(GRAIL_ODDS) == 0 ? GRAIL : SMITH_MIN + random.nextInt(SMITH_MAX - SMITH_MIN + 1);
        } else {
            efficiency = OTHERS_MIN + random.nextInt(OTHERS_MAX - OTHERS_MIN + 1);
        }
        stack.set(ModComponents.TOOL_EFFICIENCY.get(), efficiency);
    }

    public static int efficiency(ItemStack stack) {
        return stack.getOrDefault(ModComponents.TOOL_EFFICIENCY.get(), 100);
    }

    public static boolean isGrail(ItemStack stack) {
        return efficiency(stack) >= GRAIL;
    }

    // Vanilla mining speed = (tool speed + Efficiency enchantment) x Haste, fatigue, water, jumping...
    // Scale only the tool speed part, by its Efficiency and by half if it's a dull pickaxe (Wear):
    // new = original x (tool x factor + enchant) / (tool + enchant).
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!isTool(tool)) return;
        float factor = efficiency(tool) / 100.0F * (Wear.isDullPickaxe(tool) ? 0.5F : 1.0F);
        if (factor == 1.0F) return;
        float toolSpeed = tool.getDestroySpeed(event.getState());
        if (toolSpeed <= 1.0F) return; // wrong tool for this block: no bonus either way
        float enchant = (float) player.getAttributeValue(Attributes.MINING_EFFICIENCY);
        float scaled = event.getNewSpeed() * (toolSpeed * factor + enchant) / (toolSpeed + enchant);
        event.setNewSpeed(scaled);
    }

    // Catch-all, once a second: any tool in a player's inventory that still has no Efficiency gets the everyone-else roll.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            roll(inventory.getItem(i), null, player.getRandom());
        }
    }
}
