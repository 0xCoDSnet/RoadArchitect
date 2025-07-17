package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EdgeStorageTest {
    @Test
    void addAndNeighbors() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN);
        Node b = new Node("b", new BlockPos(9, 64, 0));

        assertTrue(storage.add(a, b));
        assertEquals(Set.of("b"), storage.neighbors("a"));
        assertEquals(Set.of("a"), storage.neighbors("b"));
    }

    @Test
    void addInvalidDistance() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN);
        Node c = new Node("c", new BlockPos(20, 64, 0));

        assertFalse(storage.add(a, c));
        assertTrue(storage.all().isEmpty());
    }

    @Test
    void removeEdge() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN);
        Node b = new Node("b", new BlockPos(9, 64, 0));
        storage.add(a, b);

        assertTrue(storage.remove("a", "b"));
        assertTrue(storage.all().isEmpty());
    }

    @Test
    void clear() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN);
        Node b = new Node("b", new BlockPos(9, 64, 0));
        storage.add(a, b);

        storage.clear();
        assertTrue(storage.all().isEmpty());
    }

    @Test
    void allIsUnmodifiable() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN);
        Node b = new Node("b", new BlockPos(9, 64, 0));
        storage.add(a, b);

        Map<String, Set<String>> view = storage.all();
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }
}
