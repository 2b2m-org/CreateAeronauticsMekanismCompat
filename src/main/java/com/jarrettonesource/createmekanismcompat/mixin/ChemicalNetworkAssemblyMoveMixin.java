package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.assembly.MekanismAssemblyMoveTracker;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.transmitter.PressurizedTube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChemicalNetwork.class, remap = false)
public abstract class ChemicalNetworkAssemblyMoveMixin {
    @Inject(
            method = "disperse(Lmekanism/common/content/network/transmitter/PressurizedTube;Lmekanism/api/chemical/ChemicalStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmc$skipDeferredTransmitterDisperseForAssemblyMove(
            PressurizedTube tube,
            ChemicalStack stack,
            CallbackInfo callback
    ) {
        if (tube == null || stack == null || !stack.isRadioactive()) {
            return;
        }
        if (MekanismAssemblyMoveTracker.isDeferredTransmitterSourceMove(tube.getLevel(), tube.getBlockPos())) {
            callback.cancel();
        }
    }
}
