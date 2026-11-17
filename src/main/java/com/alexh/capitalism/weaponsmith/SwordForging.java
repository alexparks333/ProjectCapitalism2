package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

// Every sword in the world gets its own stats, rolled once:
//  - crafted: the moment it leaves the result slot (mixin/ItemMixin), Weaponsmith ranges if a Weaponsmith made it;
//  - found in loot (chests, barrels, mob drops, fishing): when the loot is generated (SwordLootModifier);
//  - anything else (villager trades, the creative menu...): as soon as it's in a player's inventory (below).
// The stats:
//  - Sharpness: its damage as a % of normal.
//  - Edge: how many hits it lands before it goes dull (see Sharpness).
// A Weaponsmith forges better swords, with a very rare "grail" roll on each stat. Everyone else (and a Crafter block)
// gets the lower ranges, and so does every sword that wasn't crafted. A sword that already has stats (e.g. a Diamond
// Sword upgraded to Netherite) keeps them.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class SwordForging {

    // Everyone else (and every sword that wasn't crafted): sharpness 60-110, edge 113-169
    public static final int OTHERS_SHARPNESS_MIN = 60;
    public static final int OTHERS_SHARPNESS_MAX = 110;
    public static final int OTHERS_EDGE_MIN = 113;
    public static final int OTHERS_EDGE_MAX = 169;
    // Weaponsmith: sharpness 78-120 (or 135 on a grail roll), edge 150-225 (or 263 on a grail roll)
    public static final int SMITH_SHARPNESS_MIN = 78;
    public static final int SMITH_SHARPNESS_MAX = 120;
    public static final int SMITH_EDGE_MIN = 150;
    public static final int SMITH_EDGE_MAX = 225;
    public static final int GRAIL_SHARPNESS = 135;
    public static final int GRAIL_EDGE = 263;
    // 1 in 150 crafts, rolled separately for each stat
    public static final int GRAIL_ODDS = 150;

    private SwordForging() {
    }

    public static boolean isForged(ItemStack stack) {
        return stack.has(ModComponents.SWORD_SHARPNESS.get()) || stack.has(ModComponents.SWORD_EDGE.get());
    }

    // crafter = null for a Crafter block (counts as "everyone else")
    public static void roll(ItemStack stack, @Nullable Player crafter, RandomSource random) {
        if (!Sharpness.isSword(stack) || isForged(stack)) return;
        // The real job only: Creative mode doesn't make you a Weaponsmith here
        boolean smith = crafter != null && JobEvents.has(crafter, Job.WEAPONSMITH);
        int sharpness;
        int edge;
        if (smith) {
            sharpness = random.nextInt(GRAIL_ODDS) == 0 ? GRAIL_SHARPNESS : between(random, SMITH_SHARPNESS_MIN, SMITH_SHARPNESS_MAX);
            edge = random.nextInt(GRAIL_ODDS) == 0 ? GRAIL_EDGE : between(random, SMITH_EDGE_MIN, SMITH_EDGE_MAX);
        } else {
            sharpness = between(random, OTHERS_SHARPNESS_MIN, OTHERS_SHARPNESS_MAX);
            edge = between(random, OTHERS_EDGE_MIN, OTHERS_EDGE_MAX);
        }
        stack.set(ModComponents.SWORD_SHARPNESS.get(), sharpness);
        stack.set(ModComponents.SWORD_EDGE.get(), edge);
    }

    // Catch-all, once a second: any sword in a player's inventory that still has no stats gets the everyone-else roll.
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % 20 != 0) return;
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            roll(inventory.getItem(i), null, player.getRandom());
        }
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    // Damage as a % of normal
    public static int sharpness(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SWORD_SHARPNESS.get(), 100);
    }

    public static boolean isGrailSharpness(ItemStack stack) {
        return sharpness(stack) >= GRAIL_SHARPNESS;
    }

    public static boolean isGrailEdge(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SWORD_EDGE.get(), 0) >= GRAIL_EDGE;
    }
}
