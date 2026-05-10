package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.mounted.miner.MountedDigitalMinerControllers;
import java.util.Set;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = TileEntityDigitalMiner.class, remap = false)
public abstract class TileEntityDigitalMinerMixin {
    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    private void cmc$startMounted(CallbackInfo callback) {
        if (MountedDigitalMinerControllers.prepareMountedStart((TileEntityDigitalMiner) (Object) this)) {
            callback.cancel();
        }
    }

    @Inject(method = "reset", at = @At("HEAD"))
    private void cmc$resetMounted(CallbackInfo callback) {
        MountedDigitalMinerControllers.reset((TileEntityDigitalMiner) (Object) this);
    }

    @Inject(
            method = "onUpdateServer",
            at = @At(value = "INVOKE", target = "Lmekanism/common/inventory/slot/EnergyInventorySlot;fillContainerOrConvert()V", shift = At.Shift.AFTER)
    )
    private void cmc$scanMountedBeforeMiningGate(CallbackInfoReturnable<Boolean> callback) {
        MountedDigitalMinerControllers.scanMounted((TileEntityDigitalMiner) (Object) this);
    }

    @Redirect(method = "onUpdateServer", at = @At(value = "INVOKE", target = "Lmekanism/common/tile/machine/TileEntityDigitalMiner;tryMineBlock()V"))
    private void cmc$mineMounted(TileEntityDigitalMiner miner) {
        if (!MountedDigitalMinerControllers.mineMounted(miner)) {
            ((TileEntityDigitalMinerAccessor) miner).cmc$invokeTryMineBlock();
        }
    }

    @Inject(method = "getChunkSet", at = @At("HEAD"), cancellable = true)
    private void cmc$getMountedChunkSet(CallbackInfoReturnable<Set<ChunkPos>> callback) {
        MountedDigitalMinerControllers.mountedChunkSet((TileEntityDigitalMiner) (Object) this)
                .ifPresent(callback::setReturnValue);
    }
}
