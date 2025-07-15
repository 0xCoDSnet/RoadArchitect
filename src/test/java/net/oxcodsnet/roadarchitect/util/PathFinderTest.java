package net.oxcodsnet.roadarchitect.util;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Unit tests for {@link PathFinder}. */
public class PathFinderTest {
    @Test
    public void testSimplePath() {
        PathFinder.Environment env = new PathFinder.Environment() {
            @Override
            public Iterable<BlockPos> neighbors(BlockPos pos) {
                List<BlockPos> n = new ArrayList<>();
                n.add(pos.add(1, 0, 0));
                n.add(pos.add(-1, 0, 0));
                n.add(pos.add(0, 0, 1));
                n.add(pos.add(0, 0, -1));
                return n;
            }

            @Override
            public double cost(BlockPos from, BlockPos to) {
                return 1.0;
            }

            @Override
            public double heuristic(BlockPos pos, BlockPos goal) {
                return pos.getManhattanDistance(goal);
            }
        };

        List<BlockPos> path = PathFinder.findPath(BlockPos.ORIGIN, new BlockPos(3, 0, 0), env);
        assertFalse(path.isEmpty());
        assertEquals(new BlockPos(3, 0, 0), path.get(path.size() - 1));
        assertEquals(4, path.size());
    }
}