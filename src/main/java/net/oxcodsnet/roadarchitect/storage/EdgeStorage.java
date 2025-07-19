package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит соединения между узлами. Ребро считается допустимым, если окружности
 * радиуса R вокруг узлов пересекаются.
 * <p>Stores edges between nodes. An edge is valid when circles of radius R
 * around the nodes intersect.</p>
 */
public class EdgeStorage {
    private final double radius;
    private final Map<String, Set<String>> edges = new ConcurrentHashMap<>();

    /**
     * Создает хранилище рёбер с указанным радиусом проверки.
     * <p>Creates storage for edges using the specified radius.</p>
     *
     * @param radius радиус для проверки соединений / radius used to validate connections
     */
    public EdgeStorage(double radius) {
        this.radius = radius;
    }

    /**
     * Загружает хранилище рёбер из объекта NBT.
     * <p>Loads edge storage from the given NBT compound.</p>
     */
    public static EdgeStorage fromNbt(NbtCompound tag, double radius) {
        EdgeStorage storage = new EdgeStorage(radius);
        for (String key : tag.getKeys()) {
            NbtList list = tag.getList(key, NbtElement.STRING_TYPE);
            Set<String> set = storage.edges.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet());
            for (int i = 0; i < list.size(); i++) {
                set.add(list.getString(i));
            }
        }
        return storage;
    }

    /**
     * Возвращает используемый радиус проверки соединений.
     * <p>Returns the radius used to validate connections.</p>
     */
    public double radius() {
        return radius;
    }

    /**
     * Добавляет ребро, если узлы находятся на допустимом расстоянии.
     * <p>Adds an edge if the nodes are within intersection distance.</p>
     *
     * @param a первый узел / first node
     * @param b второй узел / second node
     * @return {@code true} если ребро было добавлено / {@code true} if the edge was added
     */
    public boolean add(Node a, Node b) {
        if (a.id().equals(b.id())) {
            return false;
        }
        if (!intersects(a.pos(), b.pos())) {
            return false;
        }
        edges.computeIfAbsent(a.id(), k -> ConcurrentHashMap.newKeySet()).add(b.id());
        edges.computeIfAbsent(b.id(), k -> ConcurrentHashMap.newKeySet()).add(a.id());
        return true;
    }

    /**
     * Удаляет ребро между указанными узлами, если оно существует.
     * <p>Removes the edge between the two ids if present.</p>
     *
     * @param id1 идентификатор первого узла / first node id
     * @param id2 идентификатор второго узла / second node id
     * @return {@code true} если ребро было удалено / {@code true} if the edge was removed
     */
    public boolean remove(String id1, String id2) {
        boolean removed = false;
        Set<String> a = edges.get(id1);
        if (a != null) {
            removed |= a.remove(id2);
            if (a.isEmpty()) {
                edges.remove(id1);
            }
        }
        Set<String> b = edges.get(id2);
        if (b != null) {
            removed |= b.remove(id1);
            if (b.isEmpty()) {
                edges.remove(id2);
            }
        }
        return removed;
    }

    /**
     * Возвращает неизменяемую карту всех рёбер.
     * <p>Returns an unmodifiable map of all edges.</p>
     *
     * @return карта от id узла к соседним id / map from node id to connected node ids
     */
    public Map<String, Set<String>> all() {
        Map<String, Set<String>> copy = new HashMap<>();
        for (var entry : edges.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Возвращает неизменяемое множество соседей указанного узла.
     * <p>Returns an unmodifiable view of neighbors for the given node.</p>
     *
     * @param id идентификатор узла / node id
     * @return множество id соседей / set of neighbor ids
     */
    public Set<String> neighbors(String id) {
        Set<String> set = edges.get(id);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    /**
     * Удаляет все сохранённые рёбра.
     * <p>Removes all stored edges.</p>
     */
    public void clear() {
        edges.clear();
    }

    /**
     * Сериализует все рёбра в объект NBT.
     * <p>Serializes all edges into an NBT compound.</p>
     */
    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
            NbtList list = new NbtList();
            for (String id : entry.getValue()) {
                list.add(NbtString.of(id));
            }
            tag.put(entry.getKey(), list);
        }
        return tag;
    }

    /**
     * Проверяет, пересекаются ли окружности узлов.
     * <p>Checks whether the circles around the given nodes intersect.</p>
     */
    private boolean intersects(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        double distanceSquared = dx * dx + dz * dz;
        double diameter = radius * 2.0;
        return distanceSquared <= diameter * diameter;
    }
}
