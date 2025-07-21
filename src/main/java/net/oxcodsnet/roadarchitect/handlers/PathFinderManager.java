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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Управляет вычислением путей между узлами при различных событиях сервера.
 * <p>Handles path calculation between nodes on various server events.</p>
 */
public class PathFinderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathFinderManager");

    /**
     * Выполняет расчёт всех путей и возвращает найденные маршруты.
     * <p>Computes all paths and returns the resulting paths.</p>
     */
    static Map<String, List<BlockPos>> computePaths(ServerWorld world) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage storage = PathStorage.get(world);
        PathFinder finder = new PathFinder(graph.nodes(), new PathFinder.WorldSurfaceProvider(world), world, 10240);

        Map<String, List<BlockPos>> result = new HashMap<>();

        for (Map.Entry<String, Map<String, EdgeStorage.Status>> entry : graph.edges().allWithStatus().entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, EdgeStorage.Status> edge : entry.getValue().entrySet()) {
                if (edge.getValue() == EdgeStorage.Status.NEW && from.compareTo(edge.getKey()) < 0) {
                    List<BlockPos> path = finder.findPath(from, edge.getKey());
                    storage.putPath(from, edge.getKey(), path);
                    graph.edges().setStatus(from, edge.getKey(), EdgeStorage.Status.PROCESSED);

                    if (!path.isEmpty()) {
                        String key = RoadBuilderStorage.makeKey(from, edge.getKey());
                        result.put(key, path);
                        LOGGER.info("Computed path {} -> {} ({} steps)", from, edge.getKey(), path.size());
                    }
                }
            }
        }

        storage.markDirty();
        graph.markDirty();
        LOGGER.info("Path calculation completed for world {}", world.getRegistryKey().getValue());
        return result;
    }
}