package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LanternBlock;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

public final class LampPostDecoration implements Decoration {
    private static final int PLACE_FLAGS = Block.NOTIFY_ALL | Block.NO_REDRAW;
    private static final int MAX_SUPPORT_DEPTH = 3;
    private static final int MAX_UP_SEARCH = 3;
    private static final int HEIGHT = 3;

    private final BlockState postState; // fence/wall/etc.
    private final BlockState lampState;
    private final Direction facing; // куда вылетает «крюк»

    public LampPostDecoration(BlockState postState, BlockState lampState) {
        this(postState, lampState, Direction.NORTH);
    }

    private LampPostDecoration(BlockState postState, BlockState lampState, Direction facing) {
        this.postState = postState;
        this.lampState = lampState;
        this.facing = facing;
    }

    public LampPostDecoration facing(Direction facing) {
        return new LampPostDecoration(this.postState, this.lampState, facing);
    }

    @Override
    public void place(StructureWorldAccess world, BlockPos basePos, Random random) {
        tryPlace(world, basePos, random);
    }

    /** Ставит фонарь «Г» и возвращает true при успехе. */
    public boolean tryPlace(StructureWorldAccess world, BlockPos basePos, Random random) {
        BlockPos top = computeTop(world, basePos);
        if (top == null) return false;

        // Проверяем стойку (включая уровень armBase)
        for (int i = 1; i <= HEIGHT; i++) {
            if (!world.getBlockState(top.up(i)).isReplaceable()) return false;
        }

        // Вылет с уровня верхней стойки
        BlockPos armBase = top.up(HEIGHT);
        BlockPos hook    = armBase.offset(this.facing);
        if (!world.getBlockState(hook).isReplaceable()) return false;

        BlockPos lantern = hook.down();
        if (!world.getBlockState(lantern).isReplaceable()) return false;

        // Установка
        placeSupport(world, top);

        // 1..HEIGHT-1 — голая стойка
        for (int i = 1; i < HEIGHT; i++) {
            world.setBlockState(top.up(i), this.postState, PLACE_FLAGS);
        }

        // ВЕРХ СТОЙКИ: задаём соединение в сторону «крюка»
        BlockState armBaseState = withFenceConnection(this.postState, this.facing);
        world.setBlockState(armBase, armBaseState, PLACE_FLAGS);

        // КРЮК: задаём соединение назад, к стойке
        BlockState hookState = withFenceConnection(this.postState, this.facing.getOpposite());
        world.setBlockState(hook, hookState, PLACE_FLAGS);

        // Подвесной фонарь
        BlockState ls = this.lampState;
        if (ls.contains(LanternBlock.HANGING)) {
            ls = ls.with(LanternBlock.HANGING, true);
        }
        world.setBlockState(lantern, ls, PLACE_FLAGS);

        return true;
    }

    /** Проставляет для заборов BooleanProperty направления (N/E/S/W) = true. Для иных блоков возвращает исходное состояние. */
    private static BlockState withFenceConnection(BlockState state, Direction dir) {
        BooleanProperty prop = switch (dir) {
            case NORTH -> Properties.NORTH;
            case SOUTH -> Properties.SOUTH;
            case EAST  -> Properties.EAST;
            case WEST  -> Properties.WEST;
            default    -> null;
        };
        if (prop != null && state.contains(prop)) {
            return state.with(prop, true);
        }
        return state; // не забор или нет нужного свойства
    }

    private void placeSupport(StructureWorldAccess world, BlockPos top) {
        world.setBlockState(top, this.postState, PLACE_FLAGS);
        BlockPos cur = top.down();
        int depth = 0;
        while (cur.getY() >= world.getBottomY()
                && depth < MAX_SUPPORT_DEPTH
                && !world.getBlockState(cur).isSolidBlock(world, cur)) {
            world.setBlockState(cur, this.postState, PLACE_FLAGS);
            cur = cur.down();
            depth++;
        }
    }

    private BlockPos computeTop(StructureWorldAccess world, BlockPos base) {
        int bottomY = world.getBottomY();
        boolean baseSolid = world.getBlockState(base).isSolidBlock(world, base);

        if (!baseSolid) {
            BlockPos probe = base.down();
            int depth = 0;
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down();
                depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) return null;
            return probe.up();
        } else {
            BlockPos top = base.up();
            int rise = 0;
            while (rise < MAX_UP_SEARCH && !world.getBlockState(top).isReplaceable()) {
                top = top.up(); rise++;
            }
            if (!world.getBlockState(top).isReplaceable()) return null;

            int depth = 0;
            BlockPos probe = top.down();
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down(); depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) return null;
            return top;
        }
    }
}
