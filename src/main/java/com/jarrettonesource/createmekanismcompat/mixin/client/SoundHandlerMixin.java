package com.jarrettonesource.createmekanismcompat.mixin.client;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import mekanism.client.sound.SoundHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SoundHandler.class, remap = false)
public abstract class SoundHandlerMixin {
    @Inject(method = "isClientPlayerInRange", at = @At("HEAD"), cancellable = true)
    private static void cmc$isMountedMachineSoundInRange(SoundInstance sound, CallbackInfoReturnable<Boolean> callback) {
        if (!CmcConfig.ENABLE_MOUNTED_MACHINE_SOUNDS.get() || sound.isRelative()
                || sound.getAttenuation() == SoundInstance.Attenuation.NONE) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            return;
        }

        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(sound.getX(), sound.getZ());
        if (subLevel == null || subLevel.isRemoved()) {
            return;
        }

        Sound resolved = sound.getSound();
        if (resolved == null) {
            sound.resolve(minecraft.getSoundManager());
            resolved = sound.getSound();
            if (resolved == null) {
                return;
            }
        }

        float attenuationRange = Math.max(sound.getVolume(), 1.0F) * resolved.getAttenuationDistance();
        double distanceSqr = Sable.HELPER.distanceSquaredWithSubLevels(
                level,
                player.position(),
                sound.getX(),
                sound.getY(),
                sound.getZ()
        );
        callback.setReturnValue(distanceSqr < attenuationRange * attenuationRange);
    }
}
