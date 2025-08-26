package net.oxcodsnet.roadarchitect.worldgen.style.decoration;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;

/**
 * Lamp post in the shape of an "L" with a lantern.
 */
public final class LampPostDecoration implements Decoration {
    private static final int PLACE_FLAGS = Block.NOTIFY_ALL | Block.NO_REDRAW;
    private static final int MAX_SUPPORT_DEPTH = 3;
    private static final int MAX_UP_SEARCH = 3;
    private static final int HEIGHT = 3;

    private final BlockState postState;
    private final BlockState lampState;
    private final Direction facing;

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
        BlockPos top = computeTop(world, basePos);
        if (top == null) return;

        placeSupport(world, top);

        // vertical pole
        for (int i = 1; i <= HEIGHT; i++) {
            BlockPos cur = top.up(i);
            if (!world.getBlockState(cur).isReplaceable()) return;
            world.setBlockState(cur, this.postState, PLACE_FLAGS);
        }

        // horizontal arm and lantern
        BlockPos arm = top.up(HEIGHT + 1);
        if (world.getBlockState(arm).isReplaceable()) {
            world.setBlockState(arm, this.postState, PLACE_FLAGS);
            BlockPos hook = arm.offset(this.facing);
            if (world.getBlockState(hook).isReplaceable()) {
                world.setBlockState(hook, this.postState, PLACE_FLAGS);
                BlockPos lantern = hook.down();
                if (world.getBlockState(lantern).isReplaceable()) {
                    world.setBlockState(lantern, this.lampState, PLACE_FLAGS);
                }
            }
        }
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
        BlockState baseState = world.getBlockState(base);
        boolean baseSolid = baseState.isSolidBlock(world, base);

        if (!baseSolid) {
            BlockPos probe = base.down();
            int depth = 0;
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down();
                depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) {
                return null;
            }
            return probe.up();
        } else {
            BlockPos top = base.up();
            int rise = 0;
            while (rise < MAX_UP_SEARCH && !world.getBlockState(top).isReplaceable()) {
                top = top.up();
                rise++;
            }
            if (!world.getBlockState(top).isReplaceable()) {
                return null;
            }
            int depth = 0;
            BlockPos probe = top.down();
            while (probe.getY() >= bottomY
                    && depth < MAX_SUPPORT_DEPTH
                    && !world.getBlockState(probe).isSolidBlock(world, probe)) {
                probe = probe.down();
                depth++;
            }
            if (!world.getBlockState(probe).isSolidBlock(world, probe)) {
                return null;
            }
            return top;
        }
    }
}
