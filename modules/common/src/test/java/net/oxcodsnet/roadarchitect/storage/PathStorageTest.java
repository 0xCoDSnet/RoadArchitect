package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PathStorageTest {

    @Test
    void put_get_status_update() {
        PathStorage s = new PathStorage();
        List<BlockPos> path = List.of(new BlockPos(0, 64, 0), new BlockPos(4, 64, 4));

        s.putPath("a", "b", path, PathStorage.Status.READY);
        assertEquals(path, s.getPath("a", "b"));
        assertEquals(PathStorage.Status.READY, s.getStatus("a|b"));

        s.setStatus("a|b", PathStorage.Status.PROCESSING);
        assertEquals(PathStorage.Status.PROCESSING, s.getStatus("a|b"));
    }

    @Test
    void writeNbt_and_fromNbt_roundTrip() {
        PathStorage s = new PathStorage();
        s.putPath("a", "b", List.of(new BlockPos(0, 64, 0), new BlockPos(8, 64, 8)), PathStorage.Status.READY);
        s.putPath("b", "c", List.of(), PathStorage.Status.PENDING);

        NbtCompound tag = new NbtCompound();
        NbtCompound out = s.writeNbt(tag, null);
        assertTrue(out.contains("paths", NbtElement.LIST_TYPE));

        PathStorage restored = PathStorage.fromNbt(out, null);
        assertEquals(s.getPath("a", "b"), restored.getPath("a", "b"));
        assertEquals(s.allStatuses(), restored.allStatuses());
    }
}

