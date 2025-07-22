package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinderManager");

    private static final ExecutorService PATH_EXECUTOR = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("ra-path-", 0).factory());
    private record PathJob(String from, String to, List<BlockPos> path, double durationMs) { }

    static Map<String, List<BlockPos>> computePaths(ServerWorld world) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage   storage = PathStorage.get(world);

        // один потокобезопасный PathFinder, общий для всех задач
        PathFinder finder = new PathFinder(graph.nodes(), world, 2048*2);
        finder.prefillHeightCache(-500, -500, 500, 500);

        List<CompletableFuture<PathJob>> futures = new ArrayList<>();

        for (Map.Entry<String, Map<String, EdgeStorage.Status>> entry : graph.edges().allWithStatus().entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, EdgeStorage.Status> edge : entry.getValue().entrySet()) {
                String to = edge.getKey();
                if (edge.getValue() == EdgeStorage.Status.NEW && from.compareTo(to) < 0) {
                    // запускаем асинхронный поиск
                    futures.add(CompletableFuture.supplyAsync(() -> {
                        long start = System.nanoTime();
                        List<BlockPos> path = finder.findPath(from, to);
                        double ms = (System.nanoTime() - start) / 1_000_000.0;
                        return new PathJob(from, to, path, ms);
                    }, PATH_EXECUTOR));
                }
            }
        }

        // ждём все поиски
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        Map<String, List<BlockPos>> result = new HashMap<>();

        // обрабатываем результаты на серверном потоке
        for (CompletableFuture<PathJob> future : futures) {
            try {
                PathJob job = future.get();
                storage.putPath(job.from(), job.to(), job.path());
                graph.edges().setStatus(job.from(), job.to(), EdgeStorage.Status.PROCESSED);

                if (!job.path().isEmpty()) {
                    String key = RoadBuilderStorage.makeKey(job.from(), job.to());
                    result.put(key, job.path());
                    LOGGER.info(">>> Computed path {} -> {} ({} steps)", job.from(), job.to(), job.path().size());
                    LOGGER.info(">>> Время выполнения: {} мс", job.durationMs());
                } else {
                    LOGGER.info("!<Не найден путь {} -> {}>! Время выполнения: {} мс",
                            job.from(), job.to(), job.durationMs());
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Path computation failed", e);
                Thread.currentThread().interrupt();
            }
        }

        storage.markDirty();
        graph.markDirty();
        LOGGER.info("Path calculation completed for world {}", world.getRegistryKey().getValue());
        return result;
    }

//    /**
//     * Выполняет расчёт всех путей и возвращает найденные маршруты.
//     * <p>Computes all paths and returns the resulting paths.</p>
//     */
//
//
//    static Map<String, List<BlockPos>> computePaths(ServerWorld world) {
//        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
//        PathStorage storage = PathStorage.get(world);
//        PathFinder finder = new PathFinder(graph.nodes(), world, 1024);
//        finder.prefillHeightCache(-500, -500, 500, 500);
//
//        Map<String, List<BlockPos>> result = new HashMap<>();
//
//        for (Map.Entry<String, Map<String, EdgeStorage.Status>> entry : graph.edges().allWithStatus().entrySet()) {
//            String from = entry.getKey();
//            for (Map.Entry<String, EdgeStorage.Status> edge : entry.getValue().entrySet()) {
//                if (edge.getValue() == EdgeStorage.Status.NEW && from.compareTo(edge.getKey()) < 0) {
//
//                    long start = System.nanoTime();
//                    List<BlockPos> path = finder.findPath(from, edge.getKey());
//                    long end = System.nanoTime();
//                    double durationMs = (end - start) / 1_000_000.0;
//
//                    storage.putPath(from, edge.getKey(), path);
//                    graph.edges().setStatus(from, edge.getKey(), EdgeStorage.Status.PROCESSED);
//
//                    if (!path.isEmpty()) {
//                        String key = RoadBuilderStorage.makeKey(from, edge.getKey());
//                        result.put(key, path);
//                        LOGGER.info(">>> Computed path {} -> {} ({} steps)", from, edge.getKey(), path.size());
//                        LOGGER.info(">>> Время выполнения: {} мс", durationMs);
//                    }
//                    else {
//                        LOGGER.info("!<Не найден путь>! Время выполнения: {} мс", durationMs);
//                    }
//                }
//            }
//        }
//
//        storage.markDirty();
//        graph.markDirty();
//        LOGGER.info("Path calculation completed for world {}", world.getRegistryKey().getValue());
//        return result;
//    }
}