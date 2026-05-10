package com.jarrettonesource.createmekanismcompat.mounted;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class MountedMekanismContextResolver {
    private MountedMekanismContextResolver() {
    }

    public static Optional<MountedMekanismContext> resolve(BlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return Optional.empty();
        }
        SubLevel subLevel = Sable.HELPER.getContaining(blockEntity);
        if (subLevel == null || subLevel.isRemoved()) {
            return Optional.empty();
        }
        BlockPos localBlockPos = blockEntity.getBlockPos();
        Vec3 localCenter = Vec3.atCenterOf(localBlockPos);
        Vec3 globalCenter = subLevel.logicalPose().transformPosition(localCenter);
        BlockPos globalBlockPos = BlockPos.containing(globalCenter);
        return Optional.of(new MountedMekanismContext(
                serverLevel,
                subLevel,
                subLevel.getUniqueId(),
                localBlockPos,
                localCenter,
                globalCenter,
                globalBlockPos
        ));
    }

    public static Vec3 localToGlobal(MountedMekanismContext context, BlockPos localPos) {
        return context.subLevel().logicalPose().transformPosition(Vec3.atCenterOf(localPos));
    }
}
