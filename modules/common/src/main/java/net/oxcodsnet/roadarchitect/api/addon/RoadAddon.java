package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Addon contract for Road Architect.
 * Implementations can register trigger types and react to pipeline events.
 */
public interface RoadAddon {
    /**
     * Unique addon id, e.g. {@code roadarchitect:ambush}.
     */
    Identifier id();

    /**
     * Called once during addon bootstrap to register trigger types and perform setup.
     * Use {@link RoadAddons#registerTriggerType(TriggerType)} to expose triggers.
     */
    void onRegister();

    /**
     * Called when a road path is finalized and marked READY by the post-processor.
     * Addons can inspect the path and place markers using {@link RoadAddons#placeMarker}.
     *
     * @param world server world
     * @param pathKey key used by PathStorage
     * @param refinedPath final refined path points
     */
    default void onPathReady(ServerWorld world, String pathKey, List<BlockPos> refinedPath) {
        // optional
    }
}

