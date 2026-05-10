package com.jarrettonesource.createmekanismcompat.mixin.client;

import dev.ryanhcode.sable.Sable;
import java.util.ArrayList;
import java.util.List;
import mekanism.client.gui.machine.GuiDigitalMiner;
import mekanism.common.MekanismLang;
import mekanism.common.tile.machine.TileEntityDigitalMiner;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GuiDigitalMiner.class, remap = false)
public abstract class GuiDigitalMinerMixin {
    @Inject(method = "lambda$addGuiElements$0", at = @At("RETURN"), cancellable = true)
    private void cmc$showMountedScanningText(CallbackInfoReturnable<List<Component>> callback) {
        TileEntityDigitalMiner tile = ((GuiDigitalMiner) (Object) this).getTileEntity();
        if (tile.getToMine() != 0 || !tile.isRunning() || tile.getLevel() == null || Sable.HELPER.getContaining(tile.getLevel(), tile.getBlockPos()) == null) {
            return;
        }
        List<Component> rows = new ArrayList<>(callback.getReturnValue());
        if (rows.size() < 3) {
            return;
        }
        rows.set(2, MekanismLang.MINER_TO_MINE.translate(scanningText()));
        callback.setReturnValue(rows);
    }

    private String scanningText() {
        int dots = (int) ((System.currentTimeMillis() / 500L) % 4L);
        return "Scanning" + ".".repeat(dots);
    }
}
