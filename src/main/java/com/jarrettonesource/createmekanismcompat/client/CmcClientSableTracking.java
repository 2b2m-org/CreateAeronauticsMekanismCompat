package com.jarrettonesource.createmekanismcompat.client;

import com.jarrettonesource.createmekanismcompat.CreateMekanismCompat;
import com.jarrettonesource.createmekanismcompat.network.MekanismTeleportSableStatePayload;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class CmcClientSableTracking {
    private CmcClientSableTracking() {
    }

    public static void applyTeleportState(MekanismTeleportSableStatePayload payload) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        if (payload.insideSubLevel()) {
            SubLevel subLevel = resolveSubLevel(player, payload);
            if (subLevel == null || subLevel.isRemoved()) {
                CreateMekanismCompat.LOGGER.warn("Mekanism teleporter target sublevel {} is not tracked on the client", payload.subLevelId());
                clear(player);
                return;
            }
            if (player instanceof EntityStickExtension stick) {
                stick.sable$setPlotPosition(new Vec3(payload.localX(), payload.localY(), payload.localZ()));
            }
            if (player instanceof EntityMovementExtension movement) {
                movement.sable$setTrackingSubLevel(subLevel);
                movement.sable$setLastTrackingSubLevelID(payload.subLevelId());
            }
        } else {
            clear(player);
        }

        EntitySubLevelUtil.setOldPosNoMovement(player);
    }

    private static SubLevel resolveSubLevel(LocalPlayer player, MekanismTeleportSableStatePayload payload) {
        SubLevelContainer container = SubLevelContainer.getContainer(player.level());
        return container == null ? null : container.getSubLevel(payload.subLevelId());
    }

    private static void clear(LocalPlayer player) {
        if (player instanceof EntityStickExtension stick) {
            stick.sable$setPlotPosition(null);
        }
        if (player instanceof EntityMovementExtension movement) {
            movement.sable$setTrackingSubLevel(null);
            movement.sable$setLastTrackingSubLevelID(null);
        }
    }
}
