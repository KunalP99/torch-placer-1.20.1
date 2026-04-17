package torchplacer.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import torchplacer.TorchBagItem;
import torchplacer.TorchSource;

public class TorchHud {

    public static void render(GuiGraphics graphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.screen != null) return;
        if (!TorchPlacerClient.CONFIG.enabled) return;

        int count = countTorches(client, TorchPlacerClient.CONFIG.torchSource);
        int color = count > 15 ? 0xFFFFFF : count > 5 ? 0xFFFF55 : 0xFF5555;

        int x = client.getWindow().getGuiScaledWidth() - 26;
        int y = client.getWindow().getGuiScaledHeight() - 55;

        graphics.renderItem(new ItemStack(Items.TORCH), x, y);
        graphics.drawString(client.font, String.valueOf(count), x + 18, y + 5, color);
    }

    private static int countTorches(Minecraft client, TorchSource source) {
        int total = 0;
        var inv = client.player.getInventory();

        if (source != TorchSource.BAG_ONLY) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var stack = inv.getItem(i);
                if (stack.getItem() instanceof TorchBagItem) continue;
                if (TorchBagItem.isTorch(stack)) total += stack.getCount();
            }
        }

        if (source != TorchSource.INVENTORY_ONLY) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                var bagStack = inv.getItem(i);
                if (!(bagStack.getItem() instanceof TorchBagItem)) continue;
                var bagInv = TorchBagItem.loadInventory(bagStack);
                for (int j = 0; j < bagInv.getContainerSize(); j++) {
                    var torch = bagInv.getItem(j);
                    if (!torch.isEmpty() && TorchBagItem.isTorch(torch))
                        total += torch.getCount();
                }
            }
        }

        return total;
    }
}
