package com.jarrettonesource.createmekanismcompat.mixin;

import mekanism.common.content.network.transmitter.BufferedTransmitter;
import mekanism.common.lib.transmitter.DynamicBufferedNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = DynamicBufferedNetwork.class, remap = false)
public interface DynamicBufferedNetworkAccessor {
    @Invoker("updateCapacity")
    void cmc$invokeUpdateCapacity(BufferedTransmitter<?, ?, ?, ?> transmitter);

    @Invoker("updateSaveShares")
    void cmc$invokeUpdateSaveShares(BufferedTransmitter<?, ?, ?, ?> triggerTransmitter);

    @Invoker("forceScaleUpdate")
    void cmc$invokeForceScaleUpdate();
}
