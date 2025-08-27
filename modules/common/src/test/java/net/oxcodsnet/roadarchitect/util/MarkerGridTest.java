package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.PathDecorStorage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MarkerGridTest {

    private static List<BlockPos> makeLine(int n) {
        List<BlockPos> pts = new ArrayList<>(n);
        for (int i = 0; i < n; i++) pts.add(new BlockPos(i, 64, 0));
        return pts;
    }

    @Test
    void markersIndependentOfWindow() {
        String key = "A|B";
        List<BlockPos> pts = makeLine(100);
        double[] S = PathDecorUtil.computePrefix(pts);
        int step = 10;
        int phase = PathDecorUtil.phaseFor(key, step);

        List<PathDecorUtil.Marker> whole = PathDecorUtil.markersInWindow(S, 0, pts.size(), step, phase);

        List<PathDecorUtil.Marker> part1 = PathDecorUtil.markersInWindow(S, 0, 20, step, phase);
        List<PathDecorUtil.Marker> part2 = PathDecorUtil.markersInWindow(S, 20, 50, step, phase);
        List<PathDecorUtil.Marker> part3 = PathDecorUtil.markersInWindow(S, 50, 100, step, phase);

        Set<Integer> setWhole = new HashSet<>();
        for (var m : whole) setWhole.add(m.index());
        Set<Integer> setParts = new HashSet<>();
        for (var m : part1) setParts.add(m.index());
        for (var m : part2) setParts.add(m.index());
        for (var m : part3) setParts.add(m.index());

        assertEquals(setWhole, setParts, "Union of window markers must match whole markers");
    }

    @Test
    void erosionRejectsNeighbors() {
        byte T = PathDecorUtil.BOOL_TRUE;
        byte F = PathDecorUtil.BOOL_FALSE;
        // mask: T T T F F F T T T, erosion=1
        byte[] mask = new byte[]{T,T,T,F,F,F,T,T,T};
        assertTrue(PathDecorUtil.erodedAccept(mask, 1, 1, T)); // middle of first run survives
        assertFalse(PathDecorUtil.erodedAccept(mask, 0, 1, T)); // near transition rejected
        assertFalse(PathDecorUtil.erodedAccept(mask, 2, 1, T));
        assertTrue(PathDecorUtil.erodedAccept(mask, 7, 1, T));
        assertFalse(PathDecorUtil.erodedAccept(mask, 6, 1, T));
        assertFalse(PathDecorUtil.erodedAccept(mask, 8, 1, T));
    }
}
