package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.List;
import java.util.Map;

/**
 * Read-only view over computed paths and their statuses.
 */
public interface PathView {
    List<BlockPos> path(String key);

    PathStatus status(String key);

    Map<String, PathStatus> allStatuses();

    List<String> pendingForChunk(ChunkPos chunk);
}
