package com.jarrettonesource.createmekanismcompat.mounted.miner;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.miner.scan.MountedMiningExecutor;
import com.jarrettonesource.createmekanismcompat.mounted.miner.scan.MountedTargetRules;
import com.jarrettonesource.createmekanismcompat.mounted.miner.scan.SweepingTrailScanPlanner;
import java.util.List;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class MountedDigitalMinerController {
    private final SweepingTrailScanPlanner scanPlanner;
    private final MountedMiningExecutor miningExecutor;

    public MountedDigitalMinerController() {
        this.scanPlanner = new SweepingTrailScanPlanner();
        this.miningExecutor = new MountedMiningExecutor();
    }

    public int scan(MountedMekanismContext context, TileEntityDigitalMiner miner, MountedScanState state) {
        MountedScanBounds currentBounds = MountedScanBounds.current(context, miner);
        state.pruneOutside(currentBounds, miner.getRadius(), miner.getDiameter());
        int queueBefore = state.queuedTargetCount();
        int budget = CmcConfig.DIGITAL_MINER_SCAN_BUDGET.get();
        int timeBudgetMicros = CmcConfig.DIGITAL_MINER_SCAN_TIME_BUDGET_MICROS.get();
        int maxQueue = CmcConfig.DIGITAL_MINER_MAX_TARGET_QUEUE.get();
        int targetLimit = maxQueue - queueBefore;
        if (targetLimit <= 0) {
            state.recordScanStats(0, 0, 0, 0, 0, 0);
            return 0;
        }

        long deadlineNanos = System.nanoTime() + timeBudgetMicros * 1_000L;
        List<MountedMiningTarget> selectedTargets = scanPlanner.nextBatch(context, miner, state, budget, targetLimit, deadlineNanos);
        state.enqueueTargets(selectedTargets, maxQueue);
        return state.queuedTargetCount() - queueBefore;
    }

    public MountedMiningExecutor.MiningResult mine(MountedMekanismContext context, TileEntityDigitalMiner miner, MountedScanState state) {
        scan(context, miner, state);

        while (true) {
            MountedMiningTarget target = state.peekTarget();
            if (target == null) {
                return MountedMiningExecutor.MiningResult.NO_TARGET;
            }
            MountedMiningTarget refreshedTarget = resolveTarget(context, miner, target.pos());
            if (refreshedTarget == null) {
                state.removeTarget(target.pos());
                continue;
            }

            MountedMiningExecutor.MiningResult result = miningExecutor.mine(context, miner, refreshedTarget);
            if (result == MountedMiningExecutor.MiningResult.MINED || result == MountedMiningExecutor.MiningResult.UNMINEABLE) {
                state.removeTarget(target.pos());
            }
            return result;
        }
    }

    private static @Nullable MountedMiningTarget resolveTarget(MountedMekanismContext context, TileEntityDigitalMiner miner, BlockPos pos) {
        BlockState state = WorldUtils.getBlockStateIfLoaded(context.level(), pos);
        return state == null ? null : MountedTargetRules.resolve(context, miner, pos, state);
    }
}
