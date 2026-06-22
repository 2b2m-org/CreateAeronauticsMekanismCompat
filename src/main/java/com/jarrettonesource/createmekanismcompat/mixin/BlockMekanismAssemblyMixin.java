package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.assembly.MekanismAssemblyMoveTracker;
import com.jarrettonesource.createmekanismcompat.assembly.TeleporterFrequencyMoveSync;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import mekanism.common.block.BlockMekanism;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockMekanism.class, remap = false)
public abstract class BlockMekanismAssemblyMixin implements BlockSubLevelAssemblyListener {
    @Override
    public void beforeMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        MekanismAssemblyMoveTracker.markSourceMove(sourceLevel, oldPos);
        TeleporterFrequencyMoveSync.beforeMove(sourceLevel.getBlockEntity(oldPos));
        if (state.getBlock() instanceof BlockTransmitter || sourceLevel.getBlockEntity(oldPos) instanceof TileEntityTransmitter) {
            MekanismAssemblyMoveTracker.prepareTransmitterSourceMove(sourceLevel, oldPos);
        }
    }

    @Override
    public void afterMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        BlockEntity movedTile = resultingLevel.getBlockEntity(newPos);
        if (movedTile instanceof TileEntityUpdateable updateable) {
            updateable.onAdded();
        }
        if (movedTile instanceof TileEntityMekanism mekanismTile) {
            mekanismTile.resyncMasterToBounding();
        }
        TeleporterFrequencyMoveSync.afterMove(movedTile);
        if (movedTile instanceof TileEntityTransmitter transmitter) {
            MekanismAssemblyMoveTracker.markMovedTransmitter(resultingLevel, newPos);
            transmitter.onAdded();
        }
    }
}
