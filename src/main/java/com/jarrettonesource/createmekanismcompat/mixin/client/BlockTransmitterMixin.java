package com.jarrettonesource.createmekanismcompat.mixin.client;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.common.block.transmitter.BlockTransmitter;
import mekanism.common.registries.MekanismItems;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.util.MultipartUtils;
import mekanism.common.util.MultipartUtils.AdvancedRayTraceResult;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockTransmitter.class, remap = false)
public abstract class BlockTransmitterMixin {
    @Inject(method = "getShape", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedConfiguratorShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context,
            CallbackInfoReturnable<VoxelShape> callback) {
        if (!context.isHoldingItem(MekanismItems.CONFIGURATOR.value()) || !(context instanceof EntityCollisionContext entityContext)) {
            return;
        }

        Entity entity = entityContext.getEntity();
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(pos);
        if (entity == null || subLevel == null) {
            return;
        }

        TileEntityTransmitter tile = WorldUtils.getTileEntity(TileEntityTransmitter.class, level, pos);
        if (tile == null) {
            return;
        }

        MultipartUtils.RayTraceVectors ray = MultipartUtils.getRayTraceVectors(entity);
        Vec3 localStart = subLevel.renderPose().transformPositionInverse(ray.start());
        Vec3 localEnd = subLevel.renderPose().transformPositionInverse(ray.end());
        AdvancedRayTraceResult result = MultipartUtils.collisionRayTrace(pos, localStart, localEnd, tile.getCollisionBoxes());
        if (result != null && result.valid()) {
            callback.setReturnValue(result.bounds);
        }
    }
}
