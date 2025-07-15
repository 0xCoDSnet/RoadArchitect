package net.oxcodsnet.roadarchitect.util;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.util.math.BlockPos;

/**
 * Efficient container for storing unique nodes consisting of a position and
 * structure name for pathfinding.
 */
public class NodeStorage {
    /**
     * Single node identified by its position and the structure name it belongs
     * to.
     *
     * @param pos       absolute world position
     * @param structure structure identifier
     */
    public record Node(BlockPos pos, String structure) {
    }

    private final Set<Node> nodes = new HashSet<>();

    /**
     * Adds the given position and structure to the storage if not already
     * present.
     *
     * @param pos       block position to add
     * @param structure structure identifier
     * @return {@code true} if the node was added, {@code false} if it already
     *         existed
     */
    public boolean add(BlockPos pos, String structure) {
        return this.nodes.add(new Node(pos, structure));
    }

    /**
     * Removes the given node from the storage.
     *
     * @param pos       block position to remove
     * @param structure structure identifier
     * @return {@code true} if the node was removed, {@code false} otherwise
     */
    public boolean remove(BlockPos pos, String structure) {
        return this.nodes.remove(new Node(pos, structure));
    }

    /**
     * Returns whether the given node is stored.
     */
    public boolean contains(BlockPos pos, String structure) {
        return this.nodes.contains(new Node(pos, structure));
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
     * Returns all stored nodes as an immutable set.
     */
    public Set<Node> asNodeSet() {
        return Set.copyOf(this.nodes);
    }

    /**
     * Returns all stored positions as an immutable set, ignoring structure
     * names.
     */
    public Set<BlockPos> asBlockPosSet() {
        return this.nodes.stream().map(Node::pos).collect(Collectors.toUnmodifiableSet());
    }
}