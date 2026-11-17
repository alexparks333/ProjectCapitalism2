package com.alexh.capitalism.enchanter;

import com.alexh.capitalism.core.ModComponents;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.network.OpenApprenticeNamePacket;
import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import com.alexh.capitalism.weaponsmith.Wear;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// An Enchanter's Apprentice (Enchanter.md): an armor stand in a full set of one kind of armor, turned into an
// apprentice by an Enchanter hitting it with the Apprentice Wand (ApprenticeWand). It stores XP: stand it in a Transfer
// Tube booth and send XP to it or take XP from it (XpTransfer). It holds an amount of XP points (shown over its head as
// "469 / 550 XP"), not levels: how much depends on its armor (Kind).
// It can't be hurt, and its armor can't be taken off. Only Enchanters touch it: its owner (an Enchanter; whoever made or
// last placed it) picks it up with
// shift + right-click on an empty hand: it becomes an Apprentice item (ApprenticeItem) with its armor, XP and name.
// The owner names it by right-clicking it with the Apprentice Wand (a naming box opens; free, no wand use).
public class Apprentice extends ArmorStand {

    public static final int MAX_NAME = 24;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    // Each full armor set and how much XP it holds: the points it takes to get from level 0 to this level
    public enum Kind {
        LEATHER("Leather", "leather_", 15),
        CHAINMAIL("Chainmail", "chainmail_", 15),
        COPPER("Copper", "copper_", 10),
        GOLDEN("Golden", "golden_", 28),
        IRON("Iron", "iron_", 22),
        DIAMOND("Diamond", "diamond_", 32),
        NETHERITE("Netherite", "netherite_", 100);

        public final String title;
        private final String prefix;
        public final int levels;

        Kind(String title, String prefix, int levels) {
            this.title = title;
            this.prefix = prefix;
            this.levels = levels;
        }

        public long capacity() {
            return XpTransfer.pointsForLevel(this.levels);
        }

        // The kind of a full set of one armor material on this stand, or null
        public static @Nullable Kind of(ArmorStand stand) {
            for (Kind kind : values()) {
                boolean full = true;
                for (EquipmentSlot slot : ARMOR) {
                    ItemStack piece = stand.getItemBySlot(slot);
                    if (!Wear.isArmor(piece) || !BuiltInRegistries.ITEM.getKey(piece.getItem()).getPath().startsWith(kind.prefix)) {
                        full = false;
                        break;
                    }
                }
                if (full) return kind;
            }
            return null;
        }

        public static @Nullable Kind byName(@Nullable String name) {
            for (Kind kind : values()) {
                if (kind.name().equals(name)) return kind;
            }
            return null;
        }
    }

    private Kind kind = Kind.LEATHER;
    private long storedXp = 0;
    private @Nullable UUID owner;
    // The name its owner gave it, or null for "Iron Apprentice"
    private @Nullable String nickname;

    public Apprentice(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
        this.setShowArms(true);
    }

    public Kind kind() {
        return this.kind;
    }

    // What it's called: its name, or "Iron Apprentice"
    public String title() {
        return this.nickname != null ? this.nickname : this.kind.title + " Apprentice";
    }

    public @Nullable String nickname() {
        return this.nickname;
    }

    public void setNickname(@Nullable String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.length() > MAX_NAME) trimmed = trimmed.substring(0, MAX_NAME);
        this.nickname = trimmed.isEmpty() ? null : trimmed;
        this.updateName();
    }

    // From the naming box (network/RenameApprenticePacket): only its owner, nearby, with the wand in hand
    public void rename(ServerPlayer player, String name) {
        if (!this.isAlive() || player.distanceToSqr(this) > 8.0 * 8.0 || !player.getMainHandItem().is(ModItems.APPRENTICE_WAND.get())) return;
        if (!this.ownedBy(player) || !isEnchanter(player)) return;
        this.setNickname(name);
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.NEUTRAL, 0.6F, 1.6F);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.2, this.getZ(), 25, 0.3, 0.4, 0.3, 0.6);
        }
    }

    private static boolean isEnchanter(Player player) {
        return player.isCreative() || JobEvents.has(player, Job.ENCHANTER);
    }

    private boolean ownedBy(Player player) {
        return this.owner == null || this.owner.equals(player.getUUID()) || player.isCreative();
    }

    public long storedXp() {
        return this.storedXp;
    }

    public long room() {
        return Math.max(0, this.kind.capacity() - this.storedXp);
    }

    public void setStoredXp(long points) {
        this.storedXp = Math.max(0, Math.min(points, this.kind.capacity()));
        this.updateName();
    }

    public void setup(Kind kind, long storedXp, @Nullable UUID owner) {
        this.kind = kind;
        this.owner = owner;
        this.setStoredXp(storedXp);
    }

    // The floating name: "Iron Apprentice: 469 / 679 XP", or "Merlin: 469 / 679 XP" once named
    private void updateName() {
        this.setCustomName(Component.literal(this.title() + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(this.storedXp + " / " + this.kind.capacity() + " XP").withStyle(ChatFormatting.GREEN)));
        this.setCustomNameVisible(true);
    }

    // ── Can't be hurt, broken or blown up (only /kill and the void get it) ──

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        if (source.getEntity() instanceof ServerPlayer player) {
            player.sendOverlayMessage(Component.literal("Apprentices can't be broken. Its owner can pick it up (shift + right-click, empty hand)."));
        }
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    // ── Right-click: see what it holds; the owner picks it up with shift + an empty hand. The armor stays on. ──

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.SUCCESS;
        boolean picking = player.isShiftKeyDown() && player.getMainHandItem().isEmpty();
        boolean naming = player.getMainHandItem().is(ModItems.APPRENTICE_WAND.get());
        if ((picking || naming) && !isEnchanter(player)) {
            serverPlayer.sendOverlayMessage(Component.literal("Only Enchanters can " + (naming ? "name" : "pick up") + " Apprentices."));
            return InteractionResult.SUCCESS;
        }
        if (naming) {
            if (!this.ownedBy(player)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only its owner can name this Apprentice."));
                return InteractionResult.SUCCESS;
            }
            PacketDistributor.sendToPlayer(serverPlayer, new OpenApprenticeNamePacket(this.getId(), this.nickname == null ? "" : this.nickname));
            return InteractionResult.SUCCESS;
        }
        if (picking) {
            if (!this.ownedBy(player)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only its owner can pick up this Apprentice."));
                return InteractionResult.SUCCESS;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, this.toItem());
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_BREAK, SoundSource.NEUTRAL, 0.8F, 1.3F);
            this.discard();
            return InteractionResult.SUCCESS;
        }
        serverPlayer.sendOverlayMessage(Component.literal("Holds " + this.storedXp + " of " + this.kind.capacity()
            + " XP. Use a Transfer Tube to fill or empty it."));
        return InteractionResult.SUCCESS;
    }

    // The Apprentice as an item: its armor and its XP go with it
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModItems.APPRENTICE.get());
        List<ItemStack> armor = new ArrayList<>();
        for (EquipmentSlot slot : ARMOR) armor.add(this.getItemBySlot(slot).copy());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(armor));
        stack.set(ModComponents.APPRENTICE_KIND.get(), this.kind.name());
        if (this.nickname != null) stack.set(DataComponents.CUSTOM_NAME, Component.literal(this.nickname));
        stack.set(ModComponents.APPRENTICE_XP.get(), (int) Math.min(Integer.MAX_VALUE, this.storedXp));
        return stack;
    }

    // Put the armor from an Apprentice item back on (head, chest, legs, feet)
    public void wearFrom(ItemContainerContents armor) {
        for (int i = 0; i < ARMOR.length; i++) this.setItemSlot(ARMOR[i], armor.getStackInSlot(i));
    }

    // A few enchanting glyphs drift around it
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(6) == 0) {
            this.level().addParticle(ParticleTypes.ENCHANT, this.getRandomX(0.6), this.getY() + 1.0 + this.random.nextDouble(),
                this.getRandomZ(0.6), 0, 0.3, 0);
        }
    }

    // ── Saving ──

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("ApprenticeKind", this.kind.name());
        output.putLong("StoredXp", this.storedXp);
        if (this.owner != null) output.store("ApprenticeOwner", UUIDUtil.CODEC, this.owner);
        if (this.nickname != null) output.putString("ApprenticeName", this.nickname);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        Kind saved = Kind.byName(input.getStringOr("ApprenticeKind", "LEATHER"));
        this.kind = saved == null ? Kind.LEATHER : saved;
        this.owner = input.read("ApprenticeOwner", UUIDUtil.CODEC).orElse(null);
        this.nickname = input.getString("ApprenticeName").orElse(null);
        this.setStoredXp(input.getLongOr("StoredXp", 0L));
    }
}
