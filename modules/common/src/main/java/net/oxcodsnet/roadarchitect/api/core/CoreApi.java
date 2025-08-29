package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.server.world.ServerWorld;

/**
 * Read-only access point for Road Architect core data structures.
 * Implementations return safe snapshots or unmodifiable views.
 */
public interface CoreApi {
    RoadGraphView graph(ServerWorld world);

    PathView paths(ServerWorld world);

    BuildQueueView buildQueue(ServerWorld world);

    DecorView decor(ServerWorld world);
}

