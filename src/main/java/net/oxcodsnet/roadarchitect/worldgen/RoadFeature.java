package net.oxcodsnet.roadarchitect.worldgen;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Feature that places road segments stored in {@link RoadBuilderStorage}.
 */
public class RoadFeature extends Feature<RoadFeatureConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);
    private final BlockState block;

    public RoadFeature(BlockState block) {
        super(RoadFeatureConfig.CODEC);
        this.block = block;
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> context) {
        if (!(context.getWorld() instanceof StructureWorldAccess world)) {
            return false;
        }
        ServerWorld serverWorld = world.toServerWorld();
        ChunkGenerator generator = context.getGenerator();
        ChunkPos chunkPos = new ChunkPos(context.getOrigin());

        RoadBuilderStorage tasks = RoadBuilderStorage.get(serverWorld);
        PathStorage paths = PathStorage.get(serverWorld);
        List<RoadBuilderStorage.SegmentEntry> segments = List.copyOf(tasks.getSegments(chunkPos));
        for (RoadBuilderStorage.SegmentEntry segment : segments) {
            String[] ids = segment.pathKey().split("\\|");
            List<BlockPos> path = paths.getPath(ids[0], ids[1]);
            int end = Math.min(segment.end(), path.size());
            int half = Math.max(0, context.getConfig().width() / 2);
            for (int i = segment.start(); i < end; i++) {
                BlockPos pos = path.get(i);
                for (int dx = -half; dx <= half; dx++) {
                    for (int dz = -half; dz <= half; dz++) {
                        world.setBlockState(pos.add(dx, 0, dz), block, Block.NOTIFY_ALL);
                    }
                }
            }
            tasks.removeSegment(chunkPos, segment);
            LOGGER.info("Road segment {} built in chunk {}", segment.pathKey(), chunkPos);
        }
        return !segments.isEmpty();
    }
}
