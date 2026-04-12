package torchplacer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TorchBagMenu extends AbstractContainerMenu {

    public static MenuType<TorchBagMenu> TYPE;

    private final SimpleContainer bagInventory;
    private final InteractionHand hand;

    // Server-side constructor — called from TorchBagItem.use()
    public TorchBagMenu(int syncId, Inventory playerInv, InteractionHand hand) {
        super(TYPE, syncId);
        this.hand = hand;
        ItemStack bagStack = playerInv.player.getItemInHand(hand);
        this.bagInventory = TorchBagItem.loadInventory(bagStack);

        // Write NBT back to the ItemStack whenever a slot changes
        bagInventory.addListener(inv ->
                TorchBagItem.saveInventory(playerInv.player.getItemInHand(hand), bagInventory));

        // 8 torch-only bag slots — 4 columns × 2 rows, centred horizontally
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                final int index = row * 4 + col;
                addSlot(new Slot(bagInventory, index, 52 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return TorchBagItem.isTorch(stack);
                    }
                });
            }
        }

        // Player main inventory (slots 9–35 in Inventory)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 51 + row * 18));
            }
        }

        // Hotbar (slots 0–8)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, 8 + col * 18, 109));
        }
    }

    // Client-side constructor — called via ScreenHandlerRegistry (reads hand from buf)
    public TorchBagMenu(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(syncId, playerInv, InteractionHand.values()[buf.readByte()]);
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof TorchBagItem;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Slots 0-7  = bag inventory
        // Slots 8-43 = player main inventory + hotbar
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < 8) {
            // Bag → player inventory (prefer main inv, then hotbar)
            if (!moveItemStackTo(stack, 8, 44, true)) return ItemStack.EMPTY;
        } else {
            // Player → bag (only torches)
            if (!TorchBagItem.isTorch(stack)) return ItemStack.EMPTY;
            if (!moveItemStackTo(stack, 0, 8, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return original;
    }
}
