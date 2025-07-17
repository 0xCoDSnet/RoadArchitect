package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoadGraphStateTest {
    @Test
    void roundTrip() {
        RoadGraphState state = new RoadGraphState(5.0);
        Node a = state.nodes().add(BlockPos.ORIGIN);
        Node b = state.nodes().add(new BlockPos(9, 64, 0));
        state.edges().add(a, b);

        NbtCompound tag = state.writeNbt(new NbtCompound(), null);
        RoadGraphState loaded = RoadGraphState.fromNbt(tag, null);

        assertEquals(state.nodes().all(), loaded.nodes().all());
        assertEquals(state.edges().all(), loaded.edges().all());
        assertEquals(state.edges().radius(), loaded.edges().radius());
    }
}
