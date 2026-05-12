package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.ChunkTicketPolicy;
import com.jarrettonesource.createmekanismcompat.mounted.MountedAabb;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import com.jarrettonesource.createmekanismcompat.mounted.MountedTeleporterTargets;
import java.util.List;
import java.util.Set;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityTeleporter.class, remap = false)
public abstract class TileEntityTeleporterMixin {
    @Unique
    @Nullable
    private ChunkPos cmc$lastGlobalChunk;

    @Inject(method = "getToTeleport", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedEntitiesToTeleport(boolean sameDimension, Level destinationLevel, CallbackInfoReturnable<List<Entity>> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        MountedMekanismContextResolver.resolve(teleporter).ifPresent(context -> {
            AABB localBounds = ((TileEntityTeleporterAccessor) this).cmc$getTeleportBounds();
            if (localBounds == null) {
                callback.setReturnValue(List.of());
                return;
            }
            AABB globalBounds = MountedAabb.toGlobal(context, localBounds);
            TileEntityTeleporterAccessor accessor = (TileEntityTeleporterAccessor) this;
            callback.setReturnValue(context.level().getEntitiesOfClass(Entity.class, globalBounds,
                    entity -> sameDimension ? accessor.cmc$canTeleportEntity(entity, null) : accessor.cmc$canTeleportEntity(entity, destinationLevel)));
        });
    }

    @Inject(method = "getClosest", at = @At("HEAD"), cancellable = true)
    private void cmc$getProjectedClosestTeleporter(@Nullable TeleporterFrequency frequency, CallbackInfoReturnable<GlobalPos> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        MountedMekanismContextResolver.resolve(teleporter)
                .ifPresent(context -> callback.setReturnValue(MountedTeleporterTargets.resolveClosestForMountedSource(context, frequency)));
    }

    @Redirect(
            method = "cleanTeleportCache",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;")
    )
    private <T extends Entity> List<T> cmc$getMountedEntitiesForTeleportCache(Level level, Class<T> entityClass, AABB bounds) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return level.getEntitiesOfClass(entityClass, bounds);
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        return MountedMekanismContextResolver.resolve(teleporter)
                .map(context -> context.level().getEntitiesOfClass(entityClass, MountedAabb.toGlobal(context, bounds)))
                .orElseGet(() -> level.getEntitiesOfClass(entityClass, bounds));
    }

    @Redirect(
            method = "canTeleport",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;calculateEnergyCost(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/GlobalPos;)J")
    )
    private long cmc$calculateProjectedEnergyCostForReadiness(Entity entity, Level targetWorld, GlobalPos coords) {
        return cmc$calculateProjectedEnergyCost(entity, targetWorld, coords);
    }

    @Redirect(
            method = "teleport",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;calculateEnergyCost(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/GlobalPos;)J")
    )
    private long cmc$calculateProjectedEnergyCostForTeleport(Entity entity, Level targetWorld, GlobalPos coords) {
        return cmc$calculateProjectedEnergyCost(entity, targetWorld, coords);
    }

    @Redirect(
            method = "teleport",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;getTeleporterTargetPos()Lnet/minecraft/core/BlockPos;")
    )
    private BlockPos cmc$getProjectedTeleportTarget(TileEntityTeleporter targetTeleporter) {
        return MountedTeleporterTargets.resolveProjectedTarget(targetTeleporter);
    }

    @Inject(method = "getTeleporterTargetPos", at = @At("RETURN"), cancellable = true)
    private void cmc$projectMountedTeleporterTarget(CallbackInfoReturnable<BlockPos> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        callback.setReturnValue(MountedTeleporterTargets.projectIfLocal(teleporter, callback.getReturnValue()));
    }

    @Inject(method = "getChunkSet", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedChunkSet(CallbackInfoReturnable<Set<ChunkPos>> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        MountedMekanismContextResolver.resolve(teleporter)
                .map(context -> ChunkTicketPolicy.teleporterChunks(context, teleporter))
                .ifPresent(callback::setReturnValue);
    }

    @Inject(method = "onUpdateServer", at = @At("RETURN"))
    private void cmc$refreshMountedChunkTickets(CallbackInfoReturnable<Boolean> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }
        TileEntityTeleporter teleporter = (TileEntityTeleporter) (Object) this;
        MountedMekanismContextResolver.resolve(teleporter).ifPresent(context -> {
            ChunkPos current = new ChunkPos(context.globalBlockPos());
            if (!current.equals(cmc$lastGlobalChunk)) {
                cmc$lastGlobalChunk = current;
                teleporter.getChunkLoader().refreshChunkTickets();
            }
        });
    }

    @Unique
    private static long cmc$calculateProjectedEnergyCost(Entity entity, Level targetWorld, GlobalPos coords) {
        return MountedTeleporterTargets.calculateProjectedEnergyCost(entity, targetWorld, coords);
    }
}
