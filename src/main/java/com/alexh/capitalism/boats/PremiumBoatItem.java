package com.alexh.capitalism.boats;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Supplier;

// A Premium boat as an item (Boats.md: the Pirate Ship, the Speedboat): put it on water like a boat (this is vanilla's
// BoatItem, but finding the entity type when it's used, as items are made before entity types).
public class PremiumBoatItem extends Item {

    private final Supplier<? extends EntityType<? extends PremiumBoat>> type;

    public PremiumBoatItem(Supplier<? extends EntityType<? extends PremiumBoat>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        Vec3 view = player.getViewVector(1.0F);
        List<Entity> entities = level.getEntities(player, player.getBoundingBox().expandTowards(view.scale(5.0)).inflate(1.0), EntitySelector.CAN_BE_PICKED);
        Vec3 eye = player.getEyePosition();
        for (Entity entity : entities) {
            AABB box = entity.getBoundingBox().inflate(entity.getPickRadius());
            if (box.contains(eye)) return InteractionResult.PASS;
        }
        PremiumBoat ship = this.type.get().create(level, EntitySpawnReason.SPAWN_ITEM_USE);
        if (ship == null) return InteractionResult.FAIL;
        Vec3 at = hit.getLocation();
        ship.setInitialPos(at.x, at.y, at.z);
        if (level instanceof ServerLevel serverLevel) EntityType.createDefaultStackConfig(serverLevel, stack, player).apply(ship);
        ship.setYRot(player.getYRot());
        ship.setOwner(player);
        if (!level.noCollision(ship, ship.getBoundingBox())) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            level.addFreshEntity(ship);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, at);
            stack.consume(1, player);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS;
    }
}
