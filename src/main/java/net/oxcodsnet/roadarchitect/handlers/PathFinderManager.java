package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.AsyncExecutor;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            RoadArchitect.MOD_ID + "/PathFinderManager"
    );

    public static void computePaths(ServerWorld world, int preFillCacheZone, int maxSteps) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage storage = PathStorage.get(world);

        // warm up caches once
        // CacheManager.prefill(world, -preFillCacheZone, -preFillCacheZone, preFillCacheZone,  preFillCacheZone);
        PathFinder finder = new PathFinder(graph.nodes(), world, maxSteps);

        List<CompletableFuture<PathJob>> futures = new ArrayList<>();
        for (Map.Entry<String, EdgeStorage.Status> entry : graph.edges().allWithStatus().entrySet()) {
            if (entry.getValue() != EdgeStorage.Status.NEW) continue;
            String edgeId = entry.getKey();
            String[] nodes = edgeId.split("\\+", 2);
            if (nodes.length != 2) {
                LOGGER.debug("Invalid edge id: {}", edgeId);
                continue;
            }
            String from = nodes[0], to = nodes[1];

            CompletableFuture<PathJob> job = AsyncExecutor.submit(() -> {
                long start = System.nanoTime();
                List<BlockPos> path = finder.findPath(from, to);
                double ms = (System.nanoTime() - start) / 1_000_000.0;
                return new PathJob(edgeId, from, to, path, ms);
            });
            futures.add(job);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        for (CompletableFuture<PathJob> future : futures) {
            try {
                PathJob job = future.get();
                PathStorage.Status st = job.path().isEmpty() ? PathStorage.Status.FAILED : PathStorage.Status.PENDING;
                storage.putPath(job.from(), job.to(), job.path(), st);
                if (!job.path().isEmpty()) {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.SUCCESS);
                    LOGGER.info(
                            ">>> Computed path {} ({} ms)",
                            job.edgeId(), job.durationMs()
                    );
                } else {
                    graph.edges().setStatus(job.edgeId(), EdgeStorage.Status.FAILURE);
                    LOGGER.info(
                            "! No path for {} ({} ms)",
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
    }

    // overloads for backwards compatibility
    public static void computePaths(ServerWorld world) {
        computePaths(world, 50, 10480 * 2);
    }

    public static void computePaths(
            ServerWorld world, int preFillCacheZone
    ) {
        computePaths(world, preFillCacheZone, 10480 * 2);
    }

    private record PathJob(
            String edgeId, String from, String to, List<BlockPos> path, double durationMs
    ) {
    }
}