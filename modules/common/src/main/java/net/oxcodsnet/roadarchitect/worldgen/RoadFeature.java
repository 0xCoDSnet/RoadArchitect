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
import net.minecraft.util.math.Direction;
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
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.LampPostDecoration;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RoadFeature.class.getSimpleName());

    private static final BuoyDecoration BUOY = new BuoyDecoration();
    private static final int BUOY_INTERVAL = 30;

    private static final int[][] OFFSETS_8 = {
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

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

            RoadStyle style = RoadStyles.forBiome(world.getBiome(p));
            for (Decoration deco : style.decorations()) {
                if (deco instanceof LampPostDecoration lamp) {
                    int interval = RoadArchitect.CONFIG.lampInterval();
                    if (interval > 0 && i % interval == 0 && isNotWaterBlock(world, p)) {
                        placeLamp(world, p, nx, nz, halfWidth, lamp, random);
                    }
                } else if (random.nextInt(18) == 0) {
                    if (!isNotWaterBlock(world, p)) {continue;}
                    decorateSide(world, p, nx, nz, halfWidth, deco, random);
                }
            }
        }
    }

    /* ============================================================= */
    /* ======================  ВСПОМОГАТЕЛЬНОЕ  ==================== */

    private static void decorateSide(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth, Decoration deco, Random random) {
        int side = random.nextBoolean() ? 1 : -1;
        int sx = (int) Math.round(-nz * side);
        int sz = (int) Math.round(nx * side);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);
        int length = 1 + random.nextInt(3);

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

    private static void placeLamp(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth, LampPostDecoration base, Random random) {
        int sx = (int) Math.round(-nz);
        int sz = (int) Math.round(nx);

        BlockPos leftPos  = center.add( sx * (halfWidth + 1), 0,  sz * (halfWidth + 1));
        BlockPos rightPos = center.add(-sx * (halfWidth + 1), 0, -sz * (halfWidth + 1));
        Direction leftFace  = directionFrom(-sx, -sz); // «смотрит» к дороге
        Direction rightFace = directionFrom( sx,  sz);

        boolean leftFirst = random.nextBoolean();

        BlockPos firstPos     = leftFirst ? leftPos   : rightPos;
        Direction firstFacing = leftFirst ? leftFace  : rightFace;
        BlockPos secondPos     = leftFirst ? rightPos  : leftPos;
        Direction secondFacing = leftFirst ? rightFace : leftFace;

        if (isNotWaterBlock(world, firstPos)) {
            if (base.facing(firstFacing).tryPlace(world, firstPos, random)) {
                return; // удалось — вторую сторону не трогаем
            }
        }
        if (isNotWaterBlock(world, secondPos)) {
            base.facing(secondFacing).tryPlace(world, secondPos, random);
        }
    }

    /** Определяем горизонтальное направление по (dx, dz). */
    static Direction directionFrom(int dx, int dz) {
        if (dx > 0) return Direction.EAST;
        if (dx < 0) return Direction.WEST;
        if (dz > 0) return Direction.SOUTH;
        return Direction.NORTH;
    }

    private static void placeRoad(StructureWorldAccess world, BlockPos pos, BlockState stateRoad) {
        if (!isNotWaterBlock(world, pos)) {return;}
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
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        if (!world.isChunkLoaded(cx, cz)) return true;
        return !world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }


    /**
     * Собираем точки суши из [from, to), но «съедаем» по 1 точке
     * с начала и конца каждого сухого прогона (эрозия на 1).
     */
    private static List<BlockPos> collectLandPoints(
            StructureWorldAccess world, List<BlockPos> pts, int from, int to
    ) {
        int n = pts.size();
        // Расширяем окно на 1 с двух сторон, чтобы увидеть соседей за границей слайса
        int extFrom = Math.max(0, from - 1);
        int extTo   = Math.min(n, to + 1);

        // Маска суши на расширенном окне
        boolean[] landMask = new boolean[extTo - extFrom];
        for (int i = extFrom; i < extTo; i++) {
            landMask[i - extFrom] = isNotWaterBlock(world, pts.get(i));
        }

        // Собираем только «внутренние» сухие точки: у них и слева, и справа тоже суша
        List<BlockPos> out = new ArrayList<>(Math.max(0, to - from));
        for (int i = from; i < to; i++) {
            int k = i - extFrom;                // индекс внутри landMask
            if (!landMask[k]) continue;         // сама точка — вода
            boolean leftLand  = (k - 1 >= 0) && landMask[k - 1];
            boolean rightLand = (k + 1 < landMask.length) && landMask[k + 1];
            if (!leftLand || !rightLand) continue; // край прогона — пропускаем
            out.add(pts.get(i));
        }
        return out;
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
            for (int idx : computeBuoyIndices(world, pts, from, to, BUOY_INTERVAL)) {
                BUOY.place(world, pts.get(idx), random);
            }
            /* ---------- ФАЗА 2: суша ---------- */
            List<BlockPos> landPts = collectLandPoints(world, pts, from, to);
            buildRoadStripe(world, landPts, halfWidth, random);
            placedAny = true;
        }
        return placedAny;
    }

    private static List<Integer> computeBuoyIndices(
            StructureWorldAccess world, List<BlockPos> pts, int from, int to, int interval
    ) {
        int n = pts.size();

        // 1) префиксные длины (как было)
        double[] S = new double[n];
        for (int i = 1; i < n; i++) {
            BlockPos a = pts.get(i - 1), b = pts.get(i);
            S[i] = S[i - 1] + Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
        }

        // 2) маска воды только для окна [from,to)
        boolean[] water = new boolean[n];
        for (int i = from; i < to; i++) {
            water[i] = isWaterSegment(world, pts.get(i)); // теперь безопасно из-за isChunkLoaded-гарда
        }

        // 3) детерминированное размещение в пределах [from,to)
        List<Integer> out = new ArrayList<>();
        int i = from;

        // определяем локальный старт водного прогона не заглядывая далеко влево
        int runStart = -1;
        if (i < to && water[i]) {
            runStart = i;
            while (runStart > from && water[runStart - 1]) runStart--;
        }
        double nextMark = -1.0;
        if (runStart != -1) {
            double base = S[runStart];
            double progressed = S[i] - base;
            long k = (long) Math.ceil(progressed / interval);
            nextMark = base + k * interval;
        }

        while (i < to) {
            if (!water[i]) {
                runStart = -1;
                nextMark = -1.0;
                i++;
                if (i < to && water[i]) {
                    runStart = i;
                    while (runStart > from && water[runStart - 1]) runStart--;
                    double base = S[runStart];
                    double progressed = S[i] - base;
                    long k = (long) Math.ceil(progressed / interval);
                    nextMark = base + k * interval;
                }
                continue;
            }

            if (nextMark >= 0.0 && S[i] >= nextMark) {
                // water[i] уже известен, повторно мир не трогаем
                out.add(i);
                nextMark += interval;
                continue;
            }
            i++;
        }
        return out;
    }


}
