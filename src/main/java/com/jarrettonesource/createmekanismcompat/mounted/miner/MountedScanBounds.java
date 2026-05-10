package com.jarrettonesource.createmekanismcompat.mounted.miner;

import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;

public record MountedScanBounds(
        int minX,
        int maxX,
        int minY,
        int maxY,
        int minZ,
        int maxZ
) {
    public static MountedScanBounds current(MountedMekanismContext context, TileEntityDigitalMiner miner) {
        return aroundAnchor(context, miner, BlockPos.containing(context.globalCenter()));
    }

    public static MountedScanBounds aroundAnchor(MountedMekanismContext context, TileEntityDigitalMiner miner, BlockPos anchor) {
        int minX = anchor.getX() - miner.getRadius();
        int maxX = minX + miner.getDiameter() - 1;
        int minY = Math.max(miner.getMinY(), context.level().getMinBuildHeight());
        int maxY = Math.min(miner.getMaxY(), context.level().getMaxBuildHeight() - 1);
        int minZ = anchor.getZ() - miner.getRadius();
        int maxZ = minZ + miner.getDiameter() - 1;
        return new MountedScanBounds(minX, maxX, minY, maxY, minZ, maxZ);
    }

    public boolean isEmpty() {
        return maxX < minX || maxY < minY || maxZ < minZ;
    }

    public boolean contains(BlockPos pos) {
        return pos.getX() >= minX
                && pos.getX() <= maxX
                && pos.getY() >= minY
                && pos.getY() <= maxY
                && pos.getZ() >= minZ
                && pos.getZ() <= maxZ;
    }

    public boolean intersects(MountedScanSectionJob job) {
        int jobMinX = (job.chunkX() << 4) + job.minLocalX();
        int jobMaxX = (job.chunkX() << 4) + job.maxLocalX();
        int jobMinY = (job.sectionY() << 4) + job.minLocalY();
        int jobMaxY = (job.sectionY() << 4) + job.maxLocalY();
        int jobMinZ = (job.chunkZ() << 4) + job.minLocalZ();
        int jobMaxZ = (job.chunkZ() << 4) + job.maxLocalZ();
        return intersects(jobMinX, jobMaxX, jobMinY, jobMaxY, jobMinZ, jobMaxZ);
    }

    public boolean intersects(ChunkPos chunk) {
        return intersects(
                chunk.getMinBlockX(),
                chunk.getMaxBlockX(),
                minY,
                maxY,
                chunk.getMinBlockZ(),
                chunk.getMaxBlockZ()
        );
    }

    public boolean intersectsHorizontalScan(BlockPos anchor, int radius, int diameter) {
        int scanMinX = anchor.getX() - radius;
        int scanMaxX = scanMinX + diameter - 1;
        int scanMinZ = anchor.getZ() - radius;
        int scanMaxZ = scanMinZ + diameter - 1;
        return intersects(scanMinX, scanMaxX, minY, maxY, scanMinZ, scanMaxZ);
    }

    public MountedScanBounds intersection(MountedScanBounds other) {
        return new MountedScanBounds(
                Math.max(minX, other.minX),
                Math.min(maxX, other.maxX),
                Math.max(minY, other.minY),
                Math.min(maxY, other.maxY),
                Math.max(minZ, other.minZ),
                Math.min(maxZ, other.maxZ)
        );
    }

    public int minChunkX() {
        return SectionPos.blockToSectionCoord(minX);
    }

    public int maxChunkX() {
        return SectionPos.blockToSectionCoord(maxX);
    }

    public int minChunkZ() {
        return SectionPos.blockToSectionCoord(minZ);
    }

    public int maxChunkZ() {
        return SectionPos.blockToSectionCoord(maxZ);
    }

    private boolean intersects(int otherMinX, int otherMaxX, int otherMinY, int otherMaxY, int otherMinZ, int otherMaxZ) {
        return !isEmpty()
                && otherMaxX >= minX
                && otherMinX <= maxX
                && otherMaxY >= minY
                && otherMinY <= maxY
                && otherMaxZ >= minZ
                && otherMinZ <= maxZ;
    }
}
