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
    private static final int BUOY_INTERVAL = 18;

    private static final int[][] OFFSETS_8 = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1,  0},          {1,  0},
            {-1,  1}, {0,  1}, {1,  1}
    };

    public RoadFeature(Codec<RoadFeatureConfig> codec) {
        super(codec);
    }


    private static void buildRoadStripe(StructureWorldAccess world, List<BlockPos> pts, int halfWidth, Random random) {
        for (int i = 0; i < pts.size(); i++) {
            BlockPos p = pts.get(i);

            if (!isNotWaterBlock(world, p)) {
                continue;
            }

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

                    if (!isNotWaterBlock(world, p)) {
                        continue;
                    }

                    // 🔒 Новое: не кладём дорогу в воду/воду в waterlogged
                    if (isWaterSegment(world, roadPos)) {
                        continue;
                    }

                    RegistryEntry<Biome> biome = world.getBiome(roadPos);
                    RoadStyle style = RoadStyles.forBiome(biome);
                    BlockState roadState = style.palette().pick(random);
                    placeRoad(world, roadPos, roadState);
                }
            }

            if (random.nextInt(15) == 0) {
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
                stripe.add(center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j));
            }
            fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                deco.place(world,  center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j), random);
            }
        }
    }

    private static void placeRoad(StructureWorldAccess world, BlockPos pos, BlockState stateRoad) {
        world.setBlockState(pos, stateRoad, Block.NOTIFY_NEIGHBORS);
        //world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), Block.NOTIFY_NEIGHBORS);
    }



    private static boolean isWaterSegment(StructureWorldAccess world, BlockPos pos) {
        if (isNotWaterBlock(world, pos)) {
            return false;
        }
        for (int[] d : OFFSETS_8) {
            BlockPos neighbor = pos.add(d[0], 0, d[1]);
            if (isNotWaterBlock(world, neighbor)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isNotWaterBlock(StructureWorldAccess world, BlockPos pos) {
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

            /* ---------- ФАЗА 1: вода / буйки ---------- */
            List<BlockPos> landPts = new ArrayList<>();

            // 🔁 Новое: отмеряем реальное расстояние между буйками
            BlockPos lastBuoyPos = null;

            for (int i = from; i < to; i++) {
                BlockPos p = pts.get(i);
                if (isNotWaterBlock(world, p)) {
                    lastBuoyPos = null; // сбрасываем при выходе на сушу
                    landPts.add(p);
                } else {
                    if (isWaterSegment(world, p)) {
                        boolean farEnough = false;
                        if (lastBuoyPos == null) {
                            farEnough = true; // первый буй в сегменте воды
                        } else {
                            // сравниваем квадрат расстояния, чтобы не считать sqrt
                            long dx = p.getX() - lastBuoyPos.getX();
                            long dz = p.getZ() - lastBuoyPos.getZ();
                            long dist2 = dx * dx + dz * dz;
                            farEnough = dist2 >= (long) BUOY_INTERVAL * BUOY_INTERVAL;
                        }

                        if (farEnough) {
                            BUOY.place(world, p, random);
                            lastBuoyPos = p;
                        }
                    }
                }
            }

            /* ---------- ФАЗА 2: строим дорогу по суше ---------- */
            buildRoadStripe(world, landPts, halfWidth, random);

            builder.removeSegment(chunk, entry);
            placedAny = true;
        }
        return placedAny;
    }
}

