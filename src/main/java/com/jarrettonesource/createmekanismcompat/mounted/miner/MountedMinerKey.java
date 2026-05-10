package com.jarrettonesource.createmekanismcompat.mounted.miner;

import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record MountedMinerKey(ResourceKey<Level> dimension, UUID subLevelId, BlockPos localBlockPos) {
    public static MountedMinerKey from(MountedMekanismContext context) {
        return new MountedMinerKey(context.level().dimension(), context.subLevelId(), context.localBlockPos());
    }
}
