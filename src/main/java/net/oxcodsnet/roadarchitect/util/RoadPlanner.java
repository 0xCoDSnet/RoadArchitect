package net.oxcodsnet.roadarchitect.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles scheduling and placing of road blocks in chunks the player has not loaded yet.
 */
public final class RoadPlanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadPlanner");
    private static final Map<ChunkPos, List<BlockPos>> PLANNED = new ConcurrentHashMap<>();

    private RoadPlanner() {
    }

    /** Registers chunk load listener. */
    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register(RoadPlanner::onChunkLoad);
    }

    /**
     * Stores the given path. Blocks inside loaded chunks are placed immediately,
     * others are stored until the chunk loads.
     */
    public static void planRoad(ServerWorld world, List<BlockPos> path) {
        for (BlockPos pos : path) {
            ChunkPos chunk = new ChunkPos(pos);
            if (world.isChunkLoaded(chunk.x, chunk.z)) {
                world.setBlockState(pos, Blocks.GLASS.getDefaultState(), 3);
            } else {
                PLANNED.computeIfAbsent(chunk, c -> new ArrayList<>()).add(pos);
            }
        }
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        List<BlockPos> positions = PLANNED.remove(chunk.getPos());
        if (positions == null) {
            return;
        }
        LOGGER.info("Generating planned road in chunk {}", chunk.getPos());
        for (BlockPos pos : positions) {
            world.setBlockState(pos, Blocks.GLASS.getDefaultState(), 3);
        }
    }
}
