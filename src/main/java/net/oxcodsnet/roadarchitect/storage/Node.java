package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;

/**
 * Представляет узел точки интереса.
 */
public record Node(String id, BlockPos pos) {}
