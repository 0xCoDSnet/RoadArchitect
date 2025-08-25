package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.PathStorage;
import net.oxcodsnet.roadarchitect.storage.RoadBuilderStorage;
import net.oxcodsnet.roadarchitect.worldgen.style.RoadStyle;
import net.oxcodsnet.roadarchitect.worldgen.style.RoadStyles;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.BuoyDecoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.FenceDecoration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Feature that places road segments stored in {@link RoadBuilderStorage}.
 * <p>
 * Толстая линия реализована через проверку расстояния от клетки до центральной
 * прямой (|dx · dir.z − dz · dir.x| ≤ halfWidth). Такой подход избегает
 * «шахматных» дыр на диагоналях :contentReference[oaicite:2]{index=2}.
 */
public final class RoadFeature extends Feature<RoadFeatureConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadFeature");

    private static final BuoyDecoration BUOY = new BuoyDecoration();

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }


    private static void buildRoadStripe(StructureWorldAccess world, List<BlockPos> pts, int halfWidth, Random random) {
        for (int i = 0; i < pts.size(); i++) {
            BlockPos p = pts.get(i);
            int prevIdx = Math.max(0, i - 2);
            int nextIdx = Math.min(pts.size() - 1, i + 2);
            Vec3d dir = new Vec3d(
                    pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                    0.0D,
                    pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
            ).normalize();
            double nx = dir.x;
            double nz = dir.z;
            boolean diagonal = Math.abs(nx) > 0.001 && Math.abs(nz) > 0.001;

            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    double dist = Math.abs(dx * nz - dz * nx);
                    boolean inside = dist <= halfWidth + 0.01 || (diagonal && Math.max(Math.abs(dx), Math.abs(dz)) <= halfWidth);
                    if (!inside) continue;

                    BlockPos roadPos = p.add(dx, 0, dz);

                    if (!isNotWaterBlock(world, p)) {continue;}
                    RegistryEntry<Biome> biome = world.getBiome(roadPos);
                    RoadStyle style = RoadStyles.forBiome(biome);
                    BlockState roadState = style.palette().pick(random);
                    placeRoad(world, roadPos, roadState);
                }
            }

            if (random.nextInt(18) == 0) {
                if (!isNotWaterBlock(world, p)) {continue;}
                decorateSide(world, p, nx, nz, halfWidth, random);
            }
        }
    }

    /* ============================================================= */
    /* ======================  ВСПОМОГАТЕЛЬНОЕ  ==================== */

    private static void decorateSide(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth, Random random) {
        int side = random.nextBoolean() ? 1 : -1;
        int sx = (int) Math.round(-nz * side);
        int sz = (int) Math.round(nx * side);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);
        RegistryEntry<Biome> biome = world.getBiome(center);
        RoadStyle style = RoadStyles.forBiome(biome);
        int length = 1 + random.nextInt(3);

        Decoration deco = style.decoration();
        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                stripe.add(dpos);
            }
            fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) {continue;}
                deco.place(world, dpos, random);
            }
        }
    }

    private static void placeRoad(StructureWorldAccess world, BlockPos pos, BlockState stateRoad) {
        if (!isNotWaterBlock(world, pos)) {return;}
        world.setBlockState(pos, stateRoad, Block.NOTIFY_NEIGHBORS);
        //world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_NEIGHBORS);
    }


    private static boolean isNotWaterBlock(StructureWorldAccess world, BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return true;
        return !world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }

    @Override
    public boolean generate(FeatureContext<RoadFeatureConfig> ctx) {
        ServerWorld serverWorld = ctx.getWorld().toServerWorld();
        if (serverWorld == null) {
            return false;
        }

        StructureWorldAccess world = ctx.getWorld();
        ChunkPos chunk = new ChunkPos(ctx.getOrigin());

        RoadBuilderStorage builder = RoadBuilderStorage.get(serverWorld);
        PathStorage paths = PathStorage.get(serverWorld);
        List<RoadBuilderStorage.SegmentEntry> queue = new ArrayList<>(builder.getSegments(chunk));
        if (queue.isEmpty()) return false;

        int halfWidth = Math.max(0, ctx.getConfig().orthWidth() / 2);
        Random random = world.getRandom();
        boolean placedAny = false;

        for (RoadBuilderStorage.SegmentEntry entry : queue) {
            String[] ids = entry.pathKey().split("\\|", 2);
            if (ids.length != 2) {
                LOGGER.warn("Malformed path key '{}'; skipping", entry.pathKey());
                builder.removeSegment(chunk, entry);
                continue;
            }

            List<BlockPos> pts = paths.getPath(ids[0], ids[1]);
            if (pts.isEmpty()) {
                builder.removeSegment(chunk, entry);
                continue;
            }

            int from = Math.max(0, entry.start());
            int to = Math.min(pts.size(), entry.end());

            /* ---------- ФАЗА 1: буйки ---------- */
            List<BlockPos> buoys = paths.getBuoys(entry.pathKey());
            if (!buoys.isEmpty()) {
                for (BlockPos buoyPos : buoys) {
                    int idx = pts.indexOf(buoyPos);
                    if (idx >= from && idx < to) {
                        BUOY.place(world, buoyPos, random);
                    }
                }
            }
            /* ---------- ФАЗА 2: суша ---------- */
            boolean[] waterMask = paths.getWaterMask(entry.pathKey());
            List<BlockPos> landPts = new ArrayList<>(Math.max(0, to - from));
            for (int i = from; i < to; i++) {
                if (i >= waterMask.length || !waterMask[i]) {
                    landPts.add(pts.get(i));
                }
            }
            buildRoadStripe(world, landPts, halfWidth, random);
            placedAny = true;
        }
        return placedAny;
    }
}

