package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link EdgeStorage}.
 */
public class EdgeStorageTest {
    @Test
    public void testAddContainsAndRemove() {
        EdgeStorage storage = new EdgeStorage();
        BlockPos a = new BlockPos(0, 64, 0);
        BlockPos b = new BlockPos(1, 64, 1);

        assertTrue(storage.add(a, b));
        assertFalse(storage.add(a, b));
        assertTrue(storage.contains(a, b));
        assertTrue(storage.contains(b, a));
        assertEquals(1, storage.size());
        assertEquals(1, storage.degree(a));
        assertEquals(1, storage.asEdgeSet().size());
        assertTrue(storage.remove(a, b));
        assertFalse(storage.contains(a, b));
        assertEquals(0, storage.size());
    }
}