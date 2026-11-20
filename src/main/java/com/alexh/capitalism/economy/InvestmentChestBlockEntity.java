package com.alexh.capitalism.economy;

import com.alexh.capitalism.core.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

// The Investment Chest in the world (InvestmentChestBlock). It keeps NO items: every player's gold is in their own account
// (InvestmentBank), the same from any Investment Chest, like an Ender Chest. This only works its lid: open while anyone
// has it open (client/InvestmentChestRenderer).
public class InvestmentChestBlockEntity extends BlockEntity implements LidBlockEntity {

    private final ChestLidController lid = new ChestLidController();
    private int openers;

    public InvestmentChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INVESTMENT_CHEST.get(), pos, state);
    }

    public static void lidAnimateTick(Level level, BlockPos pos, BlockState state, InvestmentChestBlockEntity chest) {
        chest.lid.tickLid();
    }

    @Override
    public boolean triggerEvent(int type, int value) {
        if (type == 1) {
            this.lid.shouldBeOpen(value > 0);
            return true;
        }
        return super.triggerEvent(type, value);
    }

    // someone opened / closed it (InvestmentMenu)
    void startOpen() {
        if (this.level == null || this.remove) return;
        if (this.openers++ == 0) {
            this.level.playSound(null, this.worldPosition, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.5F, 0.8F);
            this.level.playSound(null, this.worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.35F, 1.6F);
        }
        this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openers);
    }

    void stopOpen() {
        if (this.level == null || this.remove) return;
        this.openers = Math.max(0, this.openers - 1);
        if (this.openers == 0) this.level.playSound(null, this.worldPosition, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.5F, 0.8F);
        this.level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), 1, this.openers);
    }

    @Override
    public float getOpenNess(float partialTicks) {
        return this.lid.getOpenness(partialTicks);
    }
}
