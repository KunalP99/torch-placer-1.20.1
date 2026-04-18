package torchplacer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorchPlacer implements ModInitializer {
    public static final String MOD_ID = "torch-placer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TorchBagMenu.TYPE = new ExtendedScreenHandlerType<>(TorchBagMenu::new);
        Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MOD_ID, "torch_bag"), TorchBagMenu.TYPE);
        ModBlocks.register();
        ModItems.register();
        TorchPlacerNetwork.registerServerReceiver();
        ServerTickEvents.END_SERVER_TICK.register(TorchPlacerLogic::tick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("torchesplaced")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    long count = TorchStats.get(ctx.getSource().getServer()).getCount(player.getUUID());
                    String msg = String.format("You have placed %,d torch%s in this world.",
                            count, count == 1 ? "" : "es");
                    ctx.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
        );

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel world = (ServerLevel) player.level();
            BlockPos lightPos = TorchPlacerLogic.HELD_LIGHT_POSITIONS.remove(player.getUUID());
            if (lightPos != null) {
                TorchPlacerLogic.clearLightBlock(world, lightPos);
            }
            BlockPos deferredPos = TorchPlacerLogic.DEFERRED_CLEARS.remove(player.getUUID());
            if (deferredPos != null) {
                TorchPlacerLogic.clearLightBlock(world, deferredPos);
            }
        });

        LOGGER.info("Torch It Up initialized.");
    }
}
