package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.oxcodsnet.roadarchitect.handlers.RoadGraphStateManager;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import net.oxcodsnet.roadarchitect.util.CacheManager;

public final class FabricEventBridge {

    private FabricEventBridge() {}

    public static void register() {

        // ===================== RoadGraphStateManager =====================
        ServerWorldEvents.LOAD.register((server, world) -> RoadGraphStateManager.onWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> RoadGraphStateManager.onWorldUnload(world));
        ServerLifecycleEvents.SERVER_STOPPING.register(RoadGraphStateManager::onServerStopping);

        // ===================== PostProcessor =====================
        ServerTickEvents.START_WORLD_TICK.register(RoadPostProcessor::onStartWorldTick);

        // ===================== CacheManager =====================
        ServerWorldEvents.LOAD.register((server, world) -> CacheManager.onWorldLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> CacheManager.onWorldUnload(world));
        ServerLifecycleEvents.SERVER_STOPPING.register(CacheManager::onServerStopping);
    }
}
