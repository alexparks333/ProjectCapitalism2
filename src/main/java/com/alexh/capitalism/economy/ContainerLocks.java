package com.alexh.capitalism.economy;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

// Container locking, like Project Capitalism 1: anything that holds items is locked to whoever places it.
//  - Other players can't open or break it (admins can).
//  - Owner: shift + right-click with an empty hand to lock / unlock (it stays yours for net worth either way).
//  - Looking at one shows who owns it.
// Plus the usual ways around a lock are closed off: you can't join your chest onto someone's chest to make a
// double chest, can't put a hopper under their container to drain it, and explosions won't break locked containers.
@EventBusSubscriber(modid = Capitalism.MODID)
public class ContainerLocks {

    // ── Reading ownership ──────────────────────────────────────────────────

    // Something that really holds items. The Blood Moon decorations share one block entity (it is a Container for
    // the Iron Maiden's storage), but only the Iron Maiden holds anything: the gargoyle, throne and the rest aren't
    // lockable.
    public static boolean holdsItems(@Nullable BlockEntity blockEntity) {
        if (!(blockEntity instanceof Container)) return false;
        if (blockEntity.getBlockState().getBlock() instanceof com.alexh.capitalism.decor.BloodDecorKind.Holder decor)
            return decor.kind() == com.alexh.capitalism.decor.BloodDecorKind.IRON_MAIDEN;
        return true;
    }

    public static @Nullable ContainerOwner ownerOf(@Nullable BlockEntity blockEntity) {
        if (!holdsItems(blockEntity)) return null;
        // the Mayoral Chest is no one's: it counts for no one, whatever it carries
        if (blockEntity instanceof com.alexh.capitalism.town.MayoralChestBlockEntity) return null;
        return blockEntity.getExistingDataOrNull(ModAttachments.CONTAINER_OWNER);
    }

    public static @Nullable ContainerOwner ownerAt(Level level, BlockPos pos) {
        return ownerOf(level.getBlockEntity(containerPos(level, pos)));
    }

    // Two-block-tall containers (Fermenter, Shopfront) keep their items in the bottom half. The top half has no
    // block entity, so it answers for the bottom one: otherwise breaking or pushing the top half would spill it.
    public static BlockPos containerPos(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER
            && level.getBlockEntity(pos) == null) {
            return pos.below();
        }
        // a big Blood Moon decoration's part block (the Iron Maiden's upper half): its main block holds the items
        if (state.getBlock() instanceof com.alexh.capitalism.decor.BloodPartBlock) return com.alexh.capitalism.decor.BloodPartBlock.mainOf(state, pos);
        return pos;
    }

    // The other half of a double chest, if this is one.
    private static @Nullable BlockPos otherChestHalf(BlockState state, BlockPos pos) {
        if (state.getBlock() instanceof ChestBlock && state.hasProperty(ChestBlock.TYPE) && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            return pos.relative(ChestBlock.getConnectedDirection(state));
        }
        return null;
    }

    // Locked against this player? Checks both halves of a double chest.
    private static @Nullable ContainerOwner lockedAgainst(Level level, BlockPos pos, Player player) {
        ContainerOwner owner = ownerAt(level, pos);
        if (owner != null && owner.locked() && !owner.isOwnedBy(player.getUUID())) return owner;
        BlockPos other = otherChestHalf(level.getBlockState(pos), pos);
        if (other != null) {
            ContainerOwner otherOwner = ownerAt(level, other);
            if (otherOwner != null && otherOwner.locked() && !otherOwner.isOwnedBy(player.getUUID())) return otherOwner;
        }
        return null;
    }

    private static boolean isAdmin(Player player) {
        return player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    // ── Placing: lock it to the placer ─────────────────────────────────────

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = event.getPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!holdsItems(blockEntity)) return;
        // the Town Hall Mailbox belongs to whoever holds office, not the op who put it down (and counts for no one)
        if (blockEntity instanceof com.alexh.capitalism.mail.MailboxBlockEntity mailbox && mailbox.town()) return;
        // nor is the Mayoral Chest (the Mayor's and Vice Mayor's, whoever holds office)
        if (blockEntity instanceof com.alexh.capitalism.town.MayoralChestBlockEntity) return;

        // Don't let a chest join onto someone else's chest (a double chest would open both halves).
        BlockPos other = otherChestHalf(event.getPlacedBlock(), pos);
        if (other != null) {
            ContainerOwner otherOwner = ownerAt(level, other);
            if (otherOwner != null && !otherOwner.isOwnedBy(player.getUUID())) {
                event.setCanceled(true);
                deny(player, "You can't join onto " + otherOwner.name() + "'s chest.");
                return;
            }
        }

        // Don't let a hopper go under someone else's container (it would pull their items out).
        if (blockEntity instanceof HopperBlockEntity) {
            ContainerOwner above = ownerAt(level, pos.above());
            if (above != null && !above.isOwnedBy(player.getUUID())) {
                event.setCanceled(true);
                deny(player, "You can't put a hopper under " + above.name() + "'s container.");
                return;
            }
        }

        String name = player.getGameProfile().name();
        blockEntity.setData(ModAttachments.CONTAINER_OWNER, new ContainerOwner(player.getUUID(), name, true));
        blockEntity.setChanged();
        GoldLedger.get(level.getServer()).setContainer(GlobalPos.of(level.dimension(), pos), player.getUUID(), GoldMoney.deepCount((Container) blockEntity));
        player.sendOverlayMessage(Component.literal("Locked to you.").withStyle(ChatFormatting.GOLD));
    }

    // ── Opening: only the owner (or an admin); owner toggles the lock with shift + empty hand ──

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        BlockPos pos = containerPos(level, event.getPos());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        // Shopfronts handle their own screens (customers can shop, only the owner manages it) and are always locked;
        // Mailboxes too (anyone posts mail through the slot, only the owner takes it out).
        if (blockEntity instanceof ShopfrontBlockEntity || blockEntity instanceof com.alexh.capitalism.mail.MailboxBlockEntity) return;
        ContainerOwner owner = ownerOf(blockEntity);

        // Owner: shift + right-click with an empty hand flips the lock (both halves of a double chest).
        if (owner != null && owner.isOwnedBy(player.getUUID()) && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
            ContainerOwner flipped = owner.withLocked(!owner.locked());
            blockEntity.setData(ModAttachments.CONTAINER_OWNER, flipped);
            blockEntity.setChanged();
            BlockPos other = otherChestHalf(level.getBlockState(pos), pos);
            BlockEntity otherHalf = other == null ? null : level.getBlockEntity(other);
            ContainerOwner otherOwner = ownerOf(otherHalf);
            if (otherOwner != null && otherOwner.isOwnedBy(player.getUUID())) {
                otherHalf.setData(ModAttachments.CONTAINER_OWNER, otherOwner.withLocked(flipped.locked()));
                otherHalf.setChanged();
            }
            player.sendOverlayMessage(Component.literal(flipped.locked() ? "Locked." : "Unlocked. Anyone can open it now.")
                .withStyle(flipped.locked() ? ChatFormatting.GOLD : ChatFormatting.GREEN));
            level.playSound(null, pos, flipped.locked() ? SoundEvents.CHEST_LOCKED : SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 0.6F, 1.2F);
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        ContainerOwner lockedBy = lockedAgainst(level, pos, player);
        if (lockedBy == null) return;
        if (isAdmin(player)) {
            player.sendOverlayMessage(Component.literal("Admin: opening " + lockedBy.name() + "'s container.").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        deny(player, "Locked - owned by " + lockedBy.name());
        level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    // ── Breaking: only the owner (or an admin) ─────────────────────────────

    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Player player = event.getPlayer();
        ContainerOwner owner = ownerAt(level, event.getPos());
        if (owner == null) return;

        if (owner.locked() && !owner.isOwnedBy(player.getUUID()) && !isAdmin(player)) {
            event.setCanceled(true);
            if (player instanceof ServerPlayer serverPlayer) {
                deny(serverPlayer, "You can't break " + owner.name() + "'s container.");
            }
            return;
        }
        GoldLedger.get(level.getServer()).removeContainer(GlobalPos.of(level.dimension(), containerPos(level, event.getPos())));
    }

    // ── Explosions: locked containers survive ──────────────────────────────

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        event.getAffectedBlocks().removeIf(pos -> {
            ContainerOwner owner = ownerAt(level, pos);
            return owner != null && owner.locked();
        });
    }

    // ── Looking at a container shows who owns it ───────────────────────────

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0) return;
        HitResult hit = player.pick(5.0, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) return;
        ContainerOwner owner = ownerAt(player.level(), blockHit.getBlockPos());
        if (owner == null) return;

        Component message;
        if (player.level().getBlockEntity(containerPos(player.level(), blockHit.getBlockPos())) instanceof ShopfrontBlockEntity) {
            message = Component.literal(owner.isOwnedBy(player.getUUID()) ? "Your Shopfront" : owner.name() + "'s Shop  |  Right-click to browse").withStyle(ChatFormatting.GREEN);
        } else if (owner.isOwnedBy(player.getUUID())) {
            message = Component.literal(owner.locked() ? "Locked (shift + right-click to unlock)"
                : "Unlocked (shift + right-click to lock)").withStyle(ChatFormatting.GOLD);
        } else if (owner.locked()) {
            message = Component.literal("Locked by " + owner.name()).withStyle(ChatFormatting.RED);
        } else {
            message = Component.literal(owner.name() + "'s (unlocked)").withStyle(ChatFormatting.GREEN);
        }
        player.sendOverlayMessage(message);
    }

    private static void deny(ServerPlayer player, String text) {
        player.sendOverlayMessage(Component.literal(text).withStyle(ChatFormatting.RED));
    }
}
