package torchplacer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;
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

    private static void tryPlaceTorch(ServerPlayer player, TorchPlacerConfig config) {
        int torchSlot = findTorchSlot(player);
        if (torchSlot == -1) return;

        ServerLevel world = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();
        int radius = config.scanRadius;

        record Candidate(BlockPos pos, boolean isWall, Direction wallFacing, int lightLevel) {}
        List<Candidate> candidates = new ArrayList<>();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);

                    if (!world.getBlockState(pos).isAir()) continue;

                    int light = world.getBrightness(LightLayer.BLOCK, pos);
                    if (light > config.lightThreshold) continue;

                    // Wall placement: horizontal neighbor with a sturdy face toward the torch
                    if (config.placementMode != PlacementMode.FLOOR_ONLY) {
                        for (Direction dir : Direction.Plane.HORIZONTAL) {
                            BlockPos neighbor = pos.relative(dir);
                            if (world.getBlockState(neighbor).isFaceSturdy(world, neighbor, dir.getOpposite())) {
                                // Torch faces away from the wall
                                candidates.add(new Candidate(pos, true, dir.getOpposite(), light));
                                break;
                            }
                        }
                    }

                    // Floor placement: sturdy top face directly below
                    if (config.placementMode != PlacementMode.WALLS_ONLY) {
                        BlockPos below = pos.below();
                        if (world.getBlockState(below).isFaceSturdy(world, below, Direction.UP)) {
                            candidates.add(new Candidate(pos, false, null, light));
                        }
                    }
                }
            }
        }

        // Pick the darkest spot; ties broken by closest to player
        Optional<Candidate> best = candidates.stream().min(
                Comparator.comparingInt(Candidate::lightLevel)
                          .thenComparingDouble(c -> c.pos().distSqr(playerPos))
        );

        best.ifPresent(c -> {
            if (c.isWall()) {
                world.setBlock(c.pos(),
                        Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, c.wallFacing()), 3);
            } else {
                world.setBlock(c.pos(), Blocks.TORCH.defaultBlockState(), 3);
            }
            player.getInventory().getItem(torchSlot).shrink(1);
        });
    }

    private static int findTorchSlot(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.TORCH)) {
                return i;
            }
        }
        return -1;
    }
}
