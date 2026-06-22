package com.jarrettonesource.createmekanismcompat.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.ArrayDeque;
import java.util.Map;
import mekanism.common.tile.TileEntityFluidTank;
import mekanism.common.tile.base.TileEntityUpdateable;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class CmcClientSubLevelHelper {
    private static final ThreadLocal<ArrayDeque<Boolean>> MOUNTED_RENDER_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private CmcClientSubLevelHelper() {
    }

    @Nullable
    public static ClientSubLevel resolve(BlockPos localPos) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(localPos);
        return subLevel == null || subLevel.isRemoved() ? null : subLevel;
    }

    @Nullable
    public static ClientSubLevel resolve(BlockEntity tile) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(tile);
        return subLevel == null || subLevel.isRemoved() ? null : subLevel;
    }

    public static void pushMountedRender(BlockEntity tile) {
        MOUNTED_RENDER_STACK.get().push(isMounted(tile));
    }

    public static void popMountedRender() {
        ArrayDeque<Boolean> stack = MOUNTED_RENDER_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            MOUNTED_RENDER_STACK.remove();
        }
    }

    public static boolean isMountedRenderActive() {
        for (boolean mounted : MOUNTED_RENDER_STACK.get()) {
            if (mounted) {
                return true;
            }
        }
        return false;
    }

    public static boolean isMounted(BlockPos localPos) {
        return isMountedRenderActive() || resolve(localPos) != null;
    }

    public static boolean isMounted(BlockEntity tile) {
        return resolve(tile) != null || resolve(tile.getBlockPos()) != null;
    }

    public static RenderType mountedTranslucentBlockSheet() {
        return Sheets.translucentCullBlockSheet();
    }

    public static RenderType mountedFluidTankBlockSheet() {
        return Sheets.translucentCullBlockSheet();
    }

    @Nullable
    public static Vec3 mountedRenderCullOrigin(BlockPos localPos) {
        ClientSubLevel subLevel = resolve(localPos);
        if (subLevel == null) {
            return Vec3.atLowerCornerOf(localPos);
        }

        return subLevel.renderPose().transformPosition(Vec3.atLowerCornerOf(localPos));
    }

    public static Vec3 projectCenter(BlockPos localPos) {
        ClientSubLevel subLevel = resolve(localPos);
        if (subLevel == null) {
            return Vec3.atCenterOf(localPos);
        }
        return subLevel.renderPose().transformPosition(Vec3.atCenterOf(localPos));
    }

    public static float mountedFluidScale(TileEntityFluidTank tile) {
        if (!isMounted(tile)) {
            return tile.prevScale;
        }
        return MekanismUtils.getScale(tile.prevScale, tile.fluidTank);
    }

    @Nullable
    public static TileEntityUpdateable resolveMountedUpdateTile(ClientLevel level, BlockPos globalPos) {
        TileEntityUpdateable directLocalTile = resolveMountedUpdateTileByLocalPosition(level, globalPos);
        if (directLocalTile != null) {
            return directLocalTile;
        }

        TileEntityUpdateable projectedTile = Sable.HELPER.<TileEntityUpdateable, ClientSubLevel>runIncludingSubLevels(
                level,
                Vec3.atCenterOf(globalPos),
                false,
                null,
                (subLevel, localPos) -> {
                    if (subLevel == null || subLevel.isRemoved()) {
                        return null;
                    }
                    return findProjectedMountedUpdateTile(level, subLevel, localPos, globalPos);
                }
        );
        if (projectedTile != null) {
            return projectedTile;
        }

        TileEntityUpdateable scannedTile = scanProjectedMountedUpdateTile(level, globalPos);
        if (scannedTile != null) {
            return scannedTile;
        }

        return null;
    }

    @Nullable
    private static TileEntityUpdateable resolveMountedUpdateTileByLocalPosition(ClientLevel level, BlockPos localPos) {
        ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        for (ClientSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel == null || subLevel.isRemoved()) {
                continue;
            }
            BlockEntity blockEntity = subLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(localPos);
            if (blockEntity instanceof TileEntityUpdateable tile) {
                return tile;
            }
        }
        return null;
    }

    @Nullable
    private static TileEntityUpdateable findProjectedMountedUpdateTile(ClientLevel level, ClientSubLevel subLevel,
            BlockPos localPos, BlockPos globalPos) {
        for (BlockPos candidatePos : BlockPos.betweenClosed(localPos.offset(-1, -1, -1), localPos.offset(1, 1, 1))) {
            BlockEntity blockEntity = level.getBlockEntity(candidatePos);
            if (blockEntity == null) {
                blockEntity = subLevel.getPlot().getEmbeddedLevelAccessor().getBlockEntity(candidatePos);
            }
            if (!(blockEntity instanceof TileEntityUpdateable tile) || !isInSubLevel(blockEntity, subLevel)) {
                continue;
            }

            BlockPos projectedPos = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(blockEntity.getBlockPos())));
            if (projectedPos.equals(globalPos)) {
                return tile;
            }
        }
        return null;
    }

    private static boolean isInSubLevel(BlockEntity blockEntity, ClientSubLevel subLevel) {
        return resolve(blockEntity) == subLevel || resolve(blockEntity.getBlockPos()) == subLevel;
    }

    @Nullable
    private static TileEntityUpdateable scanProjectedMountedUpdateTile(ClientLevel level, BlockPos globalPos) {
        ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        for (ClientSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel == null || subLevel.isRemoved()) {
                continue;
            }
            TileEntityUpdateable tile = scanProjectedMountedUpdateTile(subLevel, globalPos);
            if (tile != null) {
                return tile;
            }
        }
        return null;
    }

    @Nullable
    private static TileEntityUpdateable scanProjectedMountedUpdateTile(ClientSubLevel subLevel, BlockPos globalPos) {
        BlockPos center = subLevel.getPlot().getCenterBlock();
        for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
            LevelChunk chunk = holder.getChunk();
            if (chunk == null) {
                continue;
            }
            for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
                BlockEntity blockEntity = entry.getValue();
                if (!(blockEntity instanceof TileEntityUpdateable tile) || !isInSubLevel(blockEntity, subLevel)) {
                    continue;
                }

                BlockPos projectedPos = BlockPos.containing(subLevel.logicalPose().transformPosition(Vec3.atCenterOf(entry.getKey())));
                if (projectedPos.equals(globalPos)) {
                    return tile;
                }
            }
        }
        return null;
    }
}
