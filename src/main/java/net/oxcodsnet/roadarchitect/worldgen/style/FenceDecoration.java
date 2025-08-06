package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Simple decoration that places a single fence post.
 */
public final class FenceDecoration implements Decoration {
    private final BlockState fenceState;

    public FenceDecoration(BlockState fenceState) {
        this.fenceState = fenceState;
    }

    @Override
    public void place(StructureWorldAccess world, BlockPos basePos, Random random) {
        world.setBlockState(basePos.up(), this.fenceState, Block.NOTIFY_LISTENERS);
    }
}

