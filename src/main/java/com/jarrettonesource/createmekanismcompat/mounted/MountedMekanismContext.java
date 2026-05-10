package com.jarrettonesource.createmekanismcompat.mounted;

import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public record MountedMekanismContext(
        ServerLevel level,
        SubLevel subLevel,
        UUID subLevelId,
        BlockPos localBlockPos,
        Vec3 localCenter,
        Vec3 globalCenter,
        BlockPos globalBlockPos
) {
}
