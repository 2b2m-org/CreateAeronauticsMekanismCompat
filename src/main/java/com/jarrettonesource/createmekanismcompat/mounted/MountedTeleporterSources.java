package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MountedTeleporterSources {
    private MountedTeleporterSources() {
    }

    public static void tick(ServerSubLevelContainer container) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                tick(serverSubLevel);
            }
        }
    }

    public static void tick(ServerSubLevel subLevel) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
            for (BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                if (blockEntity instanceof TileEntityTeleporter teleporter) {
                    MountedMekanismContextResolver.resolve(teleporter).ifPresent(context ->
                            TileEntityMekanism.tickServer(context.level(), teleporter.getBlockPos(), teleporter.getBlockState(), teleporter));
                }
            }
        }
    }
}
