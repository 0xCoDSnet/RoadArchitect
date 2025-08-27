package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

import java.util.List;

public final class FenceDecoration implements Decoration {
    private static final int MAX_SUPPORT_DEPTH = 3;   // максимум «воздуха» под столбом
    private static final int MAX_UP_SEARCH     = 3;   // максимум поиска свободного места вверх
    private static final int PLACE_FLAGS       = Block.NOTIFY_ALL | Block.NO_REDRAW;

    private final BlockState fenceState;

    public FenceDecoration(BlockState fenceState) {
        this.fenceState = fenceState;
    }

    @Override
    public void place(StructureWorldAccess world, BlockPos basePos, Random random) {
        BlockPos top = computeTop(world, basePos);
        if (top == null) return;
        placeFromTopDown(world, top);
    }

    /**
     * Считает «верх» столба по правилам:
     * - если стартовый блок НЕ твёрдый → ищем опору вниз ≤ 3 и ставим над ней;
     * - если стартовый блок твёрдый → поднимаем позицию вверх и ищем свободный слот ≤ 3.
     * Возвращает null, если условия не выполнены.
     */
    private BlockPos computeTop(StructureWorldAccess world, BlockPos base) {
        int bottomY = world.getBottomY();

        BlockState baseState = world.getBlockState(base);
        boolean baseSolid = baseState.isSolidBlock(world, base); // твёрдый ли стартовый блок

        if (!baseSolid) {
            // Ищем опору вниз не дальше чем на 3 блока
            BlockPos probe = base.down();
            int depth = 0;
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down();
                depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) {
                return null; // опоры нет в пределах 3
            }

            // Ставим верх столба над найденной опорой
            BlockPos top = probe.up();
            if (!world.getBlockState(top).isReplaceable()) {
                return null; // место занято чем-то незаменяемым
            }
            return top;
        } else {
            // Стартовый блок твёрдый — ищем свободный слот вверх не дальше 3
            BlockPos top = base.up();
            int rise = 0;
            while (rise < MAX_UP_SEARCH && !world.getBlockState(top).isReplaceable()) {
                top = top.up();
                rise++;
            }
            if (!world.getBlockState(top).isReplaceable()) {
                return null; // свободного места в пределах 3 нет
            }

            // Доп. валидация: под top должна быть опора не дальше 3
            int depth = 0;
            BlockPos probe = top.down();
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down();
                depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) {
                return null; // «висим» в воздухе глубже чем на 3
            }
            return top;
        }
    }

    /** Ставит забор в точке top и тянет вниз до опоры, но не глубже 3. */
    private void placeFromTopDown(StructureWorldAccess world, BlockPos top) {
        world.setBlockState(top, this.fenceState, PLACE_FLAGS);

        BlockPos cur = top.down();
        int depth = 0;
        while (cur.getY() >= world.getBottomY()
                && depth < MAX_SUPPORT_DEPTH
                && !world.getBlockState(cur).isSolidBlock(world, cur)) {
            world.setBlockState(cur, this.fenceState, PLACE_FLAGS);
            cur = cur.down();
            depth++;
        }
    }

    /** Линейка столбов с той же вертикальной логикой. */
    public void placeFenceStripe(StructureWorldAccess world, List<BlockPos> stripe) {
        for (BlockPos base : stripe) {
            BlockPos top = computeTop(world, base);
            if (top != null) {
                placeFromTopDown(world, top);
            }
        }

        // Доп. проход: перерасчёт форм (если хочешь руками подтолкнуть коннекты)
        for (BlockPos base : stripe) {
            for (int dy = 0; dy <= MAX_SUPPORT_DEPTH; dy++) {
                BlockPos p = base.up(dy);                // на случай, если верх ушёл выше base
                BlockState st = world.getBlockState(p);
                if (!st.isOf(fenceState.getBlock())) continue;

                for (Direction d : Direction.Type.HORIZONTAL) {
                    BlockPos n = p.offset(d);
                    st = st.getStateForNeighborUpdate(world, world, p, d, n, world.getBlockState(n), Random.create());
                }
                world.setBlockState(p, st, PLACE_FLAGS);
            }
        }
    }
}
