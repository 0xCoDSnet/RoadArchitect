package net.oxcodsnet.roadarchitect.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentStateType;

/**
 * Utility methods for working with {@link PersistentState}.
 */
public final class PersistentStateUtil {
    private PersistentStateUtil() {
    }

    /**
     * Retrieves or creates a {@link PersistentState} for the given world.
     *
     * @param world the server world
     * @param type  the state type
     * @return existing or newly created persistent state
     */
    public static <T extends PersistentState> T get(ServerWorld world, PersistentStateType<T> type) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(type);
    }
}
