package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeometryUtilsTest {

    @Test
    void linesIntersect_detectsSimpleCross() {
        // (0,0)-(10,10) crosses (0,10)-(10,0)
        assertTrue(GeometryUtils.linesIntersect(0, 0, 10, 10, 0, 10, 10, 0));
    }

    @Test
    void linesIntersect_detectsNoIntersection() {
        // Two disjoint segments
        assertFalse(GeometryUtils.linesIntersect(0, 0, 4, 0, 6, 0, 10, 0));
        assertFalse(GeometryUtils.linesIntersect(0, 0, 0, 4, 0, 6, 0, 10));
    }

    @Test
    void linesIntersect_handlesColinearOverlapping() {
        // Colinear and overlapping on x-axis
        assertTrue(GeometryUtils.linesIntersect(0, 0, 10, 0, 5, 0, 15, 0));
    }

    @Test
    void linesIntersect_handlesColinearDisjoint() {
        // Colinear but disjoint
        assertFalse(GeometryUtils.linesIntersect(0, 0, 4, 0, 5, 0, 9, 0));
    }

    @Test
    void segmentsIntersect2D_usesXZFromBlockPos() {
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(10, 70, 10);
        BlockPos c = new BlockPos(0, 30, 10);
        BlockPos d = new BlockPos(10, 30, 0);
        assertTrue(GeometryUtils.segmentsIntersect2D(a, b, c, d));
    }
}

