package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест сериализации {@link NodeStorage}.
 * <p>Tests serialization of {@link NodeStorage}.</p>
 */
class NodeStorageSerializationTest {
    /**
     * Проверяет сохранение и загрузку узлов.
     * <p>Tests saving and loading nodes.</p>
     */
    @Test
    void roundTrip() {
        NodeStorage storage = new NodeStorage();
        Node a = storage.add(BlockPos.ORIGIN, "test");
        Node b = storage.add(new BlockPos(5, 64, 5), "test");

        NbtList tag = storage.toNbt();
        NodeStorage loaded = NodeStorage.fromNbt(tag);

        assertEquals(storage.all(), loaded.all());
        assertEquals(a.pos(), loaded.all().get(a.id()).pos());
        assertEquals(b.pos(), loaded.all().get(b.id()).pos());
    }
}
