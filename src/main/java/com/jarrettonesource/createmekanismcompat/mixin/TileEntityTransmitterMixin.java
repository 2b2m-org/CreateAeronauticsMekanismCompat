package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContextResolver;
import java.util.ArrayList;
import java.util.List;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.MultipartUtils;
import mekanism.common.util.MultipartUtils.AdvancedRayTraceResult;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityTransmitter.class, remap = false)
public abstract class TileEntityTransmitterMixin {
    @Inject(method = "getSideLookingAt(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/core/Direction;", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedSideLookingAt(Player player, CallbackInfoReturnable<Direction> callback) {
        TileEntityTransmitter tile = (TileEntityTransmitter) (Object) this;
        MountedMekanismContext context = MountedMekanismContextResolver.resolve(tile).orElse(null);
        if (context == null) {
            return;
        }

        MultipartUtils.RayTraceVectors ray = MultipartUtils.getRayTraceVectors(player);
        Vec3 localStart = context.subLevel().logicalPose().transformPositionInverse(ray.start());
        Vec3 localEnd = context.subLevel().logicalPose().transformPositionInverse(ray.end());
        AdvancedRayTraceResult result = MultipartUtils.collisionRayTrace(tile.getBlockPos(), localStart, localEnd, tile.getCollisionBoxes());
        if (result == null || !result.valid()) {
            return;
        }

        List<Direction> connectedSides = new ArrayList<>(EnumUtils.DIRECTIONS.length);
        byte connections = tile.getTransmitter().getAllCurrentConnections();
        for (Direction direction : EnumUtils.DIRECTIONS) {
            if (Transmitter.connectionMapContainsSide(connections, direction)) {
                connectedSides.add(direction);
            }
        }

        int boxIndex = result.subHit + 1;
        if (boxIndex < connectedSides.size()) {
            callback.setReturnValue(connectedSides.get(boxIndex));
        }
    }
}
