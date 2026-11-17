package com.alexh.capitalism.weaponsmith;

import com.alexh.capitalism.Capitalism;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

// Testing helper (ops only): /dmg <amount> takes that much durability off the item in your main hand, e.g. to try the
// Weaponsmith Table's repairs. ("/damage" is vanilla's command for hurting mobs, so this one is /dmg.)
// It never breaks the item: it stops at 1 durability left.
@EventBusSubscriber(modid = Capitalism.MODID)
public final class TestCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private TestCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("dmg").requires(Commands.hasPermission(OPS))
            .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                .executes(TestCommands::damage)));
    }

    private static int damage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();
        if (!stack.isDamageableItem()) {
            source.sendFailure(Component.literal("Hold something with durability (a sword, a tool...)."));
            return 0;
        }
        int max = stack.getMaxDamage();
        int damage = Math.min(max - 1, stack.getDamageValue() + IntegerArgumentType.getInteger(context, "amount"));
        stack.setDamageValue(damage);
        source.sendSuccess(() -> Component.literal("").append(stack.getHoverName())
            .append(": " + (max - damage) + "/" + max + " durability left."), false);
        return 1;
    }
}
