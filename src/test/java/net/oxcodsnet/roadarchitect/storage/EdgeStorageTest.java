package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link EdgeStorage}.
 * <p>Tests for {@link EdgeStorage}.</p>
 */
class EdgeStorageTest {
    /**
     * Проверяет добавление рёбер и соседей.
     * <p>Tests adding edges and retrieving neighbors.</p>
     */
    @Test
    void addAndNeighbors() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");

        assertTrue(storage.add(a, b));
        assertEquals(Set.of("b"), storage.neighbors("a"));
        assertEquals(Set.of("a"), storage.neighbors("b"));
    }

    /**
     * Проверяет отказ добавления при большом расстоянии.
     * <p>Tests that edges beyond range are not added.</p>
     */
    @Test
    void addInvalidDistance() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node c = new Node("c", new BlockPos(20, 64, 0), "test");

        assertFalse(storage.add(a, c));
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Проверяет удаление рёбер.
     * <p>Tests edge removal.</p>
     */
    @Test
    void removeEdge() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        assertTrue(storage.remove("a", "b"));
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Проверяет очистку хранилища.
     * <p>Tests clearing the storage.</p>
     */
    @Test
    void clear() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        storage.clear();
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Убеждается, что представление неизменяемо.
     * <p>Ensures the returned map is unmodifiable.</p>
     */
    @Test
    void allIsUnmodifiable() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        Map<String, Set<String>> view = storage.all();
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }
}
