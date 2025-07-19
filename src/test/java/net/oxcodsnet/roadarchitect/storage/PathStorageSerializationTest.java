package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests serialization of {@link PathStorage}.
 */
class PathStorageSerializationTest {
    @Test
    void roundTrip() {
        PathStorage storage = new PathStorage();
        storage.putPath("a", "b", List.of(BlockPos.ORIGIN, new BlockPos(1, 64, 1)));

        NbtCompound tag = storage.writeNbt(new NbtCompound(), null);
        PathStorage loaded = PathStorage.fromNbt(tag, null);

        assertEquals(storage.getPath("a", "b"), loaded.getPath("a", "b"));
    }
}
