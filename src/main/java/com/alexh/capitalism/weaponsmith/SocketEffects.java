package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.weaponsmith.Socketing.Gem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import org.jspecify.annotations.Nullable;

import java.util.Set;

// What the newer Molten Socket gems do (Sockets.md). The older ones (Lapis, Redstone, Emerald, Quartz crits,
// Prismarine swim, Echo, Bone slabs) live in Socketing; Ice / Blaze Powder armor is body heat (player/BodyTemperature).
//  Sword:   Prismarine  hits 50% harder while you're underwater
//           Ghast Tear  +20% damage to Nether mobs
//           Ice         20% chance to slow what you hit (Slowness II, 3 s)
//           Glowstone   what you hit glows for 10 s
//           Amethyst    each kill heals half a heart
//           Blaze       lights up the area around you while held (an invisible light block, player/DrinkEffects)
//  Pickaxe: Prismarine  mines 3x as fast underwater
//           Quartz      stone and deepslate break 25% faster
//           Blaze       lights up the area around you while held
//  Axe:     Diamond     a log you chop splits into 8 planks of its wood
//           Blaze       a log you chop comes out as Charcoal
//           Flint       25% chance to make what you hit bleed (1 damage a second for 4 s, ModEffects.BLEEDING)
//  Armor:   Bone        5% less projectile damage per piece
//           Amethyst    +5% knockback resistance per piece (attribute, Socketing.onItemAttributes)
//           Rabbit Foot +1 Luck per piece (attribute: loot and fishing, the rare catches too)
//           Slime Ball  boots only: no fall damage
@EventBusSubscriber(modid = Capitalism.MODID)
public final class SocketEffects {

    // Mobs from the Nether (the Ghast Tear sword)
    public static final Set<EntityType<?>> NETHER_MOBS = Set.of(EntityTypes.BLAZE, EntityTypes.GHAST, EntityTypes.MAGMA_CUBE,
        EntityTypes.PIGLIN, EntityTypes.PIGLIN_BRUTE, EntityTypes.ZOMBIFIED_PIGLIN, EntityTypes.HOGLIN, EntityTypes.ZOGLIN,
        EntityTypes.STRIDER, EntityTypes.WITHER_SKELETON, EntityTypes.WITHER);

    private SocketEffects() {
    }

    private static @Nullable Gem held(LivingEntity entity) {
        return Socketing.gem(entity.getMainHandItem());
    }

    // Blaze Powder in the sword or pickaxe in either hand: the light follows you
    public static boolean holdsLight(Player player) {
        return lights(player.getMainHandItem()) || lights(player.getOffhandItem());
    }

    private static boolean lights(ItemStack stack) {
        int kind = Socketing.kind(stack);
        return (kind == Socketing.SWORD || kind == Socketing.PICKAXE) && Socketing.gem(stack) == Gem.BLAZE;
    }

    // ── Hitting ──

    // Damage before armor: the Prismarine / Ghast Tear sword, and Bone armor against projectiles
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        var source = event.getSource();
        // a melee hit with a socketed sword
        if (source.getEntity() instanceof LivingEntity attacker && source.getDirectEntity() == attacker
            && Socketing.isMeleeWeapon(attacker.getMainHandItem())) {
            Gem gem = held(attacker);
            if (gem == Gem.PRISMARINE && attacker.isEyeInFluid(FluidTags.WATER)) event.setAmount(event.getAmount() * Socketing.PRISMARINE_UNDERWATER_HIT);
            if (gem == Gem.GHAST_TEAR && NETHER_MOBS.contains(victim.getType())) event.setAmount(event.getAmount() * Socketing.GHAST_TEAR_NETHER_DAMAGE);
        }
        if (source.is(DamageTypeTags.IS_PROJECTILE)) {
            int bone = Socketing.armorWith(victim, Gem.BONE);
            if (bone > 0) event.setAmount(event.getAmount() * projectileTaken(bone));
        }
    }

    // How much projectile damage gets through with this many Bone-socketed pieces (4: 80%)
    public static float projectileTaken(int bonePieces) {
        return Math.max(0f, 1f - Socketing.BONE_PROJECTILE_PER_PIECE * bonePieces);
    }

    // A hit that landed: Ice slows, Glowstone lights them up, Flint makes them bleed
    @SubscribeEvent
    public static void onHit(LivingDamageEvent.Post event) {
        var source = event.getSource();
        if (!(source.getEntity() instanceof LivingEntity attacker) || source.getDirectEntity() != attacker) return;
        LivingEntity victim = event.getEntity();
        if (victim == attacker || event.getInflictedDamage() <= 0) return;
        ItemStack weapon = attacker.getMainHandItem();
        Gem gem = Socketing.gem(weapon);
        if (gem == null) return;
        int kind = Socketing.kind(weapon);
        var random = attacker.getRandom();
        if (kind == Socketing.SWORD && gem == Gem.ICE && random.nextFloat() < Socketing.ICE_SLOW_CHANCE) {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Socketing.ICE_SLOW_TICKS, 1), attacker);
        } else if (kind == Socketing.SWORD && gem == Gem.GLOWSTONE) {
            victim.addEffect(new MobEffectInstance(MobEffects.GLOWING, Socketing.GLOWSTONE_GLOW_TICKS, 0), attacker);
        } else if (kind == Socketing.AXE && gem == Gem.FLINT && random.nextFloat() < Socketing.FLINT_BLEED_CHANCE) {
            victim.addEffect(new MobEffectInstance(ModEffects.BLEEDING, Socketing.FLINT_BLEED_TICKS, 0), attacker);
        }
    }

    // Amethyst sword: every kill heals half a heart
    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer) || killer == event.getEntity()) return;
        ItemStack weapon = killer.getMainHandItem();
        if (Socketing.isMeleeWeapon(weapon) && Socketing.gem(weapon) == Gem.AMETHYST) killer.heal(Socketing.AMETHYST_KILL_HEAL);
    }

    // Slime Ball boots: no fall damage
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (Socketing.gem(event.getEntity().getItemBySlot(EquipmentSlot.FEET)) == Gem.SLIME) event.setDamageMultiplier(0f);
    }

    // ── Mining ──

    // Prismarine pickaxe: 3x underwater; Quartz pickaxe: stone and deepslate 25% faster
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        ItemStack tool = player.getMainHandItem();
        if (!Wear.isPickaxe(tool)) return;
        Gem gem = Socketing.gem(tool);
        if (gem == Gem.PRISMARINE && player.isEyeInFluid(FluidTags.WATER)) {
            event.setNewSpeed(event.getNewSpeed() * Socketing.PRISMARINE_UNDERWATER_MINING);
        } else if (gem == Gem.QUARTZ && isStone(event.getState())) {
            event.setNewSpeed(event.getNewSpeed() * Socketing.QUARTZ_STONE_MINING);
        }
    }

    public static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(Tags.Blocks.COBBLESTONES);
    }

    // ── Chopping ──

    // Diamond axe: a log comes out as 8 planks of its wood. Blaze Powder axe: as Charcoal.
    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        ItemStack tool = event.getTool();
        if (!Socketing.isAxe(tool)) return;
        Gem gem = Socketing.gem(tool);
        if (gem != Gem.DIAMOND && gem != Gem.BLAZE) return;
        BlockState state = event.getState();
        if (!state.is(BlockTags.LOGS)) return;
        Item log = state.getBlock().asItem();
        Item into = gem == Gem.BLAZE ? Items.CHARCOAL : planksFor(state.getBlock());
        if (into == null) return;
        int count = gem == Gem.BLAZE ? 1 : Socketing.DIAMOND_PLANKS;
        boolean changed = event.getDrops().removeIf(drop -> drop.getItem().is(log));
        if (!changed) return;
        BlockPos pos = event.getPos();
        event.getDrops().add(new ItemEntity(event.getLevel(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(into, count)));
    }

    // The planks a log / wood / stem / hyphae (stripped or not) makes: oak_log -> oak_planks, crimson_stem ->
    // crimson_planks, bamboo_block -> bamboo_planks. null if there's no such planks.
    public static @Nullable Item planksFor(Block log) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(log);
        String path = key.getPath().replace("stripped_", "");
        String wood = null;
        for (String suffix : new String[]{"_log", "_wood", "_stem", "_hyphae", "_block"}) {
            if (path.endsWith(suffix)) {
                wood = path.substring(0, path.length() - suffix.length());
                break;
            }
        }
        if (wood == null) return null;
        Item planks = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(key.getNamespace(), wood + "_planks"));
        return planks == Items.AIR ? null : planks;
    }
}
