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
import net.oxcodsnet.roadarchitect.storage.PathDecorStorage;
import net.oxcodsnet.roadarchitect.util.PathDecorUtil;
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

    // OFFSETS_8 removed; water interior detection is handled in PathDecorUtil

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
                if (deco instanceof LampPostDecoration) {
                    // handled via deterministic markers below
                } else if (!RoadArchitect.CONFIG.deterministicDecorations() && random.nextInt(18) == 0) {
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

    private static void placeLampDet(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth,
                                     LampPostDecoration base, boolean leftFirst, Random random) {
        int sx = (int) Math.round(-nz);
        int sz = (int) Math.round(nx);

        BlockPos leftPos  = center.add( sx * (halfWidth + 1), 0,  sz * (halfWidth + 1));
        BlockPos rightPos = center.add(-sx * (halfWidth + 1), 0, -sz * (halfWidth + 1));
        Direction leftFace  = directionFrom(-sx, -sz);
        Direction rightFace = directionFrom( sx,  sz);

        BlockPos firstPos     = leftFirst ? leftPos   : rightPos;
        Direction firstFacing = leftFirst ? leftFace  : rightFace;
        BlockPos secondPos     = leftFirst ? rightPos  : leftPos;
        Direction secondFacing = leftFirst ? rightFace : leftFace;

        if (isNotWaterBlock(world, firstPos)) {
            if (base.facing(firstFacing).tryPlace(world, firstPos, random)) {
                return;
            }
        }
        if (isNotWaterBlock(world, secondPos)) {
            base.facing(secondFacing).tryPlace(world, secondPos, random);
        }
    }

    private static void placeSideDet(StructureWorldAccess world, BlockPos center, double nx, double nz, int halfWidth,
                                     Decoration deco, boolean leftSide, int length, Random detRandom) {
        int sideMul = leftSide ? 1 : -1;
        int sx = (int) Math.round(-nz * sideMul);
        int sz = (int) Math.round(nx * sideMul);
        int fx = (int) Math.round(nx);
        int fz = (int) Math.round(nz);

        if (deco instanceof FenceDecoration fence) {
            List<BlockPos> stripe = new ArrayList<>();
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) { continue; }
                stripe.add(dpos);
            }
            if (!stripe.isEmpty()) fence.placeFenceStripe(world, stripe);
        } else {
            for (int j = 0; j < length; j++) {
                BlockPos dpos = center.add(sx * (halfWidth + 1) + fx * j, 0, sz * (halfWidth + 1) + fz * j);
                if (!isNotWaterBlock(world, dpos)) { continue; }
                deco.place(world, dpos, detRandom);
            }
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
        PathDecorStorage decor = PathDecorStorage.get(serverWorld);
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

            // Ensure prefix S and masks cache are up-to-date
            String pathKey = entry.pathKey();
            double[] S = PathDecorUtil.ensurePrefix(decor, pathKey, pts);

            int erosion = Math.max(0, RoadArchitect.CONFIG.maskErosion());
            int buoyInterval = Math.max(0, RoadArchitect.CONFIG.buoyInterval());
            int lampInterval = Math.max(0, RoadArchitect.CONFIG.lampInterval());
            int sideInterval = Math.max(0, RoadArchitect.CONFIG.sideDecorationInterval());
            boolean det = RoadArchitect.CONFIG.deterministicDecorations();

            // Masks updated for the current window only (avoids chunk loads)
            PathDecorUtil.fillGroundMask(decor, pathKey, world, pts, from, to);
            PathDecorUtil.fillWaterInteriorMask(decor, pathKey, world, pts, from, to);

            /* ---------- ФАЗА 1: вода / буйки (детерминированно) ---------- */
            if (det && buoyInterval > 0) {
                int phase = PathDecorUtil.phaseFor(pathKey, buoyInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, buoyInterval, phase);
                byte[] waterMask = decor.getWaterInteriorMask(pathKey);
                for (PathDecorUtil.Marker m : marks) {
                    int idx = m.index();
                    if (PathDecorUtil.erodedAccept(waterMask, idx, erosion, PathDecorUtil.BOOL_TRUE)) {
                        BUOY.place(world, pts.get(idx), random);
                    }
                }
            }

            /* ---------- ФАЗА 2: суша (дорога + фонари детерминированно) ---------- */
            List<BlockPos> landPts = collectLandPoints(world, pts, from, to);
            buildRoadStripe(world, landPts, halfWidth, random);

            if (det && lampInterval > 0) {
                int phase = PathDecorUtil.phaseFor(pathKey, lampInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, lampInterval, phase);
                byte[] landMask = decor.getGroundMask(pathKey);

                for (PathDecorUtil.Marker m : marks) {
                    int i = m.index();
                    BlockPos p = pts.get(i);
                    if (!PathDecorUtil.erodedAccept(landMask, i, erosion, PathDecorUtil.BOOL_TRUE)) continue;

                    int prevIdx = Math.max(0, i - 2);
                    int nextIdx = Math.min(pts.size() - 1, i + 2);
                    net.minecraft.util.math.Vec3d dir = new net.minecraft.util.math.Vec3d(
                            pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                            0.0D,
                            pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
                    ).normalize();
                    double nx = dir.x;
                    double nz = dir.z;

                    // Seed left/right deterministically by (pathKey, ordinal)
                    boolean leftFirst = PathDecorUtil.detBool(pathKey, m.k());
                    LampPostDecoration lamp = null;
                    for (Decoration deco : RoadStyles.forBiome(world.getBiome(p)).decorations()) {
                        if (deco instanceof LampPostDecoration lp) { lamp = lp; break; }
                    }
                    if (lamp != null) {
                        placeLampDet(world, p, nx, nz, halfWidth, lamp, leftFirst, random);
                    }
                }
            }

            /* ---------- ФАЗА 3: суша / боковые украшения (детерминированно) ---------- */
            if (det && sideInterval > 0) {
                int phase = PathDecorUtil.phaseFor(pathKey, sideInterval);
                List<PathDecorUtil.Marker> marks = PathDecorUtil.markersInWindow(S, from, to, sideInterval, phase);
                byte[] landMask = decor.getGroundMask(pathKey);

                for (PathDecorUtil.Marker m : marks) {
                    int i = m.index();
                    if (!PathDecorUtil.erodedAccept(landMask, i, erosion, PathDecorUtil.BOOL_TRUE)) continue;

                    BlockPos p = pts.get(i);
                    // direction
                    int prevIdx = Math.max(0, i - 2);
                    int nextIdx = Math.min(pts.size() - 1, i + 2);
                    Vec3d dir = new Vec3d(
                            pts.get(nextIdx).getX() - pts.get(prevIdx).getX(),
                            0.0D,
                            pts.get(nextIdx).getZ() - pts.get(prevIdx).getZ()
                    ).normalize();
                    double nx = dir.x;
                    double nz = dir.z;

                    // collect non-lamp decos for this biome
                    RoadStyle styleAtP = RoadStyles.forBiome(world.getBiome(p));
                    java.util.ArrayList<Decoration> sideDecos = new java.util.ArrayList<>();
                    for (Decoration d : styleAtP.decorations()) if (!(d instanceof LampPostDecoration)) sideDecos.add(d);
                    if (sideDecos.isEmpty()) continue;

                    int choice = PathDecorUtil.detInt(pathKey, m.k(), sideDecos.size());
                    Decoration chosen = sideDecos.get(choice);
                    boolean leftSide = PathDecorUtil.detBool(pathKey, m.k());
                    int length = 1 + PathDecorUtil.detInt(pathKey, m.k() ^ 0x55AA55AAL, 3);

                    placeSideDet(world, p, nx, nz, halfWidth, chosen, leftSide, length, net.minecraft.util.math.random.Random.create(m.k() ^ pathKey.hashCode()));
                }
            }
            placedAny = true;
        }
        return placedAny;
    }

    // computeBuoyIndices removed: unified deterministic marker grid is used for all decorations


}
