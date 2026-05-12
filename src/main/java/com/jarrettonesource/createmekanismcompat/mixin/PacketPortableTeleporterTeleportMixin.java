package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.mounted.MountedTeleporterTargets;
import mekanism.common.network.to_server.PacketPortableTeleporterTeleport;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = PacketPortableTeleporterTeleport.class, remap = false)
public abstract class PacketPortableTeleporterTeleportMixin {
    @Redirect(
            method = "handle",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;calculateEnergyCost(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/GlobalPos;)J")
    )
    private long cmc$calculateProjectedEnergyCost(Entity entity, Level targetWorld, GlobalPos coords) {
        return MountedTeleporterTargets.calculateProjectedEnergyCost(entity, targetWorld, coords);
    }

    @Redirect(
            method = "handle",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;getTeleporterTargetPos()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos cmc$getProjectedPortableTeleporterTarget(TileEntityTeleporter targetTeleporter) {
        return MountedTeleporterTargets.resolveProjectedTarget(targetTeleporter);
    }
}
