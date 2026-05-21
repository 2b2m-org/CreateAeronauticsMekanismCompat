package com.jarrettonesource.createmekanismcompat.assembly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class MekanismAssemblyMoveTracker {
    // Mekanism invalidates transmitter networks on a later server tick after Sable's move returns.
    private static final long DEFERRED_TRANSMITTER_SOURCE_MOVE_TICKS = 40;
    private static final Map<MoveKey, Long> DEFERRED_TRANSMITTER_SOURCE_MOVES = new HashMap<>();
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

    private static final class MoveScope {
        private final Set<MoveKey> sourceMoves = new HashSet<>();
        private int depth;

        void begin() {
            if (depth == 0) {
                sourceMoves.clear();
            }
            depth++;
        }

        void end() {
            if (depth > 0) {
                depth--;
            }
            if (depth == 0) {
                sourceMoves.clear();
            }
        }

        boolean active() {
            return depth > 0;
        }

        Set<MoveKey> sourceMoves() {
            return sourceMoves;
        }
    }
}
