package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.CreateMekanismCompat;
import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.ChunkTicketPolicy;
import com.jarrettonesource.createmekanismcompat.mounted.MountedAabb;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import com.jarrettonesource.createmekanismcompat.network.MekanismTeleportSableStatePayload;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import java.util.List;
import java.util.Set;
import mekanism.api.event.MekanismTeleportEvent;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
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
    private BlockPos cmc$getProjectedTargetPosForTeleport(TileEntityTeleporter targetTeleporter) {
        return cmc$getProjectedTeleporterTargetPos(targetTeleporter);
    }

    @Redirect(
            method = "teleport",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/TileEntityTeleporter;teleportEntityTo(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/level/Level;Lmekanism/common/tile/TileEntityTeleporter;Lmekanism/api/event/MekanismTeleportEvent$Teleporter;ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/entity/Entity;")
    )
    private Entity cmc$teleportEntityToSableAwareTarget(
            Entity entity,
            Level level,
            TileEntityTeleporter targetTeleporter,
            MekanismTeleportEvent.Teleporter event,
            boolean preserveMotion,
            DimensionTransition.PostDimensionTransition postTransition) {
        MountedMekanismContext mountedTarget = CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()
                ? MountedMekanismContextResolver.resolve(targetTeleporter).orElse(null)
                : null;
        if (mountedTarget != null) {
            cmc$refineMountedEventTarget(targetTeleporter, mountedTarget, event);
        }
        if (CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            cmc$sendClientSableTrackingBeforeTeleport(entity, mountedTarget, event);
        }
        Entity teleported = TileEntityTeleporter.teleportEntityTo(entity, level, targetTeleporter, event, preserveMotion, postTransition);
        if (teleported != null && CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            cmc$syncServerSableTrackingAfterTeleport(teleported, mountedTarget, event);
        }
        return teleported;
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
        if (CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get() && targetWorld instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(coords.pos());
            if (blockEntity instanceof TileEntityTeleporter targetTeleporter && MountedMekanismContextResolver.resolve(targetTeleporter).isPresent()) {
                return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, GlobalPos.of(coords.dimension(), cmc$getProjectedTeleporterTargetPos(targetTeleporter)));
            }
        }
        return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, coords);
    }

    @Unique
    private static BlockPos cmc$getProjectedTeleporterTargetPos(TileEntityTeleporter targetTeleporter) {
        BlockPos localTarget = targetTeleporter.getTeleporterTargetPos();
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return localTarget;
        }
        return MountedMekanismContextResolver.resolve(targetTeleporter)
                .map(context -> BlockPos.containing(cmc$getProjectedTeleporterTarget(targetTeleporter, context)))
                .orElse(localTarget);
    }

    @Unique
    private static void cmc$refineMountedEventTarget(TileEntityTeleporter targetTeleporter, MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
        Vec3 roundedProjectedTarget = cmc$bottomCenter(cmc$getProjectedTeleporterTargetPos(targetTeleporter));
        if (event.getTarget().distanceToSqr(roundedProjectedTarget) > 1.0E-6) {
            return;
        }
        Vec3 preciseProjectedTarget = cmc$getProjectedTeleporterTarget(targetTeleporter, mountedTarget);
        event.setTargetX(preciseProjectedTarget.x);
        event.setTargetY(preciseProjectedTarget.y);
        event.setTargetZ(preciseProjectedTarget.z);
    }

    @Unique
    private static Vec3 cmc$getProjectedTeleporterTarget(TileEntityTeleporter targetTeleporter, MountedMekanismContext mountedTarget) {
        BlockPos localTarget = targetTeleporter.getTeleporterTargetPos();
        return mountedTarget.subLevel().logicalPose().transformPosition(cmc$bottomCenter(localTarget));
    }

    @Unique
    private static Vec3 cmc$bottomCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
    }

    @Unique
    private static void cmc$sendClientSableTrackingBeforeTeleport(Entity entity, @Nullable MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (mountedTarget == null) {
            CreateMekanismCompat.LOGGER.debug("Mekanism teleporter clearing Sable tracking for {} before teleport to {}",
                    player.getGameProfile().getName(), event.getTarget());
            PacketDistributor.sendToPlayer(player, MekanismTeleportSableStatePayload.clear());
            return;
        }

        Vec3 localTarget = mountedTarget.subLevel().logicalPose().transformPositionInverse(event.getTarget());
        CreateMekanismCompat.LOGGER.debug(
                "Mekanism teleporter setting Sable tracking for {} before teleport to sublevel {} local {} global {}",
                player.getGameProfile().getName(), mountedTarget.subLevelId(), localTarget, event.getTarget());
        PacketDistributor.sendToPlayer(player, MekanismTeleportSableStatePayload.inside(
                mountedTarget.subLevelId(),
                localTarget.x,
                localTarget.y,
                localTarget.z));
    }

    @Unique
    private static void cmc$syncServerSableTrackingAfterTeleport(Entity entity, @Nullable MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
        if (mountedTarget != null) {
            Vec3 localTarget = mountedTarget.subLevel().logicalPose().transformPositionInverse(event.getTarget());
            if (entity instanceof EntityStickExtension stick) {
                stick.sable$setPlotPosition(localTarget);
            }
            if (entity instanceof EntityMovementExtension movement) {
                movement.sable$setTrackingSubLevel(mountedTarget.subLevel());
                movement.sable$setLastTrackingSubLevelID(mountedTarget.subLevelId());
            }
            CreateMekanismCompat.LOGGER.debug("Mekanism teleporter server Sable tracking set for {} to sublevel {} local {}",
                    cmc$entityName(entity), mountedTarget.subLevelId(), localTarget);
        } else {
            if (entity instanceof EntityStickExtension stick) {
                stick.sable$setPlotPosition(null);
            }
            if (entity instanceof EntityMovementExtension movement) {
                movement.sable$setTrackingSubLevel(null);
                movement.sable$setLastTrackingSubLevelID(null);
            }
            CreateMekanismCompat.LOGGER.debug("Mekanism teleporter server Sable tracking cleared for {} at {}",
                    cmc$entityName(entity), event.getTarget());
        }
        EntitySubLevelUtil.setOldPosNoMovement(entity);
    }

    @Unique
    private static String cmc$entityName(Entity entity) {
        return entity instanceof ServerPlayer player ? player.getGameProfile().getName() : entity.getStringUUID();
    }
}
