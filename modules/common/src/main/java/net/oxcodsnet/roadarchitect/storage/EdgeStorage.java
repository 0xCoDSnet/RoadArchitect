package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.NbtUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит двоичные рёбра между узлами. Каждому ребру соответствует <b>единственная</b>
 * запись {@link Edge}, содержащая оба узла и текущий {@link Status}.
 * <p/>
 * Edge‑id формируется как «min(idA,idB)+"+"+max(idA,idB)» — это гарантирует, что
 * для пары узлов всегда получится одинаковый ключ, независимо от их порядка.
 * <p/>
 * Сериализация выполняется в NBT‑map, где ключи — edge‑id, а значения — Compound
 * с полями <code>a</code>, <code>b</code> и <code>status</code>.
 */
public class EdgeStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + EdgeStorage.class.getSimpleName());

    /**
     * Радиус, внутри которого допускается соединение узлов.
     */
    private final double radius;

    /**
     * Все рёбра по edge‑id.
     */
    private final Map<String, Edge> edges = new ConcurrentHashMap<>();

    public EdgeStorage(double radius) {
        this.radius = radius;
    }

    public static EdgeStorage fromNbt(NbtCompound tag, double radius) {
        EdgeStorage storage = new EdgeStorage(radius);
        for (String edgeId : tag.getKeys()) {
            NbtCompound entry = tag.getCompound(edgeId);
            String a = entry.getString("a");
            String b = entry.getString("b");
            Status status = NbtUtils.getEnumOrDefault(entry, "status", Status.class, Status.NEW);
            storage.edges.put(edgeId, new Edge(a, b, status));
        }
        return storage;
    }

    /* ───────────────────────────── Edge API ───────────────────────────── */

    public double radius() {
        return radius;
    }

    /**
     * Пытается добавить ребро между двумя узлами. Возвращает <code>true</code>,
     * если ребро было создано (или уже существовало).
     */
    public boolean add(Node a, Node b) {
        if (a.id().equals(b.id())) return false; // нельзя соединять узел сам с собой

        String edgeId = KeyUtil.edgeKey(a.id(), b.id());
        edges.put(edgeId, new Edge(a.id(), b.id(), Status.NEW));
        return true;
    }

    /* ───────────────────────────── CRUD ───────────────────────────── */

    /**
     * Удаляет ребро по его edge‑id.
     */
    public boolean remove(String edgeId) {
        return edges.remove(edgeId) != null;
    }

    /**
     * Возвращает статус ребра или <code>null</code>, если ребра нет.
     */
    public Status getStatus(String edgeId) {
        Edge edge = edges.get(edgeId);
        return edge == null ? null : edge.status();
    }

    /**
     * Обновляет статус существующего ребра.
     */
    public void setStatus(String edgeId, Status status) {
        Edge old = edges.get(edgeId);
        if (old != null) {
            edges.put(edgeId, new Edge(old.nodeA(), old.nodeB(), status));
        }
    }

    /**
     * Возвращает неизменяемое множество соседей указанного узла.
     */
    public Set<String> neighbors(String nodeId) {
        Set<String> set = new HashSet<>();
        for (Edge edge : edges.values()) {
            if (edge.connects(nodeId)) {
                set.add(edge.other(nodeId));
            }
        }
        return Collections.unmodifiableSet(set);
    }

    /* ───────────────────────────── Query helpers ───────────────────────────── */

    /**
     * Карта edgeId → Edge (неизменяемая копия).
     */
    public Map<String, Edge> all() {
        return Collections.unmodifiableMap(new HashMap<>(edges));
    }

    /**
     * Карта edgeId → Status (неизменяемая копия).
     */
    public Map<String, Status> allWithStatus() {
        Map<String, Status> map = new HashMap<>();
        for (Map.Entry<String, Edge> e : edges.entrySet()) {
            map.put(e.getKey(), e.getValue().status());
        }
        return Collections.unmodifiableMap(map);
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (Edge edge : edges.values()) {
            NbtCompound entry = new NbtCompound();
            entry.putString("a", edge.nodeA());
            entry.putString("b", edge.nodeB());
            entry.putString("status", edge.status().name());
            tag.put(edge.id(), entry);
        }
        return tag;
    }

    /* ───────────────────────────── Persistence ───────────────────────────── */

    public void clear() {
        edges.clear();
    }

    /**
     * Статус, используемый алгоритмом поиска пути.
     */
    public enum Status {
        NEW,
        SUCCESS,
        FAILURE
    }

    /* ───────────────────────────── Internals ───────────────────────────── */

    /**
     * Единственная запись, представляющая ребро между двумя узлами.
     */
    public record Edge(String nodeA, String nodeB, Status status) {
        public String id() {
            return KeyUtil.edgeKey(nodeA, nodeB);
        }

        public boolean connects(String nodeId) {
            return nodeA.equals(nodeId) || nodeB.equals(nodeId);
        }

        public String other(String nodeId) {
            return nodeA.equals(nodeId) ? nodeB : nodeA;
        }
    }
}
