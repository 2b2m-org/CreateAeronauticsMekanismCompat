package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSubLevelHelper;
import mekanism.client.render.transmitter.RenderTransmitterBase;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RenderTransmitterBase.class, remap = false)
public abstract class RenderTransmitterBaseMixin {
    @Shadow
    protected abstract boolean shouldRenderTransmitter(TileEntityTransmitter tile, Vec3 camera);

    @Inject(
            method = "shouldRender(Lmekanism/common/tile/transmitter/TileEntityTransmitter;Lnet/minecraft/world/phys/Vec3;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmc$shouldRenderMountedTransmitter(TileEntityTransmitter tile, Vec3 camera,
            CallbackInfoReturnable<Boolean> callback) {
        if (CmcClientSubLevelHelper.isMounted(tile) && shouldRenderTransmitter(tile, camera)) {
            callback.setReturnValue(true);
        }
    }
}
