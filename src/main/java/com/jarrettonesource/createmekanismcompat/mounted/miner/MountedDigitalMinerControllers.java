package com.jarrettonesource.createmekanismcompat.mounted.miner;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mixin.TileEntityDigitalMinerAccessor;
import com.jarrettonesource.createmekanismcompat.mounted.ChunkTicketPolicy;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.BitSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import mekanism.common.content.miner.ThreadMinerSearch.State;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.world.level.ChunkPos;

public final class MountedDigitalMinerControllers {
    private static final MountedDigitalMinerController CONTROLLER = new MountedDigitalMinerController();
    private static final Map<MountedMinerKey, MountedScanState> STATES = new ConcurrentHashMap<>();

    private MountedDigitalMinerControllers() {
    }

    public static boolean prepareMountedStart(TileEntityDigitalMiner miner) {
        MountedMekanismContext mounted = mountedContext(miner).orElse(null);
        if (mounted == null) {
            return false;
        }
        TileEntityDigitalMinerAccessor access = (TileEntityDigitalMinerAccessor) miner;
        access.cmc$setRunning(true);
        miner.searcher.state = State.FINISHED;
        MountedScanState state = stateFor(mounted);
        if (canEverMine(miner)) {
            state.updateAnchors(mounted.globalCenter(), CmcConfig.DIGITAL_MINER_TRAIL_SAMPLES.get());
            syncOreMap(miner, state);
        } else {
            clearOreMap(miner);
        }
        state.updateTicketCenterChunk(new ChunkPos(mounted.globalBlockPos()));
        miner.getChunkLoader().refreshChunkTickets();
        miner.setChanged();
        return true;
    }

    public static boolean scanMounted(TileEntityDigitalMiner miner) {
        MountedMekanismContext mounted = mountedContext(miner).orElse(null);
        if (mounted == null) {
            return false;
        }
        if (!miner.isRunning()) {
            MountedScanState state = stateFor(mounted);
            state.reset();
            clearOreMap(miner);
            return true;
        }
        if (!canEverMine(miner)) {
            stateFor(mounted).reset();
            clearOreMap(miner);
            return true;
        }
        MountedScanState state = stateFor(mounted);
        state.updateAnchors(mounted.globalCenter(), CmcConfig.DIGITAL_MINER_TRAIL_SAMPLES.get());
        int queueBefore = state.queuedTargetCount();
        CONTROLLER.scan(mounted, miner, state);
        syncOreMap(miner, state);

        ChunkPos currentGlobalChunk = new ChunkPos(mounted.globalBlockPos());
        if (state.updateTicketCenterChunk(currentGlobalChunk) || state.lastVisitedPositions() > 0) {
            miner.getChunkLoader().refreshChunkTickets();
        }
        if (state.queuedTargetCount() != queueBefore) {
            miner.setChanged();
        }
        return true;
    }

    public static boolean mineMounted(TileEntityDigitalMiner miner) {
        MountedMekanismContext mounted = mountedContext(miner).orElse(null);
        if (mounted == null) {
            return false;
        }
        if (!canEverMine(miner)) {
            stateFor(mounted).reset();
            clearOreMap(miner);
            return true;
        }
        MountedScanState state = stateFor(mounted);
        CONTROLLER.mine(mounted, miner, state);
        syncOreMap(miner, state);

        ChunkPos currentGlobalChunk = new ChunkPos(mounted.globalBlockPos());
        if (state.updateTicketCenterChunk(currentGlobalChunk) || state.lastVisitedPositions() > 0) {
            miner.getChunkLoader().refreshChunkTickets();
        }
        return true;
    }

    public static Optional<Set<ChunkPos>> mountedChunkSet(TileEntityDigitalMiner miner) {
        MountedMekanismContext mounted = mountedContext(miner).orElse(null);
        if (mounted == null) {
            return Optional.empty();
        }
        if (!miner.isRunning()) {
            return Optional.of(Set.of(new ChunkPos(mounted.globalBlockPos())));
        }
        MountedScanState state = stateFor(mounted);
        return Optional.of(ChunkTicketPolicy.digitalMinerChunks(mounted, miner, state));
    }

    public static void reset(TileEntityDigitalMiner miner) {
        MountedMekanismContextResolver.resolve(miner)
                .map(MountedMinerKey::from)
                .ifPresent(STATES::remove);
    }

    private static Optional<MountedMekanismContext> mountedContext(TileEntityDigitalMiner miner) {
        if (!CmcConfig.ENABLE_MOUNTED_DIGITAL_MINER.get()) {
            return Optional.empty();
        }
        return MountedMekanismContextResolver.resolve(miner);
    }

    private static boolean canEverMine(TileEntityDigitalMiner miner) {
        return miner.getInverse() || miner.getFilterManager().hasEnabledFilters();
    }

    private static MountedScanState stateFor(MountedMekanismContext context) {
        return STATES.computeIfAbsent(MountedMinerKey.from(context), key -> new MountedScanState());
    }

    private static void installSentinelOreMap(TileEntityDigitalMiner miner, int toMineCount) {
        Long2ObjectOpenHashMap<BitSet> oresToMine = new Long2ObjectOpenHashMap<>();
        BitSet bitSet = new BitSet();
        bitSet.set(0);
        oresToMine.put(ChunkPos.asLong(miner.getBlockPos()), bitSet);
        TileEntityDigitalMinerAccessor access = (TileEntityDigitalMinerAccessor) miner;
        access.cmc$setOresToMine(oresToMine);
        access.cmc$setCachedToMine(toMineCount);
    }

    private static void clearOreMap(TileEntityDigitalMiner miner) {
        TileEntityDigitalMinerAccessor access = (TileEntityDigitalMinerAccessor) miner;
        access.cmc$setOresToMine(new Long2ObjectOpenHashMap<>());
        access.cmc$setCachedToMine(0);
    }

    private static void syncOreMap(TileEntityDigitalMiner miner, MountedScanState state) {
        if (state.queuedTargetCount() > 0) {
            installSentinelOreMap(miner, state.queuedTargetCount());
        } else {
            clearOreMap(miner);
        }
    }
}
