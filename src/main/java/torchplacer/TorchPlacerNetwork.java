package torchplacer;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TorchPlacerNetwork {
    public static final ResourceLocation CONFIG_SYNC = new ResourceLocation(TorchPlacer.MOD_ID, "config_sync");

    /** Server-side per-player config store. */
    public static final Map<UUID, TorchPlacerConfig> PLAYER_CONFIGS = new HashMap<>();

    public static void registerServerReceiver() {
        ServerPlayNetworking.registerGlobalReceiver(CONFIG_SYNC, (server, player, handler, buf, responseSender) -> {
            boolean enabled = buf.readBoolean();
            int lightThreshold = buf.readByte() & 0xFF;
            int modeOrdinal = buf.readByte() & 0xFF;
            int scanRadius = buf.readByte() & 0xFF;

            PlacementMode[] modes = PlacementMode.values();
            PlacementMode mode = modeOrdinal < modes.length ? modes[modeOrdinal] : PlacementMode.BOTH;

            TorchPlacerConfig config = new TorchPlacerConfig();
            config.enabled = enabled;
            config.lightThreshold = Math.max(0, Math.min(14, lightThreshold));
            config.placementMode = mode;
            config.scanRadius = Math.max(3, Math.min(10, scanRadius));

            server.execute(() -> PLAYER_CONFIGS.put(player.getUUID(), config));
        });
    }

    public static FriendlyByteBuf buildPacket(TorchPlacerConfig config) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(config.enabled);
        buf.writeByte(config.lightThreshold);
        buf.writeByte(config.placementMode.ordinal());
        buf.writeByte(config.scanRadius);
        return buf;
    }
}
