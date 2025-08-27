package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Executes the road generation pipeline.
 */
public final class PipelineRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PipelineRunner.class.getSimpleName());

    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);
    private static volatile PipelineStage currentStage = PipelineStage.SCANNING_STRUCTURES;

    private PipelineRunner() {
    }

    public static PipelineStage getCurrentStage() {
        return currentStage;
    }

    private static void setStage(PipelineStage stage) {
        currentStage = stage;
    }

    /**
     * Runs the pipeline in the given mode starting from the provided position.
     */
    public static void runPipeline(ServerWorld world, BlockPos center, PipelineMode mode) {
        if (!RUNNING.compareAndSet(false, true)) {
            return;
        }
        try {
            setStage(PipelineStage.INITIALISATION);
            LOGGER.debug("Pipeline start: {}", mode.reason());
            switch (mode) {

                case INIT -> {
                    setStage(PipelineStage.SCANNING_STRUCTURES);
                    //long start = System.nanoTime();
                    StructureScanManager.scan(world, mode.reason(), center, RoadArchitect.CONFIG.initScanRadius());
                    //double ms = (System.nanoTime() - start) / 1_000_000.0;
                    //LOGGER.info("StructureScanManager finish: {}", ms);

                    setStage(PipelineStage.PATH_FINDING);
                    PathFinderManager.computePaths(world, 1000);

                    setStage(PipelineStage.POST_PROCESSING);
                    RoadPostProcessor.processPending(world);
                }
                default -> {
                    setStage(PipelineStage.SCANNING_STRUCTURES);
                    StructureScanManager.scan(world, mode.reason(), center, RoadArchitect.CONFIG.chunkGenerateScanRadius());

                    setStage(PipelineStage.PATH_FINDING);
                    PathFinderManager.computePaths(world, 50, RoadArchitect.CONFIG.maxConnectionDistance() * 5);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Pipeline failure", e);
        } finally {
            setStage(PipelineStage.COMPLETE);
            RUNNING.set(false);
            LOGGER.debug("Pipeline finished: {}", mode.reason());
        }
    }

    public enum PipelineMode {
        INIT("initial_chunk"),
        CHUNK("chunk_structure_trigger"),
        PERIODIC("player_periodic_trigger");

        private final String reason;

        PipelineMode(String reason) {
            this.reason = reason;
        }

        public String reason() {
            return reason;
        }
    }
}
