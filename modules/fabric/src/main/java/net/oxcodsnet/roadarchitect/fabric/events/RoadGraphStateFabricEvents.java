package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoadGraphStateFabricEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadGraphStateEvents");

    private RoadGraphStateFabricEvents() {}

    public static void register() {
        // LOAD мира → ensure state инициализирован
        ServerWorldEvents.LOAD.register((server, world) -> {
            if (!world.isClient()) {
                RoadGraphState.get(world);
                LOGGER.debug("RoadGraphState loaded for world {}", world.getRegistryKey().getValue());
            }
        });

        // UNLOAD мира → markDirty
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClient()) {
                RoadGraphState state = RoadGraphState.get(world);
                state.markDirty();
                LOGGER.debug("Saved RoadGraphState for world {} on unload", world.getRegistryKey().getValue());
            }
        });

        // Остановка сервера → markDirty для всех миров
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                RoadGraphState state = RoadGraphState.get(world);
                state.markDirty();
            }
            LOGGER.debug("Server stopping, all RoadGraphStates marked dirty");
        });
    }
}
