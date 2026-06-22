package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import mekanism.client.render.tileentity.RenderEnergyCube;
import mekanism.common.tile.TileEntityEnergyCube;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RenderEnergyCube.class, remap = false)
public abstract class RenderEnergyCubeMixin {
    @Redirect(
            method = "render(Lmekanism/common/tile/TileEntityEnergyCube;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getCenter()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 cmc$projectMountedEnergyCubeCenter(BlockPos localPos, TileEntityEnergyCube tile) {
        return CmcClientSubLevelHelper.projectCenter(localPos);
    }
}
