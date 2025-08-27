package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.registry.tag.FluidTags;
import net.oxcodsnet.roadarchitect.storage.PathDecorStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for deterministic decoration placement along stored paths.
 */
public final class PathDecorUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PathDecorUtil.class.getSimpleName());

    public static final byte BOOL_UNKNOWN = 0;
    public static final byte BOOL_TRUE = 1;
    public static final byte BOOL_FALSE = 2;

    private static final int[][] OFFSETS_8 = new int[][]{
            {-1, -1}, {0, -1}, {1, -1},
            {-1, 0}, /*self*/ {1, 0},
            {-1, 1}, {0, 1}, {1, 1}
    };

    private PathDecorUtil() {
    }

    public static long checksum(List<BlockPos> pts) {
        long h = 1469598103934665603L; // FNV offset basis
        for (BlockPos p : pts) {
            long v = p.asLong();
            h ^= v;
            h *= 1099511628211L; // FNV prime
        }
        return h;
    }

    public static double[] computePrefix(List<BlockPos> pts) {
        int n = pts.size();
        double[] S = new double[n];
        for (int i = 1; i < n; i++) {
            BlockPos a = pts.get(i - 1), b = pts.get(i);
            S[i] = S[i - 1] + Math.hypot(b.getX() - a.getX(), b.getZ() - a.getZ());
        }
        return S;
    }

    /**
        Ensures prefix cache is up-to-date for the path and returns it.
     */
    public static double[] ensurePrefix(PathDecorStorage storage, String pathKey, List<BlockPos> pts) {
        long sum = checksum(pts);
        double[] S = storage.getPrefix(pathKey);
        if (S == null || S.length != pts.size() || storage.getChecksum(pathKey) != sum) {
            S = computePrefix(pts);
            storage.ensureCapacity(pathKey, pts.size());
            storage.setPrefix(pathKey, S);
            storage.updateChecksum(pathKey, sum);
            storage.clearMasks(pathKey); // path changed — mask values invalid
            LOGGER.debug("Refreshed prefix S for {} ({} points)", pathKey, pts.size());
        }
        return S;
    }

    public static int phaseFor(String pathKey, int step) {
        if (step <= 0) return 0;
        int hash = pathKey.hashCode();
        return Math.floorMod(hash, step);
    }

    /**
     * Lower-bound binary search over S[from..to) for value x.
     */
    public static int lowerBound(double[] S, int from, int to, double x) {
        int lo = Math.max(0, from);
        int hi = Math.max(lo, Math.min(S.length, to));
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (S[mid] < x) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /**
     * Returns (inclusive) k range whose marks fall within [sFrom, sTo).
     */
    public static long[] kRange(double sFrom, double sTo, int step, int phase) {
        if (step <= 0 || sTo <= sFrom) return new long[]{1, 0};
        double a = (sFrom - phase) / step;
        double b = (sTo - phase) / step;
        long kMin = (long) Math.ceil(a);
        long kMax = (long) Math.floor(Math.nextDown(b)); // strictly < sTo
        return new long[]{kMin, kMax};
    }

    /** Updates ground mask (land/water) for indices [from,to). */
    public static void fillGroundMask(PathDecorStorage storage, String key, StructureWorldAccess world,
                                      List<BlockPos> pts, int from, int to) {
        byte[] mask = storage.getGroundMask(key);
        if (mask == null) return;
        int n = Math.min(mask.length, pts.size());
        int s = Math.max(0, from);
        int e = Math.min(n, to);
        for (int i = s; i < e; i++) {
            if (mask[i] == BOOL_UNKNOWN) {
                mask[i] = safeIsNotWaterBlock(world, pts.get(i)) ? BOOL_TRUE : BOOL_FALSE;
            }
        }
        storage.touch();
    }

    /** Updates water-interior mask (8-neighborhood) for indices [from,to). */
    public static void fillWaterInteriorMask(PathDecorStorage storage, String key, StructureWorldAccess world,
                                             List<BlockPos> pts, int from, int to) {
        byte[] mask = storage.getWaterInteriorMask(key);
        if (mask == null) return;
        int n = Math.min(mask.length, pts.size());
        int s = Math.max(0, from);
        int e = Math.min(n, to);
        for (int i = s; i < e; i++) {
            if (mask[i] != BOOL_UNKNOWN) continue;
            BlockPos p = pts.get(i);
            if (safeIsNotWaterBlock(world, p)) {
                mask[i] = BOOL_FALSE;
                continue;
            }
            boolean interior = true;
            for (int[] d : OFFSETS_8) {
                BlockPos q = p.add(d[0], 0, d[1]);
                if (safeIsNotWaterBlock(world, q)) { interior = false; break; }
            }
            mask[i] = interior ? BOOL_TRUE : BOOL_FALSE;
        }
        storage.touch();
    }

    /** Chunk-safe water check that never loads chunks. */
    public static boolean safeIsNotWaterBlock(StructureWorldAccess world, BlockPos pos) {
        ChunkPos cp = new ChunkPos(pos);
        if (!world.isChunkLoaded(cp.x, cp.z)) return true; // treat as land to avoid loads
        return !world.getBlockState(pos).getFluidState().isIn(FluidTags.WATER);
    }

    /**
     * Applies symmetric erosion by E points on both sides of a contiguous run.
     * Accepts i only if the E neighbors on the left and the E neighbors on the right
     * are within bounds and equal to 'want'. Unknowns and out-of-bounds are treated
     * as rejection (conservative).
     */
    public static boolean erodedAccept(byte[] mask, int i, int E, byte want) {
        if (mask == null || i < 0 || i >= mask.length) return false;
        if (mask[i] != want) return false;
        if (E <= 0) return true;
        for (int d = 1; d <= E; d++) {
            int l = i - d;
            int r = i + d;
            if (l < 0 || r >= mask.length) return false;
            if (mask[l] != want || mask[r] != want) return false;
        }
        return true;
    }

    /** Marker item holding index and k ordinal. */
    public record Marker(int index, long k) {}

    /**
     * Computes all marker indices for the window [from,to) given S, step and phase.
     * Does not apply suitability masks.
     */
    public static List<Marker> markersInWindow(double[] S, int from, int to, int step, int phase) {
        List<Marker> out = new ArrayList<>();
        if (S == null || S.length == 0 || step <= 0 || to <= from) return out;
        double sFrom = S[Math.max(0, from)];
        double sTo = S[Math.max(0, Math.min(to - 1, S.length - 1))];
        long[] kr = kRange(sFrom, sTo, step, phase);
        long kMin = kr[0], kMax = kr[1];
        if (kMax < kMin) return out;
        for (long k = kMin; k <= kMax; k++) {
            double m = phase + (double) k * (double) step;
            int i = lowerBound(S, from, to, m);
            if (i >= from && i < to) {
                out.add(new Marker(i, k));
            }
        }
        return out;
    }

    /** Deterministic boolean based on path key and ordinal. */
    public static boolean detBool(String pathKey, long ordinal) {
        long seed = 0x9E3779B97F4A7C15L * (long) pathKey.hashCode() + (ordinal << 1) + 0x632BE59BD9B4E019L;
        seed ^= (seed >>> 33);
        seed *= 0xff51afd7ed558ccdL;
        seed ^= (seed >>> 33);
        return (seed & 1L) == 1L;
    }

    /** Deterministic [0..bound-1] int based on path key and ordinal. */
    public static int detInt(String pathKey, long ordinal, int bound) {
        if (bound <= 1) return 0;
        long seed = 0x9E3779B97F4A7C15L * (long) pathKey.hashCode() + (ordinal * 0x9E3779B97F4A7C15L);
        seed ^= (seed >>> 33);
        seed *= 0xc4ceb9fe1a85ec53L;
        seed ^= (seed >>> 33);
        int v = (int) (seed ^ (seed >>> 32));
        return Math.floorMod(v, bound);
    }
}
