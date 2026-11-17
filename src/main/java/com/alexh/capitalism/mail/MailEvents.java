package com.alexh.capitalism.mail;

import com.alexh.capitalism.Capitalism;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

// Nobody breaks a mailbox that isn't theirs (Mailbox.md), not even an op playing in survival (economy/ContainerLocks
// lets ops break locked chests; not mailboxes). Only an op in creative can take someone else's down. The Town Hall
// Mailbox: only an op in creative, whoever holds office.
@EventBusSubscriber(modid = Capitalism.MODID)
public class MailEvents {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof MailboxBlockEntity mailbox)) return;
        boolean opInCreative = player.isCreative() && player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        if (opInCreative) return;
        if (!mailbox.town() && mailbox.isOwner(player)) return;
        event.setCanceled(true);
        player.sendOverlayMessage(Component.literal(mailbox.town() ? "The Town Hall Mailbox can't be broken."
            : "You can't break " + (mailbox.ownerName().isEmpty() ? "someone else" : mailbox.ownerName()) + "'s mailbox."));
    }
}
