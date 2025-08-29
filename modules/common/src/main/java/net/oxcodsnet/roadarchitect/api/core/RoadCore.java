package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.server.world.ServerWorld;

/**
 * Static convenience facade to obtain core read-only views.
 */
public final class RoadCore {
    private RoadCore() {}

    public static RoadGraphView graph(ServerWorld world) {
        return CoreApiImpl.INSTANCE.graph(world);
    }

    public static PathView paths(ServerWorld world) {
        return CoreApiImpl.INSTANCE.paths(world);
    }

    public static BuildQueueView buildQueue(ServerWorld world) {
        return CoreApiImpl.INSTANCE.buildQueue(world);
    }

    public static DecorView decor(ServerWorld world) {
        return CoreApiImpl.INSTANCE.decor(world);
    }
}

