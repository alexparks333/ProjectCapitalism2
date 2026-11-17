package com.alexh.capitalism.farmer;

import com.alexh.capitalism.Capitalism;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

// The Farmer's world events (Farmer.md): crops on Fast Fertilizer grow faster (FertilizerItem.grow), and Meat Slabs
// drop from animals (MeatCut)
@EventBusSubscriber(modid = Capitalism.MODID)
public final class FarmerEvents {

    private FarmerEvents() {
    }

    @SubscribeEvent
    public static void onCropGrown(CropGrowEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) FertilizerItem.grow(level, event.getPos(), level.getRandom());
    }

    // A seed on Fast Fertilizer may sprout early (FertilizerItem.sprout)
    @SubscribeEvent
    public static void onCropGrowCheck(CropGrowEvent.Pre event) {
        if (event.getResult() == CropGrowEvent.Pre.Result.DEFAULT && event.getLevel() instanceof ServerLevel level
            && FertilizerItem.sprout(level, event.getPos(), level.getRandom())) event.setResult(CropGrowEvent.Pre.Result.GROW);
    }

    // An animal killed by anyone who isn't a Farmer (or by nobody: lava, fire, falling) drops just 1 piece of meat, like
    // crops give everyone else 1 crop + 1 seed. Runs before Butchery Basics (SkillEffects), which can still add one.
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGH)
    public static void onMeatDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.animal.Animal)) return;
        if (event.getSource().getEntity() instanceof Player player
            && (player.isCreative() || com.alexh.capitalism.occupation.JobEvents.has(player, com.alexh.capitalism.occupation.Job.FARMER))) return;
        capMeat(event.getDrops());
    }

    // Leave exactly one piece of meat in the drops (the first meat stack shrinks to 1, any other meat goes)
    public static void capMeat(java.util.Collection<ItemEntity> drops) {
        boolean kept = false;
        for (java.util.Iterator<ItemEntity> it = drops.iterator(); it.hasNext(); ) {
            ItemStack stack = it.next().getItem();
            if (!stack.is(net.minecraft.tags.ItemTags.MEAT)) continue;
            if (kept) it.remove();
            else {
                stack.setCount(1);
                kept = true;
            }
        }
    }

    // A Farmer's kill with a butchering blade may drop a Meat Slab too (MeatCut)
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        Player killer = event.getSource().getEntity() instanceof Player player ? player : null;
        ItemStack slab = MeatCut.slabDrop(killer, mob, mob.getRandom());
        if (slab != null) event.getDrops().add(new ItemEntity(mob.level(), mob.getX(), mob.getY(), mob.getZ(), slab));
    }
}
