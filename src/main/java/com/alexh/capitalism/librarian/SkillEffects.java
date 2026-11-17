package com.alexh.capitalism.librarian;

import com.alexh.capitalism.Capitalism;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerXpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

// What the skill books do when it isn't a plain attribute (Skill.attribute == null). Each asks Skills.level: 1 for a
// lesson at level I or a trait, 2 for a lesson at level II. Elsewhere: Water Rationing / Camel in
// core/ModServerEvents (thirst), Merchant's Ledger in economy/NpcPricing, Winter Survival in
// player/BodyTemperature, Night Owl / Iron Stomach in player/DrinkEffects (+ occupation/JobEvents for raw food),
// Bookworm in SkillBookItem, Lucky Librarian in LibrarianTableMenu, and mixins: FishingRodItemMixin (Fisherman's
// Patience), PlayerMixin (Trail Rations), PowderSnowBlockMixin (Sure Footed), FoodDataMixin (First Aid Basics),
// and LivingEntityMixin keeps lessons through milk.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class SkillEffects {

    private static final Identifier HORSE_SPEED = Identifier.fromNamespaceAndPath(Capitalism.MODID, "horse_whispering");
    private static final Identifier ICE_SPEED = Identifier.fromNamespaceAndPath(Capitalism.MODID, "sure_footed_ice");

    private SkillEffects() {
    }

    // ── Lumberjack's Log, Stonemason's Guide: faster at their blocks (both sides, like vanilla mining) ──
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player player = event.getEntity();
        BlockState state = event.getState();
        int level = 0;
        if (state.is(BlockTags.LOGS)) level = Skills.level(player, Skill.LUMBERJACKS_LOG);
        else if (isRock(state)) level = Skills.level(player, Skill.STONEMASONS_GUIDE);
        if (level > 0) event.setNewSpeed(event.getNewSpeed() * (1.0F + 0.25F * level));
    }

    private static boolean isRock(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Blocks.COBBLESTONE)
            || state.is(Blocks.MOSSY_COBBLESTONE) || state.is(Blocks.COBBLED_DEEPSLATE) || state.is(BlockTags.STONE_BRICKS);
    }

    // ── Butchery Basics: a chance of one more piece of meat from an animal you kill ──
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Animal)) return;
        int level = Skills.level(event.getSource().getEntity(), Skill.BUTCHERY_BASICS);
        if (level == 0 || event.getEntity().getRandom().nextFloat() >= 0.25F * level) return;
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(ItemTags.MEAT)) {
                drop.getItem().grow(1);
                return;
            }
        }
    }

    // ── Archery Drills: your arrows hit harder (not a thrown trident) ──
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow) || arrow instanceof net.minecraft.world.entity.projectile.arrow.ThrownTrident) return;
        int level = Skills.level(event.getSource().getEntity(), Skill.ARCHERY_DRILLS);
        if (level > 0) event.setAmount(event.getAmount() * (1.0F + 0.15F * level));
    }

    // ── Steady Aim: a fresh arrow from a bow / crossbow flies exactly where you look (crossbow side arrows keep their
    // angle: only arrows already close to your aim are straightened) ──
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof Player owner) || !Skills.has(owner, Skill.STEADY_AIM)) return;
        Vec3 motion = arrow.getDeltaMovement();
        double speed = motion.length();
        if (speed < 0.1) return;
        // (the view vector comes from lookup-table sines: not quite length 1)
        Vec3 look = owner.getViewVector(1.0F).normalize();
        if (motion.normalize().dot(look) < Math.cos(Math.toRadians(4))) return;
        arrow.setDeltaMovement(look.scale(speed));
    }

    // ── Quiet Steps: mobs have to be closer to notice you ──
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        int level = Skills.level(event.getEntity(), Skill.QUIET_STEPS);
        if (level > 0) event.modifyVisibility(1.0 - 0.2 * level);
    }

    // ── Beekeeper's Notes: bees won't go for you ──
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getEntity() instanceof Bee && Skills.has(event.getNewAboutToBeSetTarget(), Skill.BEEKEEPERS_NOTES)) {
            event.setCanceled(true);
        }
    }

    // ── Fast Learner: a quarter more XP from every orb you pick up ──
    @SubscribeEvent
    public static void onPickupXp(PlayerXpEvent.PickupXp event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !Skills.has(player, Skill.FAST_LEARNER)) return;
        float extra = event.getOrb().getValue() * 0.25F;
        int whole = (int) extra;
        if (player.getRandom().nextFloat() < extra - whole) whole++;
        if (whole > 0) player.giveExperiencePoints(whole);
    }

    // ── Every tick: Horse Whispering (the horse you ride), Sure Footed (on ice). First Aid Basics is mixin/FoodDataMixin ──
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        // Sure Footed on ice (both sides: the client moves the player)
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean onIce = player.onGround() && Skills.has(player, Skill.SURE_FOOTED) && onIce(player);
            if (onIce && !speed.hasModifier(ICE_SPEED)) {
                speed.addTransientModifier(new AttributeModifier(ICE_SPEED, 0.2, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!onIce && speed.hasModifier(ICE_SPEED)) {
                speed.removeModifier(ICE_SPEED);
            }
        }
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        // Horse Whispering, checked twice a second
        if (serverPlayer.tickCount % 10 == 0 && serverPlayer.getVehicle() instanceof AbstractHorse horse) {
            setHorseSpeed(horse, Skills.level(serverPlayer, Skill.HORSE_WHISPERING));
        }
    }

    private static boolean onIce(Player player) {
        BlockPos below = player.getBlockPosBelowThatAffectsMyMovement();
        return player.level().getBlockState(below).getFriction(player.level(), below, player) > 0.9F;
    }

    // Getting off: the horse is back to its own speed
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && event.getEntityBeingMounted() instanceof AbstractHorse horse) setHorseSpeed(horse, 0);
    }

    private static void setHorseSpeed(Entity horse, int level) {
        if (!(horse instanceof AbstractHorse living)) return;
        AttributeInstance speed = living.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (level <= 0) {
            speed.removeModifier(HORSE_SPEED);
        } else {
            speed.addOrUpdateTransientModifier(new AttributeModifier(HORSE_SPEED, 0.15 * level, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
