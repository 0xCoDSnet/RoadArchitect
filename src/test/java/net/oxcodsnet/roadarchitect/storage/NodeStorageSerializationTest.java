package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NodeStorageSerializationTest {
    @Test
    void roundTrip() {
        NodeStorage storage = new NodeStorage();
        Node a = storage.add(BlockPos.ORIGIN);
        Node b = storage.add(new BlockPos(5, 64, 5));

        NbtList tag = storage.toNbt();
        NodeStorage loaded = NodeStorage.fromNbt(tag);

        assertEquals(storage.all(), loaded.all());
        assertEquals(a.pos(), loaded.all().get(a.id()).pos());
        assertEquals(b.pos(), loaded.all().get(b.id()).pos());
    }
}
