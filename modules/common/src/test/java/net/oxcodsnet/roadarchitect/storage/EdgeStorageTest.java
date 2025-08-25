package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EdgeStorageTest {

    @Test
    void add_and_status_and_neighbors() {
        EdgeStorage s = new EdgeStorage(128.0);
        Node a = new Node("a", new BlockPos(0, 64, 0), "village");
        Node b = new Node("b", new BlockPos(10, 64, 10), "village");
        Node c = new Node("c", new BlockPos(32, 64, 0), "village");

        assertTrue(s.add(a, b));
        assertTrue(s.add(b, c));

        // neighbors view
        Set<String> nB = s.neighbors("b");
        assertEquals(Set.of("a", "c"), nB);

        // status update
        String ab = s.all().keySet().stream().filter(k -> k.contains("a") && k.contains("b")).findFirst().orElseThrow();
        assertEquals(EdgeStorage.Status.NEW, s.getStatus(ab));
        s.setStatus(ab, EdgeStorage.Status.SUCCESS);
        assertEquals(EdgeStorage.Status.SUCCESS, s.getStatus(ab));
    }

    @Test
    void toNbt_and_fromNbt_roundTrip() {
        EdgeStorage s = new EdgeStorage(64.0);
        Node a = new Node("a", new BlockPos(0, 64, 0), "village");
        Node b = new Node("b", new BlockPos(10, 64, 10), "village");
        assertTrue(s.add(a, b));

        NbtCompound tag = s.toNbt();
        EdgeStorage restored = EdgeStorage.fromNbt(tag, 64.0);

        assertEquals(s.allWithStatus(), restored.allWithStatus());
    }
}

