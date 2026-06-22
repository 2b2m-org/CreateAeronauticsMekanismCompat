package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mekanism.client.render.MekanismRenderer;
import mekanism.client.render.RenderResizableCuboid;
import mekanism.client.render.RenderResizableCuboid.FaceDisplay;
import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = MekanismRenderer.class, remap = false)
public abstract class MekanismRendererMixin {
    @Redirect(
            method = "renderObject(Lmekanism/client/render/MekanismRenderer$Model3D;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIILmekanism/client/render/RenderResizableCuboid$FaceDisplay;Lnet/minecraft/client/Camera;Lnet/minecraft/core/BlockPos;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lmekanism/client/render/RenderResizableCuboid;renderCube(Lmekanism/client/render/MekanismRenderer$Model3D;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIILmekanism/client/render/RenderResizableCuboid$FaceDisplay;Lnet/minecraft/client/Camera;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private static void cmc$renderMountedCuboidDoubleSided(MekanismRenderer.Model3D model, PoseStack poseStack,
            VertexConsumer vertexConsumer, int color, int light, int overlay, FaceDisplay faceDisplay, Camera camera,
            Vec3 cullOrigin, MekanismRenderer.Model3D originalModel, PoseStack originalPoseStack,
            VertexConsumer originalVertexConsumer, int originalColor, int originalLight, int originalOverlay,
            FaceDisplay originalFaceDisplay, Camera originalCamera, BlockPos blockPos) {
        if (CmcClientSubLevelHelper.isMounted(blockPos)) {
            RenderResizableCuboid.renderCube(
                    model,
                    poseStack,
                    vertexConsumer,
                    color,
                    light,
                    overlay,
                    faceDisplay,
                    camera,
                    CmcClientSubLevelHelper.mountedRenderCullOrigin(blockPos)
            );
            return;
        }
        RenderResizableCuboid.renderCube(model, poseStack, vertexConsumer, color, light, overlay, faceDisplay, camera, cullOrigin);
    }
}
