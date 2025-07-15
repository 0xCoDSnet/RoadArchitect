package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NodeStorage}.
 */
public class NodeStorageTest {
    @Test
    public void testAddAndContains() {
        NodeStorage storage = new NodeStorage();
        BlockPos pos = new BlockPos(1, 64, 2);
        assertTrue(storage.add(pos));
        assertTrue(storage.contains(pos));
        assertFalse(storage.add(pos));
        assertEquals(1, storage.size());
        storage.clear();
        assertFalse(storage.contains(pos));
    }
}