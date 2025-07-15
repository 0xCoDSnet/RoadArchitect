package net.oxcodsnet.roadarchitect.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Unit tests for {@link EdgeStorageState} serialization. */
public class EdgeStorageStateTest {
    @Test
    public void testWriteAndRead() {
        EdgeStorageState state = new EdgeStorageState();
        state.getStorage().add(new BlockPos(0, 64, 0), new BlockPos(1, 64, 1));

        NbtCompound tag = state.writeNbt(new NbtCompound(), null);
        EdgeStorageState loaded = EdgeStorageState.fromNbt(tag, null);

        assertEquals(state.getStorage().asEdgeSet(), loaded.getStorage().asEdgeSet());
    }
}