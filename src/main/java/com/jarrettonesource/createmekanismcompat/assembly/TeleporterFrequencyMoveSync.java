package com.jarrettonesource.createmekanismcompat.assembly;

import mekanism.common.lib.frequency.FrequencyManager;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class TeleporterFrequencyMoveSync {
    private TeleporterFrequencyMoveSync() {
    }

    public static void beforeMove(BlockEntity blockEntity) {
        if (blockEntity instanceof TileEntityTeleporter teleporter) {
            TeleporterFrequency frequency = teleporter.getFrequencyComponent().getFrequency(FrequencyType.TELEPORTER);
            if (frequency != null) {
                FrequencyManager<TeleporterFrequency> manager = FrequencyType.TELEPORTER.getFrequencyManager(frequency);
                if (manager != null) {
                    manager.deactivate(frequency, teleporter);
                }
            }
        }
    }

    public static void afterMove(BlockEntity blockEntity) {
        if (blockEntity instanceof TileEntityTeleporter teleporter) {
            TeleporterFrequency frequency = teleporter.getFrequencyComponent().getFrequency(FrequencyType.TELEPORTER);
            if (frequency != null) {
                FrequencyManager<TeleporterFrequency> manager = FrequencyType.TELEPORTER.getFrequencyManager(frequency);
                if (manager != null) {
                    manager.validateAndUpdate(teleporter, frequency);
                }
            }
        }
    }
}
