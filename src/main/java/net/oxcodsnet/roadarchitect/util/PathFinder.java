package net.oxcodsnet.roadarchitect.util;

import java.util.*;

import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;

/** Simple A* pathfinding utility. */
public final class PathFinder {
    private static final int MAX_DISTANCE = 2048;

    private PathFinder() {
    }

    /**
     * Environment abstraction used by the A* search.
     */
    public interface Environment {
        Iterable<BlockPos> neighbors(BlockPos pos);

        double cost(BlockPos from, BlockPos to);

        double heuristic(BlockPos pos, BlockPos goal);
    }

    /**
     * Finds a path using the given environment.
     *
     * @param start start node
     * @param goal  goal node
     * @param env   environment providing neighbors and costs
     * @return list of positions from start to goal or empty if no path exists
     */
    public static List<BlockPos> findPath(BlockPos start, BlockPos goal, Environment env) {
        if (start.getManhattanDistance(goal) > MAX_DISTANCE) {
            return List.of();
        }

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, BlockPos> cameFrom = new HashMap<>();
        Map<BlockPos, Double> gScore = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        gScore.put(start, 0.0);
        open.add(new Node(start, 0.0, env.heuristic(start, goal)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.pos.equals(goal)) {
                return reconstruct(cameFrom, current.pos);
            }
            if (!closed.add(current.pos)) {
                continue;
            }

            for (BlockPos neighbor : env.neighbors(current.pos)) {
                if (closed.contains(neighbor)) {
                    continue;
                }

                double tentative = gScore.get(current.pos) + env.cost(current.pos, neighbor);
                if (tentative >= gScore.getOrDefault(neighbor, Double.POSITIVE_INFINITY)) {
                    continue;
                }

                cameFrom.put(neighbor, current.pos);
                gScore.put(neighbor, tentative);
                double f = tentative + env.heuristic(neighbor, goal);
                open.add(new Node(neighbor, tentative, f));
            }
        }
        return List.of();
    }

    private static List<BlockPos> reconstruct(Map<BlockPos, BlockPos> cameFrom, BlockPos current) {
        List<BlockPos> path = new ArrayList<>();
        path.add(current);
        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            path.add(current);
        }
        Collections.reverse(path);
        return path;
    }

    private record Node(BlockPos pos, double g, double f) {
    }

    /**
     * Environment implementation using a {@link ServerWorld} surface.
     */
    public static class WorldEnvironment implements Environment {
        private final ServerWorld world;

        public WorldEnvironment(ServerWorld world) {
            this.world = world;
        }

        @Override
        public Iterable<BlockPos> neighbors(BlockPos pos) {
            List<BlockPos> result = new ArrayList<>(4);
            int[][] dirs = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };
            for (int[] d : dirs) {
                int x = pos.getX() + d[0];
                int z = pos.getZ() + d[1];
                if (this.world.isChunkLoaded(x >> 4, z >> 4)) {
                    continue; // skip already generated chunks
                }
                int y = this.world.getChunkManager().getChunkGenerator()
                        .getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG,
                                this.world, this.world.getChunkManager().getNoiseConfig());
                result.add(new BlockPos(x, y, z));
            }
            return result;
        }

        @Override
        public double cost(BlockPos from, BlockPos to) {
            double cost = 1.0;
            if (this.world.getFluidState(to).isIn(FluidTags.WATER)) {
                cost += 5.0; // avoid water when possible
            }
            int diff = Math.abs(to.getY() - from.getY());
            if (to.getY() > this.world.getSeaLevel() + 30 || diff > 4) {
                cost += 2.0; // discourage mountains
            }
            return cost;
        }

        @Override
        public double heuristic(BlockPos pos, BlockPos goal) {
            return pos.getManhattanDistance(goal);
        }
    }
}