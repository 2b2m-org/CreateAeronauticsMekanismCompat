package com.jarrettonesource.createmekanismcompat.mounted.miner;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class MountedScanState {
    private final ArrayDeque<BlockPos> pendingAnchors = new ArrayDeque<>();
    private final ArrayDeque<MountedScanSectionJob> pendingSectionJobs = new ArrayDeque<>();
    private final LinkedHashSet<ChunkPos> recentTicketChunks = new LinkedHashSet<>();
    private final LinkedHashMap<BlockPos, MountedMiningTarget> queuedTargets = new LinkedHashMap<>();
    @Nullable
    private Vec3 lastGlobalCenter;
    @Nullable
    private ChunkPos lastTicketCenterChunk;
    @Nullable
    private MountedScanSectionJob activeSectionJob;
    private int sectionCursor;
    private int lastVisitedPositions;
    private int lastCandidatePositions;
    private int lastQueuedSectionJobs;
    private int lastSkippedUnloadedSections;
    private int lastSkippedEmptySections;
    private int lastSkippedPaletteSections;

    public void reset() {
        pendingAnchors.clear();
        pendingSectionJobs.clear();
        recentTicketChunks.clear();
        queuedTargets.clear();
        lastGlobalCenter = null;
        lastTicketCenterChunk = null;
        activeSectionJob = null;
        sectionCursor = 0;
        recordScanStats(0, 0, 0, 0, 0, 0);
    }

    public int pruneOutside(MountedScanBounds bounds, int radius, int diameter) {
        if (bounds.isEmpty()) {
            int removed = pendingAnchors.size() + pendingSectionJobs.size() + recentTicketChunks.size() + queuedTargets.size();
            if (activeSectionJob != null) {
                removed++;
            }
            reset();
            return removed;
        }

        int removed = 0;
        int before = pendingAnchors.size();
        pendingAnchors.removeIf(anchor -> !bounds.intersectsHorizontalScan(anchor, radius, diameter));
        removed += before - pendingAnchors.size();

        before = pendingSectionJobs.size();
        pendingSectionJobs.removeIf(job -> !bounds.intersects(job));
        removed += before - pendingSectionJobs.size();

        before = recentTicketChunks.size();
        recentTicketChunks.removeIf(chunk -> !bounds.intersects(chunk));
        removed += before - recentTicketChunks.size();

        before = queuedTargets.size();
        queuedTargets.entrySet().removeIf(entry -> !bounds.contains(entry.getKey()));
        removed += before - queuedTargets.size();

        if (activeSectionJob != null && !bounds.intersects(activeSectionJob)) {
            activeSectionJob = null;
            sectionCursor = 0;
            removed++;
        }
        return removed;
    }

    public void updateAnchors(Vec3 currentGlobalCenter, int maxSamples) {
        if (lastGlobalCenter == null) {
            enqueueAnchor(BlockPos.containing(currentGlobalCenter));
            lastGlobalCenter = currentGlobalCenter;
            return;
        }
        BlockPos previous = BlockPos.containing(lastGlobalCenter);
        BlockPos current = BlockPos.containing(currentGlobalCenter);
        lastGlobalCenter = currentGlobalCenter;
        if (previous.equals(current)) {
            return;
        }

        int dx = current.getX() - previous.getX();
        int dy = current.getY() - previous.getY();
        int dz = current.getZ() - previous.getZ();
        int distance = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));
        int samples = Math.max(1, Math.min(maxSamples, distance));
        for (int i = 1; i <= samples; i++) {
            double fraction = i / (double) samples;
            int x = previous.getX() + (int) Math.round(dx * fraction);
            int y = previous.getY() + (int) Math.round(dy * fraction);
            int z = previous.getZ() + (int) Math.round(dz * fraction);
            enqueueAnchor(new BlockPos(x, y, z));
        }
    }

    private void enqueueAnchor(BlockPos anchor) {
        if (!pendingAnchors.contains(anchor)) {
            pendingAnchors.add(anchor);
        }
        while (pendingAnchors.size() > 64) {
            pendingAnchors.removeFirst();
        }
    }

    @Nullable
    public BlockPos pollAnchor() {
        return pendingAnchors.pollFirst();
    }

    public void enqueueSectionJobs(List<MountedScanSectionJob> jobs) {
        pendingSectionJobs.addAll(jobs);
    }

    @Nullable
    public MountedScanSectionJob activeSectionJob() {
        if (activeSectionJob == null) {
            activeSectionJob = pendingSectionJobs.pollFirst();
            sectionCursor = 0;
        }
        return activeSectionJob;
    }

    public int sectionCursor() {
        return sectionCursor;
    }

    public void advanceSectionCursor(int amount) {
        sectionCursor += amount;
    }

    public void completeActiveSectionJob() {
        activeSectionJob = null;
        sectionCursor = 0;
    }

    public boolean hasScanBacklog() {
        return !pendingAnchors.isEmpty() || activeSectionJob != null || !pendingSectionJobs.isEmpty();
    }

    public void rememberTicketChunk(ChunkPos chunk) {
        recentTicketChunks.remove(chunk);
        recentTicketChunks.add(chunk);
        while (recentTicketChunks.size() > 81) {
            ChunkPos oldest = recentTicketChunks.iterator().next();
            recentTicketChunks.remove(oldest);
        }
    }

    public Set<ChunkPos> recentTicketChunks() {
        return Collections.unmodifiableSet(recentTicketChunks);
    }

    public boolean updateTicketCenterChunk(ChunkPos chunk) {
        if (chunk.equals(lastTicketCenterChunk)) {
            return false;
        }
        lastTicketCenterChunk = chunk;
        return true;
    }

    public void enqueueTargets(List<MountedMiningTarget> targets, int maxTargets) {
        for (MountedMiningTarget target : targets) {
            if (!queuedTargets.containsKey(target.pos()) && queuedTargets.size() >= maxTargets) {
                return;
            }
            queuedTargets.put(target.pos(), target);
        }
    }

    public boolean hasQueuedTarget(BlockPos pos) {
        return queuedTargets.containsKey(pos);
    }

    @Nullable
    public MountedMiningTarget peekTarget() {
        Iterator<Map.Entry<BlockPos, MountedMiningTarget>> iterator = queuedTargets.entrySet().iterator();
        return iterator.hasNext() ? iterator.next().getValue() : null;
    }

    public void removeTarget(BlockPos pos) {
        queuedTargets.remove(pos);
    }

    public int queuedTargetCount() {
        return queuedTargets.size();
    }

    public void recordScanStats(int visitedPositions, int candidatePositions, int queuedSectionJobs, int skippedUnloadedSections, int skippedEmptySections, int skippedPaletteSections) {
        lastVisitedPositions = visitedPositions;
        lastCandidatePositions = candidatePositions;
        lastQueuedSectionJobs = queuedSectionJobs;
        lastSkippedUnloadedSections = skippedUnloadedSections;
        lastSkippedEmptySections = skippedEmptySections;
        lastSkippedPaletteSections = skippedPaletteSections;
    }

    public int lastVisitedPositions() {
        return lastVisitedPositions;
    }

    public int lastCandidatePositions() {
        return lastCandidatePositions;
    }

    public int lastQueuedSectionJobs() {
        return lastQueuedSectionJobs;
    }

    public int lastSkippedUnloadedSections() {
        return lastSkippedUnloadedSections;
    }

    public int lastSkippedEmptySections() {
        return lastSkippedEmptySections;
    }

    public int lastSkippedPaletteSections() {
        return lastSkippedPaletteSections;
    }
}
