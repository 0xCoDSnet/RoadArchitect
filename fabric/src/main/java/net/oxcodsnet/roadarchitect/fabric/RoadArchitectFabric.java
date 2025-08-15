package net.oxcodsnet.roadarchitect.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.fabric.config.RAConfigFabricBridge;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;
import net.oxcodsnet.roadarchitect.handlers.PipelineRunner;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class RoadArchitectFabric implements ModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/Fabric");

    // чтобы не делать «тяжёлый» init много раз на каждом join
    private static final Set<RegistryKey<World>> INIT_DONE = ConcurrentHashMap.newKeySet();

    // для периодического пайплайна по интервалу
    private static final Map<RegistryKey<World>, Long> LAST_PERIODIC = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        // общий init мода (без завязки на платформу)
        RoadArchitect.init();

        // мост к owo-конфигу -> наполняет RAConfigHolder в common
        RAConfigFabricBridge.bootstrap();

        // === WORLD LIFECYCLE ===
        ServerWorldEvents.LOAD.register((server, world) -> {
            // «притрагиваемся» к стейту, чтобы создался и подхватился PersistentState
            RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
            LOGGER.info("[RoadArchitect] World loaded: {}", world.getRegistryKey().getValue());
        });

        ServerWorldEvents.UNLOAD.register((server, world) -> {
            RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance()).markDirty();
            INIT_DONE.remove(world.getRegistryKey());
            LAST_PERIODIC.remove(world.getRegistryKey());
            LOGGER.info("[RoadArchitect] World unloaded: {}", world.getRegistryKey().getValue());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            for (ServerWorld world : server.getWorlds()) {
                RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance()).markDirty();
            }
        });

        // === PLAYER JOIN -> одноразовый init-проход по миру ===
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerWorld world = handler.getPlayer().getServerWorld();
            if (world.getRegistryKey() != World.OVERWORLD) return;

            RegistryKey<World> key = world.getRegistryKey();
            if (INIT_DONE.add(key)) {
                BlockPos pos = handler.getPlayer().getBlockPos();
                PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.INIT); // общая логика из common
                LAST_PERIODIC.put(key, System.currentTimeMillis());
                LOGGER.info("[RoadArchitect] INIT pipeline at {} in {}", pos, key.getValue());
            }
        });

        // === ПЕРИОДИЧЕСКИЙ ПАЙПЛАЙН ПО ТИКУ СЕРВЕРА ===
        ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

        // === РЕАКЦИЯ НА ЗАГРУЗКУ ЧАНКА ===
        // В Fabric есть события CHUNK_LOAD/UNLOAD — отдельного «generate» нет, это норм.
        // (см. общий индекс событий) :contentReference[oaicite:1]{index=1}
        ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            ChunkPos cp = chunk.getPos();
            // центр чанка на уровне моря — нам нужен только якорь для старта пайплайна
            BlockPos center = new BlockPos(cp.getStartX() + 8, world.getSeaLevel(), cp.getStartZ() + 8);
            PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.CHUNK);
        });

        // === ПОСЛЕ-ОБРАБОТКА ПО ТИКУ МИРА ===
        // В оригинале это делалось на START_WORLD_TICK — оставляем также
        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                RoadPostProcessor.processPending((ServerWorld) world);
            }
        });

        // === РЕГИСТРАЦИЯ WORLDGEN-хука (биом-модификация) ===
        // Работает, если в датапаке есть placed_feature `roadarchitect:road`.
        // Если позже решим регать feature программно — перенесём сюда bootstrap.
        //RoadFeatureRegistry.register();

        LOGGER.info("[RoadArchitect] Fabric bootstrap complete");
    }

    private void onServerTick(MinecraftServer server) {
        final int intervalSec = RoadArchitect.CONFIG.pipelineIntervalSeconds();
        final long now = System.currentTimeMillis();

        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey() != World.OVERWORLD) continue;

            RegistryKey<World> key = world.getRegistryKey();
            long last = LAST_PERIODIC.getOrDefault(key, 0L);

            if (now - last >= intervalSec * 1000L) {
                ServerPlayerEntity anyPlayer = world.getPlayers().isEmpty() ? null : world.getPlayers().getFirst();
                if (anyPlayer != null) {
                    PipelineRunner.runPipeline(world, anyPlayer.getBlockPos(), PipelineRunner.PipelineMode.PERIODIC);
                    LAST_PERIODIC.put(key, now);
                }
            }
        }
    }
}
