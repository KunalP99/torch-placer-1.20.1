package torchplacer;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;

import java.util.EnumMap;

public class ModItems {
    public static final EnumMap<WoodTorchVariant, Item> ITEMS = new EnumMap<>(WoodTorchVariant.class);
    public static Item TORCH_BAG;
    public static Item UNDERWATER_TORCH;

    public static void register() {
        TORCH_BAG = new TorchBagItem(new Item.Properties().stacksTo(1));
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(TorchPlacer.MOD_ID, "torch_bag"), TORCH_BAG);

        for (WoodTorchVariant v : WoodTorchVariant.values()) {
            Item item = new StandingAndWallBlockItem(
                    ModBlocks.FLOOR.get(v),
                    ModBlocks.WALL.get(v),
                    new Item.Properties(),
                    Direction.DOWN
            );
            Registry.register(BuiltInRegistries.ITEM,
                    new ResourceLocation(TorchPlacer.MOD_ID, v.id + "_torch"), item);
            ITEMS.put(v, item);
        }

        UNDERWATER_TORCH = new StandingAndWallBlockItem(
                ModBlocks.UNDERWATER_FLOOR, ModBlocks.UNDERWATER_WALL,
                new Item.Properties(), Direction.DOWN);
        Registry.register(BuiltInRegistries.ITEM,
                new ResourceLocation(TorchPlacer.MOD_ID, "underwater_torch"), UNDERWATER_TORCH);

        // Add torch bag + wood torches + underwater torch to the Tools & Utilities tab, after vanilla torch
        Item[] woodTorches = ITEMS.values().toArray(new Item[0]);
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(entries -> {
            entries.addAfter(Items.TORCH, woodTorches);
            entries.addAfter(Items.TORCH, TORCH_BAG);
            entries.addAfter(Items.TORCH, UNDERWATER_TORCH);
        });
    }
}
