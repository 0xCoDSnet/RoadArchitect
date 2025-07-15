package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.util.math.BlockPos;
import java.util.HashSet;
import java.util.Set;

/**
 * Efficient container for storing unique {@link BlockPos} nodes for pathfinding.
 */
public class NodeStorage {
    private final LongOpenHashSet nodes = new LongOpenHashSet();

    /**
     * Adds the given position to the storage if it is not already present.
     *
     * @param pos the block position to add
     * @return {@code true} if the position was added, {@code false} if it was already present
     */
    public boolean add(BlockPos pos) {
        return this.nodes.add(pos.asLong());
    }

    /**
     * Removes the given position from the storage.
     *
     * @param pos the block position to remove
     * @return {@code true} if the position was removed, {@code false} if it was not present
     */
    public boolean remove(BlockPos pos) {
        return this.nodes.remove(pos.asLong());
    }

    /**
     * Returns whether the given position is stored.
     */
    public boolean contains(BlockPos pos) {
        return this.nodes.contains(pos.asLong());
    }

    /**
     * Returns the number of stored nodes.
     */
    public int size() {
        return this.nodes.size();
    }

    /**
     * Clears all stored nodes.
     */
    public void clear() {
        this.nodes.clear();
    }

    /**
     * Returns the stored positions as an immutable {@link Set}.
     */
    public Set<BlockPos> asBlockPosSet() {
        Set<BlockPos> result = new HashSet<>(this.nodes.size());
        LongIterator it = this.nodes.iterator();
        while (it.hasNext()) {
            result.add(BlockPos.fromLong(it.nextLong()));
        }
        return Set.copyOf(result);
    }

    /**
     * Returns the stored positions as an array of packed longs.
     */
    public long[] asLongArray() {
        return this.nodes.toLongArray();
    }
}
