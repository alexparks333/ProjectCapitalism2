package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;

// Swords go dull: every hit a sword lands counts (ModComponents.SWORD_HITS on the sword). Once it has landed as many
// hits as its edge (rolled when it was crafted, see SwordForging; swordHitsBeforeDull for swords that weren't crafted)
// it's DULL and does half damage (dullDamageMultiplier) until it's sharpened at a Whetstone, which sets the count back
// to 0. A sword's sharpness (also rolled when crafted) scales its own Attack Damage; enchantments add on top. Every sword counts, wood to netherite.
// Creative players don't wear it down.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Sharpness {

    private Sharpness() {
    }

    public static boolean isSword(ItemStack stack) {
        return stack.is(ItemTags.SWORDS);
    }

    // Default edge, for swords that were never crafted (loot, trades, creative menu)
    public static int maxHits() {
        try {
            return CapitalismConfig.SWORD_HITS_BEFORE_DULL.get();
        } catch (IllegalStateException notLoaded) {
            return 150;
        }
    }

    private static float dullMultiplier() {
        try {
            return (float) CapitalismConfig.DULL_DAMAGE_MULTIPLIER.get().doubleValue();
        } catch (IllegalStateException notLoaded) {
            return 0.5F;
        }
    }

    // This sword's edge: hits it lands before it goes dull
    public static int maxHits(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SWORD_EDGE.get(), maxHits());
    }

    public static int hits(ItemStack stack) {
        return stack.getOrDefault(ModComponents.SWORD_HITS.get(), 0);
    }

    // Hits left before it goes dull (0 = dull)
    public static int hitsLeft(ItemStack stack) {
        return Math.max(0, maxHits(stack) - hits(stack));
    }

    public static boolean isDull(ItemStack stack) {
        return isSword(stack) && hitsLeft(stack) <= 0;
    }

    public static void sharpen(ItemStack stack) {
        stack.remove(ModComponents.SWORD_HITS.get());
    }

    // The sword's own Attack Damage (the "7 Attack Damage" line): scaled by its sharpness, and halved when it's dull.
    // Done on the item's base damage, not on the hit, so enchantments (Sharpness, Smite...) and Strength still add
    // their full bonus on top, and the tooltip shows the real number. Recomputed whenever the sword changes.
    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack sword = event.getItemStack();
        if (!isSword(sword)) return;
        float factor = SwordForging.sharpness(sword) / 100.0F * (isDull(sword) ? dullMultiplier() : 1.0F);
        if (factor == 1.0F) return;
        for (ItemAttributeModifiers.Entry entry : List.copyOf(event.getModifiers())) {
            if (!entry.attribute().is(Attributes.ATTACK_DAMAGE) || !entry.modifier().is(Item.BASE_ATTACK_DAMAGE_ID)) continue;
            // the modifier leaves out the player's own 1 point of damage: scale the whole thing
            double scaled = (entry.modifier().amount() + 1.0) * factor - 1.0;
            event.replaceModifier(entry.attribute(),
                new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, scaled, AttributeModifier.Operation.ADD_VALUE), entry.slot());
        }
    }

    // A melee hit with a sharp sword: one hit closer to dull. (The damage itself comes from onItemAttributes.)
    @SubscribeEvent
    public static void onHit(LivingIncomingDamageEvent event) {
        DamageSource source = event.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player) || source.getDirectEntity() != player) return;
        ItemStack sword = player.getMainHandItem();
        if (!isSword(sword) || isDull(sword) || player.isCreative()) return;
        int hits = hits(sword) + 1;
        sword.set(ModComponents.SWORD_HITS.get(), hits);
        if (hits >= maxHits(sword)) {
            player.sendSystemMessage(Component.literal("Your ").append(sword.getHoverName())
                .append(" has gone dull! It does half damage until it's sharpened at a Sword Whetstone.")
                .withStyle(ChatFormatting.RED));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }
}
