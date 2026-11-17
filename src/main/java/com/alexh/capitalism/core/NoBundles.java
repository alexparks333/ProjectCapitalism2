package com.alexh.capitalism.core;

import com.alexh.capitalism.Capitalism;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// No bundles on this server (they'd undercut the Designer's bags, Designer.md): they're taken out of all generated loot
// (this modifier, data/capitalism/loot_modifiers/no_bundles.json), and one that reaches a player (crafted, traded, old)
// falls apart into its Leather and String, and whatever was inside comes out. (Turning the vanilla recipes off with
// a neoforge:never condition breaks registry loading in 26.3, so the recipe stays and the result falls apart.)
@EventBusSubscriber(modid = Capitalism.MODID)
public class NoBundles extends LootModifier {

    public static final MapCodec<NoBundles> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, NoBundles::new));

    public NoBundles(Optional<Holder<LootItemCondition>> condition, int priority) {
        super(condition, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        generatedLoot.removeIf(stack -> stack.is(ItemTags.BUNDLES));
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 5 == 0) breakBundles(player);
    }

    // Crafting one (the vanilla recipe is still there: turning it off breaks loading) gives the Leather and String back
    // straight away
    @SubscribeEvent
    public static void onCrafted(net.neoforged.neoforge.event.entity.player.PlayerEvent.ItemCraftedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getCrafting().is(ItemTags.BUNDLES)) {
            player.level().getServer().execute(() -> breakBundles(player));
        }
    }

    // Every bundle in the inventory becomes Leather + String, its contents back in the inventory (or dropped)
    public static int breakBundles(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<ItemStack> out = new ArrayList<>();
        int broken = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.is(ItemTags.BUNDLES)) continue;
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null) contents.itemCopies().forEach(out::add);
            out.add(new ItemStack(Items.STRING, stack.getCount()));
            inventory.setItem(i, new ItemStack(Items.LEATHER, stack.getCount()));
            broken++;
        }
        for (ItemStack stack : out) {
            if (!inventory.add(stack) && !stack.isEmpty()) player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        if (broken > 0) player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Bundles don't hold together here: it fell apart."));
        return broken;
    }
}
