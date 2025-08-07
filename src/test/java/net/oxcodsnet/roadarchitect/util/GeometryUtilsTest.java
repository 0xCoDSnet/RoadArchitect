package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link GeometryUtils}.
 */
class GeometryUtilsTest {
    @Test
    void segmentsIntersect() {
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 0, 10);
        BlockPos p3 = new BlockPos(0, 0, 10);
        BlockPos p4 = new BlockPos(10, 0, 0);
        assertTrue(GeometryUtils.segmentsIntersect2D(p1, p2, p3, p4));
    }

    @Test
    void segmentsDoNotIntersect() {
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 0, 0);
        BlockPos p3 = new BlockPos(0, 0, 5);
        BlockPos p4 = new BlockPos(10, 0, 5);
        assertFalse(GeometryUtils.segmentsIntersect2D(p1, p2, p3, p4));
    }
}
