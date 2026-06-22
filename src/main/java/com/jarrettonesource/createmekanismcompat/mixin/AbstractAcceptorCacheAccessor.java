package com.jarrettonesource.createmekanismcompat.mixin;

import java.util.Map;
import mekanism.common.lib.transmitter.acceptor.AbstractAcceptorCache;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbstractAcceptorCache.class, remap = false)
public interface AbstractAcceptorCacheAccessor {
    @Accessor("cachedAcceptors")
    Map<Direction, ?> cmc$getCachedAcceptors();

    @Accessor("cachedListeners")
    Map<Direction, ?> cmc$getCachedListeners();
}
