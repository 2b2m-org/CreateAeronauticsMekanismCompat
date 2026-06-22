package com.jarrettonesource.createmekanismcompat.assembly;

import com.jarrettonesource.createmekanismcompat.mixin.AbstractAcceptorCacheAccessor;
import com.jarrettonesource.createmekanismcompat.mixin.DynamicBufferedNetworkAccessor;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.EnergyNetwork;
import mekanism.common.content.network.FluidNetwork;
import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.content.network.transmitter.MechanicalPipe;
import mekanism.common.content.network.transmitter.PressurizedTube;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.content.network.transmitter.UniversalCable;
import mekanism.common.lib.transmitter.DynamicBufferedNetwork;
import mekanism.common.lib.transmitter.TransmitterNetworkRegistry;
import mekanism.common.lib.transmitter.acceptor.AbstractAcceptorCache;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;

public final class MekanismAssemblyMoveTracker {
    // Mekanism invalidates transmitter networks on a later server tick after Sable's move returns.
    private static final long DEFERRED_TRANSMITTER_SOURCE_MOVE_TICKS = 40;
    private static final int POST_MOVE_TRANSMITTER_RESYNC_ATTEMPTS = 10;
    private static final long POST_MOVE_TRANSMITTER_RESYNC_INTERVAL_TICKS = 2;
    private static final Map<MoveKey, Long> DEFERRED_TRANSMITTER_SOURCE_MOVES = new HashMap<>();
    private static final Map<MoveKey, PendingTransmitterResync> PENDING_TRANSMITTER_RESYNCS = new HashMap<>();
    private static final ThreadLocal<MoveScope> ACTIVE_MOVE = new ThreadLocal<>();

    private MekanismAssemblyMoveTracker() {
    }

    public static void beginMoveBlocks() {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope == null) {
            scope = new MoveScope();
            ACTIVE_MOVE.set(scope);
        }
        scope.begin();
    }

    public static void endMoveBlocks() {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope == null) {
            return;
        }
        scope.end();
        if (!scope.active()) {
            ACTIVE_MOVE.remove();
        }
    }

    public static void markSourceMove(ServerLevel level, BlockPos pos) {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope != null && scope.active()) {
            scope.sourceMoves().add(new MoveKey(level.dimension(), pos.asLong()));
        }
    }

    public static void prepareTransmitterSourceMove(ServerLevel level, BlockPos pos) {
        markDeferredTransmitterSourceMove(level, pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof TileEntityTransmitter transmitter) {
            SavedTransmitterState beforeCapture = snapshotTransmitterState(transmitter);
            captureBufferedShare(transmitter);
            SavedTransmitterState snapshot = snapshotTransmitterState(transmitter).withFallback(beforeCapture);
            if (snapshot.hasContents()) {
                MoveScope scope = ACTIVE_MOVE.get();
                if (scope != null && scope.active()) {
                    scope.transmitterStates().put(new MoveKey(level.dimension(), pos.asLong()), snapshot);
                }
            }
        }
    }

    public static void markMovedTransmitter(ServerLevel level, BlockPos pos) {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope != null && scope.active()) {
            scope.movedTransmitters().add(new MoveKey(level.dimension(), pos.asLong()));
        }
    }

    public static void refreshMovedTransmitters(SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> sourcePositions) {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope == null || !scope.active()) {
            return;
        }
        ServerLevel resultingLevel = transform.getLevel();
        for (BlockPos sourcePos : sourcePositions) {
            BlockPos movedPos = transform.apply(sourcePos);
            MoveKey sourceKey = new MoveKey(resultingLevel.dimension(), sourcePos.asLong());
            MoveKey key = new MoveKey(resultingLevel.dimension(), movedPos.asLong());
            if (!scope.movedTransmitters().contains(key)) {
                continue;
            }
            BlockEntity blockEntity = resultingLevel.getBlockEntity(movedPos);
            if (blockEntity instanceof TileEntityTransmitter transmitter) {
                refreshMovedTransmitter(transmitter, scope.transmitterStates().get(sourceKey));
            }
        }
    }

    public static synchronized void markDeferredTransmitterSourceMove(ServerLevel level, BlockPos pos) {
        MoveScope scope = ACTIVE_MOVE.get();
        if (scope != null && scope.active()) {
            long now = level.getGameTime();
            cleanupDeferredTransmitterSourceMoves(now);
            DEFERRED_TRANSMITTER_SOURCE_MOVES.put(
                    new MoveKey(level.dimension(), pos.asLong()),
                    now + DEFERRED_TRANSMITTER_SOURCE_MOVE_TICKS
            );
        }
    }

    public static boolean isSourceMove(Level level, BlockPos pos) {
        MoveScope scope = ACTIVE_MOVE.get();
        return level != null && scope != null && scope.active() && scope.sourceMoves().contains(new MoveKey(level.dimension(), pos.asLong()));
    }

    public static synchronized boolean isDeferredTransmitterSourceMove(Level level, BlockPos pos) {
        if (level == null) {
            return false;
        }
        long now = level.getGameTime();
        cleanupDeferredTransmitterSourceMoves(now);
        Long expiresAt = DEFERRED_TRANSMITTER_SOURCE_MOVES.get(new MoveKey(level.dimension(), pos.asLong()));
        return expiresAt != null && expiresAt >= now;
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (PENDING_TRANSMITTER_RESYNCS.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<MoveKey, PendingTransmitterResync>> iterator = PENDING_TRANSMITTER_RESYNCS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MoveKey, PendingTransmitterResync> entry = iterator.next();
            MoveKey key = entry.getKey();
            ServerLevel level = event.getServer().getLevel(key.dimension());
            if (level == null) {
                iterator.remove();
                continue;
            }

            PendingTransmitterResync pending = entry.getValue();
            long now = level.getGameTime();
            if (now < pending.nextAttemptTick()) {
                continue;
            }

            BlockEntity blockEntity = level.getBlockEntity(BlockPos.of(key.pos()));
            if (!(blockEntity instanceof TileEntityTransmitter tile)) {
                iterator.remove();
                continue;
            }

            if (resyncMovedTransmitter(tile, pending.snapshot()) || pending.remainingAttempts() <= 1) {
                iterator.remove();
            } else {
                entry.setValue(new PendingTransmitterResync(
                        pending.remainingAttempts() - 1,
                        now + POST_MOVE_TRANSMITTER_RESYNC_INTERVAL_TICKS,
                        pending.snapshot()
                ));
            }
        }
    }

    private static void captureBufferedShare(TileEntityTransmitter tile) {
        Transmitter transmitter = tile.getTransmitter();
        if (!(transmitter instanceof BufferedTransmitter)) {
            return;
        }
        transmitter.validateAndTakeShare();
        tile.setChanged();
    }

    private static void refreshMovedTransmitter(TileEntityTransmitter tile, SavedTransmitterState snapshot) {
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        resetAcceptorCache(transmitter);
        transmitter.currentTransmitterConnections = 0x00;
        transmitter.refreshConnections();
        TransmitterNetworkRegistry.invalidateTransmitter(transmitter);
        scheduleTransmitterResync(tile, snapshot);
        tile.setChanged();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean resyncMovedTransmitter(TileEntityTransmitter tile, SavedTransmitterState snapshot) {
        Transmitter transmitter = tile.getTransmitter();
        resetAcceptorCache(transmitter);
        transmitter.currentTransmitterConnections = 0x00;
        transmitter.refreshConnections();

        if (!(transmitter instanceof BufferedTransmitter bufferedTransmitter)) {
            tile.sendUpdatePacket();
            tile.setChanged();
            return true;
        }

        if (!transmitter.hasTransmitterNetwork()) {
            TransmitterNetworkRegistry.registerOrphanTransmitter(transmitter);
            return false;
        }

        if (transmitter.getTransmitterNetwork() instanceof DynamicBufferedNetwork network) {
            if (network.getCapacity() <= 0) {
                ((DynamicBufferedNetworkAccessor) network).cmc$invokeUpdateCapacity(bufferedTransmitter);
            }
            restoreSavedBuffer(bufferedTransmitter, snapshot);
            if (shouldAbsorbRestoredBuffer(network, bufferedTransmitter)) {
                network.absorbBuffer(bufferedTransmitter);
            } else {
                clearRedundantSavedBuffer(network, bufferedTransmitter);
            }
            network.updateCapacity();
            network.clampBuffer();
            ((DynamicBufferedNetworkAccessor) network).cmc$invokeForceScaleUpdate();
            refreshNetworkVisualState(network, snapshot);
            network.markDirty();
        }

        transmitter.requestsUpdate();
        tile.sendUpdatePacket();
        tile.setChanged();
        return true;
    }

    private static void restoreSavedBuffer(BufferedTransmitter<?, ?, ?, ?> bufferedTransmitter, SavedTransmitterState snapshot) {
        if (bufferedTransmitter instanceof MechanicalPipe pipe) {
            FluidStack fluid = pipe.saveShare.isEmpty() ? snapshot.fluid() : pipe.saveShare;
            if (pipe.buffer.isEmpty() && !pipe.saveShare.isEmpty()) {
                pipe.buffer.setStack(pipe.saveShare.copy());
            } else if (pipe.buffer.isEmpty() && !fluid.isEmpty()) {
                pipe.saveShare = fluid.copy();
                pipe.buffer.setStack(fluid.copy());
            }
        } else if (bufferedTransmitter instanceof PressurizedTube tube) {
            ChemicalStack chemical = tube.saveShare.isEmpty() ? snapshot.chemical() : tube.saveShare;
            if (tube.chemicalTank.isEmpty() && !tube.saveShare.isEmpty()) {
                tube.chemicalTank.setStack(tube.saveShare.copy());
            } else if (tube.chemicalTank.isEmpty() && !chemical.isEmpty()) {
                tube.saveShare = chemical.copy();
                tube.chemicalTank.setStack(chemical.copy());
            }
        } else if (bufferedTransmitter instanceof UniversalCable cable) {
            long energy = cable.lastWrite > 0 ? cable.lastWrite : snapshot.energy();
            if (cable.buffer.isEmpty() && cable.lastWrite > 0) {
                cable.buffer.setEnergy(cable.lastWrite);
            } else if (cable.buffer.isEmpty() && energy > 0) {
                cable.lastWrite = energy;
                cable.buffer.setEnergy(energy);
            }
        }
    }

    private static boolean shouldAbsorbRestoredBuffer(
            DynamicBufferedNetwork<?, ?, ?, ?> network,
            BufferedTransmitter<?, ?, ?, ?> bufferedTransmitter
    ) {
        if (bufferedTransmitter instanceof MechanicalPipe pipe && network instanceof FluidNetwork fluidNetwork) {
            return !pipe.buffer.isEmpty() && fluidNetwork.fluidTank.isEmpty();
        } else if (bufferedTransmitter instanceof PressurizedTube tube && network instanceof ChemicalNetwork chemicalNetwork) {
            return !tube.chemicalTank.isEmpty() && chemicalNetwork.chemicalTank.isEmpty();
        } else if (bufferedTransmitter instanceof UniversalCable cable && network instanceof EnergyNetwork energyNetwork) {
            return !cable.buffer.isEmpty() && energyNetwork.energyContainer.isEmpty();
        }
        return true;
    }

    private static void clearRedundantSavedBuffer(
            DynamicBufferedNetwork<?, ?, ?, ?> network,
            BufferedTransmitter<?, ?, ?, ?> bufferedTransmitter
    ) {
        if (bufferedTransmitter instanceof MechanicalPipe pipe && network instanceof FluidNetwork fluidNetwork && !fluidNetwork.fluidTank.isEmpty()) {
            pipe.buffer.setStack(FluidStack.EMPTY);
            pipe.saveShare = FluidStack.EMPTY;
        } else if (bufferedTransmitter instanceof PressurizedTube tube && network instanceof ChemicalNetwork chemicalNetwork && !chemicalNetwork.chemicalTank.isEmpty()) {
            tube.chemicalTank.setStack(ChemicalStack.EMPTY);
            tube.saveShare = ChemicalStack.EMPTY;
        } else if (bufferedTransmitter instanceof UniversalCable cable && network instanceof EnergyNetwork energyNetwork && !energyNetwork.energyContainer.isEmpty()) {
            cable.buffer.setEnergy(0);
            cable.lastWrite = 0;
        }
    }

    private static SavedTransmitterState snapshotTransmitterState(TileEntityTransmitter tile) {
        Transmitter<?, ?, ?> transmitter = tile.getTransmitter();
        if (transmitter instanceof MechanicalPipe pipe) {
            return SavedTransmitterState.fluid(snapshotFluidState(pipe), snapshotScale(pipe));
        } else if (transmitter instanceof PressurizedTube tube) {
            return SavedTransmitterState.chemical(snapshotChemicalState(tube), snapshotScale(tube));
        } else if (transmitter instanceof UniversalCable cable) {
            return SavedTransmitterState.energy(snapshotEnergyState(cable), snapshotScale(cable));
        }
        return SavedTransmitterState.EMPTY;
    }

    private static FluidStack snapshotFluidState(MechanicalPipe pipe) {
        if (!pipe.saveShare.isEmpty()) {
            return pipe.saveShare.copy();
        }
        if (!pipe.buffer.isEmpty()) {
            return pipe.buffer.getFluid().copy();
        }
        if (pipe.hasTransmitterNetwork() && pipe.getTransmitterNetwork() instanceof FluidNetwork network) {
            if (!network.fluidTank.isEmpty()) {
                return network.fluidTank.getFluid().copy();
            }
            if (!network.lastFluid.isEmpty()) {
                return network.lastFluid.copy();
            }
        }
        return FluidStack.EMPTY;
    }

    private static ChemicalStack snapshotChemicalState(PressurizedTube tube) {
        if (!tube.saveShare.isEmpty()) {
            return tube.saveShare.copy();
        }
        if (!tube.chemicalTank.isEmpty()) {
            return tube.chemicalTank.getStack().copy();
        }
        if (tube.hasTransmitterNetwork() && tube.getTransmitterNetwork() instanceof ChemicalNetwork network) {
            if (!network.chemicalTank.isEmpty()) {
                return network.chemicalTank.getStack().copy();
            }
            if (!network.lastChemical.is(MekanismAPI.EMPTY_CHEMICAL_KEY)) {
                return new ChemicalStack(network.lastChemical, 1);
            }
        }
        return ChemicalStack.EMPTY;
    }

    private static long snapshotEnergyState(UniversalCable cable) {
        if (cable.lastWrite > 0) {
            return cable.lastWrite;
        }
        if (!cable.buffer.isEmpty()) {
            return cable.buffer.getEnergy();
        }
        if (cable.hasTransmitterNetwork() && cable.getTransmitterNetwork() instanceof EnergyNetwork network) {
            return network.energyContainer.getEnergy();
        }
        return 0;
    }

    private static float snapshotScale(Transmitter<?, ?, ?> transmitter) {
        if (transmitter.hasTransmitterNetwork() && transmitter.getTransmitterNetwork() instanceof DynamicBufferedNetwork<?, ?, ?, ?> network) {
            return network.currentScale;
        }
        return 0.0F;
    }

    private static void refreshNetworkVisualState(DynamicBufferedNetwork<?, ?, ?, ?> network, SavedTransmitterState snapshot) {
        if (network instanceof FluidNetwork fluidNetwork) {
            FluidStack fluid = !fluidNetwork.fluidTank.isEmpty() ? fluidNetwork.fluidTank.getFluid() : snapshot.fluid();
            if (!fluid.isEmpty()) {
                fluidNetwork.lastFluid = fluid.copyWithAmount(1);
                fluidNetwork.currentScale = visibleScale(fluidNetwork.currentScale, snapshot.scale(), fluid.getAmount(), fluidNetwork.getCapacity());
            }
        } else if (network instanceof ChemicalNetwork chemicalNetwork) {
            ChemicalStack chemical = !chemicalNetwork.chemicalTank.isEmpty()
                    ? chemicalNetwork.chemicalTank.getStack()
                    : snapshot.chemical();
            if (!chemical.isEmpty()) {
                chemicalNetwork.lastChemical = chemical.getChemicalHolder();
                chemicalNetwork.currentScale = visibleScale(
                        chemicalNetwork.currentScale,
                        snapshot.scale(),
                        chemical.getAmount(),
                        chemicalNetwork.getCapacity()
                );
            }
        } else if (network instanceof EnergyNetwork energyNetwork) {
            long energy = energyNetwork.energyContainer.getEnergy() > 0 ? energyNetwork.energyContainer.getEnergy() : snapshot.energy();
            if (energy > 0) {
                if (energyNetwork.energyContainer.isEmpty()) {
                    energyNetwork.energyContainer.setEnergy(Math.min(energy, energyNetwork.energyContainer.getMaxEnergy()));
                }
                energyNetwork.currentScale = visibleScale(
                        energyNetwork.currentScale,
                        snapshot.scale(),
                        energy,
                        energyNetwork.energyContainer.getMaxEnergy()
                );
            }
        }
    }

    private static float visibleScale(float currentScale, float snapshotScale, long amount, long capacity) {
        if (currentScale > 0.0F) {
            return currentScale;
        }
        if (snapshotScale > 0.0F) {
            return snapshotScale;
        }
        if (amount <= 0 || capacity <= 0) {
            return 0.0F;
        }
        return Math.min(1.0F, (float) amount / (float) capacity);
    }

    private static void scheduleTransmitterResync(TileEntityTransmitter tile, SavedTransmitterState snapshot) {
        Level level = tile.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        SavedTransmitterState effectiveSnapshot = snapshot == null ? snapshotTransmitterState(tile) : snapshot.withFallback(snapshotTransmitterState(tile));
        PENDING_TRANSMITTER_RESYNCS.put(
                new MoveKey(serverLevel.dimension(), tile.getBlockPos().asLong()),
                new PendingTransmitterResync(
                        POST_MOVE_TRANSMITTER_RESYNC_ATTEMPTS,
                        serverLevel.getGameTime() + 1,
                        effectiveSnapshot
                )
        );
    }

    private static void resetAcceptorCache(Transmitter<?, ?, ?> transmitter) {
        AbstractAcceptorCache<?, ?> cache = transmitter.getAcceptorCache();
        ((AbstractAcceptorCacheAccessor) cache).cmc$getCachedAcceptors().clear();
        ((AbstractAcceptorCacheAccessor) cache).cmc$getCachedListeners().clear();
        cache.currentAcceptorConnections = 0x00;
        if (transmitter.getLevel() instanceof ServerLevel serverLevel) {
            cache.initializeCache(serverLevel);
        }
        for (Direction direction : Direction.values()) {
            transmitter.markDirtyAcceptor(direction);
        }
    }

    private static void cleanupDeferredTransmitterSourceMoves(long now) {
        Iterator<Map.Entry<MoveKey, Long>> iterator = DEFERRED_TRANSMITTER_SOURCE_MOVES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now) {
                iterator.remove();
            }
        }
    }

    private record MoveKey(ResourceKey<Level> dimension, long pos) {
    }

    private record PendingTransmitterResync(int remainingAttempts, long nextAttemptTick, SavedTransmitterState snapshot) {
    }

    private record SavedTransmitterState(FluidStack fluid, ChemicalStack chemical, long energy, float scale) {
        private static final SavedTransmitterState EMPTY = new SavedTransmitterState(
                FluidStack.EMPTY,
                ChemicalStack.EMPTY,
                0,
                0.0F
        );

        private static SavedTransmitterState fluid(FluidStack fluid, float scale) {
            return new SavedTransmitterState(fluid.isEmpty() ? FluidStack.EMPTY : fluid.copy(), ChemicalStack.EMPTY, 0, scale);
        }

        private static SavedTransmitterState chemical(ChemicalStack chemical, float scale) {
            return new SavedTransmitterState(FluidStack.EMPTY, chemical.isEmpty() ? ChemicalStack.EMPTY : chemical.copy(), 0, scale);
        }

        private static SavedTransmitterState energy(long energy, float scale) {
            return new SavedTransmitterState(FluidStack.EMPTY, ChemicalStack.EMPTY, Math.max(0, energy), scale);
        }

        private boolean hasContents() {
            return !fluid.isEmpty() || !chemical.isEmpty() || energy > 0;
        }

        private SavedTransmitterState withFallback(SavedTransmitterState fallback) {
            if (fallback == null || fallback == EMPTY) {
                return this;
            }
            return new SavedTransmitterState(
                    fluid.isEmpty() ? fallback.fluid() : fluid,
                    chemical.isEmpty() ? fallback.chemical() : chemical,
                    energy > 0 ? energy : fallback.energy(),
                    scale > 0.0F ? scale : fallback.scale()
            );
        }
    }

    private static final class MoveScope {
        private final Set<MoveKey> sourceMoves = new HashSet<>();
        private final Set<MoveKey> movedTransmitters = new LinkedHashSet<>();
        private final Map<MoveKey, SavedTransmitterState> transmitterStates = new HashMap<>();
        private int depth;

        void begin() {
            if (depth == 0) {
                sourceMoves.clear();
                movedTransmitters.clear();
                transmitterStates.clear();
            }
            depth++;
        }

        void end() {
            if (depth > 0) {
                depth--;
            }
            if (depth == 0) {
                sourceMoves.clear();
                movedTransmitters.clear();
                transmitterStates.clear();
            }
        }

        boolean active() {
            return depth > 0;
        }

        Set<MoveKey> sourceMoves() {
            return sourceMoves;
        }

        Set<MoveKey> movedTransmitters() {
            return movedTransmitters;
        }

        Map<MoveKey, SavedTransmitterState> transmitterStates() {
            return transmitterStates;
        }
    }
}
