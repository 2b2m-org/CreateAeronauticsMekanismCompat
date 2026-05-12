package com.jarrettonesource.createmekanismcompat.mounted;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.lib.frequency.FrequencyManager;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MountedTeleporterTargets {
    private MountedTeleporterTargets() {
    }

    public static GlobalPos resolveClosestForMountedSource(MountedMekanismContext context, TeleporterFrequency frequency) {
        if (frequency == null) {
            return null;
        }
        GlobalPos localSourcePos = GlobalPos.of(context.level().dimension(), context.localBlockPos());
        GlobalPos sourcePos = GlobalPos.of(context.level().dimension(), context.globalBlockPos());
        GlobalPos closest = getClosestMountedAware(context, frequency, sourcePos, localSourcePos);
        if (closest != null) {
            return closest;
        }

        FrequencyManager<TeleporterFrequency> manager = FrequencyType.TELEPORTER.getFrequencyManager(frequency);
        TeleporterFrequency managedFrequency = manager == null ? null : manager.getFrequency(frequency.getKey());
        if (managedFrequency == null || managedFrequency == frequency) {
            return null;
        }

        return getClosestMountedAware(context, managedFrequency, sourcePos, localSourcePos);
    }

    private static GlobalPos getClosestMountedAware(MountedMekanismContext context, TeleporterFrequency frequency, GlobalPos sourcePos, GlobalPos localSourcePos) {
        GlobalPos closest = null;
        double closestDistance = Double.MAX_VALUE;
        boolean closestSameDimension = false;
        for (GlobalPos coords : frequency.getActiveCoords()) {
            if (coords.equals(localSourcePos)) {
                continue;
            }

            GlobalPos comparisonCoords = getComparisonCoords(context, coords);
            boolean sameDimension = sourcePos.dimension() == comparisonCoords.dimension();
            double distance = distanceSqr(sourcePos.pos(), comparisonCoords.pos());
            if (closest == null || !closestSameDimension && sameDimension || closestSameDimension == sameDimension && distance < closestDistance) {
                closest = coords;
                closestDistance = distance;
                closestSameDimension = sameDimension;
            }
        }
        return closest;
    }

    private static GlobalPos getComparisonCoords(MountedMekanismContext sourceContext, GlobalPos coords) {
        ServerLevel level = sourceContext.level().getServer().getLevel(coords.dimension());
        if (level == null) {
            return coords;
        }
        BlockEntity blockEntity = level.getBlockEntity(coords.pos());
        if (blockEntity instanceof TileEntityTeleporter targetTeleporter) {
            return MountedMekanismContextResolver.resolve(targetTeleporter)
                    .map(targetContext -> GlobalPos.of(coords.dimension(), targetContext.globalBlockPos()))
                    .orElse(coords);
        }
        return coords;
    }

    private static double distanceSqr(BlockPos first, BlockPos second) {
        double x = (double) first.getX() - second.getX();
        double y = (double) first.getY() - second.getY();
        double z = (double) first.getZ() - second.getZ();
        return x * x + y * y + z * z;
    }

    public static BlockPos resolveProjectedTarget(TileEntityTeleporter teleporter) {
        return projectIfLocal(teleporter, teleporter.getTeleporterTargetPos());
    }

    public static BlockPos projectIfLocal(TileEntityTeleporter teleporter, BlockPos targetPos) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return targetPos;
        }
        return MountedMekanismContextResolver.resolve(teleporter)
                .map(context -> shouldProject(context, targetPos) ? BlockPos.containing(MountedMekanismContextResolver.localToGlobal(context, targetPos)) : targetPos)
                .orElse(targetPos);
    }

    public static long calculateProjectedEnergyCost(Entity entity, Level targetWorld, GlobalPos coords) {
        if (CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get() && targetWorld instanceof ServerLevel serverLevel) {
            BlockEntity blockEntity = serverLevel.getBlockEntity(coords.pos());
            if (blockEntity instanceof TileEntityTeleporter targetTeleporter && MountedMekanismContextResolver.resolve(targetTeleporter).isPresent()) {
                BlockPos targetPos = resolveProjectedTarget(targetTeleporter);
                return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, GlobalPos.of(coords.dimension(), targetPos));
            }
        }
        return TileEntityTeleporter.calculateEnergyCost(entity, targetWorld, coords);
    }

    private static boolean shouldProject(MountedMekanismContext context, BlockPos targetPos) {
        Vec3 targetCenter = Vec3.atCenterOf(targetPos);
        double localDistance = targetCenter.distanceToSqr(context.localCenter());
        double globalDistance = targetCenter.distanceToSqr(context.globalCenter());
        return localDistance < globalDistance;
    }
}
