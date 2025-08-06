package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Represents a decoration placed alongside a road.
 */
@FunctionalInterface
public interface Decoration {
    Decoration NONE = (world, pos, random) -> {};

    void place(StructureWorldAccess world, BlockPos basePos, Random random);
}

