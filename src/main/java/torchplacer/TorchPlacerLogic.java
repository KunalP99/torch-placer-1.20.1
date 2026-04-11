package torchplacer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TorchPlacerLogic {

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 0) return;

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer player : players) {
            TorchPlacerConfig config = TorchPlacerNetwork.PLAYER_CONFIGS.get(player.getUUID());
            if (config == null || !config.enabled) continue;
            tryPlaceTorch(player, config);
        }
    }

    private record TorchEntry(Runnable consume, Block floor, Block wall) {}

    private static TorchEntry findTorchEntry(ServerPlayer player) {
        var inv = player.getInventory();

        // 1. Bag priority: find the first torch bag that has torches in it
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack bagStack = inv.getItem(i);
            if (!(bagStack.getItem() instanceof TorchBagItem)) continue;

            SimpleContainer bagInv = TorchBagItem.loadInventory(bagStack);
            for (int j = 0; j < bagInv.getContainerSize(); j++) {
                ItemStack torch = bagInv.getItem(j);
                if (torch.isEmpty()) continue;
                Block[] pair = getTorchBlocks(torch);
                if (pair == null) continue;

                final int bagPlayerSlot = i, bagTorchSlot = j;
                return new TorchEntry(() -> {
                    // Re-load fresh from NBT to avoid stale reference, shrink, then save back
                    ItemStack currentBag = player.getInventory().getItem(bagPlayerSlot);
                    SimpleContainer fresh = TorchBagItem.loadInventory(currentBag);
                    fresh.getItem(bagTorchSlot).shrink(1);
                    TorchBagItem.saveInventory(currentBag, fresh);
                }, pair[0], pair[1]);
            }
        }

        // 2. Fallback: player inventory
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TORCH)) {
                final int slot = i;
                return new TorchEntry(
                        () -> player.getInventory().getItem(slot).shrink(1),
                        Blocks.TORCH, Blocks.WALL_TORCH);
            }
            for (WoodTorchVariant v : WoodTorchVariant.values()) {
                if (stack.is(ModItems.ITEMS.get(v))) {
                    final int slot = i;
                    return new TorchEntry(
                            () -> player.getInventory().getItem(slot).shrink(1),
                            ModBlocks.FLOOR.get(v), ModBlocks.WALL.get(v));
                }
            }
        }
        return null;
    }

    private static Block[] getTorchBlocks(ItemStack stack) {
        if (stack.is(Items.TORCH)) return new Block[]{Blocks.TORCH, Blocks.WALL_TORCH};
        for (WoodTorchVariant v : WoodTorchVariant.values())
            if (stack.is(ModItems.ITEMS.get(v)))
                return new Block[]{ModBlocks.FLOOR.get(v), ModBlocks.WALL.get(v)};
        return null;
    }

    private static void tryPlaceTorch(ServerPlayer player, TorchPlacerConfig config) {
        TorchEntry entry = findTorchEntry(player);
        if (entry == null) return;

        ServerLevel world = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();
        Direction facing = player.getDirection();
        Direction sideA = facing.getClockWise();
        Direction sideB = facing.getCounterClockWise();
        int radius = config.scanRadius;

        // priority: 0 = side wall, 1 = front/back wall, 2 = floor
        record Candidate(BlockPos pos, boolean isWall, Direction wallFacing, int lightLevel, int priority) {}
        List<Candidate> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);

                    if (!world.getBlockState(pos).isAir()) continue;

                    int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
                    int skyLight = world.getBrightness(LightLayer.SKY, pos);
                    int light = Math.max(blockLight, skyLight - world.getSkyDarken());
                    if (light > config.lightThreshold) continue;

                    // Wall placement: horizontal neighbor with a sturdy face toward the torch
                    if (config.placementMode != PlacementMode.FLOOR_ONLY) {
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockPos neighbor = pos.relative(dir);
                            if (world.getBlockState(neighbor).isFaceSturdy(world, neighbor, dir.getOpposite())) {
                                // Torch faces away from the wall; wall is in direction dir from torch
                                boolean isSideWall = (dir == sideA || dir == sideB);
                                int priority = isSideWall ? 0 : 1;
                                candidates.add(new Candidate(pos, true, dir.getOpposite(), light, priority));
                                break;
                            }
                        }
                    }

                    // Floor placement: sturdy top face directly below
                    if (config.placementMode != PlacementMode.WALLS_ONLY) {
                        BlockPos below = pos.below();
                        if (world.getBlockState(below).isFaceSturdy(world, below, Direction.UP)) {
                            candidates.add(new Candidate(pos, false, null, light, 2));
                        }
                    }
                }
            }
        }

        // Sort by: side wall first, then front wall, then floor; ties by light level then distance
        Optional<Candidate> best = candidates.stream().min(
                Comparator.comparingInt(Candidate::priority)
                          .thenComparingInt(Candidate::lightLevel)
                          .thenComparingDouble(c -> c.pos().distSqr(playerPos))
        );

        best.ifPresent(c -> {
            if (c.isWall()) {
                world.setBlock(c.pos(),
                        entry.wall().defaultBlockState().setValue(WallTorchBlock.FACING, c.wallFacing()), 3);
            } else {
                world.setBlock(c.pos(), entry.floor().defaultBlockState(), 3);
            }
            entry.consume().run();
        });
    }

}
