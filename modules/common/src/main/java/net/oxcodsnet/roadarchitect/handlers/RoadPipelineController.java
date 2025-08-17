package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls execution of the road generation pipeline (platform-agnostic).
 * Fabric / NeoForge должны вызывать публичные onXxx(...) методы ниже,
 * сохраняя точные кейсы из исходного register().
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPipelineController");

    /**
     * Миры, для которых уже отработал INIT по событию генерации спавн-чанка.
     */
    private static final Set<RegistryKey<World>> INITIALIZED = ConcurrentHashMap.newKeySet();

    /**
     * Кеш селекторов (ID и теги) из конфигурации, для быстрых проверок.
     */
    private static final Set<Identifier> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();

    /**
     * Счётчик тиков для периодического триггера.
     */
    private static int tickCounter = 0;

    private RoadPipelineController() {
    }

    /**
     * Вызывать при старте сервера/мода (однократно), чтобы закешировать селекторы.
     */
    public static void init() {
        cacheStructureSelectors();
        tickCounter = 0;
        LOGGER.debug("RoadPipelineController initialized (selectors cached)");
    }

    /**
     * Вызывать при обновлении конфига — перекешируем селекторы.
     */
    public static void refreshStructureSelectorCache() {
        cacheStructureSelectors();
        LOGGER.debug("RoadPipelineController reloaded selectors from config");
    }

    /* ───────────────────────── Точные кейсы из исходного register() ───────────────────────── */

    /**
     * 1) Генерация спавн-чанка ВПЕРВЫЕ → INIT.
     */
    public static void onSpawnChunkGenerated(ServerWorld world, Chunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;

        ChunkPos spawnChunk = new ChunkPos(world.getSpawnPos());
        if (!chunk.getPos().equals(spawnChunk)) return;

        if (INITIALIZED.add(world.getRegistryKey())) {
            LOGGER.debug("Spawn chunk {} generated in {}, starting INIT pipeline",
                    chunk.getPos(), world.getRegistryKey().getValue());
            PipelineRunner.runPipeline(world, world.getSpawnPos(), PipelineRunner.PipelineMode.INIT);
        }
    }

    /**
     * 2) Генерация ЛЮБОГО чанка; если внутри есть целевая структура → CHUNK.
     */
    public static void onChunkGenerated(ServerWorld world, Chunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) return;
        if (!containsTargetStructure(world, chunk)) return;

        BlockPos center = chunk.getPos().getCenterAtY(0);
        LOGGER.debug("Chunk {} generated with target structure, starting CHUNK pipeline", chunk.getPos());
        PipelineRunner.runPipeline(world, center, PipelineRunner.PipelineMode.CHUNK);
    }

    /**
     * 3) Игрок вошёл на сервер → PERIODIC (как в исходнике).
     */
    public static void onPlayerJoin(ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) player.getWorld();
        if (world.getRegistryKey() != World.OVERWORLD) return;

        BlockPos pos = player.getBlockPos();
        LOGGER.debug("Player {} joined at {}, starting PERIODIC pipeline",
                player.getName().getString(), pos);
        PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.PERIODIC);
    }

    /**
     * 4) Периодический триггер раз в N секунд (из конфига) – START_SERVER_TICK.
     */
    public static void onServerTick(MinecraftServer server) {
        int intervalTicks = Math.max(1, RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20);
        tickCounter++;
        if (tickCounter < intervalTicks) return;
        tickCounter = 0;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            World w = player.getWorld();
            if (w.getRegistryKey() != World.OVERWORLD) continue;

            BlockPos pos = player.getBlockPos();
            LOGGER.debug("Periodic trigger at player {} pos {}, starting PERIODIC pipeline",
                    player.getName().getString(), pos);
            PipelineRunner.runPipeline((ServerWorld) w, pos, PipelineRunner.PipelineMode.PERIODIC);
        }
    }

    /**
     * 5) Остановка сервера → чистим флаг и состояние контроллера.
     */
    public static void onServerStopping() {
        INITIALIZED.clear();
        tickCounter = 0;
        LOGGER.debug("Server stopping, state cleared");
    }

    /* ─────────────────────────── Вспомогательное ─────────────────────────── */

    private static void cacheStructureSelectors() {
        TARGET_IDS.clear();
        TARGET_TAGS.clear();
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        for (String sel : selectors) {
            if (sel.startsWith("#")) {
                TARGET_TAGS.add(TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(sel.substring(1))));
            } else {
                TARGET_IDS.add(Identifier.of(sel));
            }
        }
    }

    private static boolean containsTargetStructure(ServerWorld world, Chunk chunk) {
        if (!chunk.hasStructureReferences()) {
            return false;
        }

        Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        for (StructureStart start : chunk.getStructureStarts().values()) {
            Structure structure = start.getStructure();
            Identifier id = registry.getId(structure);
            if (id != null && TARGET_IDS.contains(id)) {
                return true;
            }
            RegistryEntry<Structure> entry = registry.getEntry(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.isIn(tag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
