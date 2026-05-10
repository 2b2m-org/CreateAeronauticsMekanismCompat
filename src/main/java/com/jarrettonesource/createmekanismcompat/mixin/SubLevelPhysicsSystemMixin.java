package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.mounted.MountedDimensionalStabilizerTickets;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SubLevelPhysicsSystem.class, remap = false)
public abstract class SubLevelPhysicsSystemMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void cmc$refreshMountedTicketsBeforeChunkChecks(SubLevelContainer sidelessContainer, CallbackInfo callback) {
        if (sidelessContainer instanceof ServerSubLevelContainer container) {
            MountedDimensionalStabilizerTickets.refresh(container);
        }
    }

    @Inject(method = "updatePose", at = @At("RETURN"))
    private void cmc$refreshMountedTicketsAfterPoseUpdate(ServerSubLevel serverSubLevel, CallbackInfo callback) {
        MountedDimensionalStabilizerTickets.refresh(serverSubLevel);
    }
}
