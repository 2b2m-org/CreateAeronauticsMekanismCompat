package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.tileentity.RenderFluidTank;
import mekanism.common.tile.TileEntityFluidTank;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderFluidTank.class, remap = false)
public abstract class RenderFluidTankMixin {
    @Inject(
            method = "render(Lmekanism/common/tile/TileEntityFluidTank;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void cmc$pushMountedFluidTankRender(TileEntityFluidTank tile, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler, CallbackInfo callback) {
        CmcClientSubLevelHelper.pushMountedRender(tile);
    }

    @Inject(
            method = "render(Lmekanism/common/tile/TileEntityFluidTank;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN")
    )
    private void cmc$popMountedFluidTankRender(TileEntityFluidTank tile, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler, CallbackInfo callback) {
        CmcClientSubLevelHelper.popMountedRender();
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/TileEntityFluidTank;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "FIELD", target = "Lmekanism/common/tile/TileEntityFluidTank;prevScale:F")
    )
    private float cmc$useMountedFluidAmountScale(TileEntityFluidTank tile) {
        return CmcClientSubLevelHelper.mountedFluidScale(tile);
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/TileEntityFluidTank;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType cmc$useMountedFluidTankRenderType() {
        return CmcClientSubLevelHelper.mountedFluidTankBlockSheet();
    }
}
