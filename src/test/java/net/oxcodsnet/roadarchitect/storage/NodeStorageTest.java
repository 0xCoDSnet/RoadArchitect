package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NodeStorageTest {
    @Test
    void addAndAll() {
        NodeStorage storage = new NodeStorage();
        Node node = storage.add(new BlockPos(1, 64, 1));

        Map<String, Node> all = storage.all();
        assertEquals(1, all.size());
        assertTrue(all.containsKey(node.id()));
        assertEquals(new BlockPos(1, 64, 1), all.get(node.id()).pos());
    }

    @Test
    void remove() {
        NodeStorage storage = new NodeStorage();
        Node node = storage.add(BlockPos.ORIGIN);
        assertTrue(storage.remove(node.id()));
        assertTrue(storage.all().isEmpty());
    }

    @Test
    void clear() {
        NodeStorage storage = new NodeStorage();
        storage.add(BlockPos.ORIGIN);
        storage.add(new BlockPos(5, 64, 5));
        storage.clear();
        assertTrue(storage.all().isEmpty());
    }

    @Test
    void allIsUnmodifiable() {
        NodeStorage storage = new NodeStorage();
        storage.add(BlockPos.ORIGIN);
        Map<String, Node> view = storage.all();
        assertThrows(UnsupportedOperationException.class, () -> view.clear());
    }
}
