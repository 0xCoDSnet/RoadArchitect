package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;

/**
 * Geometry helper methods for 2D segment intersection checks.
 * <p>Provides simple O(1) algorithms for determining whether lines or
 * segments intersect on the XZ-plane.</p>
 */
public final class GeometryUtils {
    private GeometryUtils() {}

    /**
     * Checks whether two segments on the XZ-plane intersect.
     *
     * @param p1 start of first segment
     * @param p2 end of first segment
     * @param p3 start of second segment
     * @param p4 end of second segment
     * @return {@code true} if segments intersect, {@code false} otherwise
     */
    public static boolean segmentsIntersect2D(BlockPos p1, BlockPos p2,
                                              BlockPos p3, BlockPos p4) {
        return linesIntersect(
                p1.getX(), p1.getZ(), p2.getX(), p2.getZ(),
                p3.getX(), p3.getZ(), p4.getX(), p4.getZ());
    }

    /**
     * Classic constant-time test for two line segments intersection.
     */
    public static boolean linesIntersect(int x1, int y1, int x2, int y2,
                                         int x3, int y3, int x4, int y4) {
        long d1 = direction(x3, y3, x4, y4, x1, y1);
        long d2 = direction(x3, y3, x4, y4, x2, y2);
        long d3 = direction(x1, y1, x2, y2, x3, y3);
        long d4 = direction(x1, y1, x2, y2, x4, y4);
        if (d1 * d2 < 0 && d3 * d4 < 0) {
            return true;
        }
        return (d1 == 0 && onSegment(x3, y3, x4, y4, x1, y1)) ||
                (d2 == 0 && onSegment(x3, y3, x4, y4, x2, y2)) ||
                (d3 == 0 && onSegment(x1, y1, x2, y2, x3, y3)) ||
                (d4 == 0 && onSegment(x1, y1, x2, y2, x4, y4));
    }

    private static long direction(int ax, int ay, int bx, int by, int cx, int cy) {
        return (long) (cx - ax) * (by - ay) - (long) (cy - ay) * (bx - ax);
    }

    private static boolean onSegment(int ax, int ay, int bx, int by, int cx, int cy) {
        return Math.min(ax, bx) <= cx && cx <= Math.max(ax, bx) &&
                Math.min(ay, by) <= cy && cy <= Math.max(ay, by);
    }
}
