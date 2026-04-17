package torchplacer.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import torchplacer.ModBlocks;
import torchplacer.TorchBagMenu;
import torchplacer.TorchPlacerConfig;
import torchplacer.TorchPlacerNetwork;
import torchplacer.WoodTorchVariant;

public class TorchPlacerClient implements ClientModInitializer {
    public static TorchPlacerConfig CONFIG;
    private static int particleTick = 0;

    @Override
    public void onInitializeClient() {
        CONFIG = TorchPlacerConfig.load();

        // Register cutout render layer so transparent pixels don't render as black
        for (WoodTorchVariant v : WoodTorchVariant.values()) {
            BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.FLOOR.get(v), RenderType.cutout());
            BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.WALL.get(v), RenderType.cutout());
        }
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNDERWATER_FLOOR, RenderType.cutout());
        BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.UNDERWATER_WALL,  RenderType.cutout());

        MenuScreens.register(TorchBagMenu.TYPE, TorchBagScreen::new);

        KeyBindings.register();
        HudRenderCallback.EVENT.register(TorchHud::render);

        // Sync config to server when joining a world
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
                ClientPlayNetworking.send(TorchPlacerNetwork.CONFIG_SYNC, TorchPlacerNetwork.buildPacket(CONFIG))
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            ItemStack mainHand = client.player.getMainHandItem();
            ItemStack offHand  = client.player.getOffhandItem();
            boolean mainSoul = mainHand.is(Items.SOUL_TORCH);
            boolean offSoul  = offHand.is(Items.SOUL_TORCH);
            if ((mainSoul || offSoul) && ++particleTick % 10 == 0) {
                var eye  = client.player.getEyePosition();
                var look = client.player.getLookAngle();
                var rand = client.player.getRandom();
                // Right-hand vector (rotate look 90° counter-clockwise around Y axis)
                double rx = -look.z;
                double rz =  look.x;
                // Main hand = right side (+), offhand = left side (-)
                double side = mainSoul ? 1.0 : -1.0;
                double tx = eye.x + look.x * 0.35 + rx * 0.35 * side;
                double ty = eye.y - 0.35;
                double tz = eye.z + look.z * 0.35 + rz * 0.35 * side;
                client.level.addParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        tx + (rand.nextDouble() - 0.5) * 0.35,
                        ty + (rand.nextDouble() - 0.5) * 0.35,
                        tz + (rand.nextDouble() - 0.5) * 0.35,
                        0.0, 0.04, 0.0);
            }

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
