package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class MountedDimensionalStabilizerTickets {
    private static final Map<TileEntityDimensionalStabilizer, Set<ChunkPos>> LAST_CHUNKS = new WeakHashMap<>();

    private MountedDimensionalStabilizerTickets() {
    }

    public static void refresh(ServerSubLevelContainer container) {
        if (!CmcConfig.ENABLE_MOUNTED_DIMENSIONAL_STABILIZER.get()) {
            return;
        }
        for (SubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel instanceof ServerSubLevel serverSubLevel && !serverSubLevel.isRemoved()) {
                refresh(serverSubLevel);
            }
        }
    }

    public static void refresh(ServerSubLevel subLevel) {
        if (!CmcConfig.ENABLE_MOUNTED_DIMENSIONAL_STABILIZER.get()) {
            return;
        }
        for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
            for (BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                if (blockEntity instanceof TileEntityDimensionalStabilizer stabilizer) {
                    refreshIfChanged(stabilizer);
                }
            }
        }
    }

    public static void refreshIfChanged(TileEntityDimensionalStabilizer stabilizer) {
        if (!CmcConfig.ENABLE_MOUNTED_DIMENSIONAL_STABILIZER.get()) {
            return;
        }
        MountedMekanismContextResolver.resolve(stabilizer).ifPresentOrElse(context -> {
            Set<ChunkPos> current = ChunkTicketPolicy.dimensionalStabilizerChunks(context, stabilizer);
            if (!current.equals(LAST_CHUNKS.get(stabilizer))) {
                LAST_CHUNKS.put(stabilizer, Set.copyOf(current));
                stabilizer.getChunkLoader().refreshChunkTickets();
            }
        }, () -> LAST_CHUNKS.remove(stabilizer));
    }
}
