package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тест сериализации {@link EdgeStorage}.
 * <p>Tests serialization of {@link EdgeStorage}.</p>
 */

class EdgeStorageSerializationTest {
    /**
     * Проверяет сохранение и загрузку.
     * <p>Tests saving and loading the storage.</p>
     */
    @Test
    void roundTrip() {
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
