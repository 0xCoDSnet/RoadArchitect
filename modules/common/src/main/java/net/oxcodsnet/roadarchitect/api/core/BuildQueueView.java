package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.util.math.ChunkPos;

import java.util.List;

/**
 * Read-only access to queued build segments per chunk.
 */
public interface BuildQueueView {
    List<BuildSegment> segments(ChunkPos chunk);
}
