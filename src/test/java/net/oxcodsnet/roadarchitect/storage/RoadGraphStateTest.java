package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link RoadGraphState}.
 * <p>Tests for {@link RoadGraphState}.</p>
 */
class RoadGraphStateTest {
    /**
     * Проверяет сериализацию и десериализацию состояния.
     * <p>Tests serialization and deserialization of the state.</p>
     */
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

    @Test
    void testAddNodeWithEdges() {
        // подготовка: создаём фиктивный серверный мир и RoadGraphState
        RoadGraphState state = new RoadGraphState(10.0);
        // заранее добавим одну ноду
        Node a = state.addNodeWithEdges(new BlockPos(0, 64, 0));
        assertTrue(state.edges().all().isEmpty());
        // добавляем вторую в пределах радиуса 10
        Node b = state.addNodeWithEdges(new BlockPos(5, 64, 5));
        // теперь между a и b должно появиться ребро
        assertTrue(state.edges().neighbors(a.id()).contains(b.id()));
        assertTrue(state.edges().neighbors(b.id()).contains(a.id()));
    }
}
