package net.oxcodsnet.roadarchitect.handlers;

import com.mojang.datafixers.util.Pair;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.StructureLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Менеджер запуска сканирования структур.
 * <p>Manager responsible for initiating structure scans.</p>
 */
public class StructureScanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    /**
     * Регистрирует все необходимые слушатели.
     * <p>Registers required event listeners. Call from
     * {@link net.oxcodsnet.roadarchitect.RoadArchitect#onInitialize()}.</p>
     */
    public static void register() {
        // После генерации спавн чанка
        ServerChunkEvents.CHUNK_GENERATE.register(StructureScanManager::onChunkGenerated);
        ServerPlayerEvents.JOIN.register(StructureScanManager::onPlayerJoinInOverworld);
    }

    /**
     * Событие захода игрока в мир. Запускаем сканирование, когда игрок заходит в Overworld.
     * <p>The player's entry event in the world. Launch scanning when the player enters Overworld.</p>
     */
    private static void onPlayerJoinInOverworld(ServerPlayerEntity playerEntity) {
        ServerWorld world = playerEntity.getServerWorld();
        if (isNotOverWorld(world)) return;
        performScan(world, "PlayerJoinInOverworld", playerEntity.getBlockPos());
    }

    /**
     * Проверяет, является ли мир не Overworld.
     * <p>Checks whether the given world is not the Overworld.</p>
     *
     * @param world серверный мир / server world
     * @return {@code true} если мир не Overworld / {@code true} if not Overworld
     */
    private static boolean isNotOverWorld(ServerWorld world) {
        if (world.isClient) return true;
        return !world.getRegistryKey().equals(World.OVERWORLD);
    }


    /**
     * Событие генерации чанка. Запускаем сканирование, когда сгенерирован спавн-чанк.
     * <p>Triggered on chunk generation to start scanning once the spawn chunk is ready.</p>
     */
    private static void onChunkGenerated(ServerWorld world, Chunk chunk) {
        if (isNotOverWorld(world)) return;
        int spawnChunkX = world.getSpawnPos().getX() >> 4;
        int spawnChunkZ = world.getSpawnPos().getZ() >> 4;

        if (chunk.getPos().x == spawnChunkX && chunk.getPos().z == spawnChunkZ) {
            performScan(world, "ChunkGenerated", world.getSpawnPos());
        }
    }


    /**
     * Выполняет сканирование через {@link StructureLocator#scanGrid} и логирует результат.
     * <p>Performs a scan using {@link StructureLocator#scanGrid} and logs the outcome.</p>
     */
    private static void performScan(ServerWorld world, String approach, BlockPos center) {
        int overallRadius = RoadArchitect.CONFIG.playerScanRadius();
        int scanRadius = 1;
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();

        LOGGER.info("[{}] Scan launch: overallRadius={}, scanRadius={}, selectors={}",
                approach, overallRadius, scanRadius, selectors);

        List<Pair<BlockPos, String>> found = StructureLocator.scanGrid(
                world, center, overallRadius, scanRadius, selectors
        );

        LOGGER.info("[{}] Scanning is completed. Found structures: {}", approach, found.size());
    }
}
