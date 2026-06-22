package com.jarrettonesource.createmekanismcompat.mixin;

import com.jarrettonesource.createmekanismcompat.config.CmcConfig;
import com.jarrettonesource.createmekanismcompat.mounted.MountedMekanismContext;
import com.jarrettonesource.createmekanismcompat.mounted.MountedTeleporterTargeting;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.event.MekanismTeleportEvent;
import mekanism.common.content.teleporter.TeleporterFrequency;
import mekanism.common.item.ItemPortableTeleporter;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.PacketPortalFX;
import mekanism.common.network.to_server.PacketPortableTeleporterTeleport;
import mekanism.common.tile.TileEntityTeleporter;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PacketPortableTeleporterTeleport.class, remap = false)
public abstract class PacketPortableTeleporterTeleportMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private void cmc$handleMountedPortableTeleporter(IPayloadContext context, CallbackInfo callback) {
        if (!CmcConfig.ENABLE_MOUNTED_TELEPORTER_TARGETS.get()) {
            return;
        }

        PacketPortableTeleporterTeleport packet = (PacketPortableTeleporterTeleport) (Object) this;
        ServerPlayer player = (ServerPlayer) context.player();
        ItemStack stack = player.getItemInHand(packet.currentHand());
        if (stack.isEmpty() || !(stack.getItem() instanceof ItemPortableTeleporter)) {
            return;
        }
        TeleporterFrequency found = (TeleporterFrequency) FrequencyType.TELEPORTER.getFrequency(packet.identity(), player.getUUID());
        if (found == null) {
            callback.cancel();
            return;
        }

        GlobalPos coords = found.getClosestCoords(GlobalPos.of(player.level().dimension(), player.blockPosition()));
        if (coords == null) {
            callback.cancel();
            return;
        }

        MinecraftServer server = player.level().getServer();
        ServerLevel teleWorld = server == null ? null : server.getLevel(coords.dimension());
        TileEntityTeleporter teleporter = WorldUtils.getTileEntity(TileEntityTeleporter.class, teleWorld, coords.pos());
        if (teleporter == null) {
            callback.cancel();
            return;
        }

        MountedMekanismContext mountedTarget = MountedTeleporterTargeting.resolveMountedTarget(teleporter);
        if (mountedTarget == null) {
            return;
        }

        Runnable energyExtraction = null;
        long energyCost = 0L;
        if (!player.isCreative()) {
            energyCost = MountedTeleporterTargeting.calculateProjectedEnergyCost(player, teleWorld, coords);
            IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
            if (energyContainer == null
                    || energyContainer.extract(energyCost, Action.SIMULATE, AutomationType.MANUAL) < energyCost) {
                callback.cancel();
                return;
            }
            long extractedEnergyCost = energyCost;
            energyExtraction = () -> energyContainer.extract(extractedEnergyCost, Action.EXECUTE, AutomationType.MANUAL);
        }

        teleporter.didTeleport.add(player.getUUID());
        teleporter.teleDelay = 5;

        BlockPos teleporterTargetPos = teleporter.getTeleporterTargetPos();
        MekanismTeleportEvent.PortableTeleporter event = new MekanismTeleportEvent.PortableTeleporter(
                player,
                teleporterTargetPos,
                coords.dimension(),
                stack,
                energyCost
        );
        MountedTeleporterTargeting.refineMountedEventTarget(teleporter, mountedTarget, event);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            callback.cancel();
            return;
        }

        if (energyExtraction != null) {
            energyExtraction.run();
        }

        player.closeContainer();
        PacketUtils.sendToAllTracking(new PacketPortalFX(player.blockPosition()), player.level(), coords.pos());
        if (player.isPassenger()) {
            player.stopRiding();
        }

        double oldX = player.getX();
        double oldY = player.getY();
        double oldZ = player.getZ();
        Level oldWorld = player.level();

        MountedTeleporterTargeting.sendClientSableTrackingBeforeTeleport(player, mountedTarget, event);
        TileEntityTeleporter.teleportEntityTo(player, teleWorld, teleporter, event, false, DimensionTransition.DO_NOTHING);
        MountedTeleporterTargeting.syncServerSableTrackingAfterTeleport(player, mountedTarget, event);

        if (player.level() != oldWorld || player.distanceToSqr(oldX, oldY, oldZ) >= 25.0D) {
            oldWorld.playSound(null, oldX, oldY, oldZ, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
        }
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
        teleporter.sendTeleportParticles();
        callback.cancel();
    }
}
