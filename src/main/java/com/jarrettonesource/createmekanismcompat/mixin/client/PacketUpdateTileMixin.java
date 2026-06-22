package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import mekanism.common.network.to_client.PacketUpdateTile;
import mekanism.common.tile.base.TileEntityUpdateable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketUpdateTile.class, remap = false)
public abstract class PacketUpdateTileMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void cmc$handleMountedTileUpdate(IPayloadContext context, CallbackInfo callback) {
        PacketUpdateTile packet = (PacketUpdateTile) (Object) this;
        Player player = context.player();
        if (!(player.level() instanceof ClientLevel level)) {
            return;
        }

        if (level.getBlockEntity(packet.pos()) instanceof TileEntityUpdateable) {
            return;
        }

        TileEntityUpdateable mountedTile = CmcClientSubLevelHelper.resolveMountedUpdateTile(level, packet.pos());
        if (mountedTile == null) {
            return;
        }

        mountedTile.handleUpdateTag(packet.updateTag(), level.registryAccess());
        callback.cancel();
    }
}
