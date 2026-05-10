package com.jarrettonesource.createmekanismcompat.mounted;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MountedAabb {
    private MountedAabb() {
    }

    public static AABB toGlobal(MountedMekanismContext context, AABB localBounds) {
        Bounds bounds = new Bounds();
        includeCorner(bounds, context, localBounds.minX, localBounds.minY, localBounds.minZ);
        includeCorner(bounds, context, localBounds.minX, localBounds.minY, localBounds.maxZ);
        includeCorner(bounds, context, localBounds.minX, localBounds.maxY, localBounds.minZ);
        includeCorner(bounds, context, localBounds.minX, localBounds.maxY, localBounds.maxZ);
        includeCorner(bounds, context, localBounds.maxX, localBounds.minY, localBounds.minZ);
        includeCorner(bounds, context, localBounds.maxX, localBounds.minY, localBounds.maxZ);
        includeCorner(bounds, context, localBounds.maxX, localBounds.maxY, localBounds.minZ);
        includeCorner(bounds, context, localBounds.maxX, localBounds.maxY, localBounds.maxZ);
        return bounds.toAabb();
    }

    private static void includeCorner(Bounds bounds, MountedMekanismContext context, double x, double y, double z) {
        bounds.include(context.subLevel().logicalPose().transformPosition(new Vec3(x, y, z)));
    }

    private static final class Bounds {
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private void include(Vec3 point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        private AABB toAabb() {
            return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
    }
}
