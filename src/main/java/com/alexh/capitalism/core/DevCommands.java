package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Dev helper (ops only): /dev kit gives a fighting kit to practise on the Blood Moon bosses (Events.md):
// a Power II + Infinity bow and an arrow, a Sharpness III diamond sword, Protection I diamond armour (put on if
// that slot is empty), steak and purified water.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class DevCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private DevCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dev").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("kit").executes(DevCommands::kit)));
    }

    private static int kit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Registry<Enchantment> enchantments = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

        ItemStack bow = enchanted(enchantments, Items.BOW, Enchantments.POWER, 2);
        bow.enchant(enchantments.getOrThrow(Enchantments.INFINITY), 1);
        give(player, bow);
        give(player, new ItemStack(Items.ARROW));
        give(player, enchanted(enchantments, Items.DIAMOND_SWORD, Enchantments.SHARPNESS, 3));

        wear(player, EquipmentSlot.HEAD, enchanted(enchantments, Items.DIAMOND_HELMET, Enchantments.PROTECTION, 1));
        wear(player, EquipmentSlot.CHEST, enchanted(enchantments, Items.DIAMOND_CHESTPLATE, Enchantments.PROTECTION, 1));
        wear(player, EquipmentSlot.LEGS, enchanted(enchantments, Items.DIAMOND_LEGGINGS, Enchantments.PROTECTION, 1));
        wear(player, EquipmentSlot.FEET, enchanted(enchantments, Items.DIAMOND_BOOTS, Enchantments.PROTECTION, 1));

        give(player, new ItemStack(Items.COOKED_BEEF, 32));
        give(player, new ItemStack(ModItems.PURIFIED_WATER.get(), 16));

        context.getSource().sendSuccess(() -> Component.literal("Boss practice kit given. Good luck!"), false);
        return 1;
    }

    private static ItemStack enchanted(Registry<Enchantment> enchantments, Item item, ResourceKey<Enchantment> key, int level) {
        ItemStack stack = new ItemStack(item);
        stack.enchant(enchantments.getOrThrow(key), level);
        return stack;
    }

    // puts it on if that slot is empty, otherwise into the inventory
    private static void wear(ServerPlayer player, EquipmentSlot slot, ItemStack stack) {
        if (player.getItemBySlot(slot).isEmpty()) player.setItemSlot(slot, stack);
        else give(player, stack);
    }

    // into the inventory, or dropped at their feet if it's full
    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }
}
