package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests serialization of {@link RoadBuilderStorage}.
 */
class RoadBuilderStorageSerializationTest {
    @Test
    void roundTrip() {
        RoadBuilderStorage storage = new RoadBuilderStorage();
        storage.addTask("a", "b", 5);

        NbtCompound tag = storage.writeNbt(new NbtCompound(), null);
        RoadBuilderStorage loaded = RoadBuilderStorage.fromNbt(tag, null);

        assertTrue(loaded.hasTask(RoadBuilderStorage.makeKey("a", "b")));
        assertEquals(5, loaded.getProgress(RoadBuilderStorage.makeKey("a", "b")));
    }
}
