package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Decoration that places a fence post. If the target position is occupied, the
 * post is shifted upwards until free space is found. After placement the fence
 * is extended downwards until it rests on a solid block, ensuring there are no
 * floating posts.
 */
public final class FenceDecoration implements Decoration {
    private final BlockState fenceState;

    private static final int MAX_SUPPORT_DEPTH = 3;   // макс. «воздуха» под столбом

    public FenceDecoration(BlockState fenceState) {
        this.fenceState = fenceState;
    }

    @Override
    public void place(StructureWorldAccess world, BlockPos basePos, Random random) {
        /* ─────────────────────────────────────────────────────
         * 1. Найти свободное место выше дорожного блока
         * ───────────────────────────────────────────────────── */
        BlockPos top = basePos.up();
        int worldTop = world.getBottomY() + world.getHeight() - 1;
        while (!world.isAir(top) && top.getY() < worldTop) {
            top = top.up();
        }
        if (!world.isAir(top)) {
            return; // свободного места нет
        }

        /* ─────────────────────────────────────────────────────
         * 2. Проверить глубину до твёрдого основания (≤ 3)
         * ───────────────────────────────────────────────────── */
        int depth = 0;
        BlockPos probe = top.down();
        while (probe.getY() >= world.getBottomY() && !world.getBlockState(probe).isSolidBlock(world, probe)) {
            depth++;
            if (depth > MAX_SUPPORT_DEPTH) {
                return; // слишком глубоко – прерываем размещение
            }
            probe = probe.down();
        }

        /* ─────────────────────────────────────────────────────
         * 3. Поставить основной столб и вытянуть вниз до опоры
         * ───────────────────────────────────────────────────── */
        setFence(world, top);

        BlockPos current = top.down();
        while (current.getY() >= world.getBottomY() && !world.getBlockState(current).isSolidBlock(world, current)) {
            setFence(world, current);
            current = current.down();
        }
    }

    /**
     * Ставим блок забора и сразу обновляем соседей, чтобы клиент отрисовал
     * соединения без задержки.
     */
    private void setFence(StructureWorldAccess world, BlockPos pos) {
        world.setBlockState(pos, this.fenceState, Block.NOTIFY_ALL_AND_REDRAW);
        world.updateNeighbors(pos, this.fenceState.getBlock());
    }
}




