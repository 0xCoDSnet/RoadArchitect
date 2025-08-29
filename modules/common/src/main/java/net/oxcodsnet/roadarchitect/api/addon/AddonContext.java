package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.oxcodsnet.roadarchitect.api.core.CoreApi;
import net.oxcodsnet.roadarchitect.api.storage.PersistentStore;
import org.slf4j.Logger;

/**
 * Context passed to addons during registration, providing access to
 * addon-scoped persistent storage and utility services.
 */
public interface AddonContext {
    /**
     * The id of the addon being initialized.
     */
    Identifier addonId();

    /**
     * Returns a world-scoped persistent store that is isolated for this addon.
     * Data stored here is saved with the world and only accessible via this API.
     */
    PersistentStore persistent(ServerWorld world);

    /**
     * A logger namespaced to the addon.
     */
    Logger logger();

    /**
     * Read-only access to core Road Architect data structures and snapshots
     * (graph, paths, build queue, decoration masks). All views are safe and
     * return immutable copies where appropriate.
     */
    CoreApi core();
}
