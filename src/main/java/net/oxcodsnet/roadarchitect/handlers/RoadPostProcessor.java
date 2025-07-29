package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.util.CacheManager;
import net.oxcodsnet.roadarchitect.util.PathFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

/**
 * Post-processes raw A* paths into detailed block sequences.
 */
public final class RoadPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPostProcessor");
    private static final ForkJoinPool EXECUTOR = new ForkJoinPool(Runtime.getRuntime().availableProcessors());

    private RoadPostProcessor() {
    }

    /** Registers event hooks for asynchronous processing. */
    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() != World.OVERWORLD) return;

            processChunk(world, chunk.getPos());
        });

        ServerTickEvents.START_WORLD_TICK.register(world -> {
            if (world.isClient()) return;
            if (world.getRegistryKey() != World.OVERWORLD) return;

            processPending(world);
        });
    }

    /**
     * Converts raw grid vertices into a full list of block positions.
     */
    private static List<BlockPos> refine(ServerWorld world, List<BlockPos> verts) {
        if (verts.isEmpty()) {
            return List.of();
        }

        List<BlockPos> out = new ArrayList<>(verts.size() * PathFinder.GRID_STEP);
        for (int i = 0; i < verts.size() - 1; i++) {
            BlockPos a = verts.get(i);
            BlockPos b = verts.get(i + 1);
            out.add(a);
            interpolate(world, a, b, out);
        }
        out.add(verts.getLast());
        return out;
    }

    private static void interpolate(ServerWorld world, BlockPos a, BlockPos b, List<BlockPos> out) {
        int dx = Integer.signum(b.getX() - a.getX());
        int dz = Integer.signum(b.getZ() - a.getZ());
        int steps = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        for (int i = 1; i < steps; i++) {
            int nx = a.getX() + dx * i;
            int nz = a.getZ() + dz * i;
            int ny = CacheManager.getHeight(world, nx, nz);
            out.add(new BlockPos(nx, ny, nz));
        }
    }

    /** Schedules processing of all pending paths for the world. */
    public static void processPending(ServerWorld world) {
        PathStorage storage = PathStorage.get(world);
        for (Map.Entry<String, PathStorage.Status> e : storage.allStatuses().entrySet()) {
            if (e.getValue() != PathStorage.Status.PENDING) {
                continue;
            }
            schedule(world, storage, e.getKey());
        }
    }

    /** Processes pending paths intersecting the given chunk. */
    private static void processChunk(ServerWorld world, ChunkPos chunk) {
        PathStorage storage = PathStorage.get(world);
        for (String key : storage.getPendingForChunk(chunk)) {
            schedule(world, storage, key);
        }
    }

    private static void schedule(ServerWorld world, PathStorage storage, String key) {
        if (!storage.tryMarkProcessing(key)) {
            return;
        }
        List<BlockPos> raw = storage.getPath(key);
        EXECUTOR.submit(() -> {
            try {
                List<BlockPos> refined = refine(world, raw);
                storage.updatePath(key, refined, PathStorage.Status.READY);
                RoadBuilderManager.queueSegments(world, Map.of(key, refined));
            } catch (Exception ex) {
                LOGGER.error("Post-processing failed for {}", key, ex);
                storage.setStatus(key, PathStorage.Status.FAILED);
            }
        });
    }
}

