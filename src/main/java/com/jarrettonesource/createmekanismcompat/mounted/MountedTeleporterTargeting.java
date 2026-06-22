package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.CreateMekanismCompat;
import com.jarrettonesource.createmekanismcompat.network.MekanismTeleportSableStatePayload;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import mekanism.api.event.MekanismTeleportEvent;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

public final class MountedTeleporterTargeting {
    private MountedTeleporterTargeting() {
    }

    public static @Nullable MountedMekanismContext resolveMountedTarget(TileEntityTeleporter targetTeleporter) {
        return MountedMekanismContextResolver.resolve(targetTeleporter).orElse(null);
    }

    public static long calculateProjectedEnergyCost(Entity entity, Level targetWorld, GlobalPos coords) {
        if (targetWorld instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(coords.pos());
            if (blockEntity instanceof TileEntityTeleporter targetTeleporter && MountedMekanismContextResolver.resolve(targetTeleporter).isPresent()) {
                return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, GlobalPos.of(coords.dimension(), getProjectedTeleporterTargetPos(targetTeleporter)));
            }
        }
        return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, coords);
    }

    public static BlockPos getProjectedTeleporterTargetPos(TileEntityTeleporter targetTeleporter) {
        return MountedMekanismContextResolver.resolve(targetTeleporter)
                .map(context -> BlockPos.containing(getProjectedTeleporterTarget(targetTeleporter, context)))
                .orElseGet(targetTeleporter::getTeleporterTargetPos);
    }

    public static void refineMountedEventTarget(TileEntityTeleporter targetTeleporter, MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
        Vec3 preciseProjectedTarget = getProjectedTeleporterTarget(targetTeleporter, mountedTarget);
        event.setTargetX(preciseProjectedTarget.x);
        event.setTargetY(preciseProjectedTarget.y);
        event.setTargetZ(preciseProjectedTarget.z);
    }

    public static void sendClientSableTrackingBeforeTeleport(Entity entity, @Nullable MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
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

    public static void syncServerSableTrackingAfterTeleport(Entity entity, @Nullable MountedMekanismContext mountedTarget, MekanismTeleportEvent.Teleporter event) {
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
                    entityName(entity), mountedTarget.subLevelId(), localTarget);
        } else {
            if (entity instanceof EntityStickExtension stick) {
                stick.sable$setPlotPosition(null);
            }
            if (entity instanceof EntityMovementExtension movement) {
                movement.sable$setTrackingSubLevel(null);
                movement.sable$setLastTrackingSubLevelID(null);
            }
            CreateMekanismCompat.LOGGER.debug("Mekanism teleporter server Sable tracking cleared for {} at {}",
                    entityName(entity), event.getTarget());
        }
        EntitySubLevelUtil.setOldPosNoMovement(entity);
    }

    public static Vec3 getProjectedTeleporterTarget(TileEntityTeleporter targetTeleporter, MountedMekanismContext mountedTarget) {
        BlockPos localTarget = targetTeleporter.getTeleporterTargetPos();
        return mountedTarget.subLevel().logicalPose().transformPosition(bottomCenter(localTarget));
    }

    public static Vec3 bottomCenter(BlockPos pos) {
        return new Vec3(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
    }

    private static String entityName(Entity entity) {
        return entity instanceof ServerPlayer player ? player.getGameProfile().getName() : entity.getStringUUID();
    }
}
