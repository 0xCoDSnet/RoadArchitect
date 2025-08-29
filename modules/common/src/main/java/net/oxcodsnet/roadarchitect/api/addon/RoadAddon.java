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
     * Called once during addon bootstrap to perform setup.
     */
    void onRegister();

    /**
     * New registration callback with context. Default delegates to legacy {@link #onRegister()} for
     * backwards compatibility. Addons can override this to access persistent storage and services.
     */
    default void onRegister(AddonContext ctx) {
        onRegister();
    }

    /**
     * Called when a road path is finalized and marked READY by the post-processor.
     * @param world server world
     * @param pathKey key used by PathStorage
     * @param refinedPath final refined path points
     */
    default void onPathReady(ServerWorld world, String pathKey, List<BlockPos> refinedPath) {
        // optional
    }

    /**
     * Called each server tick from the platform event bridge.
     */
    default void onServerTick(net.minecraft.server.MinecraftServer server) {
        // optional
    }

    /**
     * Called when a chunk is loaded.
     */
    default void onChunkLoad(ServerWorld world, net.minecraft.util.math.ChunkPos pos) {
        // optional
    }
}
