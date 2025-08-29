package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.util.math.BlockPos;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Safe, read-only view over the current road graph (nodes + edges).
 */
public interface RoadGraphView {
    Map<String, NodeView> nodes();

    Map<String, EdgeView> edges();

    Set<String> neighbors(String nodeId);

    Optional<NodeView> node(String id);

    Optional<NodeView> nearest(BlockPos pos, double maxDistance);
}

