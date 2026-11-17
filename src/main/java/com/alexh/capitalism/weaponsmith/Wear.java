package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.CapitalismConfig;
import com.alexh.capitalism.core.ModComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;

// Wear, like a sword's edge (Sharpness), for pickaxes and armor:
//  - Pickaxes, shovels and axes: every block mined with one (when it's the right tool) counts (PICKAXE_WEAR); obsidian counts 100. After
//    pickaxeMax (pickaxeBlocksBeforeDull for iron, 600; more for diamond / netherite, less for stone / wood) it's DULL: half its own mining speed (ToolForging.onBreakSpeed; the Efficiency enchantment still adds in
//    full) until it's sharpened at a Tool Whetstone (Toolsmiths make them).
//  - Armor: every hit the wearer takes that armor would block counts, on every armor piece worn (ARMOR_DENTS). After
//    armorMax (armorHitsBeforeDented for iron, 120; more for diamond / netherite, less for leather / gold) the piece is DENTED: half its armor and toughness until it's fixed at an Armor
//    Whetstone (Armorsmiths make them).
// Creative players don't wear anything down.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Wear {

    public static final int OBSIDIAN_WEAR = 100;
    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private Wear() {
    }

    // ── Pickaxes ──

    public static boolean isPickaxe(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES);
    }

    // An iron pickaxe's edge (the config); other materials get more or less (tier())
    private static int ironPickaxeMax() {
        try {
            return CapitalismConfig.PICKAXE_BLOCKS_BEFORE_DULL.get();
        } catch (IllegalStateException notLoaded) {
            return 450;
        }
    }

    // Blocks this tool mines before it goes dull: wood 113, gold 158, stone 225, copper 270, iron 450, diamond 675,
    // netherite 900 (with the default config)
    public static int pickaxeMax(ItemStack stack) {
        return Math.max(1, Math.round(ironPickaxeMax() * tier(stack)));
    }

    public static int pickaxeUsed(ItemStack stack) {
        return stack.getOrDefault(ModComponents.PICKAXE_WEAR.get(), 0);
    }

    public static int pickaxeLeft(ItemStack stack) {
        return Math.max(0, pickaxeMax(stack) - pickaxeUsed(stack));
    }

    // Pickaxes, shovels and axes all wear down the same way (the "pickaxe" counter and limits are shared)
    public static boolean wears(ItemStack stack) {
        return ToolForging.isTool(stack);
    }

    public static boolean isDullTool(ItemStack stack) {
        return wears(stack) && pickaxeLeft(stack) <= 0;
    }

    // ── Armor ──

    public static boolean isArmor(ItemStack stack) {
        return stack.is(ItemTags.HEAD_ARMOR) || stack.is(ItemTags.CHEST_ARMOR) || stack.is(ItemTags.LEG_ARMOR) || stack.is(ItemTags.FOOT_ARMOR);
    }

    // An iron armor piece's condition (the config); other materials get more or less (tier())
    private static int ironArmorMax() {
        try {
            return CapitalismConfig.ARMOR_HITS_BEFORE_DENTED.get();
        } catch (IllegalStateException notLoaded) {
            return 90;
        }
    }

    // Hits this piece takes before it's dented: leather 45, gold 54, chainmail / copper 68, iron 90, diamond 135,
    // netherite 180 (with the default config)
    public static int armorMax(ItemStack stack) {
        return Math.max(1, Math.round(ironArmorMax() * tier(stack)));
    }

    // How long a material lasts compared to iron, from the item's name (iron_pickaxe, diamond_helmet...)
    public static float tier(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("netherite_")) return 2.0F;
        if (path.startsWith("diamond_")) return 1.5F;
        if (path.startsWith("iron_") || path.startsWith("turtle_")) return 1.0F;
        if (path.startsWith("copper_") || path.startsWith("chainmail_")) return Wear.isArmor(stack) ? 0.75F : 0.6F;
        if (path.startsWith("stone_")) return 0.5F;
        if (path.startsWith("golden_")) return Wear.isArmor(stack) ? 0.6F : 0.35F;
        if (path.startsWith("leather_")) return 0.5F;
        if (path.startsWith("wooden_")) return 0.25F;
        return 1.0F;
    }

    // Obsidian is hard on a pickaxe: 100 blocks of its edge at once
    private static int wearFor(BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN) ? OBSIDIAN_WEAR : 1;
    }

    public static int armorDents(ItemStack stack) {
        return stack.getOrDefault(ModComponents.ARMOR_DENTS.get(), 0);
    }

    public static int armorLeft(ItemStack stack) {
        return Math.max(0, armorMax(stack) - armorDents(stack));
    }

    public static boolean isDented(ItemStack stack) {
        return isArmor(stack) && armorLeft(stack) <= 0;
    }

    // A Tool / Armor Whetstone: like new again
    public static void restore(ItemStack stack) {
        stack.remove(ModComponents.PICKAXE_WEAR.get());
        stack.remove(ModComponents.ARMOR_DENTS.get());
    }

    // A block mined with the right tool (pickaxe, shovel or axe): one closer to dull
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BreakBlockEvent event) {
        if (event.isCanceled() || !(event.getPlayer() instanceof ServerPlayer player) || player.isCreative()) return;
        ItemStack pickaxe = player.getMainHandItem();
        if (!wears(pickaxe) || isDullTool(pickaxe) || pickaxe.getDestroySpeed(event.getState()) <= 1.0F) return;
        int used = Math.min(pickaxeMax(pickaxe), pickaxeUsed(pickaxe) + wearFor(event.getState()));
        pickaxe.set(ModComponents.PICKAXE_WEAR.get(), used);
        if (used >= pickaxeMax(pickaxe)) {
            player.sendSystemMessage(Component.literal("Your ").append(pickaxe.getHoverName())
                .append(" has gone dull! It mines at half speed until it's sharpened at a Tool Whetstone.")
                .withStyle(ChatFormatting.RED));
            player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.5F, 0.6F);
        }
    }

    // A hit the armor would block: every worn piece takes a dent
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isCreative() || event.getAmount() <= 0) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (!isArmor(piece) || isDented(piece)) continue;
            int dents = armorDents(piece) + 1;
            piece.set(ModComponents.ARMOR_DENTS.get(), dents);
            if (dents >= armorMax(piece)) {
                player.sendSystemMessage(Component.literal("Your ").append(piece.getHoverName())
                    .append(" is dented! It gives half protection until it's fixed at an Armor Whetstone.")
                    .withStyle(ChatFormatting.RED));
                player.level().playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.PLAYERS, 0.7F, 0.5F);
            }
        }
    }

    // A dented piece: half its armor and toughness (recomputed whenever the piece changes)
    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        ItemStack piece = event.getItemStack();
        if (!isDented(piece)) return;
        for (ItemAttributeModifiers.Entry entry : List.copyOf(event.getModifiers())) {
            Holder<Attribute> attribute = entry.attribute();
            if (!attribute.is(Attributes.ARMOR) && !attribute.is(Attributes.ARMOR_TOUGHNESS)) continue;
            AttributeModifier modifier = entry.modifier();
            if (modifier.operation() != AttributeModifier.Operation.ADD_VALUE) continue;
            event.replaceModifier(attribute, new AttributeModifier(modifier.id(), modifier.amount() * 0.5, modifier.operation()), entry.slot());
        }
    }
}
