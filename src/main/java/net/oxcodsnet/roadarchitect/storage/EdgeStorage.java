package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores edges between nodes. An edge is valid when the circles of radius R around
 * the nodes intersect.
 */
public class EdgeStorage {
    private final double radius;
    private final Map<String, Set<String>> edges = new ConcurrentHashMap<>();

    /**
     * Creates storage for edges using the specified radius.
     *
     * @param radius radius used to validate connections
     */
    public EdgeStorage(double radius) {
        this.radius = radius;
    }

    /**
     * Adds an edge if the nodes are within intersection distance.
     *
     * @param a first node
     * @param b second node
     * @return {@code true} if the edge was added
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
     * Removes the edge between the two ids if present.
     *
     * @param id1 first node id
     * @param id2 second node id
     * @return {@code true} if the edge was removed
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
     * Returns an unmodifiable map of all edges.
     *
     * @return map from node id to connected node ids
     */
    public Map<String, Set<String>> all() {
        Map<String, Set<String>> copy = new HashMap<>();
        for (var entry : edges.entrySet()) {
            copy.put(entry.getKey(), Set.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Returns an unmodifiable view of neighbors for the given node.
     *
     * @param id node id
     * @return set of neighbor ids
     */
    public Set<String> neighbors(String id) {
        Set<String> set = edges.get(id);
        return set == null ? Set.of() : Collections.unmodifiableSet(set);
    }

    /**
     * Removes all stored edges.
     */
    public void clear() {
        edges.clear();
    }

    private boolean intersects(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        double distanceSquared = dx * dx + dz * dz;
        double diameter = radius * 2.0;
        return distanceSquared <= diameter * diameter;
    }
}
