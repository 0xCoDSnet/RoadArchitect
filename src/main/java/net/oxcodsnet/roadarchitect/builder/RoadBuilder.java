package net.oxcodsnet.roadarchitect.builder;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Builds a road along a precomputed path by placing glass blocks.
 */
public class RoadBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("RoadBuilder");
    private static final BlockState ROAD_BLOCK = Blocks.GLASS.getDefaultState();

    private final ServerWorld world;
    private final List<BlockPos> path;
    private int index;

    public RoadBuilder(ServerWorld world, List<BlockPos> path, int startIndex) {
        this.world = world;
        this.path = path;
        this.index = startIndex;
    }

    /**
     * Places the next block in the path.
     *
     * @return {@code true} when finished
     */
    public boolean tick() {
        if (index >= path.size()) {
            return true;
        }
        BlockPos pos = path.get(index++);
        world.setBlockState(pos, ROAD_BLOCK, 3);
        return index >= path.size();
    }

    public int index() {
        return index;
    }
}
