package net.oxcodsnet.roadarchitect.api.core;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.oxcodsnet.roadarchitect.storage.*;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage.Edge;

import java.util.*;

/**
 * Default read-only implementation backed by the common storages.
 */
public final class CoreApiImpl implements CoreApi {
    public static final CoreApi INSTANCE = new CoreApiImpl();

    private CoreApiImpl() {}

    @Override
    public RoadGraphView graph(ServerWorld world) {
        RoadGraphState state = RoadGraphState.get(world);
        return new RoadGraphView() {
            @Override
            public Map<String, NodeView> nodes() {
                Map<String, NodeView> out = new HashMap<>();
                for (Map.Entry<String, net.oxcodsnet.roadarchitect.storage.components.Node> e : state.nodes().all().entrySet()) {
                    out.put(e.getKey(), new NodeView(e.getKey(), e.getValue().pos(), e.getValue().type()));
                }
                return Collections.unmodifiableMap(out);
            }

            @Override
            public Map<String, EdgeView> edges() {
                Map<String, EdgeView> out = new HashMap<>();
                for (Map.Entry<String, Edge> e : state.edges().all().entrySet()) {
                    out.put(e.getKey(), new EdgeView(
                            e.getKey(),
                            e.getValue().nodeA(),
                            e.getValue().nodeB(),
                            toEdgeStatus(e.getValue().status())
                    ));
                }
                return Collections.unmodifiableMap(out);
            }

            @Override
            public Set<String> neighbors(String nodeId) {
                return Set.copyOf(state.edges().neighbors(nodeId));
            }

            @Override
            public Optional<NodeView> node(String id) {
                net.oxcodsnet.roadarchitect.storage.components.Node n = state.nodes().all().get(id);
                if (n == null) return Optional.empty();
                return Optional.of(new NodeView(id, n.pos(), n.type()));
            }

            @Override
            public Optional<NodeView> nearest(BlockPos pos, double maxDistance) {
                double max2 = maxDistance * maxDistance;
                String bestId = null;
                net.oxcodsnet.roadarchitect.storage.components.Node best = null;
                double bestD = Double.MAX_VALUE;
                for (Map.Entry<String, net.oxcodsnet.roadarchitect.storage.components.Node> e : state.nodes().all().entrySet()) {
                    BlockPos p = e.getValue().pos();
                    double dx = p.getX() - pos.getX();
                    double dz = p.getZ() - pos.getZ();
                    double d2 = dx * dx + dz * dz;
                    if (d2 <= max2 && d2 < bestD) {
                        bestD = d2;
                        bestId = e.getKey();
                        best = e.getValue();
                    }
                }
                if (best == null) return Optional.empty();
                return Optional.of(new NodeView(bestId, best.pos(), best.type()));
            }
        };
    }

    @Override
    public PathView paths(ServerWorld world) {
        PathStorage ps = PathStorage.get(world);
        return new PathView() {
            @Override
            public List<BlockPos> path(String key) {
                return List.copyOf(ps.getPath(key));
            }

            @Override
            public PathStatus status(String key) {
                return toPathStatus(ps.getStatus(key));
            }

            @Override
            public Map<String, PathStatus> allStatuses() {
                Map<String, PathStorage.Status> src = ps.allStatuses();
                Map<String, PathStatus> out = new HashMap<>();
                for (Map.Entry<String, PathStorage.Status> e : src.entrySet()) {
                    out.put(e.getKey(), toPathStatus(e.getValue()));
                }
                return Collections.unmodifiableMap(out);
            }

            @Override
            public List<String> pendingForChunk(ChunkPos chunk) {
                return List.copyOf(ps.getPendingForChunk(chunk));
            }
        };
    }

    @Override
    public BuildQueueView buildQueue(ServerWorld world) {
        RoadBuilderStorage rbs = RoadBuilderStorage.get(world);
        return chunk -> {
            List<BuildSegment> out = new ArrayList<>();
            for (RoadBuilderStorage.SegmentEntry s : rbs.getSegments(chunk)) {
                out.add(new BuildSegment(s.pathKey(), s.start(), s.end()));
            }
            return List.copyOf(out);
        };
    }

    @Override
    public DecorView decor(ServerWorld world) {
        PathDecorStorage ds = PathDecorStorage.get(world);
        return new DecorView() {
            @Override
            public long checksum(String pathKey) {
                return ds.getChecksum(pathKey);
            }

            @Override
            public double[] prefix(String pathKey) {
                double[] in = ds.getPrefix(pathKey);
                return in == null ? new double[0] : Arrays.copyOf(in, in.length);
            }

            @Override
            public byte[] groundMask(String pathKey) {
                byte[] in = ds.getGroundMask(pathKey);
                return in == null ? new byte[0] : Arrays.copyOf(in, in.length);
            }

            @Override
            public byte[] waterInteriorMask(String pathKey) {
                byte[] in = ds.getWaterInteriorMask(pathKey);
                return in == null ? new byte[0] : Arrays.copyOf(in, in.length);
            }
        };
    }

    private static EdgeStatus toEdgeStatus(EdgeStorage.Status s) {
        return switch (s) {
            case NEW -> EdgeStatus.NEW;
            case SUCCESS -> EdgeStatus.SUCCESS;
            case FAILURE -> EdgeStatus.FAILURE;
        };
    }

    private static PathStatus toPathStatus(PathStorage.Status s) {
        return switch (s) {
            case PENDING -> PathStatus.PENDING;
            case PROCESSING -> PathStatus.PROCESSING;
            case READY -> PathStatus.READY;
            case FAILED -> PathStatus.FAILED;
        };
    }
}
