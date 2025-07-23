package net.oxcodsnet.roadarchitect.storage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Набор тестов для {@link EdgeStorage} после перехода на хранение рёбер по edgeId.
 */
public class EdgeStorageTests {

    private static String edgeId(String id1, String id2) {
        return id1.compareTo(id2) < 0 ? id1 + "+" + id2 : id2 + "+" + id1;
    }

    /**
     * Проверяет добавление рёбер и соседей.
     */
    @Test
    void addAndNeighbors() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");

        assertTrue(storage.add(a, b));
        assertEquals(Set.of("b"), storage.neighbors("a"));
        assertEquals(Set.of("a"), storage.neighbors("b"));
        assertEquals(EdgeStorage.Status.NEW, storage.getStatus(edgeId("a", "b")));
    }

    /**
     * Проверяет отказ добавления при большом расстоянии.
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
     */
    @Test
    void removeEdge() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        assertTrue(storage.remove(edgeId("a", "b")));
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Проверяет очистку хранилища.
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
     * Убеждается, что представление all() неизменяемо.
     */
    @Test
    void allIsUnmodifiable() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        Map<String, EdgeStorage.Edge> view = storage.all();
        assertThrows(UnsupportedOperationException.class, view::clear);
    }

    /**
     * Проверяет сохранение и загрузку.
     */
    @Test
    void roundTripSerialization() {
        EdgeStorage storage = new EdgeStorage(5.0);
        Node a = new Node("a", BlockPos.ORIGIN, "test");
        Node b = new Node("b", new BlockPos(9, 64, 0), "test");
        storage.add(a, b);

        NbtCompound tag = storage.toNbt();
        EdgeStorage loaded = EdgeStorage.fromNbt(tag, 5.0);

        assertEquals(storage.allWithStatus(), loaded.allWithStatus());
        assertEquals(storage.radius(), loaded.radius());
    }
}
