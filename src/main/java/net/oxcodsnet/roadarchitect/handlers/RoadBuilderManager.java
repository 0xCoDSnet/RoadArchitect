package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tracks road-building tasks for loaded chunks.
 * <p>Road segments are generated via {@link net.oxcodsnet.roadarchitect.worldgen.RoadFeature}.</p>
 */
public class RoadBuilderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID+"/RoadBuilderManager");

    /**
     * Регистрирует обработчики событий инициализации и генерации чанков.
     * <p>Registers event handlers for world loading and chunk generation.</p>
     */
    public static void register() {
        ServerChunkEvents.CHUNK_LOAD.register(RoadBuilderManager::onChunkLoaded);
    }

    /**
     * Called when a chunk is loaded. Actual road placement is handled by the
     * {@link net.oxcodsnet.roadarchitect.worldgen.RoadFeature} during generation.
     */
    private static void onChunkLoaded(ServerWorld world, net.minecraft.world.chunk.Chunk chunk) {
        if (world.isClient()) return;
        RoadBuilderStorage storage = RoadBuilderStorage.get(world);
        if (!storage.getSegments(chunk.getPos()).isEmpty()) {
            LOGGER.debug("Chunk {} has scheduled road segments", chunk.getPos());
        }
    }
}
