package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * A* path‑finder that works on a X/Z grid with step {@link #GRID_STEP}.<br>
 * Change {@code GRID_STEP} (1‒10) — offsets and thresholds rebuild at class‑load time.
 */
public class PathFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID +"/PathFinder");

    /**
     * Horizontal grid step in blocks (safe range 1‒10).
     */
    public static final int GRID_STEP = 3;      // ← change here, everything else auto‑adapts

    /**
     * All 8 neighbour offsets generated from {@link #GRID_STEP}.
     */
    private static final int[][] OFFSETS = generateOffsets();

    private final NodeStorage nodes;
    private final SurfaceProvider surface;
    private final ServerWorld world;
    private final int maxSteps;

    // Caches -------------------------------------------------------------
    private final Long2IntMap heightCache = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final Long2DoubleMap stabilityCache = Long2DoubleMaps.synchronize(new Long2DoubleOpenHashMap());
    private final Long2ObjectMap<RegistryEntry<Biome>> biomeCache = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public PathFinder(NodeStorage nodes, SurfaceProvider surface, ServerWorld world, int maxSteps) {
        this.nodes = nodes;
        this.surface = surface;
        this.world = world;
        this.maxSteps = maxSteps;
        heightCache.defaultReturnValue(Integer.MIN_VALUE);
        stabilityCache.defaultReturnValue(Double.MAX_VALUE);
    }

    /**
     * Returns the list of BlockPos actually traversed by A*.
     */
    public List<BlockPos> findPath(String fromId, String toId) {
        return aStar(fromId, toId);
    }

    // ────────────────────────────────────────────────────────────────────────
    // A* core working on world grid cells

    private List<BlockPos> aStar(String fromId, String toId) {
        Node startNode = nodes.all().get(fromId);
        Node endNode = nodes.all().get(toId);
        if (startNode == null || endNode == null) {
            LOGGER.warn("Missing node(s) {} or {}", fromId, toId);
            return List.of();
        }

        BlockPos startPos = snap(startNode.pos());
        BlockPos endPos = snap(endNode.pos());

        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());

        record Rec(long key, double g, double f) {
        }
        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        gScore.put(startKey, 0.0);
        open.add(new Rec(startKey, 0.0, heuristic(startPos, endPos)));

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            Rec current = open.poll();
            if (current.key == endKey || reachedTarget(keyToPos(current.key), endPos)) {
                return reconstructPath(current.key, startKey, parent);
            }

            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);

            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);

                int ny = sampleHeight(nx, nz);
                if (!isTraversable(curY, ny)) continue;

                double stab = sampleStability(nx, nz, ny);
                if (stab == Double.MAX_VALUE) continue;

                double tentativeG = gScore.get(current.key)
                        + stepCost(off)
                        + elevationCost(curY, ny)
                        + biomeCost(sampleBiome(neighKey))
                        + yLevelCost(ny)
                        + stab;

                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    double f = tentativeG + heuristic(nx, nz, endPos);
                    open.add(new Rec(neighKey, tentativeG, f));
                }
            }
        }
        LOGGER.debug("Path not found between {} and {} after {} iterations", fromId, toId, iterations);
        return List.of();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Caching helpers

    private int sampleHeight(int x, int z) {
        long key = hash(x, z);
        int h = heightCache.get(key);
        if (h == Integer.MIN_VALUE) {
            h = surface.getSurfaceY(x, z);
            heightCache.put(key, h);
        }
        return h;
    }

    private double sampleStability(int x, int z, int y) {
        long key = hash(x, z);
        double cached = stabilityCache.get(key);
        if (cached != Double.MAX_VALUE) return cached;
        double val = terrainStabilityCost(x, z, y);
        stabilityCache.put(key, val);
        return val;
    }

    private RegistryEntry<Biome> sampleBiome(long key) {
        RegistryEntry<Biome> biome = biomeCache.get(key);
        if (biome == null) {
            int x = (int) (key >> 32);
            int z = (int) key;
            biome = world.getBiome(new BlockPos(x, 0, z));
            biomeCache.put(key, biome);
        }
        return biome;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Cost functions

    private static double stepCost(int[] off) {
        return (Math.abs(off[0]) == GRID_STEP && Math.abs(off[1]) == GRID_STEP) ? 1.5 : 1.0;
    }

    private static double elevationCost(int y1, int y2) {
        return Math.abs(y1 - y2) * 40.0;
    }

    private static double biomeCost(RegistryEntry<Biome> biome) {
        boolean water = biome.isIn(BiomeTags.IS_RIVER) || biome.isIn(BiomeTags.IS_OCEAN) || biome.isIn(BiomeTags.IS_DEEP_OCEAN);
        return water ? 400.0 : 0.0; // 50 * 8
    }

    private static double yLevelCost(int y) {
        return y == 62 ? 160.0 : 0.0; // 20 * 8
    }

    private double terrainStabilityCost(int x, int z, int y) {
        int cost = 0;
        for (Direction dir : Direction.Type.HORIZONTAL) {
            int ny = sampleHeight(x + dir.getOffsetX(), z + dir.getOffsetZ());
            cost += Math.abs(y - ny);
            if (cost > 2) return Double.MAX_VALUE;
        }
        return cost * 16.0;
    }

    private static boolean isTraversable(int y1, int y2) {
        return Math.abs(y1 - y2) <= 3;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Heuristic & goal test

    private static double heuristic(int x, int z, BlockPos goal) {
        int dx = Math.abs(x - goal.getX());
        int dz = Math.abs(z - goal.getZ());
        double a = dx + dz - 0.6 * Math.min(dx, dz);
        return a * 40.0;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return heuristic(a.getX(), a.getZ(), b);
    }

    private static boolean reachedTarget(BlockPos a, BlockPos b) {
        int man = Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
        return man < GRID_STEP * 2;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Path reconstruction

    /**
     * Reconstructs the route and inserts interpolation points between each pair of consecutive
     * grid vertices so that the returned list contains *every* X/Z block the road will occupy.
     */
    private List<BlockPos> reconstructPath(long goal, long start, Long2LongMap parent) {
        List<BlockPos> vertices = new ArrayList<>();
        // ❶ collect reversed chain of grid cells
        for (long k = goal; ; k = parent.get(k)) {
            BlockPos p = keyToPos(k);
            int y = sampleHeight(p.getX(), p.getZ());
            vertices.add(new BlockPos(p.getX(), y, p.getZ()));
            if (k == start) break;
        }
        Collections.reverse(vertices);

        // ❷ interpolate between each pair (GRID_STEP‑1 points each)
        List<BlockPos> smooth = new ArrayList<>(vertices.size() * GRID_STEP);
        for (int i = 0; i < vertices.size() - 1; i++) {
            BlockPos a = vertices.get(i);
            BlockPos b = vertices.get(i + 1);
            smooth.add(a);
            interpolateSegment(a, b, smooth); // adds points *between* a and b
        }
        smooth.add(vertices.get(vertices.size() - 1)); // add goal
        return smooth;
    }

    /**
     * Adds intermediate blocks spaced at 1‑block steps between two GRID_STEP‑spaced points.
     */
    private void interpolateSegment(BlockPos a, BlockPos b, List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        int y = a.getY();
        for (int i = 1; i < steps; i++) {
            int x = a.getX() + dx * i;
            int z = a.getZ() + dz * i;
            out.add(new BlockPos(x, y, z));
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Utility: grid, key encoding, offset generation

    private static BlockPos snap(BlockPos p) {
        int x = Math.floorDiv(p.getX(), GRID_STEP) * GRID_STEP;
        int z = Math.floorDiv(p.getZ(), GRID_STEP) * GRID_STEP;
        return new BlockPos(x, p.getY(), z);
    }

    private static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    private static BlockPos keyToPos(long key) {
        int x = (int) (key >> 32);
        int z = (int) key;
        return new BlockPos(x, 0, z);    // Y добавляем позже через sampleHeight
    }

    private static int[][] generateOffsets() {
        int d = GRID_STEP;
        return new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
    }

    // ────────────────────────────────────────────────────────────────────────

    /**
     * Provides surface Y for any XZ coordinate without blocking I/O.
     */
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

