package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Платформо-независимые хуки жизненного цикла для состояния графа дорог.
 * Регистрация на реальные события выполняется в fabric / neoforge модулях.
 */
public final class RoadGraphStateManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateManager");

    private RoadGraphStateManager() {
    }

    /**
     * Вызывается платформой при загрузке ServerWorld.
     */
    public static void onWorldLoad(ServerWorld world) {
        // Гарантируем, что стейт создан/поднят
        RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        LOGGER.debug("RoadGraphState loaded for world {}", world.getRegistryKey().getValue());
    }

    /**
     * Вызывается платформой при выгрузке ServerWorld.
     */
    public static void onWorldUnload(ServerWorld world) {
        RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        state.markDirty();
        LOGGER.debug("Saved RoadGraphState for world {} on unload", world.getRegistryKey().getValue());
    }

    /**
     * Вызывается платформой при остановке сервера (до закрытия миров).
     */
    public static void onServerStopping(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance()).markDirty();
        }
        LOGGER.debug("Server stopping, all RoadGraphStates marked dirty");
    }
}
