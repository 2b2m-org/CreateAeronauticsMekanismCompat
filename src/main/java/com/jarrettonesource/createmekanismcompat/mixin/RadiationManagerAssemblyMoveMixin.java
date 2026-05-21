package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.assembly.MekanismAssemblyMoveTracker;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.lib.radiation.RadiationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = RadiationManager.class, remap = false)
public abstract class RadiationManagerAssemblyMoveMixin {
    @Inject(
            method = "dumpRadiation(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lmekanism/api/chemical/ChemicalStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void cmc$skipRadiationDumpForAssemblyMove(
            Level level,
            BlockPos pos,
            ChemicalStack stack,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (stack != null && stack.isRadioactive() && MekanismAssemblyMoveTracker.isSourceMove(level, pos)) {
            callback.setReturnValue(false);
        }
    }
}
