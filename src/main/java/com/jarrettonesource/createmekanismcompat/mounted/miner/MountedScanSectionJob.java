package com.jarrettonesource.createmekanismcompat.mounted.miner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public record MountedScanSectionJob(
        int chunkX,
        int chunkZ,
        int sectionY,
        int minLocalX,
        int maxLocalX,
        int minLocalY,
        int maxLocalY,
        int minLocalZ,
        int maxLocalZ
) {
    public int size() {
        return widthX() * widthY() * widthZ();
    }

    public ChunkPos chunkPos() {
        return new ChunkPos(chunkX, chunkZ);
    }

    public BlockPos posAt(int cursor) {
        int x = cursor % widthX();
        int z = (cursor / widthX()) % widthZ();
        int y = cursor / (widthX() * widthZ());
        return new BlockPos(
                (chunkX << 4) + minLocalX + x,
                (sectionY << 4) + minLocalY + y,
                (chunkZ << 4) + minLocalZ + z
        );
    }

    public int localXAt(int cursor) {
        return minLocalX + cursor % widthX();
    }

    public int localYAt(int cursor) {
        return minLocalY + cursor / (widthX() * widthZ());
    }

    public int localZAt(int cursor) {
        return minLocalZ + (cursor / widthX()) % widthZ();
    }

    private int widthX() {
        return maxLocalX - minLocalX + 1;
    }

    private int widthY() {
        return maxLocalY - minLocalY + 1;
    }

    private int widthZ() {
        return maxLocalZ - minLocalZ + 1;
    }
}
