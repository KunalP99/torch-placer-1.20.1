package torchplacer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorchPlacer implements ModInitializer {
    public static final String MOD_ID = "torch-placer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TorchPlacerNetwork.registerServerReceiver();
        ServerTickEvents.END_SERVER_TICK.register(TorchPlacerLogic::tick);
        LOGGER.info("Torch Placer initialized.");
    }
}
