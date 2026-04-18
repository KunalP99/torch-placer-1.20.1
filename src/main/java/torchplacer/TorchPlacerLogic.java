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
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TorchPlacerLogic {

    public static final Map<UUID, BlockPos> HELD_LIGHT_POSITIONS = new HashMap<>();
    public static final Map<UUID, BlockPos> DEFERRED_CLEARS = new HashMap<>();
    private static final Map<UUID, Integer> HELD_LIGHT_LEVELS = new HashMap<>();
    private static final int SCAN_RADIUS = 4;

    public static void tick(MinecraftServer server) {
        tickDynamicLighting(server);
        if (server.getTickCount() % 40 != 0) return;

        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer player : players) {
            TorchPlacerConfig config = TorchPlacerNetwork.PLAYER_CONFIGS.get(player.getUUID());
            if (config == null || !config.enabled) continue;
            tryPlaceTorch(player, config);
        }
    }

    private record TorchEntry(Runnable consume, Block floor, Block wall) {}

    private static TorchEntry findTorchEntry(ServerPlayer player, TorchSource source) {
        var inv = player.getInventory();

        // 1. Torch bag — skipped when INVENTORY_ONLY
        if (source != TorchSource.INVENTORY_ONLY) {
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
        }

        // 2. Player inventory — skipped when BAG_ONLY
        if (source != TorchSource.BAG_ONLY) {
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() instanceof TorchBagItem) continue; // don't consume the bag itself
                Block[] pair = getTorchBlocks(stack);
                if (pair == null) continue;
                final int slot = i;
                return new TorchEntry(() -> player.getInventory().getItem(slot).shrink(1), pair[0], pair[1]);
            }
        }

        return null;
    }

    private static Block[] getTorchBlocks(ItemStack stack) {
        if (stack.is(Items.TORCH)) return new Block[]{Blocks.TORCH, Blocks.WALL_TORCH};
        if (stack.is(ModItems.UNDERWATER_TORCH)) return new Block[]{ModBlocks.UNDERWATER_FLOOR, ModBlocks.UNDERWATER_WALL};
        for (WoodTorchVariant v : WoodTorchVariant.values())
            if (stack.is(ModItems.ITEMS.get(v)))
                return new Block[]{ModBlocks.FLOOR.get(v), ModBlocks.WALL.get(v)};
        return null;
    }

    private static void tryPlaceTorch(ServerPlayer player, TorchPlacerConfig config) {
        TorchEntry entry = findTorchEntry(player, config.torchSource);
        if (entry == null) return;

        boolean isUnderwater = entry.floor() instanceof UnderwaterTorchBlock;
        ServerLevel world = (ServerLevel) player.level();
        BlockPos playerPos = player.blockPosition();
        Direction facing = player.getDirection();
        Direction sideA = facing.getClockWise();
        Direction sideB = facing.getCounterClockWise();
        // priority: 0 = side wall, 1 = front/back wall, 2 = floor
        record Candidate(BlockPos pos, boolean isWall, Direction wallFacing, int lightLevel, int priority) {}
        List<Candidate> candidates = new ArrayList<>();

        for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    BlockPos pos = playerPos.offset(dx, dy, dz);

                    var posState = world.getBlockState(pos);
                    boolean posIsWater = posState.is(Blocks.WATER);
                    if (!posState.isAir() && !(isUnderwater && posIsWater)) continue;

                    int blockLight = world.getBrightness(LightLayer.BLOCK, pos);
                    int skyLight = world.getBrightness(LightLayer.SKY, pos);
                    int light = Math.max(blockLight, skyLight - world.getSkyDarken());
                    if (light > config.lightThreshold) continue;

                    if (!hasLineOfSight(world, player, pos)) continue;

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

        // Sort by: side wall first, then front wall, then floor; ties by distance then light level
        Optional<Candidate> best = candidates.stream().min(
                Comparator.comparingInt(Candidate::priority)
                          .thenComparingDouble(c -> c.pos().distSqr(playerPos))
                          .thenComparingInt(Candidate::lightLevel)
        );

        best.ifPresent(c -> {
            boolean inWater = isUnderwater && world.getBlockState(c.pos()).is(Blocks.WATER);
            if (c.isWall()) {
                BlockState state = entry.wall().defaultBlockState().setValue(WallTorchBlock.FACING, c.wallFacing());
                if (inWater) state = state.setValue(UnderwaterWallTorchBlock.WATERLOGGED, true);
                world.setBlock(c.pos(), state, 3);
            } else {
                BlockState state = entry.floor().defaultBlockState();
                if (inWater) state = state.setValue(UnderwaterTorchBlock.WATERLOGGED, true);
                world.setBlock(c.pos(), state, 3);
            }
            TorchStats.get(world.getServer()).increment(player.getUUID());
            entry.consume().run();
        });
    }

    private static boolean hasLineOfSight(ServerLevel world, ServerPlayer player, BlockPos target) {
        Vec3 eye = player.getEyePosition();
        Vec3 center = Vec3.atCenterOf(target);
        BlockHitResult hit = world.clip(new ClipContext(
                eye, center,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                player));
        return hit.getType() == HitResult.Type.MISS;
    }

    private static void tickDynamicLighting(MinecraftServer server) {
        // Snapshot deferred clears from the previous tick but do NOT flush them yet.
        // New light positions are placed first so the client always receives
        // [place new] before [clear old], preventing any dark gap.
        Map<UUID, BlockPos> toFlush = new HashMap<>(DEFERRED_CLEARS);
        DEFERRED_CLEARS.clear();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean holdingMain = TorchBagItem.isTorch(mainHand);
            boolean holdingOff  = TorchBagItem.isTorch(offHand);
            boolean holding = holdingMain || holdingOff;
            UUID uuid = player.getUUID();
            ServerLevel world = (ServerLevel) player.level();

            if (holding) {
                int lightLevel = TorchBagItem.getTorchLightLevel(holdingMain ? mainHand : offHand);
                BlockPos target = findLightPos(player, world);
                if (target != null) {
                    BlockPos current = HELD_LIGHT_POSITIONS.get(uuid);
                    int currentLevel = HELD_LIGHT_LEVELS.getOrDefault(uuid, -1);
                    boolean posChanged   = !target.equals(current);
                    boolean levelChanged = lightLevel != currentLevel;
                    if (posChanged || levelChanged) {
                        placeLightBlock(world, target, lightLevel);
                        if (posChanged && current != null) DEFERRED_CLEARS.put(uuid, current);
                        HELD_LIGHT_POSITIONS.put(uuid, target);
                        HELD_LIGHT_LEVELS.put(uuid, lightLevel);
                    }
                }

            } else {
                BlockPos current = HELD_LIGHT_POSITIONS.remove(uuid);
                if (current != null) clearLightBlock(world, current);
                BlockPos deferred = toFlush.remove(uuid);
                if (deferred != null) clearLightBlock(world, deferred);
                DEFERRED_CLEARS.remove(uuid);
                HELD_LIGHT_LEVELS.remove(uuid);
            }
        }

        // Clear old-old positions only after all new lights have been placed.
        toFlush.forEach((uuid, pos) -> {
            ServerPlayer p = server.getPlayerList().getPlayer(uuid);
            if (p != null) clearLightBlock((ServerLevel) p.level(), pos);
        });
    }

    private static boolean isValidLightPos(BlockState state) {
        if (state.isAir() || state.is(Blocks.LIGHT)) return true;
        return state.getFluidState().getType() == Fluids.WATER;
    }

    private static BlockPos findLightPos(ServerPlayer player, ServerLevel world) {
        BlockPos feet = player.blockPosition();
        if (isValidLightPos(world.getBlockState(feet))) return feet;
        BlockPos head = feet.above();
        if (isValidLightPos(world.getBlockState(head))) return head;
        return null;
    }

    public static void clearLightBlock(ServerLevel world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.is(Blocks.LIGHT)) return;
        BlockState replacement = state.getValue(BlockStateProperties.WATERLOGGED)
                ? Fluids.WATER.defaultFluidState().createLegacyBlock()
                : Blocks.AIR.defaultBlockState();
        world.setBlock(pos, replacement, 3);
    }

    private static void placeLightBlock(ServerLevel world, BlockPos pos, int level) {
        boolean inWater = world.getFluidState(pos).getType() == Fluids.WATER;
        world.setBlock(pos, Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, level)
                .setValue(BlockStateProperties.WATERLOGGED, inWater), 3);
    }

}
