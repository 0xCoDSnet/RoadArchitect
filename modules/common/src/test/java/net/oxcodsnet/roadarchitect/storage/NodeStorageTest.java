package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeStorageTest {

    @Test
    void addAndRemove_nodes() {
        NodeStorage s = new NodeStorage();
        Node n = s.add(new BlockPos(10, 64, -5), "village");
        assertNotNull(n);
        assertTrue(s.all().containsKey(n.id()));
        assertTrue(s.remove(n.id()));
        assertFalse(s.all().containsKey(n.id()));
    }

    @Test
    void toNbt_and_fromNbt_roundTrip() {
        NodeStorage s = new NodeStorage();
        Node n1 = s.add(new BlockPos(0, 64, 0), "village");
        Node n2 = s.add(new BlockPos(100, 64, 100), "shipwreck");

        NbtList list = s.toNbt();
        assertEquals(2, list.size());

        NodeStorage restored = NodeStorage.fromNbt(list);
        assertEquals(2, restored.all().size());
        assertEquals(s.all().keySet(), restored.all().keySet());

        // sanity on content
        Node rn1 = restored.all().get(n1.id());
        assertEquals(n1.pos(), rn1.pos());
        assertEquals(n1.type(), rn1.type());
    }
}

