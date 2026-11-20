package com.alexh.capitalism.npc;

import com.alexh.capitalism.core.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

// The Boat Master (Boats.md): a town Shopkeeper an op puts at the docks with /npc setspot boat_master. He sells ordinary
// boats for next to nothing and the Premium boats (the Pirate Ship...), luxuries for players with gold to burn
// (NpcShops.BOAT_MASTER). Looks like a villager in a fisherman's outfit with a navy captain's hat, a little Pirate Ship
// across his arms (client/JobNpcRenderer, skin textures/entity/npc/boat_master.png by tools/make_npc_skins.ps1).
public class BoatMaster extends Shopkeeper {

    public static final String TITLE = "Boat Master";

    public BoatMaster(EntityType<? extends JobNpc> type, Level level) {
        super(type, level);
    }

    @Override
    public String shopKey() {
        return NpcShops.BOAT_MASTER;
    }

    @Override
    public String title() {
        return TITLE;
    }

    @Override
    protected String shopName() {
        return "The Boat Master's Docks";
    }

    @Override
    protected ChatFormatting nameColor() {
        return ChatFormatting.AQUA;
    }

    @Override
    protected ItemStack held() {
        return new ItemStack(ModItems.PIRATE_SHIP.get());
    }
}
