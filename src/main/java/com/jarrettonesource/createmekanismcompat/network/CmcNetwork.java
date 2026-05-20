package com.jarrettonesource.createmekanismcompat.network;

import com.jarrettonesource.createmekanismcompat.client.CmcClientSableTracking;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class CmcNetwork {
    private CmcNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("2");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            registrar.playToClient(
                    MekanismTeleportSableStatePayload.TYPE,
                    MekanismTeleportSableStatePayload.STREAM_CODEC,
                    (payload, context) -> CmcClientSableTracking.applyTeleportState(payload));
        } else {
            registrar.playToClient(
                    MekanismTeleportSableStatePayload.TYPE,
                    MekanismTeleportSableStatePayload.STREAM_CODEC,
                    (payload, context) -> {
                    });
        }
    }
}
