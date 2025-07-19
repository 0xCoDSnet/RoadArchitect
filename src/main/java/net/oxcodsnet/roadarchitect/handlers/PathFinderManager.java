package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID+"/PathFinderManager");
    private static ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * Регистрирует слушателей событий для запуска поиска путей.
     * <p>Registers all event listeners that trigger path finding.</p>
     */
    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> onWorldEvent(world));
        ServerPlayerEvents.JOIN.register(player -> onWorldEvent(player.getServerWorld()));
        ServerWorldEvents.UNLOAD.register((server, world) -> EXECUTOR.shutdownNow());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EXECUTOR.shutdownNow());
    }

    /**
     * Обработчик загрузки мира или входа игрока.
     * <p>Handles world load or player join events.</p>
     */
    private static void onWorldEvent(ServerWorld world) {
        if (world.isClient()) return;
        if (EXECUTOR.isShutdown()) {
            EXECUTOR = Executors.newSingleThreadExecutor();
        }
        EXECUTOR.submit(() -> computePaths(world));
    }

    /**
     * Выполняет расчёт всех путей и формирует задачи на строительство.
     * <p>Computes all paths and schedules road building tasks.</p>
     */
    private static void computePaths(ServerWorld world) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage storage = PathStorage.get(world);
        RoadBuilderStorage tasks = RoadBuilderStorage.get(world);
        PathFinder finder = new PathFinder(graph.nodes(), graph.edges(), new PathFinder.WorldSurfaceProvider(world));

        for (Map.Entry<String, Map<String, EdgeStorage.Status>> entry : graph.edges().allWithStatus().entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, EdgeStorage.Status> edge : entry.getValue().entrySet()) {
                if (edge.getValue() == EdgeStorage.Status.NEW && from.compareTo(edge.getKey()) < 0) {
                    List<BlockPos> path = finder.findPath(from, edge.getKey());
                    storage.putPath(from, edge.getKey(), path);
                    graph.edges().setStatus(from, edge.getKey(), EdgeStorage.Status.PROCESSED);

                    if (!path.isEmpty()) {
                        String key = RoadBuilderStorage.makeKey(from, edge.getKey());
                        int i = 0;
                        while (i < path.size()) {
                            ChunkPos chunk = new ChunkPos(path.get(i));
                            int start = i;
                            do {
                                i++;
                            } while (i < path.size() && new ChunkPos(path.get(i)).equals(chunk));
                            tasks.addSegment(chunk, key, start, i);
                        }
                        LOGGER.info("Queued road construction {} -> {} ({} steps)", from, edge.getKey(), path.size());
                    }
                }
            }
        }
        storage.markDirty();
        graph.markDirty();
        LOGGER.info("Path calculation completed for world {}", world.getRegistryKey().getValue());
    }
}