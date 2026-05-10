package com.jarrettonesource.createmekanismcompat.mounted.miner.scan;

import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedMiningTarget;
import dev.ryanhcode.sable.Sable;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.tags.MekanismTags;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jetbrains.annotations.Nullable;

public final class MountedTargetRules {
    private MountedTargetRules() {
    }

    public static boolean mayMatchMiner(TileEntityDigitalMiner miner, BlockState state) {
        if (isNeverTarget(miner, state)) {
            return false;
        }
        return matchesMinerMode(miner, matchingFilter(miner, state));
    }

    public static @Nullable MountedMiningTarget resolve(MountedMekanismContext context, TileEntityDigitalMiner miner, BlockPos pos, BlockState state) {
        if (isNeverTarget(miner, state)) {
            return null;
        }
        MinerFilter<?> matchingFilter = matchingFilter(miner, state);
        if (!matchesMinerMode(miner, matchingFilter)) {
            return null;
        }
        if (pos.equals(context.globalBlockPos())) {
            return null;
        }
        if (context.subLevel().equals(Sable.HELPER.getContaining(context.level(), pos))) {
            return null;
        }
        if (state.getDestroySpeed(context.level(), pos) < 0) {
            return null;
        }
        return new MountedMiningTarget(pos, state, matchingFilter);
    }

    private static boolean isNeverTarget(TileEntityDigitalMiner miner, BlockState state) {
        if (state.isAir() || state.is(MekanismTags.Blocks.MINER_BLACKLIST) || shouldSkipState(state)) {
            return true;
        }
        return MekanismUtils.isLiquidBlock(state.getBlock()) || miner.isReplaceTarget(state.getBlock().asItem());
    }

    private static boolean matchesMinerMode(TileEntityDigitalMiner miner, @Nullable MinerFilter<?> matchingFilter) {
        return miner.getInverse() == (matchingFilter == null);
    }

    private static @Nullable MinerFilter<?> matchingFilter(TileEntityDigitalMiner miner, BlockState state) {
        for (MinerFilter<?> filter : miner.getFilterManager().getEnabledFilters()) {
            if (filter.canFilter(state)) {
                return filter;
            }
        }
        return null;
    }

    private static boolean shouldSkipState(BlockState state) {
        if (state.getBlock() instanceof BedBlock) {
            return state.getValue(BlockStateProperties.BED_PART) == BedPart.FOOT;
        } else if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof DoublePlantBlock) {
            return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER;
        }
        return false;
    }
}
