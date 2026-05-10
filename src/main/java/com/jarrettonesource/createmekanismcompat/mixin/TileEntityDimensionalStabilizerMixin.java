package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.ChunkTicketPolicy;
import com.jarrettonesource.createmekanismcompat.mounted.MountedDimensionalStabilizerTickets;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import java.util.Set;
import mekanism.common.tile.machine.TileEntityDimensionalStabilizer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityDimensionalStabilizer.class, remap = false)
public abstract class TileEntityDimensionalStabilizerMixin {
    @Inject(method = "getChunkSet", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedChunkSet(CallbackInfoReturnable<Set<ChunkPos>> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_DIMENSIONAL_STABILIZER.get()) {
            return;
        }
        TileEntityDimensionalStabilizer stabilizer = (TileEntityDimensionalStabilizer) (Object) this;
        MountedMekanismContextResolver.resolve(stabilizer)
                .map(context -> ChunkTicketPolicy.dimensionalStabilizerChunks(context, stabilizer))
                .ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"))
    private void cmc$refreshMountedChunkTickets(CallbackInfoReturnable<Boolean> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_DIMENSIONAL_STABILIZER.get()) {
            return;
        }
        TileEntityDimensionalStabilizer stabilizer = (TileEntityDimensionalStabilizer) (Object) this;
        MountedDimensionalStabilizerTickets.refreshIfChanged(stabilizer);
    }
}
