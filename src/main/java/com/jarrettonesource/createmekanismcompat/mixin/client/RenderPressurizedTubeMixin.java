package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import mekanism.api.MekanismAPI;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.transmitter.PressurizedTube;
import mekanism.client.render.transmitter.RenderPressurizedTube;
import mekanism.common.tile.transmitter.TileEntityPressurizedTube;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderPressurizedTube.class, remap = false)
public abstract class RenderPressurizedTubeMixin {
    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityPressurizedTube;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD")
    )
    private void cmc$pushMountedPressurizedTubeRender(TileEntityPressurizedTube tile, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler,
            CallbackInfo callback) {
        CmcClientSubLevelHelper.pushMountedRender(tile);
    }

    @Inject(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityPressurizedTube;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("RETURN")
    )
    private void cmc$popMountedPressurizedTubeRender(TileEntityPressurizedTube tile, float partialTick,
            PoseStack poseStack, MultiBufferSource buffer, int light, int overlayLight, ProfilerFiller profiler,
            CallbackInfo callback) {
        CmcClientSubLevelHelper.popMountedRender();
    }

    @Inject(
            method = "shouldRenderTransmitter(Lmekanism/common/tile/transmitter/TileEntityPressurizedTube;Lnet/minecraft/world/phys/Vec3;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmc$shouldRenderMountedVisualChemical(TileEntityPressurizedTube tile, Vec3 camera,
            CallbackInfoReturnable<Boolean> callback) {
        if (!CmcClientSubLevelHelper.isMounted(tile)) {
            return;
        }
        PressurizedTube transmitter = tile.getTransmitter();
        if (transmitter.hasTransmitterNetwork()
                && transmitter.getTransmitterNetwork() instanceof ChemicalNetwork network
                && !network.lastChemical.is(MekanismAPI.EMPTY_CHEMICAL_KEY)) {
            callback.setReturnValue(true);
        }
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityPressurizedTube;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Sheets;translucentCullBlockSheet()Lnet/minecraft/client/renderer/RenderType;")
    )
    private RenderType cmc$useMountedPressurizedTubeRenderType() {
        return CmcClientSubLevelHelper.mountedTranslucentBlockSheet();
    }

    @Redirect(
            method = "render(Lmekanism/common/tile/transmitter/TileEntityPressurizedTube;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
    )
    private float cmc$useMountedPressurizedTubeScale(float minimumScale, float currentScale) {
        if (CmcClientSubLevelHelper.isMountedRenderActive() && currentScale <= minimumScale) {
            return 1.0F;
        }
        return Math.max(minimumScale, currentScale);
    }
}
