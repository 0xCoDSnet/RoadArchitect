package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.util.AsyncExecutor;
import net.oxcodsnet.roadarchitect.util.CacheManager;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Map.Entry;

/**
 * Post-processes raw A* paths into detailed block sequences
 * and applies parallel-road Y-merge post-optimization.
 */
public final class RoadPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPostProcessor");

    private RoadPostProcessor() {}

    // ====== ПАРАМЕТРЫ (можно вынести в конфиг позже) ======
    private static final int TOLERANCE_BLOCKS = 30;         // близость, блоки
    private static final int ANGLE_THRESHOLD_DEG = 35;     // почти параллельные
    private static final int TAIL_ANGLE_MAX_DEG = 35;      // угол после схождения
    private static final double AVG_DIST_FACTOR = 1.5;     // < tolerance * factor
    private static final double SCORE_LIMIT = 50.0;        // угол + dist/10
    private static final int MAX_ITER = 1;                 // N-циклы
    private static final boolean UNTIL_STABLE = true;      // до стабилизации

    // ====== Регистрация хуков (как в твоей версии) ======
    public static void register() {
//        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
//            if (world.isClient()) return;
//            if (world.getRegistryKey() != World.OVERWORLD) return;
//            processChunk(world, chunk.getPos());
//        });

        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() != World.OVERWORLD) return;
            processPending(world);
        });
    }

    // ====== refine как было ======
    private static List<BlockPos> refine(ServerWorld world, List<BlockPos> verts) {
        if (verts.isEmpty()) return List.of();

        List<BlockPos> out = new ArrayList<>(verts.size() * PathFinder.GRID_STEP);
        for (int i = 0; i < verts.size() - 1; i++) {
            BlockPos a = verts.get(i);
            BlockPos b = verts.get(i + 1);
            out.add(a.down());
            interpolate(world, a, b, out);
        }
        out.add(verts.getLast().down());
        return out;
    }

    private static void interpolate(ServerWorld world, BlockPos a, BlockPos b, List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        for (int i = 1; i < steps; i++) {
            int nx = a.getX() + dx * i;
            int nz = a.getZ() + dz * i;
            int ny = CacheManager.getHeight(world, nx, nz) - 1;
            out.add(new BlockPos(nx, ny, nz));
        }
    }

    // ====== Планирование как у тебя: по одному ключу ======
    public static void processPending(ServerWorld world) {
        PathStorage storage = PathStorage.get(world);
        for (Entry<String, PathStorage.Status> e : storage.allStatuses().entrySet()) {
            if (e.getValue() != PathStorage.Status.PENDING) continue;
            schedule(world, storage, e.getKey());
            break;
        }
    }

    private static void processChunk(ServerWorld world, ChunkPos chunk) {
        PathStorage storage = PathStorage.get(world);
        for (String key : storage.getPendingForChunk(chunk)) {
            schedule(world, storage, key);
        }
    }

    /**
     * ВАЖНО про статусы:
     * - baseKey: tryMarkProcessing(baseKey) -> PROCESSING; дальше либо updatePath(..., READY), либо FAILED.
     * - partnerKey: tryMarkProcessing(partner) -> PROCESSING; если отказались от слияния -> setStatus(partner, PENDING);
     *   если слили -> updatePath(..., READY).
     * Никаких «скрытых» переводов статусов в finally.
     */
    private static void schedule(ServerWorld world, PathStorage storage, String baseKey) {
        if (!storage.tryMarkProcessing(baseKey)) return; // уже не PENDING
        final List<BlockPos> baseRawInitial = storage.getPath(baseKey);

        AsyncExecutor.execute(() -> {
            // ключ, который сейчас «активен» в N-цикле
            String activeKey = baseKey;
            List<BlockPos> activeRaw = new ArrayList<>(baseRawInitial);

            // какие партнёры мы помечали PROCESSING, но ещё не сделали READY
            final Set<String> partnersMarked = new HashSet<>();
            // какие ключи мы точно довели до READY через updatePath
            final Set<String> becameReady = new HashSet<>();

            // что реально отдаём строителю (refined-данные)
            final Map<String, List<BlockPos>> toBuild = new HashMap<>();

            try {
                int iter = 0;
                boolean changed;

                do {
                    changed = false;
                    iter++;

                    // 1) Ищем лучшего параллельного соседа среди PENDING
                    MergeCandidate cand = findBestParallelPartner(storage, activeKey, activeRaw);
                    if (cand == null) break;

                    // 2) Пытаемся пометить соседа как PROCESSING
                    if (!storage.tryMarkProcessing(cand.otherKey)) {
                        // сосед уже не PENDING — пробуем дальше на этой же итерации
                        continue;
                    }
                    partnersMarked.add(cand.otherKey);

                    // 3) Проверяем схождение
                    List<BlockPos> otherRaw = storage.getPath(cand.otherKey);
                    Convergence conv = findConvergence(activeRaw, otherRaw);
                    if (conv == null) {
                        // схождения нет — ОБЯЗАТЕЛЬНО вернуть статус
                        storage.setStatus(cand.otherKey, PathStorage.Status.PENDING);
                        partnersMarked.remove(cand.otherKey);
                        continue;
                    }

                    // 4) Строим Y
                    BuildResult br = buildY(world, activeKey, activeRaw, cand.otherKey, otherRaw, conv);

                    // 4.1) Ноги → READY
                    for (Entry<String, List<BlockPos>> leg : br.legsRaw.entrySet()) {
                        List<BlockPos> refined = refine(world, leg.getValue());
                        storage.updatePath(leg.getKey(), refined, PathStorage.Status.READY);
                        toBuild.put(leg.getKey(), refined);
                        becameReady.add(leg.getKey());
                    }

                    // 4.2) Ствол (persist) → READY
                    List<BlockPos> trunkRefined = refine(world, br.trunkRaw.path);
                    storage.updatePath(br.trunkRaw.key, trunkRefined, PathStorage.Status.READY);
                    toBuild.put(br.trunkRaw.key, trunkRefined);
                    becameReady.add(br.trunkRaw.key);

                    // 4.3) Следующая итерация — уже по стволу
                    activeKey = br.trunkRaw.key;
                    activeRaw = br.trunkRaw.path;

                    changed = true;
                } while (iter < MAX_ITER && (!UNTIL_STABLE || changed || iter == 1));

                // Если ничего не объединили — просто дорисовываем исходный путь
                if (toBuild.isEmpty()) {
                    List<BlockPos> refined = refine(world, activeRaw);
                    storage.updatePath(activeKey, refined, PathStorage.Status.READY);
                    toBuild.put(activeKey, refined);
                    becameReady.add(activeKey);
                }

                // Разом ставим задачи строителю (несколько ключей)
                RoadBuilderManager.queueSegments(world, toBuild);

            } catch (Exception ex) {
                LOGGER.error("Post-processing failed for {}", baseKey, ex);
                storage.setStatus(baseKey, PathStorage.Status.FAILED);
                // партнёров, которых мы пометили PROCESSING, но не довели до READY — откатим
                for (String p : partnersMarked) {
                    if (!becameReady.contains(p)) {
                        storage.setStatus(p, PathStorage.Status.PENDING);
                    }
                }
                return; // важно выйти, чтобы не падать в finally
            }

            // Гарантируем, что «помеченные» партнёры либо стали READY, либо возвращены в PENDING
            for (String p : partnersMarked) {
                if (!becameReady.contains(p) && storage.getStatus(p) == PathStorage.Status.PROCESSING) {
                    storage.setStatus(p, PathStorage.Status.PENDING);
                }
            }

        });
    }

    // ====== Детект «почти параллельных» пар (по одному активному ключу) ======
    private static MergeCandidate findBestParallelPartner(PathStorage storage, String baseKey, List<BlockPos> baseRaw) {
        AABB bbBase = AABB.of(baseRaw).inflate(TOLERANCE_BLOCKS * 2);

        double bestScore = Double.POSITIVE_INFINITY;
        String bestKey = null;

        for (Entry<String, PathStorage.Status> e : storage.allStatuses().entrySet()) {
            if (e.getValue() != PathStorage.Status.PENDING) continue; // только PENDING!
            String otherKey = e.getKey();
            if (otherKey.equals(baseKey)) continue;

            List<BlockPos> otherRaw = storage.getPath(otherKey);
            if (otherRaw.size() < 2) continue;

            // bbox отсев
            AABB bbOther = AABB.of(otherRaw);
            if (!bbBase.intersectsInflated(bbOther)) continue;

            // почти параллельны?
            double angle = angleDeg(dir(baseRaw), dir(otherRaw));
            if (angle >= ANGLE_THRESHOLD_DEG) continue;

            double minDist = minPointToPointDist(baseRaw, otherRaw);
            if (minDist >= TOLERANCE_BLOCKS) continue;

            double score = angle + minDist * 0.5;
            if (score < bestScore) { bestScore = score; bestKey = otherKey; }
        }

        return bestKey == null ? null : new MergeCandidate(bestKey, bestScore);
    }

    // ====== Поиск точки схождения ======
    private static Convergence findConvergence(List<BlockPos> a, List<BlockPos> b) {
        int n = a.size(), m = b.size();
        if (n < 3 || m < 3) return null;

        double bestScore = Double.POSITIVE_INFINITY;
        int bestI = -1, bestJ = -1;

        for (int i = 1; i < n - 1; i++) {
            BlockPos ai = a.get(i);
            int jBest = -1; double dMin = Double.POSITIVE_INFINITY;
            for (int j = 1; j < m - 1; j++) {
                double d = hypot2D(ai, b.get(j));
                if (d < dMin) { dMin = d; jBest = j; }
            }
            if (jBest <= 0 || dMin >= TOLERANCE_BLOCKS * 2) continue;

            List<BlockPos> tailA = a.subList(i, n);
            List<BlockPos> tailB = b.subList(jBest, m);
            if (tailA.size() < 2 || tailB.size() < 2) continue;

            double angTail = angleDeg(dir(tailA), dir(tailB));
            double avgDist = minPointToPointDist(tailA, tailB);
            double score = angTail + (avgDist / 10.0);

            if (angTail < TAIL_ANGLE_MAX_DEG && avgDist < (AVG_DIST_FACTOR * TOLERANCE_BLOCKS) && score < bestScore) {
                bestScore = score; bestI = i; bestJ = jBest;
            }
        }

        if (bestScore >= SCORE_LIMIT || bestI < 0) return null;
        return new Convergence(bestI, bestJ);
    }

    // ====== Построение Y и persist-ствола ======
    private static BuildResult buildY(ServerWorld world,
                                      String keyA, List<BlockPos> a,
                                      String keyB, List<BlockPos> b,
                                      Convergence conv) {
        BlockPos pa = a.get(conv.i);
        BlockPos pb = b.get(conv.j);

        int jx = (int)Math.round((pa.getX() + pb.getX()) / 2.0);
        int jz = (int)Math.round((pa.getZ() + pb.getZ()) / 2.0);
        int jy = CacheManager.getHeight(world, jx, jz); // refine потом даст .down()
        BlockPos J = new BlockPos(jx, jy, jz);

        List<BlockPos> legA = new ArrayList<>(a.subList(0, conv.i + 1));
        legA.set(legA.size() - 1, J);

        List<BlockPos> legB = new ArrayList<>(b.subList(0, conv.j + 1));
        legB.set(legB.size() - 1, J);

        List<BlockPos> tailA = a.subList(conv.i, a.size());
        List<BlockPos> tailB = b.subList(conv.j, b.size());

        List<BlockPos> chosenTail;
        String chosenKey;
        if (tailA.size() > tailB.size() || (tailA.size() == tailB.size() && conv.i <= conv.j)) {
            chosenTail = tailA; chosenKey = keyA;
        } else {
            chosenTail = tailB; chosenKey = keyB;
        }

        List<BlockPos> trunk = new ArrayList<>(chosenTail.size());
        trunk.add(J);
        trunk.addAll(chosenTail.subList(1, chosenTail.size()));
        String trunkKey = chosenKey + "#J@" + jx + "," + jz; // persist

        Map<String, List<BlockPos>> legs = new HashMap<>();
        legs.put(keyA, legA);
        legs.put(keyB, legB);

        return new BuildResult(legs, new Trunk(trunkKey, trunk));
    }

    // ====== Вспомогательные структуры и математика ======
    private record MergeCandidate(String otherKey, double score) {}
    private record Convergence(int i, int j) {}
    private record Trunk(String key, List<BlockPos> path) {}
    private static final class BuildResult {
        final Map<String, List<BlockPos>> legsRaw;
        final Trunk trunkRaw;
        BuildResult(Map<String, List<BlockPos>> legsRaw, Trunk trunkRaw) {
            this.legsRaw = legsRaw; this.trunkRaw = trunkRaw;
        }
    }

    private record AABB(int x1, int z1, int x2, int z2) {
        static AABB of(List<BlockPos> pts) {
            int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
            for (BlockPos p : pts) {
                if (p.getX() < minX) minX = p.getX();
                if (p.getZ() < minZ) minZ = p.getZ();
                if (p.getX() > maxX) maxX = p.getX();
                if (p.getZ() > maxZ) maxZ = p.getZ();
            }
            return new AABB(minX, minZ, maxX, maxZ);
        }
        AABB inflate(int r) { return new AABB(x1 - r, z1 - r, x2 + r, z2 + r); }
        boolean intersectsInflated(AABB other) {
            return this.x1 <= other.x2 && this.x2 >= other.x1 && this.z1 <= other.z2 && this.z2 >= other.z1;
        }
    }

    private static double angleDeg(int[] v1, int[] v2) {
        double n1 = Math.hypot(v1[0], v1[1]);
        double n2 = Math.hypot(v2[0], v2[1]);
        if (n1 == 0 || n2 == 0) return 180;
        double dot = (v1[0] * v2[0] + v1[1] * v2[1]) / (n1 * n2);
        dot = Math.max(-1, Math.min(1, dot));
        double ang = Math.toDegrees(Math.acos(dot));
        return Math.min(ang, 180 - ang);
    }

    private static int[] dir(List<BlockPos> pts) {
        BlockPos s = pts.get(0), e = pts.get(pts.size() - 1);
        return new int[]{ e.getX() - s.getX(), e.getZ() - s.getZ() };
    }

    private static double minPointToPointDist(List<BlockPos> a, List<BlockPos> b) {
        double min = Double.POSITIVE_INFINITY;
        for (BlockPos pa : a) for (BlockPos pb : b) {
            double d = hypot2D(pa, pb);
            if (d < min) min = d;
        }
        return min;
    }

    private static double hypot2D(BlockPos p1, BlockPos p2) {
        int dx = p1.getX() - p2.getX();
        int dz = p1.getZ() - p2.getZ();
        return Math.hypot(dx, dz);
    }
}
