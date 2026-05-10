package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import mekanism.generators.common.config.MekanismGeneratorsConfig;
import mekanism.generators.common.tile.TileEntityWindGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityWindGenerator.class, remap = false)
public abstract class TileEntityWindGeneratorMixin {
    @Inject(method = "getMultiplier", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedWindMultiplier(CallbackInfoReturnable<Double> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_WIND_GENERATOR.get()) {
            return;
        }
        TileEntityWindGenerator generator = (TileEntityWindGenerator) (Object) this;
        MountedMekanismContextResolver.resolve(generator)
                .map(TileEntityWindGeneratorMixin::cmc$mountedMultiplier)
                .ifPresent(callback::setReturnValue);
    }

    @Unique
    private static double cmc$mountedMultiplier(MountedMekanismContext context) {
        Level level = context.level();
        BlockPos top = BlockPos.containing(context.globalCenter()).above(4);
        if (!level.getFluidState(top).isEmpty() || !level.canSeeSky(top)) {
            return 0D;
        }

        int minBuildHeight = level.getMinBuildHeight();
        int maxLevelHeight = Math.min(level.getMaxBuildHeight(), minBuildHeight + level.dimensionType().logicalHeight()) - 1;
        int minY = Math.max(MekanismGeneratorsConfig.generators.windGenerationMinY.get(), minBuildHeight);
        int maxY = Math.min(MekanismGeneratorsConfig.generators.windGenerationMaxY.get(), maxLevelHeight);
        int clampedY = Math.min(maxY, Math.max(minY, top.getY()));
        long minGeneration = MekanismGeneratorsConfig.generators.windGenerationMin.get();
        long maxGeneration = MekanismGeneratorsConfig.generators.windGenerationMax.get();
        double slope = ((double) (maxGeneration - minGeneration)) / (maxY - minY);
        double generation = minGeneration + (slope * (clampedY - minY));
        return generation / minGeneration;
    }
}
