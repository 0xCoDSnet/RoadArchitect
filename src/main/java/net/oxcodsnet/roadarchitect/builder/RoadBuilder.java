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

    public RoadBuilder(ServerWorld world, List<BlockPos> path) {
        this.world = world;
        this.path = path;
    }

    /**
     * Places blocks for the specified segment of the path.
     *
     * @param startIndex start index in the path, inclusive
     * @param endIndex   end index in the path, exclusive
     */
    public void buildSegment(int startIndex, int endIndex) {
        if (startIndex >= path.size()) {
            return;
        }
        int to = Math.min(endIndex, path.size());
        for (int i = startIndex; i < to; i++) {
            BlockPos pos = path.get(i);
            world.setBlockState(pos, ROAD_BLOCK, 3);
        }
        LOGGER.debug("Built path segment {}..{}", startIndex, to);
    }

    public int length() {
        return path.size();
    }
}
