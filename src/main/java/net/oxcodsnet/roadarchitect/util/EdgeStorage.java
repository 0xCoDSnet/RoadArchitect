package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Efficient container for storing undirected edges between {@link BlockPos} nodes.
 */
public class EdgeStorage {
    private final Long2ObjectOpenHashMap<LongOpenHashSet> adjacency = new Long2ObjectOpenHashMap<>();

    /**
     * Adds an undirected edge between the given positions.
     *
     * @param a the first node
     * @param b the second node
     * @return {@code true} if the edge was added, {@code false} if it already existed
     */
    public boolean add(BlockPos a, BlockPos b) {
        long packedA = a.asLong();
        long packedB = b.asLong();
        LongOpenHashSet setA = this.adjacency.computeIfAbsent(packedA, k -> new LongOpenHashSet());
        LongOpenHashSet setB = this.adjacency.computeIfAbsent(packedB, k -> new LongOpenHashSet());
        boolean added = setA.add(packedB);
        setB.add(packedA);
        return added;
    }

    /**
     * Removes the undirected edge between the given positions.
     *
     * @param a the first node
     * @param b the second node
     * @return {@code true} if the edge was removed, {@code false} if it was not present
     */
    public boolean remove(BlockPos a, BlockPos b) {
        long packedA = a.asLong();
        long packedB = b.asLong();
        LongOpenHashSet setA = this.adjacency.get(packedA);
        LongOpenHashSet setB = this.adjacency.get(packedB);
        if (setA == null || !setA.remove(packedB)) {
            return false;
        }
        if (setA.isEmpty()) {
            this.adjacency.remove(packedA);
        }
        if (setB != null) {
            setB.remove(packedA);
            if (setB.isEmpty()) {
                this.adjacency.remove(packedB);
            }
        }
        return true;
    }

    /**
     * Returns whether an edge between the given positions exists.
     */
    public boolean contains(BlockPos a, BlockPos b) {
        LongOpenHashSet setA = this.adjacency.get(a.asLong());
        return setA != null && setA.contains(b.asLong());
    }

    /**
     * Returns all neighbors connected to the given node.
     */
    public Set<BlockPos> neighbors(BlockPos pos) {
        LongOpenHashSet set = this.adjacency.get(pos.asLong());
        if (set == null) {
            return Set.of();
        }
        Set<BlockPos> result = new HashSet<>(set.size());
        LongIterator it = set.iterator();
        while (it.hasNext()) {
            result.add(BlockPos.fromLong(it.nextLong()));
        }
        return Set.copyOf(result);
    }

    /**
     * Clears all stored edges.
     */
    public void clear() {
        this.adjacency.clear();
    }

    /**
     * Returns the number of stored edges.
     */
    public int size() {
        int count = 0;
        for (LongOpenHashSet set : this.adjacency.values()) {
            count += set.size();
        }
        return count / 2;
    }
}