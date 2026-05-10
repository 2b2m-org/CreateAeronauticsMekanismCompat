package com.jarrettonesource.createmekanismcompat.mixin;

import mekanism.common.tile.TileEntityTeleporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = TileEntityTeleporter.class, remap = false)
public interface TileEntityTeleporterAccessor {
    @Accessor("teleportBounds")
    @Nullable
    AABB cmc$getTeleportBounds();

    @Invoker("canTeleportEntity")
    boolean cmc$canTeleportEntity(Entity entity, @Nullable Level destinationLevel);
}
