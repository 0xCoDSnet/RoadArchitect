package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinderManager");

    private static final ExecutorService PATH_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("ra-path-", 0).factory());

    /**
     * Результат одной задачи поиска пути.
     */
    private record PathJob(String edgeId, String from, String to, List<BlockPos> path, double durationMs) {
    }

    /**
     * Запускает вычисление всех новых (Status.NEW) рёбер на карте и возвращает найденные маршруты.
     * Ключ результата совпадает с {@link RoadBuilderStorage#makeKey(String, String)}.
     */
    static Map<String, List<BlockPos>> computePaths(ServerWorld world) {
        return computePaths(world, 50);
    }

    static Map<String, List<BlockPos>> computePaths(ServerWorld world, int preFillCacheZone) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage storage = PathStorage.get(world);

        // Один thread‑safe экземпляр PathFinder на все задачи
        PathFinder finder = new PathFinder(graph.nodes(), world, 10480*2);
        long startNs_test = System.nanoTime();
        finder.prefillHeightCacheAndBiomeCache(-preFillCacheZone, -preFillCacheZone, preFillCacheZone, preFillCacheZone);
        double ms_test_1 = (System.nanoTime() - startNs_test) / 1_000_000.0;
        LOGGER.warn("prefillHeightCacheAndBiomeCache: {} ms", ms_test_1);

        List<CompletableFuture<PathJob>> futures = new ArrayList<>();

        for (Map.Entry<String, EdgeStorage.Status> entry : graph.edges().allWithStatus().entrySet()) {
            String edgeId = entry.getKey();
            EdgeStorage.Status status = entry.getValue();

            if (status != EdgeStorage.Status.NEW) continue;

            String[] nodes = splitEdgeId(edgeId);
            if (nodes.length != 2) {
                LOGGER.warn("Invalid edge id: {}", edgeId);
                continue;
            }
            String from = nodes[0];
            String to = nodes[1];

            futures.add(CompletableFuture.supplyAsync(() -> {
                long startNs = System.nanoTime();
                List<BlockPos> path = finder.findPath(from, to);
                double ms = (System.nanoTime() - startNs) / 1_000_000.0;
                return new PathJob(edgeId, from, to, path, ms);
            }, PATH_EXECUTOR));
        }

        // Ожидаем завершения всех задач
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        Map<String, List<BlockPos>> result = new HashMap<>();

        for (CompletableFuture<PathJob> future : futures) {
            try {
                PathJob job = future.get();
                storage.putPath(job.from(), job.to(), job.path());
                if (!job.path().isEmpty()) {
                    String key = RoadBuilderStorage.makeKey(job.from(), job.to());
                    result.put(key, job.path());
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.SUCCESS);
                    LOGGER.info(">>> Computed path {} ({} steps)", job.edgeId(), job.path().size());
                    LOGGER.info(">>> Duration: {} ms", job.durationMs());
                } else {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.FAILURE);
                    LOGGER.info("!<No path for {}>! Duration: {} ms", job.edgeId(), job.durationMs());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Path computation failed", e);
                Thread.currentThread().interrupt();
            }
        }

        storage.markDirty();
        graph.markDirty();

        double ms_test_2 = (System.nanoTime() - startNs_test) / 1_000_000.0;
        LOGGER.warn("prefillHeightCacheAndBiomeCache: {} ms", ms_test_2);
        LOGGER.info("Path calculation completed for world {}", world.getRegistryKey().getValue());
        return result;
    }

    /**
     * edgeId имеет формат "min+max" (отсортированная пара UUID).
     */
    private static String[] splitEdgeId(String edgeId) {
        return edgeId.split("\\+", 2);
    }


}