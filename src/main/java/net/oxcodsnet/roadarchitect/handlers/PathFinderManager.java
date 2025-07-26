package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.CacheManager;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RoadArchitect.MOD_ID + "/PathFinderManager"
    );

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static final ForkJoinPool PATH_EXECUTOR = new ForkJoinPool(CORES);

    private record PathJob(
            String edgeId, String from, String to, List<BlockPos> path, double durationMs
    ) {}

    public static Map<String, List<BlockPos>> computePaths(ServerWorld world, int preFillCacheZone, int maxSteps) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage storage = PathStorage.get(world);

        // warm up caches once
        CacheManager.prefill(world, -preFillCacheZone, -preFillCacheZone, preFillCacheZone,  preFillCacheZone);
        PathFinder finder = new PathFinder(graph.nodes(), world, maxSteps);

        List<CompletableFuture<PathJob>> futures = new ArrayList<>();
        for (var entry : graph.edges().allWithStatus().entrySet()) {
            if (entry.getValue() != EdgeStorage.Status.NEW) continue;
            String edgeId = entry.getKey();
            String[] nodes = edgeId.split("\\+", 2);
            if (nodes.length != 2) {
                LOGGER.debug("Invalid edge id: {}", edgeId);
                continue;
            }
            String from = nodes[0], to = nodes[1];

            CompletableFuture<PathJob> job = CompletableFuture.supplyAsync(() -> {
                long start = System.nanoTime();
                List<BlockPos> path = finder.findPath(from, to);
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                return new PathJob(edgeId, from, to, path, ms);
            }, PATH_EXECUTOR);
            futures.add(job);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Map<String, List<BlockPos>> result = new HashMap<>();
        for (var future : futures) {
            try {
                PathJob job = future.get();
                storage.putPath(job.from(), job.to(), job.path());
                if (!job.path().isEmpty()) {
                    result.put(RoadBuilderStorage.makeKey(
                            job.from(), job.to()), job.path()
                    );
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.SUCCESS);
                    LOGGER.debug(
                            ">>> Computed path {} ({} steps)",
                            job.edgeId(), job.path().size()
                    );
                } else {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.FAILURE);
                    LOGGER.debug(
                            "!<No path for {}>! Duration: {} ms",
                            job.edgeId(), job.durationMs()
                    );
                }
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("Path computation failed", e);
                Thread.currentThread().interrupt();
            }
        }

        storage.markDirty();
        graph.markDirty();
        LOGGER.debug("Path calculation completed for world {}",
                world.getRegistryKey().getValue()
        );
        return result;
    }

    // overloads for backwards compatibility
    public static Map<String, List<BlockPos>> computePaths(ServerWorld world) {
        return computePaths(world, 50, 10480 * 2);
    }

    public static Map<String, List<BlockPos>> computePaths(
            ServerWorld world, int preFillCacheZone
    ) {
        return computePaths(world, preFillCacheZone, 10480 * 2);
    }
}