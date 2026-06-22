package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.assembly.MekanismAssemblyMoveTracker;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public abstract class SubLevelAssemblyHelperMoveMixin {
    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private static void cmc$beginMekanismMoveTracking(
            ServerLevel sourceLevel,
            SubLevelAssemblyHelper.AssemblyTransform transform,
            Iterable<BlockPos> positions,
            CallbackInfo callback
    ) {
        MekanismAssemblyMoveTracker.beginMoveBlocks();
    }

    @Inject(method = "moveBlocks", at = @At("RETURN"))
    private static void cmc$endMekanismMoveTracking(
            ServerLevel sourceLevel,
            SubLevelAssemblyHelper.AssemblyTransform transform,
            Iterable<BlockPos> positions,
            CallbackInfo callback
    ) {
        MekanismAssemblyMoveTracker.refreshMovedTransmitters(transform, positions);
        MekanismAssemblyMoveTracker.endMoveBlocks();
    }
}
