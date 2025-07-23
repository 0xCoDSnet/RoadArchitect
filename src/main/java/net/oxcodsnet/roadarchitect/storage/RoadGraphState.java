package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.components.Node;

/**
 * Сохраняет узлы и рёбра дорог как {@link PersistentState}.
 * <p>Stores road nodes and edges as a {@link PersistentState}.</p>
 */
public class RoadGraphState extends PersistentState {
    private static final String KEY = "road_graph";
    private static final String NODES_KEY = "nodes";
    private static final String EDGES_KEY = "edges";
    private static final String RADIUS_KEY = "radius";

    public static final Type<RoadGraphState> TYPE = new Type<>(
            () -> new RoadGraphState(RoadArchitect.CONFIG.maxConnectionDistance()),
            RoadGraphState::fromNbt,
            DataFixTypes.SAVED_DATA_SCOREBOARD
    );

    private final NodeStorage nodeStorage;
    private final EdgeStorage edgeStorage;

    /**
     * Создает новое состояние графа дорог с указанным радиусом соединений.
     * <p>Creates a new road graph state with the given connection radius.</p>
     */
    public RoadGraphState(double radius) {
        this(new NodeStorage(), new EdgeStorage(radius));
    }

    /**
     * Внутренний конструктор с заданными хранилищами.
     * <p>Internal constructor using the provided storages.</p>
     */
    private RoadGraphState(NodeStorage nodes, EdgeStorage edges) {
        this.nodeStorage = nodes;
        this.edgeStorage = edges;
    }



    /**
     * Получает или создает состояние графа для мира.
     * <p>Gets or creates the road graph state for the given world.</p>
     */
    public static RoadGraphState get(ServerWorld world, double radius) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, KEY);
    }

    /**
     * Возвращает хранилище узлов.
     * <p>Returns the node storage.</p>
     */
    public NodeStorage nodes() {

        return nodeStorage;
    }

    /**
     * Возвращает хранилище рёбер.
     * <p>Returns the edge storage.</p>
     */
    public EdgeStorage edges() {
        return edgeStorage;
    }

    /**
     * Добавляет новый узел и сразу строит с ним все допустимые рёбра
     *
     * @param pos позиция для нового узла
     * @return созданный узел
     */
    public Node addNodeWithEdges(BlockPos pos, String type) {
        Node newNode = this.nodeStorage.add(pos, type);
        for (Node other : this.nodeStorage.all().values()) {
            if (!other.id().equals(newNode.id())) {
                connect(newNode, other);
            }
        }
        this.markDirty();
        return newNode;
    }

    /**
     * Пытается соединить два узла, запрещая «крестовые» рёбра.
     */
    public void connect(Node NodeA, Node NodeB) {

        String idNodeA = NodeA.id();
        String idNodeB = NodeB.id();
        if (NodeA == null || NodeB == null || idNodeA.equals(idNodeB)) {
            return;
        }

        // 1) проверяем радиус
        double dx = NodeA.pos().getX() - NodeB.pos().getX();
        double dz = NodeA.pos().getZ() - NodeB.pos().getZ();
        double max = edgeStorage.radius() * 2.0;
        if (dx * dx + dz * dz > max * max) {
            return;
        }

        // 2) уже существует?
        if (edgeStorage.all().containsKey(edgeId(idNodeA, idNodeB))) {
            return;
        }

        // 3) пересекает ли новое ребро какие-нибудь существующие?
        for (EdgeStorage.Edge e : edgeStorage.all().values()) {
            if (e.connects(idNodeA) || e.connects(idNodeB)) continue;
            Node n1 = nodeStorage.all().get(e.nodeA());
            Node n2 = nodeStorage.all().get(e.nodeB());
            if (n1 == null || n2 == null) continue;

            if (segmentsIntersect2D(NodeA.pos(), NodeB.pos(), n1.pos(), n2.pos())) {
                return;
            }
        }

        // 4) всё чисто — делегируем фактическое создание
        boolean added = edgeStorage.add(NodeA, NodeB);
        if (added) this.markDirty();
    }

    /*========== helpers ==========*/

    private static String edgeId(String idA, String idB) {
        return idA.compareTo(idB) < 0 ? idA + "+" + idB : idB + "+" + idA;
    }

    private static boolean segmentsIntersect2D(BlockPos p1, BlockPos p2,
                                               BlockPos p3, BlockPos p4) {
        return linesIntersect(
                p1.getX(), p1.getZ(), p2.getX(), p2.getZ(),
                p3.getX(), p3.getZ(), p4.getX(), p4.getZ());
    }

    /** Классический O(1) тест пересечения двух отрезков на плоскости. */
    private static boolean linesIntersect(int x1, int y1, int x2, int y2,
                                          int x3, int y3, int x4, int y4) {
        long d1 = direction(x3, y3, x4, y4, x1, y1);
        long d2 = direction(x3, y3, x4, y4, x2, y2);
        long d3 = direction(x1, y1, x2, y2, x3, y3);
        long d4 = direction(x1, y1, x2, y2, x4, y4);
        if (d1 * d2 < 0 && d3 * d4 < 0) return true;
        return (d1 == 0 && onSeg(x3, y3, x4, y4, x1, y1)) ||
                (d2 == 0 && onSeg(x3, y3, x4, y4, x2, y2)) ||
                (d3 == 0 && onSeg(x1, y1, x2, y2, x3, y3)) ||
                (d4 == 0 && onSeg(x1, y1, x2, y2, x4, y4));
    }

    private static long direction(int ax, int ay, int bx, int by, int cx, int cy) {
        return (long) (cx - ax) * (by - ay) - (long) (cy - ay) * (bx - ax);
    }

    private static boolean onSeg(int ax, int ay, int bx, int by, int cx, int cy) {
        return Math.min(ax, bx) <= cx && cx <= Math.max(ax, bx) &&
                Math.min(ay, by) <= cy && cy <= Math.max(ay, by);
    }



    /**
     * Восстанавливает состояние графа из NBT.
     * <p>Restores the road graph state from NBT.</p>
     */
    public static RoadGraphState fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        double radius = tag.getDouble(RADIUS_KEY);
        NodeStorage nodes = NodeStorage.fromNbt(tag.getList(NODES_KEY, NbtElement.COMPOUND_TYPE));
        EdgeStorage edges = EdgeStorage.fromNbt(tag.getCompound(EDGES_KEY), radius);
        return new RoadGraphState(nodes, edges);
    }
    /**
     * Сохраняет состояние в NBT.
     * <p>Writes this state into an NBT compound.</p>
     */
    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        tag.putDouble(RADIUS_KEY, edgeStorage.radius());
        tag.put(NODES_KEY, nodeStorage.toNbt());
        tag.put(EDGES_KEY, edgeStorage.toNbt());
        return tag;
    }
}
