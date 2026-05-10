package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedScanBounds;
import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedScanState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public final class ChunkTicketPolicy {
    private ChunkTicketPolicy() {
    }

    public static Set<ChunkPos> digitalMinerChunks(MountedMekanismContext context, TileEntityDigitalMiner miner, MountedScanState state) {
        LinkedHashSet<ChunkPos> chunks = new LinkedHashSet<>();
        MountedScanBounds bounds = MountedScanBounds.current(context, miner);
        addDigitalMinerScanChunks(chunks, context, bounds);
        for (ChunkPos recentChunk : state.recentTicketChunks()) {
            if (bounds.intersects(recentChunk)) {
                chunks.add(recentChunk);
            }
        }
        return trim(chunks, CmcConfig.DIGITAL_MINER_MAX_TICKET_CHUNKS.get());
    }

    public static Set<ChunkPos> teleporterChunks(MountedMekanismContext context, TileEntityTeleporter teleporter) {
        return Collections.singleton(new ChunkPos(context.globalBlockPos()));
    }

    public static Set<ChunkPos> dimensionalStabilizerChunks(MountedMekanismContext context, TileEntityDimensionalStabilizer stabilizer) {
        ChunkPos center = new ChunkPos(context.globalBlockPos());
        return new LinkedHashSet<>(selectedDimensionalStabilizerGrid(stabilizer, center));
    }

    private static List<ChunkPos> selectedDimensionalStabilizerGrid(TileEntityDimensionalStabilizer stabilizer, ChunkPos center) {
        List<ChunkPos> chunks = new ArrayList<>();
        for (int x = -TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; x <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; x++) {
            for (int z = -TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; z <= TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS; z++) {
                if (stabilizer.isChunkLoadingAt(x + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS, z + TileEntityDimensionalStabilizer.MAX_LOAD_RADIUS)) {
                    chunks.add(new ChunkPos(center.x + x, center.z + z));
                }
            }
        }
        sortByDistanceTo(chunks, center);
        return chunks;
    }

    private static Set<ChunkPos> trim(LinkedHashSet<ChunkPos> chunks, int maxSize) {
        if (chunks.size() <= maxSize) {
            return chunks;
        }
        LinkedHashSet<ChunkPos> trimmed = new LinkedHashSet<>();
        int added = 0;
        for (ChunkPos chunk : chunks) {
            trimmed.add(chunk);
            added++;
            if (added >= maxSize) {
                break;
            }
        }
        return trimmed;
    }

    private static void sortByDistanceTo(List<ChunkPos> chunks, ChunkPos center) {
        chunks.sort(Comparator
                .comparingInt((ChunkPos chunk) -> Math.abs(chunk.x - center.x) + Math.abs(chunk.z - center.z))
                .thenComparingInt(chunk -> Math.abs(chunk.x - center.x))
                .thenComparingInt(chunk -> Math.abs(chunk.z - center.z)));
    }

    private static void addDigitalMinerScanChunks(LinkedHashSet<ChunkPos> chunks, MountedMekanismContext context, MountedScanBounds bounds) {
        if (bounds.isEmpty()) {
            return;
        }
        int centerX = BlockPos.containing(context.globalCenter()).getX();
        int centerZ = BlockPos.containing(context.globalCenter()).getZ();
        int centerChunkX = SectionPos.blockToSectionCoord(centerX);
        int centerChunkZ = SectionPos.blockToSectionCoord(centerZ);

        List<ChunkPos> scanChunks = new ArrayList<>();
        for (int chunkX = bounds.minChunkX(); chunkX <= bounds.maxChunkX(); chunkX++) {
            for (int chunkZ = bounds.minChunkZ(); chunkZ <= bounds.maxChunkZ(); chunkZ++) {
                scanChunks.add(new ChunkPos(chunkX, chunkZ));
            }
        }
        sortByDistanceTo(scanChunks, new ChunkPos(centerChunkX, centerChunkZ));
        chunks.addAll(scanChunks);
    }
}
