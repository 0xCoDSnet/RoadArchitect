package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.oxcodsnet.roadarchitect.RoadArchitect;

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
     * @param pos позиция для нового узла
     * @return созданный узел
     */
    public Node addNodeWithEdges(BlockPos pos, String type) {
        // 1) создаём и сохраняем новую ноду
        Node newNode = this.nodeStorage.add(pos, type);
        // 2) для каждого уже существующего узла пытаемся добавить ребро
        for (Node other : this.nodeStorage.all().values()) {
            // skip self
            if (!other.id().equals(newNode.id())) {
                this.edgeStorage.add(newNode, other);
            }
        }
        // 3) помечаем состояние как изменённое для сохранения
        this.markDirty();
        return newNode;
    }

    @Override
    /**
     * Сохраняет состояние в NBT.
     * <p>Writes this state into an NBT compound.</p>
     */
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        tag.putDouble(RADIUS_KEY, edgeStorage.radius());
        tag.put(NODES_KEY, nodeStorage.toNbt());
        tag.put(EDGES_KEY, edgeStorage.toNbt());
        return tag;
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
     * Получает или создает состояние графа для мира.
     * <p>Gets or creates the road graph state for the given world.</p>
     */
    public static RoadGraphState get(ServerWorld world, double radius) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, KEY);
    }
}
