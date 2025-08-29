package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.util.math.BlockPos;

/**
 * Immutable node snapshot.
 */
public record NodeView(String id, BlockPos pos, String type) {}

