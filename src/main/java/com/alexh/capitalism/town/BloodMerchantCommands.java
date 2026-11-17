package com.alexh.capitalism.town;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModEntities;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;

// Admin commands for the Blood Merchant (Town.md), under /npc (ops):
//  - /npc bloodmerchant: he stands exactly where you are, facing the way you face (any other Blood Merchant goes)
//  - /npc bloodmerchant remove: every Blood Merchant goes
@EventBusSubscriber(modid = Capitalism.MODID)
public final class BloodMerchantCommands {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private BloodMerchantCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("npc").requires(Commands.hasPermission(OPS))
            .then(Commands.literal("bloodmerchant").executes(BloodMerchantCommands::place)
                .then(Commands.literal("remove").executes(BloodMerchantCommands::remove))));
    }

    // every Blood Merchant in a loaded part of any world
    private static List<BloodMerchant> all(CommandSourceStack source) {
        List<BloodMerchant> found = new ArrayList<>();
        for (ServerLevel level : source.getServer().getAllLevels()) {
            found.addAll(level.getEntities(ModEntities.BLOOD_MERCHANT.get(), m -> true));
        }
        return found;
    }

    private static int place(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        int removed = 0;
        for (BloodMerchant old : all(context.getSource())) {
            old.leave();
            removed++;
        }
        ServerLevel level = player.level();
        BloodMerchant merchant = new BloodMerchant(ModEntities.BLOOD_MERCHANT.get(), level);
        merchant.placeAt(player.position(), player.getYRot());
        merchant.setInvisible(!BloodMerchant.isNight(level));
        level.addFreshEntity(merchant);
        int moved = removed;
        context.getSource().sendSuccess(() -> Component.literal((moved > 0 ? "The Blood Merchant moved here" : "The Blood Merchant will stand here")
            + (BloodMerchant.isNight(level) ? "." : " (he only shows himself at night).")).withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context) {
        List<BloodMerchant> found = all(context.getSource());
        if (found.isEmpty()) {
            context.getSource().sendFailure(Component.literal("No Blood Merchant here (he has to be in a loaded area)."));
            return 0;
        }
        for (BloodMerchant m : found) m.leave();
        context.getSource().sendSuccess(() -> Component.literal("The Blood Merchant is gone.").withStyle(ChatFormatting.YELLOW), true);
        return found.size();
    }
}
