package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PathFinder}.
 */
class PathFinderTest {
    @Test
    void findSimplePath() {
        NodeStorage nodes = new NodeStorage();
        Node a = nodes.add(new BlockPos(0, 64, 0), "test");
        Node b = nodes.add(new BlockPos(0, 64, 1), "test");
        Node c = nodes.add(new BlockPos(0, 64, 2), "test");

        EdgeStorage edges = new EdgeStorage(5.0);
        edges.add(a, b);
        edges.add(b, c);

        PathFinder.SurfaceProvider provider = (x, z) -> 64;
        PathFinder finder = new PathFinder(nodes, edges, provider);

        List<BlockPos> path = finder.findPath(a.id(), c.id());
        assertEquals(List.of(new BlockPos(0, 64, 0), new BlockPos(0, 64, 1), new BlockPos(0, 64, 2)), path);
    }

    @Test
    void pathNotFound() {
        NodeStorage nodes = new NodeStorage();
        Node a = nodes.add(new BlockPos(0, 64, 0), "test");
        Node b = nodes.add(new BlockPos(10, 64, 0), "test");
        nodes.add(new BlockPos(20, 64, 0), "test");

        EdgeStorage edges = new EdgeStorage(5.0);
        edges.add(a, b);
        // third node is isolated

        PathFinder.SurfaceProvider provider = (x, z) -> 64;
        PathFinder finder = new PathFinder(nodes, edges, provider);

        List<BlockPos> path = finder.findPath(a.id(), "missing-id");
        assertTrue(path.isEmpty());
    }

    @Test
    void pathUsesGeneratorHeights() {
        NodeStorage nodes = new NodeStorage();
        Node a = nodes.add(new BlockPos(0, 0, 0), "test");
        Node b = nodes.add(new BlockPos(0, 0, 1), "test");
        Node c = nodes.add(new BlockPos(0, 0, 2), "test");

        EdgeStorage edges = new EdgeStorage(5.0);
        edges.add(a, b);
        edges.add(b, c);

        class RecordingGenerator {
            private final List<int[]> calls = new ArrayList<>();

            int getHeight(int x, int z) {
                calls.add(new int[]{x, z});
                return 70;
            }
        }
        RecordingGenerator generator = new RecordingGenerator();

        PathFinder.SurfaceProvider provider = generator::getHeight;
        PathFinder finder = new PathFinder(nodes, edges, provider);

        List<BlockPos> path = finder.findPath(a.id(), c.id());
        assertEquals(List.of(new BlockPos(0, 70, 0), new BlockPos(0, 70, 1), new BlockPos(0, 70, 2)), path);
    }
}
