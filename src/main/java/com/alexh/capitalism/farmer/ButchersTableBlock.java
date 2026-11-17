package com.alexh.capitalism.farmer;

import com.alexh.capitalism.occupation.Job;
import com.alexh.capitalism.occupation.JobEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

// The Butcher's Table (Farmer.md > Butchering): a thick end-grain chopping block with a cleaver stuck in it. Only
// Farmers can use it: put in a Meat Slab and cut it up (ButchersTableMenu, the butchery game). It keeps its Butcher
// Knife between uses (ButchersTableBlockEntity; breaking the table drops it); a slab left in it comes back when the menu
// closes.
public class ButchersTableBlock extends net.minecraft.world.level.block.BaseEntityBlock {

    private static final Component TITLE = Component.literal("Butcher's Table");

    public ButchersTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (!player.isCreative() && !JobEvents.has(player, Job.FARMER)) {
                serverPlayer.sendOverlayMessage(Component.literal("Only a Farmer can use the Butcher's Table."));
                return InteractionResult.SUCCESS;
            }
            ButchersTableBlockEntity table;
            if (level.getBlockEntity(pos) instanceof ButchersTableBlockEntity found) {
                table = found;
            } else {
                // a table placed before it kept knives
                table = new ButchersTableBlockEntity(pos, state);
                level.setBlockEntity(table);
            }
            net.minecraft.world.Container knife = table.knife();
            player.openMenu(new SimpleMenuProvider((id, inventory, p) ->
                new ButchersTableMenu(id, inventory, knife, ContainerLevelAccess.create(level, pos)), TITLE));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ButchersTableBlockEntity(pos, state);
    }
}
