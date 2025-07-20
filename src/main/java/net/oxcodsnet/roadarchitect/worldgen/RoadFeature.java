package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature that places road segments stored in {@link RoadBuilderStorage}.
 */
public class RoadFeature extends Feature<RoadFeatureConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadFeature");

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> ctx) {
        // Datapack‑phase runs only on the logical server; silently skip otherwise.
        ServerWorld serverWorld = ctx.getWorld().toServerWorld();
        StructureWorldAccess structureWorldAccess = ctx.getWorld();
        if (serverWorld == null) {
            return false;
        }
        // Segments are queued per chunk — look up the list for ours.
        ChunkPos chunk = new ChunkPos(ctx.getOrigin());
        RoadBuilderStorage builder = RoadBuilderStorage.get(serverWorld);
        List<RoadBuilderStorage.SegmentEntry> queue = new ArrayList<>(builder.getSegments(chunk));
        if (queue.isEmpty()) {
            return false; // nothing to build here
        }

        PathStorage paths = PathStorage.get(serverWorld);
        int half = Math.max(0, ctx.getConfig().width() / 2);
        BlockState road = Blocks.COBBLESTONE.getDefaultState(); // TODO: surface style via config

        boolean placed = false;

        for (RoadBuilderStorage.SegmentEntry seg : queue) {
            // Key format "from|to" (lexicographically sorted)
            String[] ids = seg.pathKey().split("\\|", 2);
            if (ids.length != 2) {
                LOGGER.warn("Malformed path key {} — skipping", seg.pathKey());
                builder.removeSegment(chunk, seg);
                continue;
            }

            List<BlockPos> pts = paths.getPath(ids[0], ids[1]);
            if (pts.isEmpty()) {
                builder.removeSegment(chunk, seg);
                continue; // nothing to do, path was deleted
            }

            int from = Math.max(0, seg.start());
            int to = Math.min(pts.size(), seg.end());

            for (int i = from; i < to; i++) {
                BlockPos p = pts.get(i);

                // Lay an (width×width) square centred on the path point.
                for (int dx = -half; dx <= half; dx++) {
                    for (int dz = -half; dz <= half; dz++) {
                        BlockPos pos = p.add(dx, 0, dz);
                        structureWorldAccess.setBlockState(pos, road, Block.NOTIFY_LISTENERS);
                        structureWorldAccess.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
                    }
                }
            }

            // Segment finished — remove it so it won't be processed again.
            builder.removeSegment(chunk, seg);
            placed = true;
        }

        return placed;
    }
}

