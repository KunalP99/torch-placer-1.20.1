package torchplacer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class TorchPlacerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("torch-placer.json");

    public boolean enabled = false;
    /** Place a torch when block light level is at or below this value (0–14). */
    public int lightThreshold = 7;
    public PlacementMode placementMode = PlacementMode.BOTH;
    /** How many blocks from the player to scan (3–10). */
    public int scanRadius = 5;

    public static TorchPlacerConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                TorchPlacerConfig cfg = GSON.fromJson(reader, TorchPlacerConfig.class);
                if (cfg != null) return cfg;
            } catch (IOException e) {
                TorchPlacer.LOGGER.error("Failed to load config, using defaults", e);
            }
        }
        return new TorchPlacerConfig();
    }

    public void save() {
        try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            TorchPlacer.LOGGER.error("Failed to save config", e);
        }
    }
}
