package com.alexh.capitalism.npc;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// A town shopkeeper who isn't standing in for a job: the Boat Master (BoatMaster) and the Butcher (Butcher). An op puts
// one with /npc setspot <shop>, and unlike a job's NPC he's always there while his spot is set (NpcManager). His shop is
// NpcShops.sections(shopKey()) and his skin textures/entity/npc/<shopKey>.png (tools/make_npc_skins.ps1). Everything
// else (can't be hurt, moved or killed, silent, never despawns) is JobNpc's.
public abstract class Shopkeeper extends JobNpc {

    protected Shopkeeper(EntityType<? extends JobNpc> type, Level level) {
        super(type, level);
    }

    // NpcShops' key for his shop (also his spot's key and his skin's name)
    public abstract String shopKey();

    // The shop key his menu opens with (NpcShops.sections): his shopKey, unless his stock changes (the Butcher's daily cuts)
    protected String menuKey(net.minecraft.server.MinecraftServer server) {
        return this.shopKey();
    }

    // His name over his head
    public abstract String title();

    // The shop screen's title
    protected abstract String shopName();

    protected abstract ChatFormatting nameColor();

    // What he holds across his arms
    protected abstract ItemStack held();

    public void setUp() {
        this.setCustomName(Component.literal(this.title()).withStyle(this.nameColor()));
        this.setCustomNameVisible(true);
        this.setItemSlot(EquipmentSlot.MAINHAND, this.held());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            Shopkeeper self = this;
            String key = this.menuKey(serverPlayer.level().getServer());
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, who) -> new NpcShopMenu(id, inventory, key, self),
                Component.literal(this.shopName())), buf -> buf.writeUtf(key));
        }
        return InteractionResult.SUCCESS;
    }

    // Always there while his spot is set (and he's the one standing on it)
    @Override
    public boolean stillWanted() {
        return NpcManager.isCurrentShopkeeper(this);
    }

    @Override
    public String skin() {
        return this.shopKey();
    }
}
