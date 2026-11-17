package com.alexh.capitalism.bounty;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.calendar.Calendar;
import com.alexh.capitalism.core.TougherMobs;
import com.alexh.capitalism.event.BloodMoon;
import com.alexh.capitalism.event.BloodMoonBoss;
import com.alexh.capitalism.fishing.CatchTier;
import com.alexh.capitalism.fishing.RareCatch;
import com.alexh.capitalism.fishing.RareCatchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.brewing.PlayerBrewedPotionEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.TradeWithVillagerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

// Counts bounty progress from what players do (Bounties.md > How tasks count). Only tasks already taken count
// (Bounties.count only touches open tasks), so nothing done before taking one counts. The mod's own actions call the
// public hooks below (a Mailbox letter, a Shopfront sale, feeding a pet, uncorking a bottle, cracking a pirate chest,
// a rare catch, the Blood Moon starting and ending).
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BountyEvents {

    private BountyEvents() {
    }

    private static boolean isNight(Level level) {
        return !Calendar.isDay(Calendar.clock(level));
    }

    // ── fighting ──

    // who has hurt each boss (for "help slay" tasks)
    private static final Map<Entity, Set<UUID>> HELPERS = new WeakHashMap<>();
    // kills this Blood Moon, and who's been alive all through it
    private static final Map<UUID, Integer> MOON_KILLS = new HashMap<>();
    private static final Set<UUID> MOON_SURVIVORS = new HashSet<>();

    static boolean isBoss(Entity e) {
        return e instanceof BloodMoonBoss || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss
            || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.monster.warden.Warden
            || e instanceof net.minecraft.world.entity.monster.ElderGuardian;
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent.Post event) {
        if (isBoss(event.getEntity()) && event.getSource().getEntity() instanceof ServerPlayer player) {
            HELPERS.computeIfAbsent(event.getEntity(), e -> new HashSet<>()).add(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        if (dead instanceof ServerPlayer player) MOON_SURVIVORS.remove(player.getUUID());
        // bosses: everyone who hurt it, or was within 32 blocks when it died
        if (isBoss(dead)) {
            Set<UUID> helped = HELPERS.getOrDefault(dead, Set.of());
            for (ServerPlayer p : level.players()) {
                if (helped.contains(p.getUUID()) || p.distanceToSqr(dead) <= 32 * 32) {
                    Bounties.count(p, t -> t.kind == BountyTask.Kind.HELP_KILL && t.matchesEntity(dead.getType()), 1);
                }
            }
            HELPERS.remove(dead);
        }
        if (!(event.getSource().getEntity() instanceof ServerPlayer killer)) return;
        boolean night = isNight(level);
        Bounties.count(killer, t -> (t.kind == BountyTask.Kind.KILL || (t.kind == BountyTask.Kind.KILL_AT_NIGHT && night)) && t.matchesEntity(dead.getType()), 1);
        if (BloodMoon.isActive(level) && dead instanceof Enemy) {
            if (TougherMobs.levelOf(dead) == TougherMobs.Level.TWO || TougherMobs.levelOf(dead) == TougherMobs.Level.BOSS) {
                Bounties.count(killer, t -> t.kind == BountyTask.Kind.BLOOD_MONSTER, 1);
            }
            int kills = MOON_KILLS.merge(killer.getUUID(), 1, Integer::sum);
            Bounties.setProgress(killer, t -> t.kind == BountyTask.Kind.BLOOD_KILLS, kills);
        }
    }

    // the Blood Moon rose (event/BloodMoon.start): everyone on now starts surviving it; kill counts start from nothing
    public static void bloodMoonStarted(MinecraftServer server) {
        MOON_KILLS.clear();
        MOON_SURVIVORS.clear();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            MOON_SURVIVORS.add(p.getUUID());
            Bounties.setProgress(p, t -> t.kind == BountyTask.Kind.BLOOD_KILLS, 0);
        }
    }

    // the Blood Moon set (event/BloodMoon.end): who survived it all, who topped the kill board
    public static void bloodMoonEnded(MinecraftServer server, boolean dawn, UUID top) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (dawn && MOON_SURVIVORS.contains(p.getUUID()) && p.isAlive()) Bounties.count(p, t -> t.kind == BountyTask.Kind.BLOOD_SURVIVE, 1);
            if (top != null && p.getUUID().equals(top)) Bounties.count(p, t -> t.kind == BountyTask.Kind.BLOOD_TOP, 1);
        }
        MOON_SURVIVORS.clear();
    }

    @SubscribeEvent
    public static void onLogoutMoon(PlayerEvent.PlayerLoggedOutEvent event) {
        MOON_SURVIVORS.remove(event.getEntity().getUUID());
    }

    // winning a raid: Hero of the Village
    @SubscribeEvent
    public static void onEffect(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getEffectInstance().getEffect().is(MobEffects.HERO_OF_THE_VILLAGE)) {
            Bounties.count(player, t -> t.kind == BountyTask.Kind.RAID_WIN, 1);
        }
    }

    // ── blocks ──

    static boolean matchesBlock(BountyTask task, BlockState state) {
        for (String target : task.targets) {
            if (target.startsWith("#")) {
                if (state.is(TagKey.create(Registries.BLOCK, Identifier.parse(target.substring(1))))) return true;
            } else if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString().equals(target)) {
                return true;
            }
        }
        return false;
    }

    // last, so a break something else (town protection...) cancelled doesn't count
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        BlockState state = event.getState();
        Bounties.count(player, t -> t.kind == BountyTask.Kind.MINE && matchesBlock(t, state), 1);
        boolean ripe = !(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state);
        if (ripe) Bounties.count(player, t -> t.kind == BountyTask.Kind.HARVEST && matchesBlock(t, state), 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.isCanceled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        BlockState state = event.getPlacedBlock();
        Bounties.count(player, t -> t.kind == BountyTask.Kind.PLANT && matchesBlock(t, state), 1);
    }

    // a structure's chest opened for the first time (its loot table still unrolled), a vault opened with a key
    private static final Map<UUID, Integer> VAULT_KEYS = new HashMap<>();
    private static final Map<UUID, Boolean> VAULT_OMINOUS = new HashMap<>();

    @SubscribeEvent
    public static void onUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Level level = player.level();
        BlockPos pos = event.getPos();
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest && chest.getLootTable() != null) {
            String table = chest.getLootTable().identifier().getPath();
            Bounties.count(player, t -> t.kind == BountyTask.Kind.LOOT_CHEST && t.targets.stream().anyMatch(table::startsWith), 1);
        }
        if (level.getBlockState(pos).is(Blocks.VAULT)) {
            ItemStack held = event.getItemStack();
            if (held.is(Items.TRIAL_KEY) || held.is(Items.OMINOUS_TRIAL_KEY)) {
                // counted next tick, if the key was taken (the vault opened for them)
                VAULT_KEYS.put(player.getUUID(), player.getInventory().countItem(held.getItem()));
                VAULT_OMINOUS.put(player.getUUID(), held.is(Items.OMINOUS_TRIAL_KEY));
            }
        }
    }

    // ── animals ──

    @SubscribeEvent
    public static void onBreed(BabyEntitySpawnEvent event) {
        if (event.getCausedByPlayer() instanceof ServerPlayer player) Bounties.count(player, t -> t.kind == BountyTask.Kind.BREED, 1);
    }

    @SubscribeEvent
    public static void onTame(AnimalTameEvent event) {
        if (event.getTamer() instanceof ServerPlayer player) {
            Bounties.count(player, t -> t.kind == BountyTask.Kind.TAME && t.matchesEntity(event.getAnimal().getType()), 1);
        }
    }

    @SubscribeEvent
    public static void onUseEntity(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();
        if (held.is(Items.SHEARS) && target instanceof net.minecraft.world.entity.animal.sheep.Sheep sheep && sheep.readyForShearing()) {
            Bounties.count(player, t -> t.kind == BountyTask.Kind.SHEAR, 1);
        }
        if (held.is(Items.BUCKET) && target instanceof net.minecraft.world.entity.animal.cow.AbstractCow cow && !cow.isBaby()) {
            Bounties.count(player, t -> t.kind == BountyTask.Kind.MILK, 1);
        }
    }

    // (pets/Pets) the owner fed their pet
    public static void petFed(ServerPlayer owner) {
        Bounties.count(owner, t -> t.kind == BountyTask.Kind.PET_FEED, 1);
    }

    // has a pet (a claimed animal of theirs in a loaded part of their world)
    public static boolean hasPet(ServerPlayer player) {
        for (Entity e : player.level().getAllEntities()) {
            if (e instanceof Animal a && com.alexh.capitalism.pets.Pets.claimed(a) && com.alexh.capitalism.pets.Pets.isOwner(a, player)) return true;
        }
        return false;
    }

    // ── fishing ──

    @SubscribeEvent
    public static void onFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        for (ItemStack s : event.getDrops()) {
            if (s.is(ItemTags.FISHES) || s.getItem() instanceof RareCatchItem) Bounties.count(player, t -> t.kind == BountyTask.Kind.CATCH, 1);
            Bounties.count(player, t -> t.kind == BountyTask.Kind.CATCH_ITEM && t.matchesItem(s.getItem()), 1);
            Bounties.count(player, t -> t.kind == BountyTask.Kind.TREASURE && t.matchesItem(s.getItem()), 1);
            catchSet(player, s);
        }
    }

    // "a Pufferfish and a Tropical Fish": each of its targets once (progress is a set of bits)
    private static void catchSet(ServerPlayer player, ItemStack s) {
        BountyData data = Bounties.of(player);
        for (BountyData.Open o : data.open()) {
            BountyTask task = o.task();
            if (task == null || task.kind != BountyTask.Kind.CATCH_SET) continue;
            int i = task.targets.indexOf(BuiltInRegistries.ITEM.getKey(s.getItem()).toString());
            if (i < 0) continue;
            Bounties.setProgress(player, t -> t == task, Math.min(o.amount(), Integer.bitCount(caught(player, task, i))));
        }
    }

    private static final Map<UUID, Map<String, Integer>> CAUGHT_BITS = new HashMap<>();

    private static int caught(ServerPlayer player, BountyTask task, int index) {
        Map<String, Integer> m = CAUGHT_BITS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        int bits = m.getOrDefault(task.id, 0) | (1 << index);
        m.put(task.id, bits);
        return bits;
    }

    // (fishing/RareCatchModifier.land) a player landed a rare catch (so the Luck of the Sea rule holds)
    public static void rareCaught(ServerPlayer player, RareCatch rare) {
        Bounties.count(player, t -> t.kind == BountyTask.Kind.RARE_FISH, 1);
        if (rare.tier().ordinal() >= CatchTier.EPIC.ordinal()) Bounties.count(player, t -> t.kind == BountyTask.Kind.EPIC_FISH, 1);
    }

    // (fishing/RareFindItems) uncorked a Message in a Bottle / cracked a Sunken Pirate Chest
    public static void uncorked(ServerPlayer player) {
        Bounties.count(player, t -> t.kind == BountyTask.Kind.UNCORK, 1);
    }

    public static void crackedChest(ServerPlayer player) {
        Bounties.count(player, t -> t.kind == BountyTask.Kind.CRACK_CHEST, 1);
    }

    // ── making things ──

    @SubscribeEvent
    public static void onSmelt(PlayerEvent.ItemSmeltedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ItemStack stack = event.getSmelting();
        Bounties.count(player, t -> t.kind == BountyTask.Kind.SMELT && t.matchesItem(stack.getItem()), stack.getCount());
    }

    @SubscribeEvent
    public static void onBrew(PlayerBrewedPotionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Bounties.count(player, t -> t.kind == BountyTask.Kind.BREW, 1);
    }

    @SubscribeEvent
    public static void onEnchant(PlayerEnchantItemEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Bounties.count(player, t -> t.kind == BountyTask.Kind.ENCHANT, 1);
    }

    // a repair bought from a town NPC smith (npc/NpcShopMenu.repair; the anvil repairs nothing, Smiths.md)
    public static void onNpcRepair(ServerPlayer player) {
        Bounties.count(player, t -> t.kind == BountyTask.Kind.REPAIR, 1);
    }

    // ── town ──

    @SubscribeEvent
    public static void onTrade(TradeWithVillagerEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) Bounties.count(player, t -> t.kind == BountyTask.Kind.TRADE, 1);
    }

    // (mail/MailboxMenu) sent something by Mailbox
    public static void mailSent(ServerPlayer sender) {
        Bounties.count(sender, t -> t.kind == BountyTask.Kind.MAIL, 1);
    }

    // (economy/ShopfrontBlockEntity.buy) a sale: the buyer bought, the owner (if they're on) sold; a meal (the mod's
    // food) from a Cook's Shopfront counts for "buy a meal from a Cook"
    public static void shopSale(ServerPlayer buyer, ServerPlayer owner, ItemStack goods, boolean ownerIsCook) {
        Bounties.count(buyer, t -> t.kind == BountyTask.Kind.SHOP_BUY, 1);
        boolean meal = goods.has(net.minecraft.core.component.DataComponents.FOOD)
            && BuiltInRegistries.ITEM.getKey(goods.getItem()).getNamespace().equals(Capitalism.MODID);
        if (meal && ownerIsCook) Bounties.count(buyer, t -> t.kind == BountyTask.Kind.COOK_BUY, 1);
        if (owner != null) Bounties.count(owner, t -> t.kind == BountyTask.Kind.SHOP_SELL, 1);
    }

    // ── going places ──

    private static final Map<UUID, Vec3> LAST_POS = new HashMap<>();
    private static final Map<UUID, double[]> PARTIAL = new HashMap<>();
    // biomes seen today
    private static final Map<UUID, Set<String>> BIOMES = new HashMap<>();
    private static final Map<UUID, Long> BIOME_DAY = new HashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        // a vault key taken since last tick: the vault opened for them
        Integer keys = VAULT_KEYS.remove(player.getUUID());
        if (keys != null) {
            boolean ominous = VAULT_OMINOUS.getOrDefault(player.getUUID(), false);
            if (player.getInventory().countItem(ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY) < keys) {
                Bounties.count(player, t -> t.kind == (ominous ? BountyTask.Kind.OMINOUS_VAULT : BountyTask.Kind.VAULT), 1);
            }
        }
        if (player.tickCount % 10 != 0) return;
        Vec3 now = player.position();
        Vec3 last = LAST_POS.put(player.getUUID(), now);
        if (last == null) return;
        if (Bounties.wants(player, BountyTask.Kind.REACH_LOW) && now.y <= -50) Bounties.count(player, t -> t.kind == BountyTask.Kind.REACH_LOW, 1);
        if (Bounties.wants(player, BountyTask.Kind.REACH_HIGH) && now.y >= 200) Bounties.count(player, t -> t.kind == BountyTask.Kind.REACH_HIGH, 1);
        if (Bounties.wants(player, BountyTask.Kind.BIOMES) && player.tickCount % 20 == 0) {
            long day = Bounties.today(player.level());
            if (!Long.valueOf(day).equals(BIOME_DAY.get(player.getUUID()))) {
                BIOME_DAY.put(player.getUUID(), day);
                BIOMES.put(player.getUUID(), new HashSet<>());
            }
            Set<String> seen = BIOMES.computeIfAbsent(player.getUUID(), id -> new HashSet<>());
            player.level().getBiome(player.blockPosition()).unwrapKey().ifPresent(k -> seen.add(k.identifier().toString()));
            Bounties.setProgress(player, t -> t.kind == BountyTask.Kind.BIOMES, seen.size());
        }
        double d = Math.hypot(now.x - last.x, now.z - last.z);
        if (d > 40 || d <= 0) return;   // standing still, or a teleport
        Entity vehicle = player.getVehicle();
        BountyTask.Kind how = vehicle == null ? (player.isFallFlying() ? null : BountyTask.Kind.WALK)
            : vehicle instanceof net.minecraft.world.entity.vehicle.boat.AbstractBoat ? BountyTask.Kind.BOAT
            : vehicle instanceof net.minecraft.world.entity.animal.equine.AbstractHorse ? BountyTask.Kind.HORSE : null;
        travel(player, BountyTask.Kind.TRAVEL, 0, d);
        if (how != null) travel(player, how, how == BountyTask.Kind.WALK ? 1 : how == BountyTask.Kind.BOAT ? 2 : 3, d);
    }

    private static void travel(ServerPlayer player, BountyTask.Kind kind, int slot, double d) {
        if (!Bounties.wants(player, kind)) return;
        double[] partial = PARTIAL.computeIfAbsent(player.getUUID(), id -> new double[4]);
        partial[slot] += d;
        int whole = (int) partial[slot];
        partial[slot] -= whole;
        if (whole > 0) Bounties.count(player, t -> t.kind == kind, whole);
    }

    // back from the Nether (alive: dying doesn't change dimension this way)
    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getFrom() == Level.NETHER && event.getTo() == Level.OVERWORLD) {
            Bounties.count(player, t -> t.kind == BountyTask.Kind.NETHER_RETURN, 1);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        LAST_POS.remove(id);
        PARTIAL.remove(id);
        VAULT_KEYS.remove(id);
        VAULT_OMINOUS.remove(id);
    }
}
