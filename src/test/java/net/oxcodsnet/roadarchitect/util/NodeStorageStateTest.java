package net.oxcodsnet.roadarchitect.util;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link NodeStorageState} serialization.
 */
public class NodeStorageStateTest {
    @Test
    public void testWriteAndRead() {
        NodeStorageState state = new NodeStorageState();
        state.getStorage().add(new BlockPos(1, 64, 2), "village");
        state.getStorage().add(new BlockPos(3, 70, 5), "outpost");

        NbtCompound tag = state.writeNbt(new NbtCompound(), null);
        NodeStorageState loaded = NodeStorageState.fromNbt(tag, null);

        assertEquals(state.getStorage().asNodeSet(), loaded.getStorage().asNodeSet());
    }
}