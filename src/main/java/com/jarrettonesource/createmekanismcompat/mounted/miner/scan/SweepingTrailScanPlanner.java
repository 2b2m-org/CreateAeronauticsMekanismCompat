package com.jarrettonesource.createmekanismcompat.mounted.miner.scan;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedMiningTarget;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedScanBounds;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedScanSectionJob;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedScanState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;

public final class SweepingTrailScanPlanner {
    private static final int TIME_CHECK_INTERVAL = 256;

    public List<MountedMiningTarget> nextBatch(MountedMekanismContext context, TileEntityDigitalMiner miner, MountedScanState state, int budget, int targetLimit, long deadlineNanos) {
        state.updateAnchors(context.globalCenter(), CmcConfig.DIGITAL_MINER_TRAIL_SAMPLES.get());
        MountedScanBounds currentBounds = MountedScanBounds.current(context, miner);
        state.pruneOutside(currentBounds, miner.getRadius(), miner.getDiameter());
        List<MountedMiningTarget> targets = new ArrayList<>(Math.max(0, Math.min(budget, targetLimit)));
        if (budget <= 0 || targetLimit <= 0 || currentBounds.isEmpty() || miner.getTotalSize() <= 0 || miner.getDiameter() <= 0 || miner.getMaxY() < miner.getMinY()) {
            state.recordScanStats(0, 0, 0, 0, 0, 0);
            return targets;
        }

        ScanStats stats = new ScanStats();
        while (stats.visitedPositions < budget && targets.size() < targetLimit && System.nanoTime() < deadlineNanos) {
            MountedScanSectionJob job = state.activeSectionJob();
            if (job == null) {
                BlockPos anchor = state.pollAnchor();
                if (anchor == null) {
                    break;
                }
                List<MountedScanSectionJob> jobs = sectionJobs(context, miner, anchor, currentBounds, stats);
                state.enqueueSectionJobs(jobs);
                continue;
            }

            state.rememberTicketChunk(job.chunkPos());
            LevelChunk chunk = context.level().getChunkSource().getChunkNow(job.chunkX(), job.chunkZ());
            if (chunk == null) {
                state.completeActiveSectionJob();
                stats.skippedUnloadedSections++;
                continue;
            }
            int sectionIndex = context.level().getSectionIndexFromSectionY(job.sectionY());
            if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                state.completeActiveSectionJob();
                continue;
            }
            LevelChunkSection section = chunk.getSection(sectionIndex);
            int remainingBudget = budget - stats.visitedPositions;
            int remainingJob = job.size() - state.sectionCursor();
            int toInspect = Math.min(remainingBudget, remainingJob);
            int inspected = 0;
            for (; inspected < toInspect && targets.size() < targetLimit; inspected++) {
                if ((stats.visitedPositions & (TIME_CHECK_INTERVAL - 1)) == 0 && System.nanoTime() >= deadlineNanos) {
                    break;
                }
                int i = inspected;
                int cursor = state.sectionCursor() + i;
                BlockPos pos = job.posAt(cursor);
                stats.visitedPositions++;
                if (!currentBounds.contains(pos)) {
                    continue;
                }
                BlockState blockState = section.getBlockState(job.localXAt(cursor), job.localYAt(cursor), job.localZAt(cursor));
                MountedMiningTarget target = MountedTargetRules.resolve(context, miner, pos, blockState);
                if (target != null && !state.hasQueuedTarget(target.pos())) {
                    targets.add(target);
                }
            }
            state.advanceSectionCursor(inspected);
            if (state.sectionCursor() >= job.size()) {
                state.completeActiveSectionJob();
            }
        }
        state.recordScanStats(stats.visitedPositions, targets.size(), stats.queuedSectionJobs, stats.skippedUnloadedSections, stats.skippedEmptySections, stats.skippedPaletteSections);
        return targets;
    }

    private List<MountedScanSectionJob> sectionJobs(MountedMekanismContext context, TileEntityDigitalMiner miner, BlockPos anchor, MountedScanBounds currentBounds, ScanStats stats) {
        MountedScanBounds scanBounds = MountedScanBounds.aroundAnchor(context, miner, anchor).intersection(currentBounds);
        if (scanBounds.isEmpty()) {
            return List.of();
        }
        int minX = scanBounds.minX();
        int maxX = scanBounds.maxX();
        int minY = scanBounds.minY();
        int maxY = scanBounds.maxY();
        int minZ = scanBounds.minZ();
        int maxZ = scanBounds.maxZ();

        List<ChunkPos> chunks = chunksInRange(minX, maxX, minZ, maxZ, anchor);
        List<Integer> sectionYs = sectionYsInRange(minY, maxY);
        List<MountedScanSectionJob> jobs = new ArrayList<>();
        ServerLevel level = context.level();
        for (int sectionY : sectionYs) {
            int sectionIndex = level.getSectionIndexFromSectionY(sectionY);
            for (ChunkPos chunkPos : chunks) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x, chunkPos.z);
                if (chunk == null) {
                    stats.skippedUnloadedSections++;
                    continue;
                }
                if (sectionIndex < 0 || sectionIndex >= chunk.getSections().length) {
                    continue;
                }
                LevelChunkSection section = chunk.getSection(sectionIndex);
                if (section.hasOnlyAir()) {
                    stats.skippedEmptySections++;
                    continue;
                }
                if (!section.maybeHas(state -> MountedTargetRules.mayMatchMiner(miner, state))) {
                    stats.skippedPaletteSections++;
                    continue;
                }

                int sectionMinX = chunkPos.getMinBlockX();
                int sectionMaxX = chunkPos.getMaxBlockX();
                int sectionMinY = sectionY << 4;
                int sectionMaxY = sectionMinY + 15;
                int sectionMinZ = chunkPos.getMinBlockZ();
                int sectionMaxZ = chunkPos.getMaxBlockZ();
                jobs.add(new MountedScanSectionJob(
                        chunkPos.x,
                        chunkPos.z,
                        sectionY,
                        Math.max(minX, sectionMinX) - sectionMinX,
                        Math.min(maxX, sectionMaxX) - sectionMinX,
                        Math.max(minY, sectionMinY) - sectionMinY,
                        Math.min(maxY, sectionMaxY) - sectionMinY,
                        Math.max(minZ, sectionMinZ) - sectionMinZ,
                        Math.min(maxZ, sectionMaxZ) - sectionMinZ
                ));
            }
        }
        stats.queuedSectionJobs += jobs.size();
        return jobs;
    }

    private List<ChunkPos> chunksInRange(int minX, int maxX, int minZ, int maxZ, BlockPos anchor) {
        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
        int centerChunkX = SectionPos.blockToSectionCoord(anchor.getX());
        int centerChunkZ = SectionPos.blockToSectionCoord(anchor.getZ());
        List<ChunkPos> chunks = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                chunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        chunks.sort(Comparator
                .comparingInt((ChunkPos chunk) -> Math.abs(chunk.x - centerChunkX) + Math.abs(chunk.z - centerChunkZ))
                .thenComparingInt(chunk -> Math.abs(chunk.x - centerChunkX))
                .thenComparingInt(chunk -> Math.abs(chunk.z - centerChunkZ)));
        return chunks;
    }

    private List<Integer> sectionYsInRange(int minY, int maxY) {
        int minSectionY = SectionPos.blockToSectionCoord(minY);
        int maxSectionY = SectionPos.blockToSectionCoord(maxY);
        List<Integer> sectionYs = new ArrayList<>();
        for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
            sectionYs.add(sectionY);
        }
        return sectionYs;
    }

    private static final class ScanStats {
        private int visitedPositions;
        private int queuedSectionJobs;
        private int skippedUnloadedSections;
        private int skippedEmptySections;
        private int skippedPaletteSections;
    }
}
