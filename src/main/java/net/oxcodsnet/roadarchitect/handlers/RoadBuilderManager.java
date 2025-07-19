package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.builder.RoadBuilder;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Отвечает за создание {@link RoadBuilder} и запуск строительства дорог, когда пути готовы.
 * <p>Manages {@link RoadBuilder} instances and starts construction when paths are ready.</p>
 */
public class RoadBuilderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    private static final Map<ServerWorld, Map<String, RoadBuilder>> RUNNING = new ConcurrentHashMap<>();

    /**
     * Регистрирует обработчики событий инициализации и генерации чанков.
     * <p>Registers event handlers for world loading and chunk generation.</p>
     */
    public static void register() {
        ServerWorldEvents.LOAD.register((server, world) -> onWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> RUNNING.remove(world));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RUNNING.clear());
        ServerChunkEvents.CHUNK_GENERATE.register(RoadBuilderManager::onChunkGenerated);
    }

    /**
     * Создает карту строителей для загруженного мира.
     * <p>Initializes builder tracking for the loaded world.</p>
     */
    private static void onWorldLoad(ServerWorld world) {
        if (world.isClient()) return;
        RUNNING.put(world, new ConcurrentHashMap<>());
    }

    /**
     * При генерации чанка запускает строительство всех отложенных сегментов.
     * <p>When a chunk generates, builds all queued segments within it.</p>
     */
    private static void onChunkGenerated(ServerWorld world, net.minecraft.world.chunk.Chunk chunk) {
        if (world.isClient()) return;

        RoadBuilderStorage tasks = RoadBuilderStorage.get(world);
        PathStorage paths = PathStorage.get(world);
        Map<String, RoadBuilder> builders = RUNNING.computeIfAbsent(world, w -> new ConcurrentHashMap<>());

        for (RoadBuilderStorage.SegmentEntry segment : List.copyOf(tasks.getSegments(chunk.getPos()))) {
            String key = segment.pathKey();
            RoadBuilder builder = builders.computeIfAbsent(key, k -> {
                String[] ids = k.split("\\|");
                List<BlockPos> path = paths.getPath(ids[0], ids[1]);
                return new RoadBuilder(world, path);
            });

            builder.buildSegment(segment.start(), segment.end());
            tasks.removeSegment(chunk.getPos(), segment);
            LOGGER.info("Road segment {} built in chunk {}", key, chunk.getPos());
        }
    }
}
