package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для {@link NodeStorage}.
 * <p>Tests for {@link NodeStorage}.</p>
 */
class NodeStorageTest {
    /**
     * Проверяет добавление и получение всех узлов.
     * <p>Tests adding nodes and retrieving them.</p>
     */
    @Test
    void addAndAll() {
        NodeStorage storage = new NodeStorage();
        Node node = storage.add(new BlockPos(1, 64, 1), "test");

        Map<String, Node> all = storage.all();
        assertEquals(1, all.size());
        assertTrue(all.containsKey(node.id()));
        assertEquals(new BlockPos(1, 64, 1), all.get(node.id()).pos());
    }

    /**
     * Проверяет удаление узла.
     * <p>Tests node removal.</p>
     */
    @Test
    void remove() {
        NodeStorage storage = new NodeStorage();
        Node node = storage.add(BlockPos.ORIGIN, "test");
        assertTrue(storage.remove(node.id()));
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Проверяет очистку хранилища.
     * <p>Tests clearing the storage.</p>
     */
    @Test
    void clear() {
        NodeStorage storage = new NodeStorage();
        storage.add(BlockPos.ORIGIN, "test");
        storage.add(new BlockPos(5, 64, 5), "test");
        storage.clear();
        assertTrue(storage.all().isEmpty());
    }

    /**
     * Убеждается в неизменяемости представления.
     * <p>Ensures the returned map is unmodifiable.</p>
     */
    @Test
    void allIsUnmodifiable() {
        NodeStorage storage = new NodeStorage();
        storage.add(BlockPos.ORIGIN, "test");
        Map<String, Node> view = storage.all();
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }
}
