package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;

/**
 * A* path‑finder that builds a route on the world grid (4×4 blocks) directly<br>
 * between two stored nodes. The algorithm reproduces the cost model from the
 * Settlement Roads mod but never explores other nodes from {@link NodeStorage}.
 */
public class PathFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger("roadarchitect/PathFinder");

    /** Grid step in blocks (must divide the neighbour offsets). */
    private static final int GRID_STEP = 4;

    private final NodeStorage nodes;
    private final SurfaceProvider surface;
    private final ServerWorld world;
    private final int maxSteps;

    // ────────────────────────────────────────────────────────────────────────

    public PathFinder(NodeStorage nodes, SurfaceProvider surface, ServerWorld world, int maxSteps) {
        this.nodes = nodes;
        this.surface = surface;
        this.world = world;
        this.maxSteps = maxSteps;
    }

    /** Public API: returns a list of BlockPos actually traversed by A*. */
    public List<BlockPos> findPath(String fromId, String toId) {
        return aStar(fromId, toId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // A* core working on world grid cells, not on graph edges.

    private List<BlockPos> aStar(String fromId, String toId) {
        Node startNode = nodes.all().get(fromId);
        Node endNode = nodes.all().get(toId);
        if (startNode == null || endNode == null) {
            LOGGER.warn("Missing node(s) {} or {}", fromId, toId);
            return List.of();
        }

        BlockPos startPos = snap(startNode.pos());
        BlockPos endPos = snap(endNode.pos());
        int startY = surface.getSurfaceY(startPos.getX(), startPos.getZ());
        int endY = surface.getSurfaceY(endPos.getX(), endPos.getZ());
        startPos = new BlockPos(startPos.getX(), startY, startPos.getZ());
        endPos = new BlockPos(endPos.getX(), endY, endPos.getZ());

        record Rec(long key, double g, double f) {
        }
        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());

        gScore.put(startKey, 0.0);
        open.add(new Rec(startKey, 0.0, heuristic(startPos, endPos)));

        int[][] OFFSETS = {
                {GRID_STEP, 0}, {-GRID_STEP, 0}, {0, GRID_STEP}, {0, -GRID_STEP},
                {GRID_STEP, GRID_STEP}, {GRID_STEP, -GRID_STEP}, {-GRID_STEP, GRID_STEP}, {-GRID_STEP, -GRID_STEP}
        };

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            Rec current = open.poll();
            if (current.key == endKey || reachedTarget(keyToPos(current.key), endPos)) {
                return reconstructPath(current.key, startKey, parent);
            }

            BlockPos curPos = keyToPos(current.key);
            int curY = surface.getSurfaceY(curPos.getX(), curPos.getZ());

            for (int[] off : OFFSETS) {
                int nx = curPos.getX() + off[0];
                int nz = curPos.getZ() + off[1];
                long neighKey = hash(nx, nz);

                int neighY = surface.getSurfaceY(nx, nz);
                BlockPos neighPos = new BlockPos(nx, neighY, nz);

                if (!isTraversable(curY, neighY)) {
                    continue; // слишком резкий подъём / спуск
                }

                double tentativeG = gScore.get(current.key)
                        + stepCost(off)
                        + elevationCost(curY, neighY)
                        + biomeCost(neighPos)
                        + yLevelCost(neighY)
                        + terrainStabilityCost(neighPos, neighY);

                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    double f = tentativeG + heuristic(neighPos, endPos);
                    open.add(new Rec(neighKey, tentativeG, f));
                }
            }
            LOGGER.info(String.valueOf(iterations));
        }
        LOGGER.debug("Path not found between {} and {} after {} iterations", fromId, toId, iterations);
        return List.of();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Cost helpers — replicate Settlement Roads weighting.

    private static double stepCost(int[] off) {
        return (Math.abs(off[0]) == GRID_STEP && Math.abs(off[1]) == GRID_STEP) ? 1.5 : 1.0;
    }

    private static double elevationCost(int y1, int y2) {
        return Math.abs(y1 - y2) * 40.0;
    }

    private double biomeCost(BlockPos pos) {
        RegistryEntry<Biome> biome = world.getBiome(pos);
        boolean water = biome.isIn(BiomeTags.IS_RIVER) || biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN);
        return water ? 50.0 * 8.0 : 0.0;
    }

    private static double yLevelCost(int y) {
        return y == 62 ? 20.0 * 8.0 : 0.0;
    }

    private double terrainStabilityCost(BlockPos pos, int y) {
        int cost = 0;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            int ny = surface.getSurfaceY(pos.getX() + dir.getOffsetX(), pos.getZ() + dir.getOffsetZ());
            cost += Math.abs(y - ny);
            if (cost > 2) {
                return Double.MAX_VALUE; // клетка слишком неустойчива
            }
        }
        return cost * 16.0;
    }

    private static boolean isTraversable(int y1, int y2) {
        return Math.abs(y1 - y2) <= 3;
    }

    private static boolean reachedTarget(BlockPos a, BlockPos b) {
        int manhattan = Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
        return manhattan < GRID_STEP * 2; // условие как в оригинале
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dz = Math.abs(a.getZ() - b.getZ());
        double approx = dx + dz - 0.6 * Math.min(dx, dz);
        return approx * 30.0;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Path reconstruction

    private List<BlockPos> reconstructPath(long goalKey, long startKey, Long2LongMap parent) {
        List<BlockPos> cells = new ArrayList<>();
        long k = goalKey;
        while (k != startKey) {
            cells.add(keyToPos(k));
            k = parent.get(k);
        }
        cells.add(keyToPos(startKey));
        cells.sort(Comparator.comparingInt(BlockPos::getY)); // optional ordering by insertion
        java.util.Collections.reverse(cells); // from start to goal
        return cells;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utility: grid snapping & key encoding

    private static BlockPos snap(BlockPos p) {
        int x = Math.floorDiv(p.getX(), GRID_STEP) * GRID_STEP;
        int z = Math.floorDiv(p.getZ(), GRID_STEP) * GRID_STEP;
        return new BlockPos(x, p.getY(), z);
    }

    private static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static BlockPos keyToPos(long key) {
        int x = (int) (key >> 32);
        int z = (int) key;
        return new BlockPos(x, 0, z);
    }

    // ────────────────────────────────────────────────────────────────────────
    /** Provides surface Y for any XZ coordinate without blocking I/O. */
    public interface SurfaceProvider {
        int getSurfaceY(int x, int z);
    }

    /**
     * Реализация {@link SurfaceProvider}, использующая данные мира.
     * Значения высоты вычисляются через генератор чанков, поэтому
     * доступны даже для ещё не сгенерированных участков.
     * <p>Surface provider that delegates to a {@link ServerWorld}. Heights are
     * computed via the world's {@link net.minecraft.world.gen.chunk.ChunkGenerator}
     * and therefore available for ungenerated chunks.</p>
     */
    public static class WorldSurfaceProvider implements SurfaceProvider {
        private final ServerWorld world;

        public WorldSurfaceProvider(ServerWorld world) {
            this.world = world;
        }

        @Override
        public int getSurfaceY(int x, int z) {
            return world.getChunkManager().getChunkGenerator()
                    .getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world, world.getChunkManager().getNoiseConfig());
        }
    }
}

