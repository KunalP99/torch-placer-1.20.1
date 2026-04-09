package torchplacer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.chat.Component;
import torchplacer.TorchPlacerConfig;
import torchplacer.TorchPlacerNetwork;

public class TorchPlacerClient implements ClientModInitializer {
    public static TorchPlacerConfig CONFIG;

    @Override
    public void onInitializeClient() {
        CONFIG = TorchPlacerConfig.load();

        KeyBindings.register();

        // Sync config to server when joining a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(TorchPlacerNetwork.CONFIG_SYNC, TorchPlacerNetwork.buildPacket(CONFIG))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            if (KeyBindings.KEY_TOGGLE.consumeClick()) {
                CONFIG.enabled = !CONFIG.enabled;
                CONFIG.save();
                String state = CONFIG.enabled ? "ON" : "OFF";
                client.player.displayClientMessage(Component.literal("Auto Torch: " + state), true);
                if (client.getConnection() != null) {
                    ClientPlayNetworking.send(TorchPlacerNetwork.CONFIG_SYNC, TorchPlacerNetwork.buildPacket(CONFIG));
                }
            }

            if (KeyBindings.KEY_CONFIG.consumeClick()) {
                client.setScreen(new TorchPlacerConfigScreen(client.screen));
            }
        });
    }
}
