package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.assembly.MekanismAssemblyMoveTracker;
import com.jarrettonesource.createmekanismcompat.assembly.TeleporterFrequencyMoveSync;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BlockTile.class, remap = false)
public abstract class BlockTileAssemblyMixin implements BlockSubLevelAssemblyListener {
    @Override
    public void beforeMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        MekanismAssemblyMoveTracker.markSourceMove(sourceLevel, oldPos);
        TeleporterFrequencyMoveSync.beforeMove(sourceLevel.getBlockEntity(oldPos));
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
    }
}
