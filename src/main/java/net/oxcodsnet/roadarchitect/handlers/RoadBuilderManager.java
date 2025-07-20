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

    //...
}
