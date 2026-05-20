package com.jarrettonesource.createmekanismcompat.network;

import com.jarrettonesource.createmekanismcompat.CreateMekanismCompat;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record MekanismTeleportSableStatePayload(
        boolean insideSubLevel,
        @Nullable UUID subLevelId,
        double localX,
        double localY,
        double localZ
) implements CustomPacketPayload {
    public static final Type<MekanismTeleportSableStatePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CreateMekanismCompat.MOD_ID, "mekanism_teleport_sable_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MekanismTeleportSableStatePayload> STREAM_CODEC =
            StreamCodec.ofMember(MekanismTeleportSableStatePayload::write, MekanismTeleportSableStatePayload::read);

    public static MekanismTeleportSableStatePayload clear() {
        return new MekanismTeleportSableStatePayload(false, null, 0.0, 0.0, 0.0);
    }

    public static MekanismTeleportSableStatePayload inside(UUID subLevelId, double localX, double localY, double localZ) {
        return new MekanismTeleportSableStatePayload(true, subLevelId, localX, localY, localZ);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeBoolean(insideSubLevel);
        if (insideSubLevel) {
            buffer.writeUUID(subLevelId);
            buffer.writeDouble(localX);
            buffer.writeDouble(localY);
            buffer.writeDouble(localZ);
        }
    }

    private static MekanismTeleportSableStatePayload read(RegistryFriendlyByteBuf buffer) {
        boolean insideSubLevel = buffer.readBoolean();
        if (!insideSubLevel) {
            return clear();
        }
        return inside(buffer.readUUID(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
    }

    @Override
    public Type<MekanismTeleportSableStatePayload> type() {
        return TYPE;
    }
}
