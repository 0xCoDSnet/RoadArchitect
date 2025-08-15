package net.oxcodsnet.roadarchitect.neoforge.events;

import net.minecraft.server.world.ServerWorld;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.oxcodsnet.roadarchitect.handlers.RoadGraphStateManager;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import net.oxcodsnet.roadarchitect.util.CacheManager;

public final class NeoForgeEventBridge {

    private NeoForgeEventBridge() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.register(NeoForgeEventBridge.class);
    }

    @SubscribeEvent
    private static void onWorldLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphStateManager.onWorldLoad(world);
            CacheManager.onWorldLoad(world);
        }
    }

    @SubscribeEvent
    private static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadGraphStateManager.onWorldUnload(world);
            CacheManager.onWorldUnload(world);
        }
    }

    @SubscribeEvent
    private static void onServerStopping(ServerStoppingEvent event) {
        RoadGraphStateManager.onServerStopping(event.getServer());
        CacheManager.onServerStopping(event.getServer());
    }

    @SubscribeEvent
    private static void onLevelTick(LevelTickEvent.Pre event) {
        if (event.getLevel() instanceof ServerWorld world) {
            RoadPostProcessor.onStartWorldTick(world);
        }
    }
}
