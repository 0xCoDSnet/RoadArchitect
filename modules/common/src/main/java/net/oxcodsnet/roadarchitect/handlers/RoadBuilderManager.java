package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Tracks road-building tasks for loaded chunks.
 * <p>Road segments are generated via {@link net.oxcodsnet.roadarchitect.worldgen.RoadFeature}.</p>
 */
public class RoadBuilderManager {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadBuilderManager.class.getSimpleName());

    /**
     * Формирует задачи на строительство для переданных путей.
     * <p>Queues building tasks for the given paths.</p>
     */
    static void queueSegments(ServerWorld world, Map<String, List<BlockPos>> paths) {
        RoadBuilderStorage storage = RoadBuilderStorage.get(world);

        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String key = entry.getKey();
            List<BlockPos> path = entry.getValue();
            int i = 0;
            while (i < path.size()) {
                ChunkPos chunk = new ChunkPos(path.get(i));
                int start = i;
                do {
                    i++;
                } while (i < path.size() && new ChunkPos(path.get(i)).equals(chunk));
                storage.addSegment(chunk, key, start, i);
            }
            LOGGER.debug("Queued road construction {} ({} steps)", key, path.size());
        }
    }
}
