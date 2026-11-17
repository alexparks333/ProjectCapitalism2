package com.alexh.capitalism.guide;

import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.core.ModItems;
import com.alexh.capitalism.network.DiscoverySyncPacket;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

// Unlocking guide entries: taking something out of a machine's result slot (Juicer, Fermenter, Bartending Table,
// Sushi Table, Cooking Table, Oven), crafting it, smelting it, or (wild rice) picking it up "discovers" it.
// Ops: /discoveries all|reset [player].
@EventBusSubscriber(modid = Capitalism.MODID)
public final class Discoveries {

    private static final PermissionCheck OPS = new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    private Discoveries() {
    }

    // Call from a result slot's onTake (runs on both sides; only the server records it).
    public static void discover(Player player, ItemStack made) {
        if (!(player instanceof ServerPlayer serverPlayer) || made.isEmpty()) return;
        Identifier id = BuiltInRegistries.ITEM.getKey(made.getItem());
        if (serverPlayer.getData(ModAttachments.DISCOVERIES.get()).add(id)) {
            PacketDistributor.sendToPlayer(serverPlayer, new DiscoverySyncPacket(List.of(id), false));
        }
    }

    public static void sync(ServerPlayer player) {
        DiscoveryData data = player.getData(ModAttachments.DISCOVERIES.get());
        PacketDistributor.sendToPlayer(player, new DiscoverySyncPacket(List.copyOf(data.items()), true));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) sync(player);
    }

    // Blocks and plates: crafting one unlocks it.
    @SubscribeEvent
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        discover(event.getEntity(), event.getCrafting());
    }

    // Cooked Rice: taking it out of a Furnace or Smoker.
    @SubscribeEvent
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        discover(event.getEntity(), event.getSmelting());
    }

    // Wild rice isn't made, it's found: picking it up unlocks it.
    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        ItemStack stack = event.getOriginalStack();
        if (stack.is(ModItems.RICE_SEEDS.get()) || stack.is(ModItems.RICE.get())) discover(event.getPlayer(), stack);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("discoveries")
            .requires(Commands.hasPermission(OPS))
            .then(Commands.argument("action", StringArgumentType.word())
                .suggests((context, builder) -> builder.suggest("all").suggest("reset").buildFuture())
                .executes(context -> run(context.getSource().getPlayerOrException(), StringArgumentType.getString(context, "action")))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> run(EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "action"))))));
    }

    // all = every item from this mod (unlocks the whole guide), reset = lock everything again
    private static int run(ServerPlayer player, String action) {
        DiscoveryData data = player.getData(ModAttachments.DISCOVERIES.get());
        if (action.equals("reset")) {
            data.clear();
        } else if (action.equals("all")) {
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getNamespace().equals(Capitalism.MODID)) data.add(id);
            }
        } else {
            return 0;
        }
        sync(player);
        player.sendSystemMessage(Component.literal(action.equals("all") ? "Every guide entry unlocked." : "Guide entries locked again.")
            .withStyle(ChatFormatting.YELLOW));
        return 1;
    }
}
