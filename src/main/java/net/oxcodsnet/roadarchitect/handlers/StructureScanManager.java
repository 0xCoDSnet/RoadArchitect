package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.StructureLocator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Менеджер запуска сканирования структур.
 * <p>Manager responsible for initiating structure scans.</p>
 */
public class StructureScanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);
    // Флаги, чтобы сканирования выполнились только один раз per world
    private static final Set<RegistryKey<World>> chunkScanDone = ConcurrentHashMap.newKeySet();

    /**
     * Регистрирует все необходимые слушатели.
     * <p>Registers required event listeners. Call from
     * {@link net.oxcodsnet.roadarchitect.RoadArchitect#onInitialize()}.</p>
     */
    public static void register() {
        // После генерации спавн чанка
        ServerChunkEvents.CHUNK_GENERATE.register(StructureScanManager::onChunkGenerated);
    }


    /**
     * Событие генерации чанка. Запускаем сканирование, когда сгенерирован спавн-чанк.
     * <p>Triggered on chunk generation to start scanning once the spawn chunk is ready.</p>
     */
    private static void onChunkGenerated(ServerWorld world, Chunk chunk) {
        if (world.isClient) return;
        if (!world.getRegistryKey().equals(World.OVERWORLD)) return;
        if (chunkScanDone.add(world.getRegistryKey())){

            int spawnChunkX = world.getSpawnPos().getX() >> 4;
            int spawnChunkZ = world.getSpawnPos().getZ() >> 4;

            if (chunk.getPos().x == spawnChunkX && chunk.getPos().z == spawnChunkZ) {
                    performScan(world, "ChunkGenerated");
                }
            }
    }




    /**
     * Выполняет сканирование через {@link StructureLocator#scanGrid} и логирует результат.
     * <p>Performs a scan using {@link StructureLocator#scanGrid} and logs the outcome.</p>
     */
    private static void performScan(ServerWorld world, String approach) {
        int overallRadius = RoadArchitect.CONFIG.playerScanRadius();
        int scanRadius = 1;
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        BlockPos center = world.getSpawnPos();

        LOGGER.info("[{}] Запуск сканирования: overallRadius={}, scanRadius={}, selectors={}",
                approach, overallRadius, scanRadius, selectors);

        List<BlockPos> found = StructureLocator.scanGrid(
                world, center, overallRadius, scanRadius, selectors
        );

        LOGGER.info("[{}] Сканирование завершено. Найдено структур: {}", approach, found.size());
    }
}
