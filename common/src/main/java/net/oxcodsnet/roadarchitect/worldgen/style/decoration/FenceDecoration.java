package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

/**
 * Decoration that places a fence post. If the target position is occupied, the
 * post is shifted upwards until free space is found. After placement the fence
 * is extended downwards until it rests on a solid block, ensuring there are no
 * floating posts.
 */
public final class FenceDecoration implements Decoration {
    private static final int MAX_SUPPORT_DEPTH = 3;   // макс. «воздуха» под столбом
    private final BlockState fenceState;

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
        setFenceSmart(world, top);

        BlockPos current = top.down();
        while (current.getY() >= world.getBottomY() && !world.getBlockState(current).isSolidBlock(world, current)) {
            setFenceSmart(world, current);
            current = current.down();
        }
    }

    /**
     * Ставит забор, не заменяя твёрдые блоки и не оставляя его в воздухе.
     *
     * @param world StructureWorldAccess (во время world-гена) :contentReference[oaicite:4]{index=4}
     * @param pos   «базовая» точка, от которой считаем ±3 блока
     */
    private void setFenceSmart(StructureWorldAccess world, BlockPos pos) {

        // ── ШАГ 1. ищем первый заменяемый блок ↑ до +3 ──
        BlockPos head = pos;
        for (int i = 0; i < 3 && !world.getBlockState(head).isReplaceable(); i++) {
            head = head.up();
        }

        if (!world.getBlockState(head).isReplaceable()) return;

        // ── ШАГ 2. ставим «верхушку» ──
        world.setBlockState(head, this.fenceState, Block.NO_REDRAW);

        // ── ШАГ 3. тянем ножку вниз до первого твёрдого или −3 ──
        BlockPos leg = head.down();
        for (int i = 0; i < 3 && world.getBlockState(leg).isReplaceable(); i++, leg = leg.down()) {
            world.setBlockState(leg, this.fenceState, Block.NO_REDRAW);
        }
    }

    public void placeFenceStripe(StructureWorldAccess world, List<BlockPos> stripe) {

        // 1-й проход – «сырые» столбики, но уже с вертикальной логикой
        for (BlockPos p : stripe) {
            setFenceSmart(world, p);
        }

        // 2-й проход – пересчитываем формы (как раньше)
        for (BlockPos p : stripe) {
            BlockState st = world.getBlockState(p);
            for (Direction d : Direction.Type.HORIZONTAL) {
                BlockPos n = p.offset(d);
                st = st.getStateForNeighborUpdate(d, world.getBlockState(n), world, p, n);  // соединения забора
            }
            world.setBlockState(p, st, Block.NO_REDRAW);
        }
    }
}




