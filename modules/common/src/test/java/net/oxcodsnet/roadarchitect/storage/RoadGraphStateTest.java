package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoadGraphStateTest {

    @Test
    void connect_respectsRadiusAndAvoidsCrossing() {
        // radius = 16 → maxDistanceSquared check uses (radius*2)^2 inside state.connect
        RoadGraphState state = new RoadGraphState(16.0);

        // Build two non-crossing edges: A-B and C-D
        Node a = state.nodes().add(new BlockPos(0, 64, 0), "village");
        Node b = state.nodes().add(new BlockPos(8, 64, 0), "village");
        Node c = state.nodes().add(new BlockPos(0, 64, 8), "village");
        Node d = state.nodes().add(new BlockPos(8, 64, 8), "village");

        state.connect(a, b);
        state.connect(c, d);
        assertEquals(2, state.edges().all().size());

        // Now try to add crossing edge A-D → should be denied
        state.connect(a, d);
        assertEquals(2, state.edges().all().size(), "Crossing edge must not be added");

        // Too far apart: A-(100,0) must be denied by radius
        Node far = state.nodes().add(new BlockPos(100, 64, 0), "village");
        state.connect(a, far);
        assertEquals(2, state.edges().all().size(), "Edge beyond radius must not be added");
    }

    @Test
    void addNodeWithEdges_connectsToExistingWithinRadius() {
        RoadGraphState state = new RoadGraphState(32.0);
        Node a = state.nodes().add(new BlockPos(0, 64, 0), "village");
        Node b = state.nodes().add(new BlockPos(20, 64, 0), "village");

        // Add new node near both A and B → edges should appear if not crossing
        Node c = state.addNodeWithEdges(new BlockPos(10, 64, 0), "village");
        // Expect edges A-C and C-B
        assertEquals(2, state.edges().all().size());
        assertTrue(state.edges().neighbors(c.id()).contains(a.id()));
        assertTrue(state.edges().neighbors(c.id()).contains(b.id()));
    }
}

