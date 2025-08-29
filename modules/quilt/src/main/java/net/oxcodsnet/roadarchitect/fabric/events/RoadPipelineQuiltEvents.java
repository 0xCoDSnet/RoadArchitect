package net.oxcodsnet.roadarchitect.fabric.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.WorldChunk;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;

/**
 * Fabric-адаптер: подписывается на события Fabric и делегирует в common.
 * Семантика полностью соответствует прежнему register() в RoadPipelineController.
 */
public final class RoadPipelineQuiltEvents {
    private RoadPipelineQuiltEvents() {
    }

    public static void register() {
        // 0) Инициализация контроллера (кеш селекторов)
        RoadPipelineController.init();

        // 1) Первая генерация спавн-чанка (и вообще генерация чанка)
        ServerChunkEvents.CHUNK_LOAD.register((ServerWorld world, WorldChunk chunk) -> {
            RoadPipelineController.onSpawnChunkGenerated(world, chunk);
            RoadPipelineController.onChunkGenerated(world, chunk);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onChunkLoad(world, chunk.getPos());
        });

        // 3) Игрок зашёл
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RoadPipelineController.onPlayerJoin(handler.getPlayer()));

        // 4) Периодика по тикам
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            RoadPipelineController.onServerTick(server);
            net.oxcodsnet.roadarchitect.api.addon.RoadAddons.onServerTick(server);
        });

        // 5) Остановка сервера
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> RoadPipelineController.onServerStopping());
    }
}
