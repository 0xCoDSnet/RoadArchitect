package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.Bootstrap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.ChunkPos;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import net.oxcodsnet.roadarchitect.util.KeyUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests serialization of {@link RoadBuilderStorage}.
 */
class RoadBuilderStorageSerializationTest {
    @Test
    @Disabled("Requires game bootstrap in Minecraft 1.21.7")
    void roundTrip() {
        Bootstrap.initialize();
        RoadBuilderStorage storage = new RoadBuilderStorage();
        ChunkPos chunk = new ChunkPos(0, 0);
        String key = KeyUtil.pathKey("a", "b");
        storage.addSegment(chunk, key, 1, 5);

        NbtCompound tag = storage.writeNbt(new NbtCompound(), null);
        RoadBuilderStorage loaded = RoadBuilderStorage.fromNbt(tag, null);

        RoadBuilderStorage.SegmentEntry entry = loaded.getSegments(chunk).getFirst();
        assertEquals(key, entry.pathKey());
        assertEquals(1, entry.start());
        assertEquals(5, entry.end());
    }
}
