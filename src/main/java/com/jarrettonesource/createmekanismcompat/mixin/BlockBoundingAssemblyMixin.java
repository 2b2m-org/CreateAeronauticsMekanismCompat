package com.jarrettonesource.createmekanismcompat.mixin;

import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mekanism.common.block.BlockBounding;
import mekanism.common.tile.TileEntityBoundingBlock;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BlockBounding.class, remap = false)
public abstract class BlockBoundingAssemblyMixin implements BlockSubLevelAssemblyListener {
    @Unique
    private static final Map<String, BlockPos> cmc$pendingBoundingMasters = new ConcurrentHashMap<>();

    @Override
    public void beforeMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        TileEntityBoundingBlock sourceTile = WorldUtils.getTileEntity(TileEntityBoundingBlock.class, sourceLevel, oldPos, true);
        if (sourceTile == null || !sourceTile.canRedirectFrom(oldPos)) {
            return;
        }
        cmc$pendingBoundingMasters.put(cmc$key(sourceLevel, oldPos), sourceTile.getMainPos());

        // Sable removes the source blocks after copying them. Unlinking here prevents
        // Mekanism's bounding block removal hook from deleting the source master early.
        sourceTile.setMainLocation(null, false);
    }

    @Override
    public void afterMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        TileEntityBoundingBlock movedTile = WorldUtils.getTileEntity(TileEntityBoundingBlock.class, resultingLevel, newPos, true);
        if (movedTile == null) {
            return;
        }

        BlockPos oldMain = cmc$pendingBoundingMasters.remove(cmc$key(sourceLevel, oldPos));
        if (oldMain == null && movedTile.canRedirectFrom(newPos)) {
            oldMain = movedTile.getMainPos();
        }
        if (oldMain == null || oldMain.equals(oldPos)) {
            return;
        }

        movedTile.setMainLocation(newPos.offset(oldMain.subtract(oldPos)), true);
    }

    @Unique
    private static String cmc$key(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.asLong();
    }
}
