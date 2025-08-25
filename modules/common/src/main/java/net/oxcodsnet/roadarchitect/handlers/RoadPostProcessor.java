package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.registry.tag.FluidTags;
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
 * Дополнительно: нормализация высот + подробное логирование мест «иголок».
 */
public final class RoadPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPostProcessor");
    // ====== ПАРАМЕТРЫ (можно вынести в конфиг позже) ======
    private static final int TOLERANCE_BLOCKS = 45;          // близость, блоки
    private static final int ANGLE_THRESHOLD_DEG = 35;       // почти параллельные
    private static final int TAIL_ANGLE_MAX_DEG = 35;        // угол после схождения
    private static final double AVG_DIST_FACTOR = 1.5;       // < tolerance * factor
    private static final double SCORE_LIMIT = 50.0;          // угол + dist/10
    private static final int MAX_ITER = 5;                   // N-циклы (Для лучшего эффекта, должно быть нечётным)
    private static final boolean UNTIL_STABLE = true;        // до стабилизации
    private static final int TRIM_RADIUS_L1 = 50;        // L1-радиус обрезки (как в PathFinder)
    // ====== Нормализация высот (профиль) ======
    private static final int SMOOTH_MEDIAN_WINDOW = 5;       // нечётное: 3/5/7
    private static final int SMOOTH_GRAD_MAX = 1;            // макс. разница Y между соседями
    private static final int SMOOTH_PASSES = 2;              // число прогонов
    private static final int DESPIKE_DELTA = 2;              // чувствительность «иголки»
    private static final boolean LOG_GRAD_CLAMP = true;     // включить подробный лог клампа
    private static final int BUOY_INTERVAL = 25;             // шаг размещения буёв
    private static final int[][] OFFSETS_8 = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };
    private RoadPostProcessor() {
    }

    // ====== Регистрация хуков ======
    public static void onStartWorldTick(ServerWorld world) {
        if (world.isClient()) return;
        if (world.getRegistryKey() != World.OVERWORLD) return;
        processPending(world);
    }


    // ====== Обрезка начала и конца пути по L1-радиусу (XZ) ======
    private static int manhattanXZ(BlockPos a, BlockPos b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getZ() - b.getZ());
    }

    /**
     * Обрезает последовательность вершин с начала и конца, удаляя точки,
     * находящиеся в пределах радиуса R (по Манхэттену в XZ) от ИСХОДНЫХ
     * крайних точек. Если после обрезки остаётся < 2 вершин — возвращаем
     * исходный путь без изменений.
     */
    private static List<BlockPos> trimByManhattan(List<BlockPos> path) {
        if (path == null || path.size() < 2) return path;

        BlockPos start0 = path.getFirst();
        BlockPos end0 = path.getLast();

        int i = 0;
        while (i < path.size() && manhattanXZ(path.get(i), start0) <= TRIM_RADIUS_L1) i++;

        int j = path.size() - 1;
        while (j >= 0 && manhattanXZ(path.get(j), end0) <= TRIM_RADIUS_L1) j--;

        if (i <= 0 && j >= path.size() - 1) {
            // ничего не обрезали
            return path;
        }

        if (j - i + 1 < 2) {
            // по условию — продолжаем без обрезки
            return path;
        }

        return new ArrayList<>(path.subList(i, j + 1));
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

    private static NormalizeResult normalizeHeights(List<BlockPos> refined) {
        if (refined.size() < 3) {
            return new NormalizeResult(refined, 0, 0);
        }

        int n = refined.size();
        int[] y = new int[n];
        for (int i = 0; i < n; i++) y[i] = refined.get(i).getY();

        int spikes = 0;
        int clamps = 0;

        for (int pass = 0; pass < SMOOTH_PASSES; pass++) {
            // 1) Медианный фильтр
            if (SMOOTH_MEDIAN_WINDOW >= 3 && (SMOOTH_MEDIAN_WINDOW & 1) == 1) {
                int r = SMOOTH_MEDIAN_WINDOW / 2;
                int[] tmp = Arrays.copyOf(y, n);
                int[] win = new int[SMOOTH_MEDIAN_WINDOW];
                for (int i = 0; i < n; i++) {
                    int s = Math.max(0, i - r);
                    int e = Math.min(n - 1, i + r);
                    int k = 0;
                    for (int j = s; j <= e; j++) win[k++] = y[j];
                    for (; k < win.length; k++) win[k] = (i < r) ? y[0] : y[n - 1];
                    Arrays.sort(win);
                    tmp[i] = win[win.length / 2];
                }
                y = tmp;
            }

            // 2) Ограничение градиента: вперёд
            for (int i = 1; i < n; i++) {
                int old = y[i];
                int lo = y[i - 1] - SMOOTH_GRAD_MAX;
                int hi = y[i - 1] + SMOOTH_GRAD_MAX;
                int clamped = Math.max(lo, Math.min(hi, old));
                if (clamped != old) {
                    clamps++;
                    if (LOG_GRAD_CLAMP) {
                        BlockPos p = refined.get(i);
                        LOGGER.debug("[PostProcess] Clamp↑ at {}: {} -> {} (ref={})", p, old, clamped, y[i - 1]);
                    }
                    y[i] = clamped;
                }
            }
            // 2b) Ограничение градиента: назад
            for (int i = n - 2; i >= 0; i--) {
                int old = y[i];
                int lo = y[i + 1] - SMOOTH_GRAD_MAX;
                int hi = y[i + 1] + SMOOTH_GRAD_MAX;
                int clamped = Math.max(lo, Math.min(hi, old));
                if (clamped != old) {
                    clamps++;
                    if (LOG_GRAD_CLAMP) {
                        BlockPos p = refined.get(i);
                        LOGGER.debug("[PostProcess] Clamp↓ at {}: {} -> {} (ref={})", p, old, clamped, y[i + 1]);
                    }
                    y[i] = clamped;
                }
            }

            // 3) Срез одиночных «иголок» (пик над обоими соседями)
            for (int i = 1; i < n - 1; i++) {
                int a = y[i - 1];
                int b = y[i + 1];
                int m = Math.max(a, b);
                if (y[i] - m >= DESPIKE_DELTA) {
                    BlockPos p = refined.get(i);
                    int old = y[i];
                    int neu = (a + b) / 2;
                    y[i] = neu;
                    spikes++;
                    LOGGER.warn("[PostProcess] Срезан пик высоты в {}: {} -> {} (соседи: {}/{})",
                            p, old, neu, a, b);
                }
            }
        }

        List<BlockPos> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BlockPos p = refined.get(i);
            out.add(new BlockPos(p.getX(), y[i], p.getZ()));
        }
        return new NormalizeResult(out, spikes, clamps);
    }

    private static WaterBuoyData computeWaterData(ServerWorld world, List<BlockPos> pts) {
        int n = pts.size();
        boolean[] water = new boolean[n];
        for (int i = 0; i < n; i++) {
            water[i] = isWaterSegment(world, pts.get(i));
        }
        List<Integer> idx = computeBuoyIndices(pts, water, 0, n, BUOY_INTERVAL);
        List<BlockPos> buoys = new ArrayList<>(idx.size());
        for (int i : idx) {
            buoys.add(pts.get(i));
        }
        return new WaterBuoyData(water, buoys);
    }

    private static List<Integer> computeBuoyIndices(List<BlockPos> pts, boolean[] water,
                                                    int from, int to, int interval) {
        int n = pts.size();
        double[] s = new double[n];
        for (int i = 1; i < n; i++) {
            BlockPos a = pts.get(i - 1);
            BlockPos b = pts.get(i);
            s[i] = s[i - 1] + Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
        }
        List<Integer> out = new ArrayList<>();
        int i = from;
        int runStart = -1;
        if (i < to && water[i]) {
            runStart = i;
            while (runStart > from && water[runStart - 1]) runStart--;
        }
        double nextMark = -1.0;
        if (runStart != -1) {
            double base = s[runStart];
            double progressed = s[i] - base;
            long k = (long) Math.ceil(progressed / interval);
            nextMark = base + k * interval;
        }
        while (i < to) {
            if (!water[i]) {
                runStart = -1;
                nextMark = -1.0;
                i++;
                if (i < to && water[i]) {
                    runStart = i;
                    while (runStart > from && water[runStart - 1]) runStart--;
                    double base = s[runStart];
                    double progressed = s[i] - base;
                    long k = (long) Math.ceil(progressed / interval);
                    nextMark = base + k * interval;
                }
                continue;
            }
            if (nextMark >= 0.0 && s[i] >= nextMark) {
                out.add(i);
                nextMark += interval;
                continue;
            }
            i++;
        }
        return out;
    }

    private static boolean isWaterSegment(ServerWorld world, BlockPos pos) {
        if (isNotWaterBlock(world, pos)) {
            return false;
        }
        for (int[] d : OFFSETS_8) {
            BlockPos neighbor = pos.add(d[0], 0, d[1]);
            if (isNotWaterBlock(world, neighbor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNotWaterBlock(ServerWorld world, BlockPos pos) {
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return true;
        return !world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
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
     * если слили -> updatePath(..., READY).
     * Никаких «скрытых» переводов статусов в finally.
     */
    private static void schedule(ServerWorld world, PathStorage storage, String baseKey) {
        if (!storage.tryMarkProcessing(baseKey)) return; // уже не PENDING
        final List<BlockPos> baseRawInitial = storage.getPath(baseKey);

        AsyncExecutor.execute(() -> {
            String activeKey = baseKey;
            List<BlockPos> activeRaw = new ArrayList<>(baseRawInitial);
            // Применяем обрезку по Манхэттену к активному пути до любой обработки
            activeRaw = trimByManhattan(activeRaw);

            final Set<String> partnersMarked = new HashSet<>();
            final Set<String> becameReady = new HashSet<>();
            final Map<String, List<BlockPos>> toBuild = new HashMap<>();

            try {
                int iter = 0;
                boolean changed;

                do {
                    changed = false;
                    // Повторно применяем обрезку в начале итерации (после возможных обновлений activeRaw)
                    //activeRaw = trimByManhattan(activeRaw, TRIM_RADIUS_L1);
                    iter++;

                    // 1) Ищем лучшего параллельного соседа среди PENDING
                    MergeCandidate cand = findBestParallelPartner(storage, activeKey, activeRaw);
                    if (cand == null) break;

                    // 2) Пытаемся пометить соседа как PROCESSING
                    if (!storage.tryMarkProcessing(cand.otherKey)) {
                        continue;
                    }
                    partnersMarked.add(cand.otherKey);

                    // 3) Проверяем схождение
                    List<BlockPos> otherRaw = storage.getPath(cand.otherKey);

                    // Обрезаем путь партнёра до всех манипуляций
                    otherRaw = trimByManhattan(otherRaw);

                    Convergence conv = findConvergence(activeRaw, otherRaw);
                    if (conv == null) {
                        storage.setStatus(cand.otherKey, PathStorage.Status.PENDING);
                        partnersMarked.remove(cand.otherKey);
                        continue;
                    }
                    // 4) Строим Y
                    BuildResult br = buildY(world, activeKey, activeRaw, cand.otherKey, otherRaw, conv);

                    // 4.1) Ноги → refine → normalize → READY
                    for (Entry<String, List<BlockPos>> leg : br.legsRaw.entrySet()) {
                        List<BlockPos> refined = refine(world, leg.getValue());
                        WaterBuoyData wb = computeWaterData(world, refined);
                        NormalizeResult nr = normalizeHeights(refined);
                        storage.updatePath(leg.getKey(), nr.path(), wb.waterMask(), wb.buoys(), PathStorage.Status.READY);
                        toBuild.put(leg.getKey(), nr.path());

                        if (!nr.path().isEmpty()) {
                            BlockPos s = nr.path().getFirst();
                            BlockPos t = nr.path().getLast();
                            LOGGER.debug(
                                    "[PostProcess] READY (leg) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                    leg.getKey(), nr.path().size(), nr.spikesCut(), nr.gradClamped(), s, t
                            );
                        }
                        becameReady.add(leg.getKey());
                    }

                    // 4.2) Ствол (persist) → refine → normalize → READY
                    List<BlockPos> trunkRefined = refine(world, br.trunkRaw.path);
                    WaterBuoyData trunkWb = computeWaterData(world, trunkRefined);
                    NormalizeResult trunkNR = normalizeHeights(trunkRefined);
                    storage.updatePath(br.trunkRaw.key, trunkNR.path(), trunkWb.waterMask(), trunkWb.buoys(), PathStorage.Status.READY);
                    toBuild.put(br.trunkRaw.key, trunkNR.path());

                    if (!trunkNR.path().isEmpty()) {
                        BlockPos s = trunkNR.path().getFirst();
                        BlockPos t = trunkNR.path().getLast();
                        LOGGER.debug(
                                "[PostProcess] READY (trunk) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                br.trunkRaw.key, trunkNR.path().size(), trunkNR.spikesCut(), trunkNR.gradClamped(), s, t
                        );
                    }
                    becameReady.add(br.trunkRaw.key);

                    // 4.3) Следующая итерация — уже по стволу
                    activeKey = br.trunkRaw.key;
                    activeRaw = br.trunkRaw.path;

                    changed = true;
                } while (iter < MAX_ITER && (!UNTIL_STABLE || changed || iter == 1));

                // Если ничего не объединили — просто дорисовываем исходный путь
                if (toBuild.isEmpty()) {
                    List<BlockPos> refined = refine(world, activeRaw);
                    WaterBuoyData wb = computeWaterData(world, refined);
                    NormalizeResult nr = normalizeHeights(refined);
                    storage.updatePath(activeKey, nr.path(), wb.waterMask(), wb.buoys(), PathStorage.Status.READY);
                    toBuild.put(activeKey, nr.path());

                    if (!nr.path().isEmpty()) {
                        BlockPos s = nr.path().getFirst();
                        BlockPos t = nr.path().getLast();
                        LOGGER.debug(
                                "[PostProcess] READY (single) key={} points={}, spikesCut={}, gradClamped={}, start={}, end={}",
                                activeKey, nr.path().size(), nr.spikesCut(), nr.gradClamped(), s, t
                        );
                    }
                    becameReady.add(activeKey);
                }

                // Разом ставим задачи строителю
                RoadBuilderManager.queueSegments(world, toBuild);

            } catch (Exception ex) {
                LOGGER.error("Post-processing failed for {}", baseKey, ex);
                storage.setStatus(baseKey, PathStorage.Status.FAILED);
                for (String p : partnersMarked) {
                    if (!becameReady.contains(p)) {
                        storage.setStatus(p, PathStorage.Status.PENDING);
                    }
                }
                return;
            }

            // Гарантируем возврат статусов партнёров
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
            //otherRaw = trimByManhattan(otherRaw, TRIM_RADIUS_L1);
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
            if (score < bestScore) {
                bestScore = score;
                bestKey = otherKey;
            }
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
            int jBest = -1;
            double dMin = Double.POSITIVE_INFINITY;
            for (int j = 1; j < m - 1; j++) {
                double d = hypot2D(ai, b.get(j));
                if (d < dMin) {
                    dMin = d;
                    jBest = j;
                }
            }
            if (jBest <= 0 || dMin >= TOLERANCE_BLOCKS * 2) continue;

            List<BlockPos> tailA = a.subList(i, n);
            List<BlockPos> tailB = b.subList(jBest, m);
            if (tailA.size() < 2 || tailB.size() < 2) continue;

            double angTail = angleDeg(dir(tailA), dir(tailB));
            double avgDist = minPointToPointDist(tailA, tailB);
            double score = angTail + (avgDist / 10.0);

            if (angTail < TAIL_ANGLE_MAX_DEG && avgDist < (AVG_DIST_FACTOR * TOLERANCE_BLOCKS) && score < bestScore) {
                bestScore = score;
                bestI = i;
                bestJ = jBest;
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

        int jx = (int) Math.round((pa.getX() + pb.getX()) / 2.0);
        int jz = (int) Math.round((pa.getZ() + pb.getZ()) / 2.0);
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
            chosenTail = tailA;
            chosenKey = keyA;
        } else {
            chosenTail = tailB;
            chosenKey = keyB;
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
        BlockPos s = pts.getFirst(), e = pts.getLast();
        return new int[]{e.getX() - s.getX(), e.getZ() - s.getZ()};
    }

    private static double minPointToPointDist(List<BlockPos> a, List<BlockPos> b) {
        double min = Double.POSITIVE_INFINITY;
        for (BlockPos pa : a)
            for (BlockPos pb : b) {
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

    // ====== Нормализация высот с логированием ======
    private record NormalizeResult(List<BlockPos> path, int spikesCut, int gradClamped) {
    }

    private record WaterBuoyData(boolean[] waterMask, List<BlockPos> buoys) {
    }

    // ====== Вспомогательные структуры и математика ======
    private record MergeCandidate(String otherKey, double score) {
    }

    private record Convergence(int i, int j) {
    }

    private record Trunk(String key, List<BlockPos> path) {
    }

    private static final class BuildResult {
        final Map<String, List<BlockPos>> legsRaw;
        final Trunk trunkRaw;

        BuildResult(Map<String, List<BlockPos>> legsRaw, Trunk trunkRaw) {
            this.legsRaw = legsRaw;
            this.trunkRaw = trunkRaw;
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

        AABB inflate(int r) {
            return new AABB(x1 - r, z1 - r, x2 + r, z2 + r);
        }

        boolean intersectsInflated(AABB other) {
            return this.x1 <= other.x2 && this.x2 >= other.x1 && this.z1 <= other.z2 && this.z2 >= other.z1;
        }
    }
}
