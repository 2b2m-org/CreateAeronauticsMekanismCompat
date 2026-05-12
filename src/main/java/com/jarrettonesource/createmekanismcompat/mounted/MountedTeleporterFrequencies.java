package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.lib.frequency.FrequencyManager;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MountedTeleporterFrequencies {
    private MountedTeleporterFrequencies() {
    }

    public static void refresh(ServerSubLevelContainer container) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                refresh(serverSubLevel);
            }
        }
    }

    public static void refresh(ServerSubLevel subLevel) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
            for (BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                if (blockEntity instanceof TileEntityTeleporter teleporter) {
                    refreshIfMounted(teleporter);
                }
            }
        }
    }

    public static void refreshIfMounted(TileEntityTeleporter teleporter) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get() || MountedMekanismContextResolver.resolve(teleporter).isEmpty()) {
            return;
        }
        TeleporterFrequency frequency = teleporter.getFrequency(FrequencyType.TELEPORTER);
        if (frequency == null) {
            return;
        }
        FrequencyManager<TeleporterFrequency> manager = FrequencyType.TELEPORTER.getFrequencyManager(frequency);
        if (manager != null) {
            manager.validateAndUpdate(teleporter, frequency);
        }
    }
}
