package com.alexh.capitalism.npc;

import com.alexh.capitalism.core.ModItems;
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

// The Boat Master (Boats.md): a town NPC an op puts at the docks with /npc setspot boat_master. Unlike a job's NPC he's
// always there (he isn't standing in for anyone). He sells ordinary boats for next to nothing and the Premium boats
// (the Pirate Ship...), luxuries for players with gold to burn (NpcShops.BOAT_MASTER). Looks like a villager in a
// fisherman's outfit with a navy captain's hat, a little Pirate Ship across his arms (client/JobNpcRenderer, skin
// textures/entity/npc/boat_master.png by tools/make_npc_skins.ps1). Everything else (can't be hurt, moved or killed,
// silent, never despawns) is JobNpc's.
public class BoatMaster extends JobNpc {

    public static final String TITLE = "Boat Master";

    public BoatMaster(EntityType<? extends JobNpc> type, Level level) {
        super(type, level);
    }

    // His name over his head, and the little ship he holds
    public void setUp() {
        this.setCustomName(Component.literal(TITLE).withStyle(ChatFormatting.AQUA));
        this.setCustomNameVisible(true);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.PIRATE_SHIP.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && hand == InteractionHand.MAIN_HAND) {
            BoatMaster self = this;
            serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, who) -> new NpcShopMenu(id, inventory, NpcShops.BOAT_MASTER, self),
                Component.literal("The Boat Master's Docks")), buf -> buf.writeUtf(NpcShops.BOAT_MASTER));
        }
        return InteractionResult.SUCCESS;
    }

    // Always there while his spot is set (and he's the one standing on it)
    @Override
    public boolean stillWanted() {
        return NpcManager.isCurrentBoatMaster(this);
    }

    @Override
    public String skin() {
        return NpcShops.BOAT_MASTER;
    }
}
