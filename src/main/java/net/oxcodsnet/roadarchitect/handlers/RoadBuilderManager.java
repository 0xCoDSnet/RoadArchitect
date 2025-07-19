package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.builder.RoadBuilder;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages {@link RoadBuilder} instances and starts construction when paths are ready.
 */
public class RoadBuilderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    private static final Map<ServerWorld, Map<String, RoadBuilder>> RUNNING = new ConcurrentHashMap<>();

    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> onWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> RUNNING.remove(world));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RUNNING.clear());
        ServerTickEvents.END_WORLD_TICK.register(RoadBuilderManager::tick);
    }

    private static void onWorldLoad(ServerWorld world) {
        if (world.isClient()) return;
        RUNNING.put(world, new ConcurrentHashMap<>());
    }

    private static void tick(ServerWorld world) {
        if (world.isClient()) return;

        Map<String, RoadBuilder> builders = RUNNING.computeIfAbsent(world, w -> new ConcurrentHashMap<>());
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        PathStorage paths = PathStorage.get(world);
        RoadBuilderStorage tasks = RoadBuilderStorage.get(world);

        // ensure tasks exist for all processed paths
        Map<String, Map<String, EdgeStorage.Status>> edges = graph.edges().allWithStatus();
        for (Map.Entry<String, Map<String, EdgeStorage.Status>> entry : edges.entrySet()) {
            String from = entry.getKey();
            for (Map.Entry<String, EdgeStorage.Status> e : entry.getValue().entrySet()) {
                String to = e.getKey();
                if (e.getValue() != EdgeStorage.Status.PROCESSED) continue;
                if (from.compareTo(to) >= 0) continue;
                String key = RoadBuilderStorage.makeKey(from, to);
                if (!tasks.hasTask(key)) {
                    List<BlockPos> path = paths.getPath(from, to);
                    if (!path.isEmpty()) {
                        tasks.addTask(from, to, 0);
                        LOGGER.info("Queued road construction {} -> {} ({} steps)", from, to, path.size());
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> entry : tasks.tasks()) {
            String key = entry.getKey();
            RoadBuilder builder = builders.get(key);
            if (builder == null) {
                String[] ids = key.split("\\|");
                List<BlockPos> path = paths.getPath(ids[0], ids[1]);
                builder = new RoadBuilder(world, path);
                builders.put(key, builder);
            }

            int start = entry.getValue();
            int end = Math.min(start + 1, builder.length());
            builder.buildSegment(start, end);

            if (end >= builder.length()) {
                builders.remove(key);
                tasks.removeTask(key);
                LOGGER.info("Road construction completed between {}", key);
            } else {
                tasks.updateProgress(key, end);
            }
        }
    }
}
