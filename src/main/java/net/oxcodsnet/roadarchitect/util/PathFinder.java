package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.world.ChunkTicketType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    public static final int GRID_STEP = 3; // horizontal granularity
    private static final int[][] OFFSETS = generateOffsets();
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
        LOGGER.info("Finding path from {} to {}", fromId, toId);
        return aStar(fromId, toId);
    }



    public void prefillHeightCache(int minX, int minZ, int maxX, int maxZ) {
        int step = GRID_STEP;
        // пул, автоматически равный числу доступных ядер
        ExecutorService executor = Executors.newWorkStealingPool();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int x = minX; x <= maxX; x += step) {
            for (int z = minZ; z <= maxZ; z += step) {
                final int fx = x;
                final int fz = z;
                futures.add(CompletableFuture.runAsync(() -> {
                    int fy = world.getChunkManager()
                            .getChunkGenerator()
                            .getHeight(
                                    fx, fz,
                                    Heightmap.Type.WORLD_SURFACE,
                                    world,
                                    world.getChunkManager().getNoiseConfig()
                            );
                    long key = hash(fx, fz);
                    heightCache.put(key, fy);
                }, executor));
            }
        }

        // ждём, пока все задачи закончатся
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
        LOGGER.info("Height cache prefilling complete: [{}..{}] x [{}..{}]",
                minX, maxX, minZ, maxZ);
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

        BlockPos startPos = snap(startNode.pos());
        BlockPos endPos = snap(endNode.pos());

        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());

        record Rec(long key, double g, double f) { }
        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        gScore.put(startKey, 0.0);
        open.add(new Rec(startKey, 0.0, heuristic(startPos, endPos)));

//        LOGGER.info("-! while !-");
        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            Rec current = open.poll();
            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);

//            // ── LOS shortcut ───────────────────────────────────────────
//            if (hasLineOfSight(curX, curZ, curY, endPos)) {
//                return finishWithLos(current.key, startKey, parent, endPos);
//            }
            // ── reached by radius? ─────────────────────────────────────
            if (current.key == endKey || reachedTarget(new BlockPos(curX, 0, curZ), endPos)) {
                return reconstructPath(current.key, startKey, parent);
            }
//            LOGGER.info("-! for !-");
            // ── expand neighbours ──────────────────────────────────────
            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);

                int ny = sampleHeight(nx, nz);
                if (!isTraversable(curY, ny)) continue;

//                double stab = sampleStability(nx, nz, ny);
//                if (stab == Double.MAX_VALUE) continue;

                double tentativeG = gScore.get(current.key)
                        + stepCost(off)
                        + elevationCost(curY, ny)
                        + biomeCost(sampleBiome(neighKey))
                        + yLevelCost(ny);
//                        + stab;

                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    double f = tentativeG + heuristic(nx, nz, endPos);
                    open.add(new Rec(neighKey, tentativeG, f));
                }
//                LOGGER.info("iterations: {}", iterations);
            }
        }
        LOGGER.debug("Path not found between {} and {} after {} iterations", fromId, toId, iterations);
        return List.of();
    }


    // ────────────────────────────────────────────────────────────────────────
    // Line‑of‑sight helper

    /**
     * Returns true if every intermediate cell from (x,z) to goal meets the same traversability
     * criteria (|ΔY| ≤ 3 and stability ≤ 2). Step is 1 block along Bresenham line.
     */
    private boolean hasLineOfSight(int x, int z, int y, BlockPos goal) {
        int gx = goal.getX();
        int gz = goal.getZ();
        int dx = Integer.signum(gx - x);
        int dz = Integer.signum(gz - z);
        int steps = Math.max(Math.abs(gx - x), Math.abs(gz - z));
        int prevY = y;
        for (int i = 1; i <= steps; i++) {
            int cx = x + dx * i;
            int cz = z + dz * i;
            int cy = sampleHeight(cx, cz);
            if (!isTraversable(prevY, cy)) return false;
            if (sampleStability(cx, cz, cy) == Double.MAX_VALUE) return false;
            prevY = cy;
        }
        return true;
    }

    /**
     * Combines A* prefix with straight LOS finish.
     */
    private List<BlockPos> finishWithLos(long currentKey, long startKey, Long2LongMap parent, BlockPos goal) {
        List<BlockPos> path = reconstructPath(currentKey, startKey, parent); // already interpolated
        BlockPos last = path.getLast();
        interpolateSegment(last, goal, path);
        int yGoal = sampleHeight(goal.getX(), goal.getZ());
        path.add(new BlockPos(goal.getX(), yGoal, goal.getZ()));
        return path;
    }

    // ────────────────────────────────────────────────────────────────────────
    // Caching helpers

//    private int sampleHeight(int x, int z) {
//        long key = hash(x, z);
//        return heightCache.computeIfAbsent(key, k -> {
//            Chunk chunk = world.getChunkManager().getChunk(x >> 4, z >> 4, ChunkStatus.SURFACE, true);
//            Heightmap hm = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
//            return hm.get(x & 15, z & 15);
//        });
//    }

    private int sampleHeight(int x, int z) {
        long key = hash(x, z);
        int cached = heightCache.get(key);
        if (cached != heightCache.defaultReturnValue()) {
            return cached;
        }
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
        if (c != Double.MAX_VALUE) return c;
        c = terrainStabilityCost(x, z, y);
        stabilityCache.put(k, c);
        return c;
    }

    private RegistryEntry<Biome> sampleBiome(long k) {
        RegistryEntry<Biome> biome = biomeCache.get(k);
        BiomeSource source = world.getChunkManager().getChunkGenerator().getBiomeSource();
        if (biome == null) {
            int x = (int) (k >> 32), z = (int) k;
            biome = source.getBiome(x, 0, z, world.getChunkManager().getNoiseConfig().getMultiNoiseSampler());
//            biome = world.getBiomeForNoiseGen(x, 0, z);;
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

    private static double biomeCost(RegistryEntry<Biome> b) {
        return (b.isIn(BiomeTags.IS_RIVER) || b.isIn(BiomeTags.IS_OCEAN) || b.isIn(BiomeTags.IS_DEEP_OCEAN) || b.isIn(BiomeTags.IS_MOUNTAIN) || b.isIn(BiomeTags.IS_BEACH)) ? 400.0 : 0.0;
    }

    private static double yLevelCost(int y) {
        return y == 62 ? 160.0 : 0.0;
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
        return (Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ())) < GRID_STEP * 2;
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
        out.add(v.get(v.size() - 1));
        return out;
    }

    /**
     * Adds intermediate 1‑block steps between two points (inclusive start, exclusive end).
     */
    private void interpolateSegment(BlockPos a, BlockPos b, List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        int y = a.getY();
        for (int i = 1; i < steps; i++) {
            out.add(new BlockPos(a.getX() + dx * i, y, a.getZ() + dz * i));
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
}
