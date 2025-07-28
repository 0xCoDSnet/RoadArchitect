package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controls execution of the road generation pipeline.
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPipelineController");

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Set<World> INITIALIZED = ConcurrentHashMap.newKeySet();
    private static int tickCounter = 0;
    private static final int INTERVAL_TICKS = RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20;

    // Cached selectors for fast matching
    private static final Set<Identifier> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();

    private RoadPipelineController() {}

    /**
     * Registers event hooks for pipeline triggers.
     */
    public static void register() {
        cacheStructureSelectors();

        //TODO: Нужна оптимизация Pipline

        // Первичная инициализация при первом загруженном чанке
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.getRegistryKey() == World.OVERWORLD && INITIALIZED.add(world)) {
                LOGGER.info("First chunk generate in {}, starting pipeline", world.getRegistryKey().getValue());
                startPipelineInit(world, "initial_chunk");
            }
        });


        // Запуск при генерации чанка, содержащего требуемую структуру
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) return;
            if (!containsTargetStructure((ServerWorld) world, chunk)) return;
            LOGGER.info("Chunk {} generated with target structure, starting pipeline", chunk.getPos());
            startPipeline(world, chunk, "chunk_structure_trigger");
        });

        // Периодический запуск каждый INTERVAL_TICKS на позиции игрока
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < INTERVAL_TICKS) return;
            tickCounter = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                World world = player.getWorld();
                if (world.getRegistryKey() != World.OVERWORLD) continue;
                BlockPos pos = player.getBlockPos();
                LOGGER.info("Periodic trigger at player {} pos {}, starting pipeline", player.getName().getString(), pos);
                startPipelineAtPos((ServerWorld) world, pos, "player_periodic_trigger");
            }
        });
    }

    /**
     * Pre-caches identifiers and tags from config for O(1) matching.
     */
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

    /* ───────────────────────────── Pipeline helpers ───────────────────────────── */

    private static void startPipeline(ServerWorld world, Chunk chunk, String reason) {
        startPipelineAtPos(world, chunk.getPos().getCenterAtY(0), reason);
    }

    /**
     * Запускает пайплайн от заданной позиции.
     */
    private static void startPipelineAtPos(ServerWorld world, BlockPos center, String reason) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        try {
            LOGGER.info("Pipeline start: {}", reason);
            StructureScanManager.scan(world, reason, center, RoadArchitect.CONFIG.chunkGenerateScanRadius());
            Map<String, List<BlockPos>> paths = PathFinderManager.computePaths(world, 50, RoadArchitect.CONFIG.maxConnectionDistance()*2);
            RoadBuilderManager.queueSegments(world, paths);
        } catch (Exception e) {
            LOGGER.error("Pipeline failure", e);
        } finally {
            RUNNING.set(false);
            LOGGER.info("Pipeline finished: {}", reason);
        }
    }

    private static void startPipelineInit(ServerWorld world, String reason) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        try {
            LOGGER.info("Pipeline start: {}", reason);
            long start1 = System.nanoTime();
            BlockPos center = world.getSpawnPos();
            StructureScanManager.scan(world, reason, center, RoadArchitect.CONFIG.initScanRadius());
            double ms1 = (System.nanoTime() - start1) / 1_000_000.0;
            LOGGER.info("StructureScanManager finished in {} ms", ms1);
            long start2 = System.nanoTime();
            Map<String, List<BlockPos>> rawPaths = PathFinderManager.computePaths(world, 1000, RoadArchitect.CONFIG.maxConnectionDistance()*10);
            double ms2 = (System.nanoTime() - start2) / 1_000_000.0;
            LOGGER.info("PathFinderManager finished in {} ms", ms2);
            long start3 = System.nanoTime();
            RoadBuilderManager.queueSegments(world, rawPaths);
            double ms3 = (System.nanoTime() - start3) / 1_000_000.0;
            LOGGER.info("RoadBuilderManager finished in {} ms", ms3);

        } catch (Exception e) {
            LOGGER.error("Pipeline failure", e);
        } finally {
            RUNNING.set(false);
            LOGGER.info("Pipeline finished: {}", reason);
        }
    }

    /* ───────────────────────────── Structure matching ───────────────────────────── */

    private static boolean containsTargetStructure(ServerWorld world, Chunk chunk) {
        if (!chunk.hasStructureReferences()) return false;

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        for (StructureStart start : chunk.getStructureStarts().values()) {
            Structure structure = start.getStructure();
            Identifier id = registry.getId(structure);
            if (id != null && TARGET_IDS.contains(id)) {
                return true;
            }
            RegistryEntry<Structure> entry = registry.getEntry(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.isIn(tag)) return true;
                }
            }
        }
        return false;
    }
}
