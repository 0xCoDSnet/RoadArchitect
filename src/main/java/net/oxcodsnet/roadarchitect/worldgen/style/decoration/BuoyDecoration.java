package net.oxcodsnet.roadarchitect.worldgen.style.decoration;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Буй: еловое бревно → забор → факел.
 * Ставится прямо в клетку дороги-воды (лог замещает воду),
 * без “ноги” до дна — как просили.
 */
public final class BuoyDecoration implements Decoration {
    private static final BlockState LOG   = Blocks.SPRUCE_LOG.getDefaultState();
    private static final BlockState FENCE = Blocks.SPRUCE_FENCE.getDefaultState();
    private static final BlockState TORCH = Blocks.TORCH.getDefaultState();

    @Override
    public void place(StructureWorldAccess world, BlockPos basePos, Random random) {
        // ── Если в клетке не воздух/вода — не рискуем ──
        //if (!world.getBlockState(basePos).getFluidState().isStill()) return;

        // Ставим бревно на уровень дороги-воды, fence и факел — выше
        world.setBlockState(basePos,             LOG,   Block.NOTIFY_NEIGHBORS);
        world.setBlockState(basePos.up(),        FENCE, Block.NOTIFY_NEIGHBORS);
        world.setBlockState(basePos.up(2),       TORCH, Block.NOTIFY_NEIGHBORS);
    }
}

