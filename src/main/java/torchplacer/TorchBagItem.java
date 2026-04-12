package torchplacer;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class TorchBagItem extends Item {

    public TorchBagItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            ((ServerPlayer) player).openMenu(new ExtendedScreenHandlerFactory() {
                @Override
                public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                    return new TorchBagMenu(id, inv, hand);
                }

                @Override
                public Component getDisplayName() {
                    return Component.translatable("item.torch-placer.torch_bag");
                }

                @Override
                public void writeScreenOpeningData(ServerPlayer p, FriendlyByteBuf buf) {
                    buf.writeByte(hand.ordinal());
                }
            });
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    public static boolean isTorch(ItemStack stack) {
        if (stack.is(Items.TORCH)) return true;
        for (WoodTorchVariant v : WoodTorchVariant.values()) {
            if (stack.is(ModItems.ITEMS.get(v))) return true;
        }
        return false;
    }

    public static SimpleContainer loadInventory(ItemStack bagStack) {
        SimpleContainer container = new SimpleContainer(16);
        CompoundTag tag = bagStack.getTag();
        if (tag != null && tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            ListTag list = tag.getCompound("Inventory").getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag itemTag = list.getCompound(i);
                int slot = itemTag.getByte("Slot") & 0xFF;
                if (slot < 16) {
                    container.setItem(slot, ItemStack.of(itemTag));
                }
            }
        }
        return container;
    }

    public static void saveInventory(ItemStack bagStack, SimpleContainer container) {
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("Slot", (byte) i);
            stack.save(itemTag);
            list.add(itemTag);
        }
        CompoundTag invTag = new CompoundTag();
        invTag.put("Items", list);
        bagStack.getOrCreateTag().put("Inventory", invTag);
    }
}
