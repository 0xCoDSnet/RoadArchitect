package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores per-path decoration metrics and suitability masks.
 * - Prefix lengths S (double[n])
 * - Ground mask (tri-state per index: 0=unknown, 1=land, 2=water)
 * - Water-interior mask (tri-state: 0=unknown, 1=interior water, 2=not)
 * - Simple checksum of path positions for invalidation
 */
public final class PathDecorStorage extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PathDecorStorage.class.getSimpleName());

    private static final String KEY = "road_path_decor";

    private static final String ENTRIES_KEY = "entries";
    private static final String PATH_KEY = "path";
    private static final String CHECKSUM_KEY = "sum";
    private static final String PREFIX_KEY = "S";
    private static final String GROUND_KEY = "G";
    private static final String WATER_INNER_KEY = "W";

    public static final Type<PathDecorStorage> TYPE = new Type<>(PathDecorStorage::new, PathDecorStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final ConcurrentMap<String, double[]> prefix = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> groundMask = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, byte[]> waterInteriorMask = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> checksums = new ConcurrentHashMap<>();

    public static PathDecorStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE, KEY);
    }

    public static PathDecorStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        PathDecorStorage storage = new PathDecorStorage();
        NbtList list = tag.getList(ENTRIES_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            String key = e.getString(PATH_KEY);
            long sum = e.getLong(CHECKSUM_KEY);
            storage.checksums.put(key, sum);

            // doubles
            NbtList sList = e.getList(PREFIX_KEY, NbtElement.STRING_TYPE);
            double[] S = new double[sList.size()];
            for (int j = 0; j < sList.size(); j++) {
                // use string to avoid double precision issues with NBT lacking double arrays in older APIs
                S[j] = Double.parseDouble(sList.getString(j));
            }
            storage.prefix.put(key, S);

            // masks
            storage.groundMask.put(key, getByteArray(e, GROUND_KEY));
            storage.waterInteriorMask.put(key, getByteArray(e, WATER_INNER_KEY));
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList out = new NbtList();
        for (Map.Entry<String, double[]> e : prefix.entrySet()) {
            String key = e.getKey();
            NbtCompound obj = new NbtCompound();
            obj.putString(PATH_KEY, key);
            obj.putLong(CHECKSUM_KEY, checksums.getOrDefault(key, 0L));

            NbtList sList = new NbtList();
            double[] S = e.getValue();
            for (double v : S) {
                sList.add(NbtString.of(Double.toString(v)));
            }
            obj.put(PREFIX_KEY, sList);

            putByteArray(obj, GROUND_KEY, groundMask.get(key));
            putByteArray(obj, WATER_INNER_KEY, waterInteriorMask.get(key));
            out.add(obj);
        }
        tag.put(ENTRIES_KEY, out);
        return tag;
    }

    public double[] getPrefix(String pathKey) {
        return prefix.get(pathKey);
    }

    public byte[] getGroundMask(String pathKey) {
        return groundMask.get(pathKey);
    }

    public byte[] getWaterInteriorMask(String pathKey) {
        return waterInteriorMask.get(pathKey);
    }

    public long getChecksum(String pathKey) {
        return checksums.getOrDefault(pathKey, 0L);
    }

    public void ensureCapacity(String pathKey, int n) {
        double[] S = prefix.get(pathKey);
        if (S == null || S.length != n) {
            prefix.put(pathKey, new double[n]);
            groundMask.put(pathKey, new byte[n]);
            waterInteriorMask.put(pathKey, new byte[n]);
            markDirty();
        }
    }

    public void updateChecksum(String pathKey, long sum) {
        Long prev = checksums.put(pathKey, sum);
        if (prev == null || prev.longValue() != sum) {
            markDirty();
        }
    }

    public void setPrefix(String pathKey, double[] S) {
        prefix.put(pathKey, S);
        markDirty();
    }

    public void clearMasks(String pathKey) {
        byte[] g = groundMask.get(pathKey);
        byte[] w = waterInteriorMask.get(pathKey);
        if (g != null) Arrays.fill(g, (byte) 0);
        if (w != null) Arrays.fill(w, (byte) 0);
        markDirty();
    }

    /** Exposes dirty mark as public for helpers. */
    public void touch() {
        markDirty();
    }

    private static byte[] getByteArray(NbtCompound obj, String key) {
        // store as list of numbers in string form to keep it simple without custom codecs
        NbtList list = obj.getList(key, NbtElement.STRING_TYPE);
        byte[] arr = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = Byte.parseByte(list.getString(i));
        }
        return arr;
    }

    private static void putByteArray(NbtCompound obj, String key, byte[] arr) {
        NbtList list = new NbtList();
        if (arr != null) {
            for (byte b : arr) list.add(NbtString.of(Byte.toString(b)));
        }
        obj.put(key, list);
    }
}
