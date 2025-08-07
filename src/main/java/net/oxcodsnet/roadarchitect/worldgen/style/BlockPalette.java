package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/**
 * Weighted random palette of block states used for road surfaces.
 */
public final class BlockPalette {
    private final List<Entry> entries;
    private final int totalWeight;

    private BlockPalette(List<Entry> entries, int totalWeight) {
        this.entries = entries;
        this.totalWeight = totalWeight;
    }

    public BlockState pick(Random random) {
        int r = random.nextInt(this.totalWeight);
        for (Entry e : this.entries) {
            if (r < e.weight) {
                return e.state;
            }
            r -= e.weight;
        }
        return this.entries.getFirst().state;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<Entry> entries = new ArrayList<>();
        private int totalWeight;

        public Builder add(BlockState state, int weight) {
            this.entries.add(new Entry(state, weight));
            this.totalWeight += weight;
            return this;
        }

        public BlockPalette build() {
            return new BlockPalette(List.copyOf(this.entries), this.totalWeight);
        }
    }

    private record Entry(BlockState state, int weight) {
    }
}

