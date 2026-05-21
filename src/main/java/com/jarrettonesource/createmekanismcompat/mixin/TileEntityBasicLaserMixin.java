package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import mekanism.common.tile.laser.TileEntityBasicLaser;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = TileEntityBasicLaser.class, remap = false)
public abstract class TileEntityBasicLaserMixin {
    @Redirect(
            method = "onUpdateServer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;clip(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;")
    )
    private BlockHitResult cmc$clipLaserInBeamSpace(Level level, ClipContext context) {
        BlockHitResult result = level.clip(context);
        if (!CmcConfig.ENABLE_MOUNTED_LASERS.get()) {
            return result;
        }
        if (result.getType() == HitResult.Type.MISS) {
            return result;
        }

        SubLevel beamSubLevel = Sable.HELPER.getContaining(level, context.getFrom());
        SubLevel hitSubLevel = Sable.HELPER.getContaining(level, result.getBlockPos());
        if (beamSubLevel == hitSubLevel) {
            return result;
        }

        Vec3 hitLocation = result.getLocation();
        if (hitSubLevel != null) {
            hitLocation = hitSubLevel.logicalPose().transformPosition(hitLocation);
        }
        if (beamSubLevel != null) {
            hitLocation = beamSubLevel.logicalPose().transformPositionInverse(hitLocation);
        }
        return new BlockHitResult(hitLocation, result.getDirection(), result.getBlockPos(), result.isInside());
    }
}
