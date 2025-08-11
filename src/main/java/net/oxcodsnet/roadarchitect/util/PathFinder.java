package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
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
 * A* | ARA* поиск пути по настраиваемой X/Z-сети.
 * <ul>
 *   <li>Потокобезопасные кэши (FastUtil + централизованный CacheManager)</li>
 *   <li>Поддержка позиционного поиска (между произвольными BlockPos)</li>
 *   <li>Регулируемый вес эвристики (ε), чтобы переключаться между A* (ε=1) и Weighted A*</li>
 * </ul>
 */
public class PathFinder {

    /* ================ USER-TUNABLE PARAMS ================ */
    public static final int GRID_STEP = 4;

    /** Inflation factor ε для ARA* (Weighted A*) */
    public static final double HEURISTIC_WEIGHT = 1.8;

    /** Масштаб эвристики (будет предложен профайлером) */
    public static final double HEURISTIC_SCALE = 95;

    /** Раннее завершение по манхэттену (радиус в блоках) */
    private static final int EARLY_STOP_L1 = 65;

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinder");
    private static final int[][] OFFSETS = generateOffsets();

    private static final Map<TagKey<Biome>, Double> BIOME_COSTS = Map.of(
            BiomeTags.IS_RIVER, 400.0,
            BiomeTags.IS_OCEAN, 999.0,
            BiomeTags.IS_DEEP_OCEAN, 999.0,
            BiomeTags.IS_MOUNTAIN, 160.0,
            BiomeTags.IS_BEACH, 160.0
    );

    /* =================== ПРОФАЙЛЕР =================== */

    /** Щёлкалка профилирования */
    private static final boolean PROFILING_ENABLED = true;

    /** Счётчики вызовов семплеров на один запуск поиска */
    private long profHeightCalls = 0L;
    private long profBiomeCalls = 0L;
    private long profStabCalls = 0L;

    private static final class ProfileSession {
        final BlockPos start;
        final BlockPos goal;

        int iterations = 0;
        long neighborsChecked = 0L;
        long relaxationsAccepted = 0L;

        /** Сумма инкрементальных стоимостей по ВСЕМ принятым релаксациям */
        double sumStepCosts = 0.0;

        /** Средняя цена шага по фактическому пути (если найден) */
        double avgStepOnPath = 0.0;
        boolean pathFound = false;

        ProfileSession(BlockPos start, BlockPos goal) {
            this.start = start;
            this.goal = goal;
        }
    }

    /* ===================================================== */

    private final NodeStorage nodes;
    private final ServerWorld world;
    private final int maxSteps;
    private final double heuristicWeight;

    /* ── горячие ссылки на объекты генерации мира ── */
    private final ChunkGenerator generator;
    private final NoiseConfig noiseConfig;
    private final MultiNoiseUtil.MultiNoiseSampler noiseSampler;
    private final BiomeSource biomeSource;

    public PathFinder(NodeStorage nodes, ServerWorld world, int maxSteps) {
        this(nodes, world, maxSteps, HEURISTIC_WEIGHT);
    }

    public PathFinder(NodeStorage nodes, ServerWorld world, int maxSteps, double heuristicWeight) {
        this.nodes = nodes;
        this.world = world;
        this.maxSteps = maxSteps;
        this.heuristicWeight = heuristicWeight;

        this.generator = world.getChunkManager().getChunkGenerator();
        this.noiseConfig = world.getChunkManager().getNoiseConfig();
        this.noiseSampler = noiseConfig.getMultiNoiseSampler();
        this.biomeSource = generator.getBiomeSource();
    }

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
        return y <= 63 ? 240.0 : 0.0;
    }

    private static boolean isSteep(int y1, int y2) {
        return Math.abs(y1 - y2) > 3;
    }

    private static double heuristic(int x, int z, BlockPos goal) {
        int dx = Math.abs(x - goal.getX());
        int dz = Math.abs(z - goal.getZ());
        double a = dx + dz - 0.5 * Math.min(dx, dz); // октильная эвристика
        return a * HEURISTIC_SCALE;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return heuristic(a.getX(), a.getZ(), b);
    }

    private static BlockPos snap(BlockPos p) {
        int x = Math.floorDiv(p.getX(), GRID_STEP) * GRID_STEP;
        int z = Math.floorDiv(p.getZ(), GRID_STEP) * GRID_STEP;
        return new BlockPos(x, p.getY(), z);
    }

    private static int[][] generateOffsets() {
        int d = GRID_STEP;
        return new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {d, -d}, {-d, d}, {-d, -d}};
    }

    /* ───────────────────────── Публичный API ───────────────────────── */

    /** Поиск по идентификаторам узлов (как и раньше). */
    public List<BlockPos> findPath(String fromId, String toId) {
        Node startNode = nodes.all().get(fromId);
        Node endNode = nodes.all().get(toId);
        if (startNode == null || endNode == null) {
            LOGGER.debug("Missing node(s) {} or {}", fromId, toId);
            return List.of();
        }
        return aStarPositions(snap(startNode.pos()), snap(endNode.pos()));
    }

    /** Поиск между произвольными позициями (для локального реплана). */
    public List<BlockPos> findPath(BlockPos from, BlockPos to) {
        return aStarPositions(snap(from), snap(to));
    }

    /* ───────────────────────── Реализация A* ───────────────────────── */

    private List<BlockPos> aStarPositions(BlockPos startPos, BlockPos endPos) {
        ProfileSession ps = PROFILING_ENABLED ? new ProfileSession(startPos, endPos) : null;
        try {
            return aStarPositions(startPos, endPos, ps);
        } finally {
            if (PROFILING_ENABLED) {
                logProfile(ps);
                profHeightCalls = profBiomeCalls = profStabCalls = 0L;
            }
        }
    }

    private List<BlockPos> aStarPositions(BlockPos startPos, BlockPos endPos, ProfileSession ps) {
        record Rec(long key, double g, double f) {}

        long startKey = hash(startPos.getX(), startPos.getZ());
        long endKey = hash(endPos.getX(), endPos.getZ());

        PriorityQueue<Rec> open = new PriorityQueue<>(Comparator.comparingDouble(r -> r.f));
        Long2DoubleMap gScore = new Long2DoubleOpenHashMap();
        gScore.defaultReturnValue(Double.MAX_VALUE);
        Long2LongMap parent = new Long2LongOpenHashMap();

        gScore.put(startKey, 0.0);
        open.add(new Rec(startKey, 0.0, heuristic(startPos, endPos) * heuristicWeight));

        int iterations = 0;
        while (!open.isEmpty() && iterations++ < maxSteps) {
            if (ps != null) ps.iterations++;

            Rec current = open.poll();
            if (current.g() > gScore.get(current.key())) {
                continue; // фильтр "протухших" записей
            }

            int curX = (int) (current.key >> 32);
            int curZ = (int) current.key;
            int curY = sampleHeight(curX, curZ);

            int md = Math.abs(curX - endPos.getX()) + Math.abs(curZ - endPos.getZ());
            if (md <= EARLY_STOP_L1 || current.key == endKey) {
                List<BlockPos> path = reconstructVertices(current.key, startKey, parent);
                if (ps != null) {
                    ps.avgStepOnPath = computeAvgCostOfPathVertices(path);
                    ps.pathFound = true;
                }
                return path;
            }

            for (int[] off : OFFSETS) {
                int nx = curX + off[0];
                int nz = curZ + off[1];
                long neighKey = hash(nx, nz);
                if (ps != null) ps.neighborsChecked++;

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

                double inc = stepCost(off)
                        + elevationCost(curY, ny)
                        + bCost
                        + yLevelCost(ny)
                        + stab;

                double tentativeG = gScore.get(current.key) + inc;

                if (tentativeG < gScore.get(neighKey)) {
                    parent.put(neighKey, current.key);
                    gScore.put(neighKey, tentativeG);
                    if (ps != null) {
                        ps.relaxationsAccepted++;
                        ps.sumStepCosts += inc;
                    }
                    double f = tentativeG + heuristic(nx, nz, endPos) * heuristicWeight;
                    open.add(new Rec(neighKey, tentativeG, f));
                }
            }
        }

        LOGGER.debug("Path not found between {} and {} after {} iterations", startPos, endPos, iterations);
        return List.of();
    }

    /* ───────────────────────── Быстрые семплеры ───────────────────────── */

    private int sampleHeight(int x, int z) {
        profHeightCalls++;
        long key = hash(x, z);
        return CacheManager.getHeight(world, key, () ->
                generator.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, noiseConfig)
        );
    }

    private double sampleStability(int x, int z, int y) {
        profStabCalls++;
        long key = hash(x, z);
        return CacheManager.getStability(world, key, () -> terrainStabilityCost(x, z, y));
    }

    private RegistryEntry<Biome> sampleBiome(int x, int z, int y) {
        profBiomeCalls++;
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

    /* ───────────────────────── Утилиты ───────────────────────── */

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

    /** Средняя «цена шага» по уже восстановленному списку вершин пути. */
    private double computeAvgCostOfPathVertices(List<BlockPos> path) {
        if (path.size() < 2) {
            return 0.0;
        }
        double sum = 0.0;
        int cnt = 0;
        for (int i = 1; i < path.size(); i++) {
            BlockPos a = path.get(i - 1);
            BlockPos b = path.get(i);
            int dx = b.getX() - a.getX();
            int dz = b.getZ() - a.getZ();
            int ay = a.getY();
            int by = b.getY();

            // шаг в терминах OFFSETS (±GRID_STEP по x/z)
            int[] off = new int[]{dx, dz};

            double inc = stepCost(off)
                    + elevationCost(ay, by)
                    + biomeCost(sampleBiome(b.getX(), b.getZ(), by))
                    + yLevelCost(by)
                    + sampleStability(b.getX(), b.getZ(), by);

            if (inc == Double.MAX_VALUE) {
                continue;
            }
            sum += inc;
            cnt++;
        }
        return cnt > 0 ? (sum / (double) cnt) : 0.0;
    }

    /* ───────────────────────── Лог профайлера ───────────────────────── */

    private void logProfile(ProfileSession ps) {
        if (ps == null) {
            return;
        }

        double avgStepAll = ps.relaxationsAccepted > 0
                ? ps.sumStepCosts / (double) ps.relaxationsAccepted
                : 0.0;

        double base = (ps.pathFound && ps.avgStepOnPath > 0.0) ? ps.avgStepOnPath : avgStepAll;

        double suggested = base > 0.0 ? Math.max(5.0, Math.min(120.0, base)) : 0.0;

        LOGGER.info(
                """
                [A* profiler] {} -> {}
                  iterations={}  neighbors={}  relaxations={}
                  calls: height={}  biome={}  stability={}
                  avg-step: path={}  all-relaxations={}
                  suggest HEURISTIC_SCALE ≈ {}
                """,
                ps.start.toShortString(), ps.goal.toShortString(),
                ps.iterations, ps.neighborsChecked, ps.relaxationsAccepted,
                profHeightCalls, profBiomeCalls, profStabCalls,
                String.format(Locale.ROOT, "%.2f", ps.avgStepOnPath),
                String.format(Locale.ROOT, "%.2f", avgStepAll),
                String.format(Locale.ROOT, "%.1f", suggested)
        );
    }
}
