package com.jarrettonesource.createmekanismcompat;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.network.CmcNetwork;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(CreateMekanismCompat.MOD_ID)
public final class CreateMekanismCompat {
    public static final String MOD_ID = "create_mekanism_compat";
    public static final String DISPLAY_NAME = "Create Aeronautics: Mekanism Compatibility";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CreateMekanismCompat(IEventBus modBus, ModContainer modContainer) {
        modBus.addListener(this::commonSetup);
        modBus.addListener(CmcNetwork::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, CmcConfig.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("{} loaded", DISPLAY_NAME);
    }
}
