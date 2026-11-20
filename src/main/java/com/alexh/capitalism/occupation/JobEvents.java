package com.alexh.capitalism.occupation;

import com.alexh.capitalism.core.ModEffects;
import com.alexh.capitalism.Capitalism;
import com.alexh.capitalism.block.AssemblyTableBlock;
import com.alexh.capitalism.block.BartendingTableBlock;
import com.alexh.capitalism.block.FermenterBlock;
import com.alexh.capitalism.block.OvenBlock;
import com.alexh.capitalism.weaponsmith.SmithTableBlock;
import com.alexh.capitalism.core.ModAttachments;
import com.alexh.capitalism.network.JobSyncPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;

// What each job actually does (pros and cons that aren't handled in the machines/food code themselves).
//  - Bartender: the only job that can use the Fermenter and the Bartending Table.
//  - Cook: the only job that can use the Sushi Table, Cooking Table and Oven; can't eat raw meat or fish.
//  - Weaponsmith: the only job that can use the Weaponsmith Table (the rest is in weaponsmith/).
@EventBusSubscriber(modid = Capitalism.MODID)
public class JobEvents {

    // What a Cook refuses to eat
    private static final Set<Item> RAW = Set.of(
        Items.BEEF, Items.PORKCHOP, Items.CHICKEN, Items.MUTTON, Items.RABBIT,
        Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH,
        Items.ROTTEN_FLESH, Items.SPIDER_EYE);

    public static boolean has(Player player, Job job) {
        if (player instanceof ServerPlayer) return player.getData(ModAttachments.JOBS.get()).has(job);
        return ClientJobData.has(job);
    }

    public static void sync(ServerPlayer player) {
        JobData data = player.getData(ModAttachments.JOBS.get());
        PacketDistributor.sendToPlayer(player, new JobSyncPacket(data.mask(), data.slots(Job.Category.ESSENTIAL), data.slots(Job.Category.FUN), data.changing()));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        sync(player);
        // no jobs yet (everyone starts with none): how to begin
        JobData data = player.getData(ModAttachments.JOBS.get());
        if (JobDraft.anyActive()) {
            JobDraft.onLogin(player);
        } else if (data.jobs().isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Welcome! Type ").withStyle(net.minecraft.ChatFormatting.GOLD)
                .append(net.minecraft.network.chat.Component.literal("/pick").withStyle(net.minecraft.ChatFormatting.YELLOW, net.minecraft.ChatFormatting.BOLD))
                .append(net.minecraft.network.chat.Component.literal(" to choose your jobs: one Essential and one Fun. Choose well: they're for good!").withStyle(net.minecraft.ChatFormatting.GOLD)));
        } else if (data.changing() || !data.full()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You have a job slot to fill: type /pick.").withStyle(net.minecraft.ChatFormatting.GOLD));
        }
    }

    // Job-only machines (checked on both sides so the menu never even tries to open):
    // Bartender: Fermenter, Bartending Table. Cook: Sushi Table, Cooking Table, Oven. Weaponsmith, Toolsmith, Armorsmith: their tables.
    // Any smith (Weaponsmith, Toolsmith, Armorsmith): the Lava Creator.
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        String machine = null;
        Job needed = null;
        if (block instanceof FermenterBlock) { machine = "the Fermenter"; needed = Job.BARTENDER; }
        else if (block instanceof BartendingTableBlock) { machine = "the Bartending Table"; needed = Job.BARTENDER; }
        else if (block instanceof AssemblyTableBlock table) {
            machine = table.kind() == AssemblyTableBlock.Kind.SUSHI ? "the Sushi Table" : "the Cooking Table";
            needed = Job.COOK;
        }
        else if (block instanceof OvenBlock) { machine = "the Oven"; needed = Job.COOK; }
        else if (block instanceof com.alexh.capitalism.block.OsmosisMachineBlock) { machine = "the Advanced Osmosis Machine"; needed = Job.BARTENDER; }
        else if (block instanceof SmithTableBlock table) { machine = "the " + table.kind().title; needed = table.kind().job; }
        // the Lava Creator (LavaCreator.md): any of the three smiths, no one else (water buckets included)
        else if (block instanceof com.alexh.capitalism.weaponsmith.LavaCreatorBlock) {
            if (player.isCreative() || has(player, Job.WEAPONSMITH) || has(player, Job.TOOLSMITH) || has(player, Job.ARMORSMITH)) return;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (player instanceof ServerPlayer serverPlayer) serverPlayer.sendOverlayMessage(Component.literal("Only smiths can use the Lava Creator."));
            return;
        }
        if (machine == null || player.isCreative() || has(player, needed)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.FAIL);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("Only " + needed.title + "s can use " + machine + "."));
        }
    }

    // Cooks won't eat raw meat or fish.
    @SubscribeEvent
    public static void onStartEating(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player) || player.isCreative()) return;
        ItemStack stack = event.getItem();
        // (Iron Stomach from a Zombie's Hangover Cure lets them.)
        if (!(RAW.contains(stack.getItem()) || com.alexh.capitalism.farmer.MeatCut.isCut(stack)) || !has(player, Job.COOK) || player.hasEffect(ModEffects.IRON_STOMACH)
            || com.alexh.capitalism.librarian.Skills.has(player, com.alexh.capitalism.librarian.Skill.IRON_STOMACH)) return;
        event.setCanceled(true);
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendOverlayMessage(Component.literal("You're a Cook: you won't eat that raw!"));
        }
    }
}
