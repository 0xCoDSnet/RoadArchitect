package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeSource;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.LongAdder;

/**
 * A* path‑finder working on a configurable X/Z grid.<br>
 * <ul>
 *   <li>Thread‑safe caches (FastUtil&nbsp;+ synchronize)</li>
 *   <li>Early <b>line‑of‑sight</b> shortcut: if текущая вершина видит цель без
 *       резких перепадов, поиск прерывается и путь достраивается прямой.</li>
 *   <li>Optional interpolation between grid vertices → вернётся каждый блок дороги.</li>
 * </ul>
 */
public class PathFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinder");


    /*================ USER‑TUNABLE PARAMS ================*/
    public static final int GRID_STEP = 4;
    private static final int[][] OFFSETS = generateOffsets();
    private static final Map<TagKey<Biome>, Double> BIOME_COSTS = Map.of(
            BiomeTags.IS_RIVER, 80.0,
            BiomeTags.IS_OCEAN, 999.0,
            BiomeTags.IS_DEEP_OCEAN, 999.0,
            BiomeTags.IS_MOUNTAIN, 50.0,
            BiomeTags.IS_BEACH, 50.0
    );
    /*=====================================================*/
    private final LongAdder heightCacheHits = new LongAdder();
    private final LongAdder heightCacheMisses = new LongAdder();
    private final LongAdder stabilityCacheHits = new LongAdder();
    private final LongAdder stabilityCacheMisses = new LongAdder();
    private final LongAdder biomeCacheHits = new LongAdder();
    private final LongAdder biomeCacheMisses = new LongAdder();
    /*=====================================================*/

    private final NodeStorage nodes;
    private final ServerWorld world;
    private final int maxSteps;

    /*── thread‑safe caches ──*/
    private final Long2IntMap heightCache = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private final Long2DoubleMap stabilityCache = Long2DoubleMaps.synchronize(new Long2DoubleOpenHashMap());
    private final Long2ObjectMap<RegistryEntry<Biome>> biomeCache = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    public PathFinder(NodeStorage nodes, ServerWorld world, int maxSteps) {
        this.nodes = nodes;
        this.world = world;
        this.maxSteps = maxSteps;
        heightCache.defaultReturnValue(Integer.MIN_VALUE);
        stabilityCache.defaultReturnValue(Double.MAX_VALUE);
    }

    /**
     * Returns the list of BlockPos actually traversed by A*.
     */
    public List<BlockPos> findPath(String fromId, String toId) {
        List<BlockPos> path = aStar(fromId, toId);
        logCacheStats();
        return path;
    }

    public void prefillHeightCacheAndBiomeCache(int minX, int minZ, int maxX, int maxZ) {
        int step = GRID_STEP;
        ExecutorService executor = Executors.newWorkStealingPool();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int x = minX; x <= maxX; x += step) {
            for (int z = minZ; z <= maxZ; z += step) {
                final int fx = x;
                final int fz = z;
                long key = hash(fx, fz);
                futures.add(CompletableFuture.runAsync(() -> {
                    int fy = world.getChunkManager()
                            .getChunkGenerator()
                            .getHeight(
                                    fx, fz,
                                    Heightmap.Type.WORLD_SURFACE_WG,
                                    world,
                                    world.getChunkManager().getNoiseConfig()
                            );
                    heightCache.put(key, fy);
                }, executor));
                futures.add(CompletableFuture.runAsync(() -> {
                    RegistryEntry<Biome> biome = world.getChunkManager()
                            .getChunkGenerator()
                            .getBiomeSource()
                            .getBiome(fx, 0, fz,
                                    world.getChunkManager().getNoiseConfig().getMultiNoiseSampler()
                            );
                    biomeCache.put(key, biome);
                }, executor));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        LOGGER.info("Height, Biome caches prefilling complete: [{}..{}] x [{}..{}]", minX, maxX, minZ, maxZ);
    }

    // ────────────────────────────────────────────────────────────────────────
    // A* core with LOS‑shortcut

    private List<BlockPos> aStar(String fromId, String toId) {
        Node startNode = nodes.all().get(fromId);
        Node endNode = nodes.all().get(toId);
        if (startNode == null || endNode == null) {
            LOGGER.warn("Missing node(s) {} or {}", fromId, toId);
            return List.of();
        }

        record Rec(long key, double g, double f) { }

        BlockPos startPos = snap(startNode.pos());
        BlockPos endPos = snap(endNode.pos());
        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());

        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        gScore.put(startKey, 0.0);
        open.add(new Rec(startKey, 0.0, heuristic(startPos, endPos)));

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            Rec current = open.poll();
            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);

            // ───────────────────────────────────────
            if (current.key == endKey) {
                List<BlockPos> path = reconstructPath(current.key, startKey, parent);
                LOGGER.info("Path found between {} and {} after {} iterations", fromId, toId, iterations);
                return path;
            }

            // ── expand neighbours ──────────────────────────────────────
            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);

                int ny = sampleHeight(nx, nz);
                if (isTraversable(curY, ny)) continue;

                double stab = sampleStability(nx, nz, ny);
                if (stab == Double.MAX_VALUE) continue;

                double bCost = biomeCost(sampleBiome(nx, ny, nz));
                if (bCost >= 999.0) continue;

                double tentativeG = gScore.get(current.key)
                        + stepCost(off)
                        + elevationCost(curY, ny)
                        + bCost
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
        LOGGER.warn("Path not found between {} and {} after {} iterations", fromId, toId, iterations);
        return List.of();
    }


    // ────────────────────────────────────────────────────────────────────────
    // Caching helpers

    private int sampleHeight(int x, int z) {
        long key = hash(x, z);
        int cached = heightCache.get(key);
        if (cached != heightCache.defaultReturnValue()) {
            heightCacheHits.increment();
            return cached;
        }
        heightCacheMisses.increment();
        int h = world.getChunkManager()
                .getChunkGenerator()
                .getHeight(
                        x, z,
                        Heightmap.Type.WORLD_SURFACE,
                        world,
                        world.getChunkManager().getNoiseConfig()
                );
        heightCache.put(key, h);
        return h;
    }

    private double sampleStability(int x, int z, int y) {
        long k = hash(x, z);
        double c = stabilityCache.get(k);
        if (c != Double.MAX_VALUE) {
            stabilityCacheHits.increment();
            return c;
        }
        stabilityCacheMisses.increment();
        c = terrainStabilityCost(x, z, y);
        stabilityCache.put(k, c);
        return c;
    }

    private RegistryEntry<Biome> sampleBiome(int x, int y, int z) {
        long k = hash(x, z);
        RegistryEntry<Biome> biome = biomeCache.get(k);
        if (biome == null) {
            BiomeSource source = world.getChunkManager().getChunkGenerator().getBiomeSource();
            biome = source.getBiome(x, y, z, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
            biomeCache.put(k, biome);
        }
        return biome;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Cost functions and filters

    private static double stepCost(int[] off) {
        return (Math.abs(off[0]) == GRID_STEP && Math.abs(off[1]) == GRID_STEP) ? 1.5 : 1.0;
    }

    private static double elevationCost(int y1, int y2) {
        return Math.abs(y1 - y2) * 40.0;
    }

    private static double biomeCost(RegistryEntry<Biome> biome) {
        for (Map.Entry<TagKey<Biome>, Double> entry : BIOME_COSTS.entrySet()) {
            if (biome.isIn(entry.getKey())) {
                return entry.getValue();
            }
        }
        return 0.0;
    }

    private static double yLevelCost(int y) {
        return y <= 63 ? 160 : 0.0;
    }

    private double terrainStabilityCost(int x, int z, int y) {
        int c = 0;
        for (Direction d : Direction.Type.HORIZONTAL) {
            int ny = sampleHeight(x + d.getOffsetX(), z + d.getOffsetZ());
            c += Math.abs(y - ny);
            if (c > 2) return Double.MAX_VALUE;
        }
        return c * 16.0;
    }

    private static boolean isTraversable(int y1, int y2) {
        return Math.abs(y1 - y2) > 3;
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

    // ────────────────────────────────────────────────────────────────────────
    // Path reconstruction with interpolation

    private List<BlockPos> reconstructPath(long goal, long start, Long2LongMap parent) {
        List<BlockPos> v = new ArrayList<>();
        for (long k = goal; ; k = parent.get(k)) {
            BlockPos p = keyToPos(k);
            int y = sampleHeight(p.getX(), p.getZ());
            v.add(new BlockPos(p.getX(), y, p.getZ()));
            if (k == start) break;
        }
        Collections.reverse(v);
        List<BlockPos> out = new ArrayList<>(v.size() * GRID_STEP);
        for (int i = 0; i < v.size() - 1; i++) {
            BlockPos a = v.get(i), b = v.get(i + 1);
            out.add(a);
            interpolateSegment(a, b, out);
        }
        out.add(v.getLast());
        return out;
    }

    /**
     * Adds intermediate 1‑block steps between two points (inclusive start, exclusive end).
     */
    private void interpolateSegment(BlockPos a, BlockPos b, List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        for (int i = 1; i < steps; i++) {
            int nx = a.getX() + dx * i;
            int nz = a.getZ() + dz * i;
            int ny = sampleHeight(nx, nz);
            out.add(new BlockPos(nx, ny, nz));
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

    private static BlockPos keyToPos(long k) {
        return new BlockPos((int) (k >> 32), 0, (int) k);
    }

    private static int[][] generateOffsets() {
        int d = GRID_STEP;
        return new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
    }

    // ----------------------------------------------------------------------
    private void logCacheStats() {
        LOGGER.info(
                "Cache stats — height: hits={}, misses={}; stability: hits={}, misses={}; biome: hits={}, misses={}",
                heightCacheHits.sum(), heightCacheMisses.sum(),
                stabilityCacheHits.sum(), stabilityCacheMisses.sum(),
                biomeCacheHits.sum(), biomeCacheMisses.sum()
        );
    }
}
