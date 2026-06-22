package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.client.render.transmitter.RenderLogisticalTransporter;
import mekanism.common.tile.transmitter.TileEntityLogisticalTransporterBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderLogisticalTransporter.class, remap = false)
public abstract class RenderLogisticalTransporterMixin {
    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityLogisticalTransporterBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void cmc$pushMountedLogisticalTransporterRender(TileEntityLogisticalTransporterBase tile,
            float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlayLight,
            ProfilerFiller profiler, CallbackInfo callback) {
        CmcClientSubLevelHelper.pushMountedRender(tile);
    }

    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityLogisticalTransporterBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN")
    )
    private void cmc$popMountedLogisticalTransporterRender(TileEntityLogisticalTransporterBase tile, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler,
            CallbackInfo callback) {
        CmcClientSubLevelHelper.popMountedRender();
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityLogisticalTransporterBase;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType cmc$useMountedLogisticalTransporterRenderType() {
        return CmcClientSubLevelHelper.mountedTranslucentBlockSheet();
    }
}
