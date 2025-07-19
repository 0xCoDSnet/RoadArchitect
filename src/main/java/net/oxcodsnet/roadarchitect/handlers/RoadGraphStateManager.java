package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Менеджер загрузки и сохранения состояния графа дорог при событиях Fabric.
 */
public class RoadGraphStateManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    /**
     * Регистрирует слушатели для автоматической загрузки и сохранения RoadGraphState.
     */
    public static void register() {
        // Предзагрузка состояния при загрузке мира
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.isClient()) {
                RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
                LOGGER.info("RoadGraphState loaded for world {}", world.getRegistryKey().getValue());
            }
        });

        // Сохранение состояния при выгрузке мира
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClient()) {
                RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
                state.markDirty();
                LOGGER.info("Saved RoadGraphState for world {} on unload", world.getRegistryKey().getValue());
            }
        });

        // Сохранение состояния при остановке сервера
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
                state.markDirty();
            }
            LOGGER.info("Server stopping, all RoadGraphStates marked dirty");
        });
    }
}

