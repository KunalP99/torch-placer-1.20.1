package torchplacer.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String CATEGORY = "category.torch-placer";

    public static KeyMapping KEY_TOGGLE;
    public static KeyMapping KEY_CONFIG;

    public static void register() {
        KEY_TOGGLE = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.torch-placer.toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_L,
                CATEGORY
        ));
        KEY_CONFIG = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.torch-placer.config",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                CATEGORY
        ));
    }
}
