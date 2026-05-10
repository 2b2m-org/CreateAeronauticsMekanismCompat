package com.jarrettonesource.createmekanismcompat.mounted.miner;

import mekanism.common.content.miner.MinerFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record MountedMiningTarget(BlockPos pos, BlockState state, @Nullable MinerFilter<?> matchingFilter) {
}
