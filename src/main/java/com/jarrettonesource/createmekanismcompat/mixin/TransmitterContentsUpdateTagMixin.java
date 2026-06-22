package com.jarrettonesource.createmekanismcompat.mixin;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.FluidNetwork;
import mekanism.common.content.network.transmitter.Transmitter;
import mekanism.common.lib.transmitter.DynamicNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Transmitter.class, remap = false)
public abstract class TransmitterContentsUpdateTagMixin {
    @Shadow
    public abstract boolean hasTransmitterNetwork();

    @Shadow
    public abstract DynamicNetwork<?, ?, ?> getTransmitterNetwork();

    @Shadow
    protected abstract void handleContentsUpdateTag(
            DynamicNetwork<?, ?, ?> network,
            CompoundTag tag,
            HolderLookup.Provider provider
    );

    @Inject(method = "handleUpdateTag", at = @At("RETURN"))
    private void cmc$applyContentsToExistingClientNetwork(
            CompoundTag tag,
            HolderLookup.Provider provider,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (hasTransmitterNetwork() && cmc$hasContentUpdate(tag)) {
            DynamicNetwork<?, ?, ?> network = getTransmitterNetwork();
            handleContentsUpdateTag(network, tag, provider);
            cmc$primeClientVisualStorage(network, tag, provider);
        }
    }

    private static void cmc$primeClientVisualStorage(
            DynamicNetwork<?, ?, ?> network,
            CompoundTag tag,
            HolderLookup.Provider provider
    ) {
        if (network instanceof FluidNetwork fluidNetwork && fluidNetwork.fluidTank.isEmpty() && tag.contains("fluid", Tag.TAG_COMPOUND)) {
            FluidStack fluid = FluidStack.parseOptional(provider, tag.getCompound("fluid"));
            if (!fluid.isEmpty()) {
                fluidNetwork.fluidTank.setStack(fluid);
            }
        } else if (network instanceof ChemicalNetwork chemicalNetwork && chemicalNetwork.chemicalTank.isEmpty()
                && tag.contains("chemical", Tag.TAG_STRING)) {
            ChemicalStack chemical = new ChemicalStack(
                    Chemical.parseOptionalHolder(provider, tag.getString("chemical")),
                    1
            );
            if (!chemical.isEmpty() && !chemical.getChemicalHolder().is(MekanismAPI.EMPTY_CHEMICAL_KEY)) {
                chemicalNetwork.chemicalTank.setStack(chemical);
            }
        }
    }

    private static boolean cmc$hasContentUpdate(CompoundTag tag) {
        return tag.contains("fluid", Tag.TAG_COMPOUND)
                || tag.contains("chemical", Tag.TAG_STRING)
                || tag.contains("energy", Tag.TAG_LONG)
                || tag.contains("scale", Tag.TAG_FLOAT);
    }
}
