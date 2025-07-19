package net.oxcodsnet.roadarchitect.util;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.storage.NodeStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Поиск пути по уже вычисленным рёбрам.
 * <p>Finds a path using pre-computed edges.</p>
 */
public class PathFinder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID+"/PathFinder");

    private final NodeStorage nodes;
    private final EdgeStorage edges;
    private final SurfaceProvider surface;

    /**
     * Создаёт новый PathFinder с указанными хранилищами и провайдером поверхности.
     * <p>Constructs a new PathFinder with the given storages and surface provider.</p>
     */
    public PathFinder(NodeStorage nodes, EdgeStorage edges, SurfaceProvider surface) {
        this.nodes = nodes;
        this.edges = edges;
        this.surface = surface;
    }

    /**
     * Находит путь между двумя узлами. Возвращает список позиций по поверхности
     * или пустой список, если путь не найден.
     * <p>Finds a path between two nodes. Returns the list of surface positions
     * or an empty list if the path cannot be found.</p>
     *
     * @param fromId id начального узла / start node id
     * @param toId   id конечного узла / target node id
     * @return список точек пути / list of path positions
     */
    public List<BlockPos> findPath(String fromId, String toId) {
        List<String> nodeIds = bfs(fromId, toId);
        if (nodeIds.isEmpty()) {
            return List.of();
        }

        List<BlockPos> path = new ArrayList<>();
        for (int i = 0; i < nodeIds.size() - 1; i++) {
            Node a = nodes.all().get(nodeIds.get(i));
            Node b = nodes.all().get(nodeIds.get(i + 1));
            if (a == null || b == null) {
                LOGGER.warn("Missing node for path segment {} -> {}", nodeIds.get(i), nodeIds.get(i + 1));
                return List.of();
            }
            addSegment(path, a.pos(), b.pos());
        }
        if (path.isEmpty()) {
            Node single = nodes.all().get(nodeIds.getFirst());
            if (single != null) {
                int y = surface.getSurfaceY(single.pos().getX(), single.pos().getZ());
                path.add(new BlockPos(single.pos().getX(), y, single.pos().getZ()));
            }
        }
        return path;
    }

    /**
     * Выполняет поиск в ширину между двумя узлами графа.
     * <p>Performs a breadth-first search between two graph nodes.</p>
     */
    private List<String> bfs(String fromId, String toId) {
        if (!nodes.all().containsKey(fromId) || !nodes.all().containsKey(toId)) {
            return List.of();
        }

        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new ArrayDeque<>();

        queue.add(fromId);
        visited.add(fromId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            if (current.equals(toId)) {
                break;
            }

            Node currentNode = nodes.all().get(current);
            if (currentNode == null) {
                continue;
            }

            for (String neighbor : edges.neighbors(current)) {
                if (visited.contains(neighbor)) {
                    continue;
                }
                Node neighborNode = nodes.all().get(neighbor);
                if (neighborNode == null) {
                    continue;
                }
                if (canTraverse(currentNode, neighborNode)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        if (!visited.contains(toId)) {
            LOGGER.debug("Path not found from {} to {}", fromId, toId);
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String at = toId; at != null; at = parent.get(at)) {
            result.add(at);
            if (at.equals(fromId)) {
                break;
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Determines whether the edge between two nodes can be traversed based on
     * terrain height differences.
     */
    private boolean canTraverse(Node a, Node b) {
        int y1 = surface.getSurfaceY(a.pos().getX(), a.pos().getZ());
        int y2 = surface.getSurfaceY(b.pos().getX(), b.pos().getZ());
        return Math.abs(y1 - y2) <= 16;
    }

    /**
     * Добавляет линейный сегмент пути между двумя точками.
     * <p>Adds a straight path segment between two points.</p>
     */
    private void addSegment(List<BlockPos> out, BlockPos start, BlockPos end) {
        int x1 = start.getX();
        int z1 = start.getZ();
        int x2 = end.getX();
        int z2 = end.getZ();

        int dx = Math.abs(x2 - x1);
        int dz = Math.abs(z2 - z1);
        int sx = x1 < x2 ? 1 : -1;
        int sz = z1 < z2 ? 1 : -1;
        int err = dx - dz;

        int x = x1;
        int z = z1;
        while (true) {
            int y = surface.getSurfaceY(x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (out.isEmpty() || !out.getLast().equals(pos)) {
                out.add(pos);
            }
            if (x == x2 && z == z2) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                z += sz;
            }
        }
    }

    /**
     * Источник высоты поверхности для расчёта пути.
     * <p>Provider of surface height values for path calculation.</p>
     */
    public interface SurfaceProvider {
        int getSurfaceY(int x, int z);
    }

    /**
     * Реализация {@link SurfaceProvider}, использующая данные мира.
     * Значения высоты вычисляются через генератор чанков, поэтому
     * доступны даже для ещё не сгенерированных участков.
     * <p>Surface provider that delegates to a {@link ServerWorld}. Heights are
     * computed via the world's {@link net.minecraft.world.gen.chunk.ChunkGenerator}
     * and therefore available for ungenerated chunks.</p>
     */
    public static class WorldSurfaceProvider implements SurfaceProvider {
        private final ServerWorld world;

        public WorldSurfaceProvider(ServerWorld world) {
            this.world = world;
        }

        @Override
        public int getSurfaceY(int x, int z) {
            return world.getChunkManager().getChunkGenerator()
                    .getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world,
                            world.getChunkManager().getNoiseConfig());
        }
    }
}

