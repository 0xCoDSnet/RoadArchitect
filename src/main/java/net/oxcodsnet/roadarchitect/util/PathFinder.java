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
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static net.oxcodsnet.roadarchitect.util.CacheManager.hash;
import static net.oxcodsnet.roadarchitect.util.CacheManager.keyToPos;

/**
 * A* path‑finder working on a configurable X/Z grid.
 * <ul>
 *   <li>Thread‑safe caches (FastUtil&nbsp;+ synchronize)</li>
 *   <li>Early <b>line‑of‑sight</b> shortcut: if the current vertex sees the goal without steep height changes, search stops early.</li>
 *   <li>Optional interpolation between grid vertices → returns every road block.</li>
 * </ul>
 */
public class PathFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinder");

    /* ================ USER‑TUNABLE PARAMS ================ */
    public static final int GRID_STEP = 3;
    /** Inflation factor ε for ARA* (Weighted A*) */
    public static final double HEURISTIC_WEIGHT = 2;
    private static final int[][] OFFSETS = generateOffsets();
    private static final Map<TagKey<Biome>, Double> BIOME_COSTS = Map.of(
            BiomeTags.IS_RIVER, 400.0,
            BiomeTags.IS_OCEAN, 999.0,
            BiomeTags.IS_DEEP_OCEAN, 999.0,
            BiomeTags.IS_MOUNTAIN, 160.0,
            BiomeTags.IS_BEACH, 160.0
    );
    /* ===================================================== */
    private final NodeStorage nodes;
    private final ServerWorld world;
    private final int maxSteps;

    /* ── hot references to world‑gen objects ── */
    private final ChunkGenerator generator;
    private final NoiseConfig noiseConfig;
    private final MultiNoiseUtil.MultiNoiseSampler noiseSampler;
    private final BiomeSource biomeSource;


    public PathFinder(NodeStorage nodes, ServerWorld world, int maxSteps) {
        this.nodes = nodes;
        this.world = world;
        this.maxSteps = maxSteps;

        this.generator   = world.getChunkManager().getChunkGenerator();
        this.noiseConfig = world.getChunkManager().getNoiseConfig();
        this.noiseSampler = noiseConfig.getMultiNoiseSampler();
        this.biomeSource = generator.getBiomeSource();
    }

    /**
     * Returns the list of {@link BlockPos} actually traversed by A*.
     */
    public List<BlockPos> findPath(String fromId, String toId) {
        return aStar(fromId, toId);
    }

    // ───────────────────────────────────────────────────────────────────────
    // A* core

    private List<BlockPos> aStar(String fromId, String toId) {
        Node startNode = nodes.all().get(fromId);
        Node endNode   = nodes.all().get(toId);
        if (startNode == null || endNode == null) {
            LOGGER.debug("Missing node(s) {} or {}", fromId, toId);
            return List.of();
        }

        record Rec(long key, double g, double f) { }

        BlockPos startPos = snap(startNode.pos());
        BlockPos endPos   = snap(endNode.pos());
        long startKey     = hash(startPos.getX(), startPos.getZ());
        long endKey       = hash(endPos.getX(), endPos.getZ());

        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        gScore.put(startKey, 0.0);
        open.add(new Rec(
                startKey,
                0.0,
                heuristic(startPos, endPos) * HEURISTIC_WEIGHT   // inflated h
        ));

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            Rec current = open.poll();
            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);

            // goal test
            if (current.key == endKey) {
                return reconstructVertices(current.key, startKey, parent);
            }

            // expand neighbours
            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);

                int ny = sampleHeight(nx, nz);
                if (isSteep(curY, ny)) {
                    continue;
                }

                double stab = sampleStability(nx, nz, ny);
                if (stab == Double.MAX_VALUE) {
                    continue;
                }

                double bCost = biomeCost(sampleBiome(nx, nz, ny));
                if (bCost >= 999.0) {
                    continue;
                }

                double tentativeG = gScore.get(current.key)
                        + stepCost(off)
                        + elevationCost(curY, ny)
                        + bCost
                        + yLevelCost(ny)
                        + stab;

                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    double f = tentativeG + heuristic(nx, nz, endPos) * HEURISTIC_WEIGHT;
                    open.add(new Rec(neighKey, tentativeG, f));
                }
            }
        }
        LOGGER.debug("Path not found between {} and {} after {} iterations", fromId, toId, iterations);
        return List.of();
    }

    // ───────────────────────────────────────────────────────────────────────
    // Caching helpers (ultrafast world‑gen API)

    private int sampleHeight(int x, int z) {
        long key = hash(x, z);
        return CacheManager.getHeight(world, key, () ->
                generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world, noiseConfig)
        );
    }

    private double sampleStability(int x, int z, int y) {
        long key = hash(x, z);
        return CacheManager.getStability(world, key, () -> terrainStabilityCost(x, z, y));
    }

    private RegistryEntry<Biome> sampleBiome(int x, int z, int y) {
        long key = hash(x, z);
        return CacheManager.getBiome(world, key, () ->
                biomeSource.getBiome(
                        BiomeCoords.fromBlock(x),
                        316,
                        BiomeCoords.fromBlock(z),
                        noiseSampler
                )
        );
    }

    // ───────────────────────────────────────────────────────────────────────
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
        int cost = 0;
        for (Direction d : Direction.Type.HORIZONTAL) {
            int ny = sampleHeight(x + d.getOffsetX(), z + d.getOffsetZ());
            cost += Math.abs(y - ny);
            if (cost > 2) {
                return Double.MAX_VALUE;
            }
        }
        return cost * 16.0;
    }

    private static boolean isSteep(int y1, int y2) {
        return Math.abs(y1 - y2) > 3;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Heuristics

    private static double heuristic(int x, int z, BlockPos goal) {
        int dx = Math.abs(x - goal.getX());
        int dz = Math.abs(z - goal.getZ());
        double a = dx + dz - 0.6 * Math.min(dx, dz);
        return a * 40.0;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return heuristic(a.getX(), a.getZ(), b);
    }

    // ───────────────────────────────────────────────────────────────────────
    // Path reconstruction

    private List<BlockPos> reconstructVertices(long goal, long start, Long2LongMap parent) {
        List<BlockPos> vertices = new ArrayList<>();
        for (long k = goal; ; k = parent.get(k)) {
            BlockPos p = keyToPos(k);
            int y = sampleHeight(p.getX(), p.getZ());
            vertices.add(new BlockPos(p.getX(), y, p.getZ()));
            if (k == start) {
                break;
            }
        }
        Collections.reverse(vertices);
        return vertices;
    }

    // ───────────────────────────────────────────────────────────────────────
    // Utility

    private static BlockPos snap(BlockPos p) {
        int x = Math.floorDiv(p.getX(), GRID_STEP) * GRID_STEP;
        int z = Math.floorDiv(p.getZ(), GRID_STEP) * GRID_STEP;
        return new BlockPos(x, p.getY(), z);
    }

    private static int[][] generateOffsets() {
        int d = GRID_STEP;
        return new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
    }
}
