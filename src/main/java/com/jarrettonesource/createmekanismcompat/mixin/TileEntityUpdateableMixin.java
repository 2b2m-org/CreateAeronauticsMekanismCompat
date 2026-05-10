package com.jarrettonesource.createmekanismcompat.mixin;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.PacketUpdateTile;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityUpdateable.class, remap = false)
public abstract class TileEntityUpdateableMixin {
    @Inject(method = "sendUpdatePacket(Lnet/minecraft/world/level/block/entity/BlockEntity;)V", at = @At("HEAD"), cancellable = true)
    private void cmc$sendMountedUpdatePacket(BlockEntity tracking, CallbackInfo callback) {
        TileEntityUpdateable tile = (TileEntityUpdateable) (Object) this;
        ServerLevel level = cmc$serverLevel(tile.getLevel());
        BlockPos plotPos = cmc$resolvePlotPos(level, tile);
        if (level == null || plotPos == null) {
            return;
        }

        if (PacketUtils.hasPlayersTracking(level, plotPos)) {
            PacketUtils.sendToAllTracking(
                    new PacketUpdateTile(plotPos, tile.getReducedUpdateTag(level.registryAccess())),
                    level,
                    plotPos
            );
        }
        callback.cancel();
    }

    private static ServerLevel cmc$serverLevel(Level level) {
        return level instanceof ServerLevel serverLevel ? serverLevel : null;
    }

    private static BlockPos cmc$resolvePlotPos(ServerLevel level, BlockEntity tile) {
        if (level == null) {
            return null;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return null;
        }

        BlockPos localPos = tile.getBlockPos();
        ChunkPos localChunk = new ChunkPos(localPos);
        for (ServerSubLevel subLevel : container.getAllSubLevels()) {
            if (subLevel.isRemoved() || subLevel.getPlot().getChunkHolder(localChunk) == null) {
                continue;
            }

            ChunkPos plotChunk = subLevel.getPlot().toGlobal(localChunk);
            BlockPos plotPos = new BlockPos(
                    plotChunk.getMinBlockX() + (localPos.getX() & 15),
                    localPos.getY(),
                    plotChunk.getMinBlockZ() + (localPos.getZ() & 15)
            );
            BlockEntity candidate = level.getBlockEntity(plotPos);
            if (candidate == tile || candidate != null && candidate.getType() == tile.getType()) {
                return plotPos;
            }
        }
        return null;
    }
}
