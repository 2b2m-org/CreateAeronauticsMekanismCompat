package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.client.particle.LaserParticle;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LaserParticle.class, remap = false)
public abstract class LaserParticleMixin extends TextureSheetParticle implements ParticleSubLevelKickable {
    @Shadow
    @Final
    private Direction direction;

    @Shadow
    @Final
    private float halfLength;

    protected LaserParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Shadow
    protected abstract int getLightColor(float partialTick);

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void cmc$renderTrackedSubLevelLaser(@NotNull VertexConsumer vertexConsumer, Camera camera, float partialTicks, CallbackInfo callback) {
        SubLevel subLevel = cmc$trackingSubLevel();
        if (subLevel == null || !CmcConfig.ENABLE_MOUNTED_LASERS.get()) {
            return;
        }

        Vec3 transformedAxis = subLevel.logicalPose().transformNormal(cmc$localAxis());
        double axisLength = transformedAxis.length();
        if (axisLength <= 1.0E-6D) {
            return;
        }

        Vector3f axis = new Vector3f(
                (float) (transformedAxis.x / axisLength),
                (float) (transformedAxis.y / axisLength),
                (float) (transformedAxis.z / axisLength)
        );
        Vector3f crossA = cmc$perpendicular(axis);
        Vector3f crossB = new Vector3f(axis).cross(crossA).normalize();
        Vec3 view = camera.getPosition();
        float centerX = (float) (Mth.lerp(partialTicks, xo, x) - view.x());
        float centerY = (float) (Mth.lerp(partialTicks, yo, y) - view.y());
        float centerZ = (float) (Mth.lerp(partialTicks, zo, z) - view.z());
        float globalHalfLength = (float) (halfLength * axisLength);
        float quadSize = getQuadSize(partialTicks);
        int light = getLightColor(partialTicks);
        cmc$drawBeamPlane(vertexConsumer, axis, crossA, globalHalfLength, centerX, centerY, centerZ, quadSize, light);
        cmc$drawBeamPlane(vertexConsumer, axis, crossB, globalHalfLength, centerX, centerY, centerZ, quadSize, light);
        callback.cancel();
    }

    @Inject(method = "getRenderBoundingBox", at = @At("HEAD"), cancellable = true)
    private void cmc$getTrackedSubLevelLaserBounds(float partialTicks, CallbackInfoReturnable<AABB> callback) {
        SubLevel subLevel = cmc$trackingSubLevel();
        if (subLevel == null || !CmcConfig.ENABLE_MOUNTED_LASERS.get()) {
            return;
        }

        Vec3 transformedAxis = subLevel.logicalPose().transformNormal(cmc$localAxis());
        double axisLength = transformedAxis.length();
        if (axisLength <= 1.0E-6D) {
            return;
        }

        double centerX = Mth.lerp(partialTicks, xo, x);
        double centerY = Mth.lerp(partialTicks, yo, y);
        double centerZ = Mth.lerp(partialTicks, zo, z);
        double dx = transformedAxis.x * halfLength;
        double dy = transformedAxis.y * halfLength;
        double dz = transformedAxis.z * halfLength;
        AABB bounds = new AABB(centerX - dx, centerY - dy, centerZ - dz, centerX + dx, centerY + dy, centerZ + dz)
                .inflate(Math.max(getQuadSize(partialTicks), 0.1F));
        callback.setReturnValue(bounds);
    }

    @Unique
    private SubLevel cmc$trackingSubLevel() {
        return ((ParticleExtension) (Object) this).sable$getTrackingSubLevel();
    }

    @Override
    public boolean sable$shouldCareAboutIntersectingSubLevels() {
        return false;
    }

    @Override
    public boolean sable$shouldKickFromTracking() {
        return false;
    }

    @Override
    public boolean sable$shouldCollideWithTrackingSubLevel() {
        return false;
    }

    @Unique
    private Vec3 cmc$localAxis() {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    @Unique
    private static Vector3f cmc$perpendicular(Vector3f direction) {
        Vector3f candidate = Math.abs(direction.y()) < 0.9F ? new Vector3f(0.0F, 1.0F, 0.0F) : new Vector3f(1.0F, 0.0F, 0.0F);
        return candidate.cross(direction, new Vector3f()).normalize();
    }

    @Unique
    private void cmc$drawBeamPlane(VertexConsumer vertexConsumer, Vector3f axis, Vector3f widthAxis, float globalHalfLength,
            float centerX, float centerY, float centerZ, float quadSize, int light) {
        Vector3f along = new Vector3f(axis).mul(globalHalfLength);
        Vector3f width = new Vector3f(widthAxis).mul(quadSize);
        Vector3f p0 = new Vector3f(centerX, centerY, centerZ).sub(along).sub(width);
        Vector3f p1 = new Vector3f(centerX, centerY, centerZ).add(along).sub(width);
        Vector3f p2 = new Vector3f(centerX, centerY, centerZ).add(along).add(width);
        Vector3f p3 = new Vector3f(centerX, centerY, centerZ).sub(along).add(width);
        float uMin = getU0();
        float uMax = getU1();
        float vMin = getV0();
        float vMax = getV1();
        cmc$addVertex(vertexConsumer, p0, uMax, vMax, light);
        cmc$addVertex(vertexConsumer, p1, uMax, vMin, light);
        cmc$addVertex(vertexConsumer, p2, uMin, vMin, light);
        cmc$addVertex(vertexConsumer, p3, uMin, vMax, light);
        cmc$addVertex(vertexConsumer, p1, uMax, vMin, light);
        cmc$addVertex(vertexConsumer, p0, uMax, vMax, light);
        cmc$addVertex(vertexConsumer, p3, uMin, vMax, light);
        cmc$addVertex(vertexConsumer, p2, uMin, vMin, light);
    }

    @Unique
    private void cmc$addVertex(VertexConsumer vertexConsumer, Vector3f position, float u, float v, int light) {
        vertexConsumer.addVertex(position.x(), position.y(), position.z())
                .setUv(u, v)
                .setColor(rCol, gCol, bCol, alpha)
                .setLight(light);
    }
}
