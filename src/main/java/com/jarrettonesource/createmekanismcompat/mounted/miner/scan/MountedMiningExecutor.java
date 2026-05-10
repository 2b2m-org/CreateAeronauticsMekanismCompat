package com.jarrettonesource.createmekanismcompat.mounted.miner.scan;

import com.jarrettonesource.createmekanismcompat.mixin.TileEntityDigitalMinerAccessor;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedMiningTarget;
import java.util.List;
import mekanism.common.CommonWorldTickHandler;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;

public final class MountedMiningExecutor {
    public MiningResult mine(MountedMekanismContext context, TileEntityDigitalMiner miner, MountedMiningTarget target) {
        TileEntityDigitalMinerAccessor access = (TileEntityDigitalMinerAccessor) miner;
        if (!access.cmc$canMine(target.state(), target.pos())) {
            return MiningResult.UNMINEABLE;
        }

        List<ItemStack> drops = access.cmc$getDrops((ServerLevel) context.level(), target.state(), target.pos());
        if (!miner.canInsert(drops)) {
            return MiningResult.BLOCKED;
        }

        CommonWorldTickHandler.fallbackItemCollector = access.cmc$getOverflowCollector();
        try {
            if (!access.cmc$setReplace(target.state(), target.pos(), target.matchingFilter())) {
                return MiningResult.BLOCKED;
            }
            access.cmc$add(drops);
            access.cmc$tryAddOverflow();
            access.cmc$setMissingStack(ItemStack.EMPTY);
            context.level().levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, target.pos(), Block.getId(target.state()));
            return MiningResult.MINED;
        } finally {
            CommonWorldTickHandler.fallbackItemCollector = null;
        }
    }

    public enum MiningResult {
        MINED,
        BLOCKED,
        UNMINEABLE,
        NO_TARGET
    }
}
