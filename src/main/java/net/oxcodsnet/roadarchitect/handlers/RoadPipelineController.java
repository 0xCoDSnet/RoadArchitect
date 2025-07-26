package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controls execution of the road generation pipeline.
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPipelineController");

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static final Set<World> INITIALIZED = ConcurrentHashMap.newKeySet();

    private RoadPipelineController() {
    }

    /**
     * Registers event hooks for pipeline triggers.
     */
    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.getRegistryKey() == World.OVERWORLD && INITIALIZED.add(world)) {
                LOGGER.info("First chunk loaded in {}, starting pipeline", world.getRegistryKey().getValue());
                startPipelineInit(world, "initial_chunk");
            }
        });

//        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
//            if (world.getRegistryKey() == World.OVERWORLD) {
//                if(chunk.hasStructureReferences()){
//                    LOGGER.info("Chunk generate in {}, starting pipeline", world.getRegistryKey().getValue());
//                    startPipeline(world, chunk,"chunk with structure references");
//                }
//            }
//        });

//        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
//            ServerPlayerEntity player = handler.getPlayer();
//            ServerWorld world = server.getOverworld();
//            if (player.getServerWorld() == world) {
//                startPipeline(world, "player_join");
//            }
//        });
    }

//    private static void startPipeline(ServerWorld world, WorldChunk chunk, String reason) {
//        if (!RUNNING.compareAndSet(false, true)) {
//            return;
//        }
//
//        LOGGER.info("Pipeline start: {}", reason);
//        try {
//            BlockPos center = chunk.getPos().getCenterAtY(0);
//            StructureScanManager.scan(world, reason, center, RoadArchitect.CONFIG.chunkGenerateScanRadius());
//            Map<String, List<BlockPos>> paths = PathFinderManager.computePaths(world, 50);
//            RoadBuilderManager.queueSegments(world, paths);
//        } catch (Exception e) {
//            LOGGER.error("Pipeline failure", e);
//        } finally {
//            RUNNING.set(false);
//            LOGGER.info("Pipeline finished: {}", reason);
//        }
//    }

    private static void startPipelineInit(ServerWorld world, String reason) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }

        LOGGER.info("Pipeline start: {}", reason);
        try {
            BlockPos center = world.getSpawnPos();
            StructureScanManager.scan(world, reason, center, RoadArchitect.CONFIG.initScanRadius());
            Map<String, List<BlockPos>> raw_paths = PathFinderManager.computePaths(world, 500);
            RoadBuilderManager.queueSegments(world, raw_paths);
        } catch (Exception e) {
            LOGGER.error("Pipeline failure", e);
        } finally {
            RUNNING.set(false);
            LOGGER.info("Pipeline finished: {}", reason);
        }

    }


}
