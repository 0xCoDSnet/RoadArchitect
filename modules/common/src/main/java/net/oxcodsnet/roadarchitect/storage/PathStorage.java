package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.KeyUtil;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранит просчитанные пути между узлами как {@link PersistentState}.
 * <p>Stores computed paths between nodes as a {@link PersistentState}.</p>
 */
public class PathStorage extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/PathStorage");
    private static final String KEY = "road_paths";
    private static final String PATHS_KEY = "paths";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String POS_KEY = "pos";
    private static final String STATUS_KEY = "status";
    private static final String WATER_KEY = "water";
    private static final String BUOYS_KEY = "buoys";

    public static final Type<PathStorage> TYPE = new Type<>(PathStorage::new, PathStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);
    private final Map<String, List<BlockPos>> paths = new ConcurrentHashMap<>();
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();
    private final Map<String, boolean[]> waterMasks = new ConcurrentHashMap<>();
    private final Map<String, List<BlockPos>> buoyPositions = new ConcurrentHashMap<>();

    /**
     * Возвращает экземпляр хранилища путей для указанного мира.
     * <p>Gets the {@code PathStorage} instance for the given world.</p>
     *
     * @param world серверный мир / server world
     * @return хранилище путей / path storage instance
     */
    public static PathStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE, KEY);
    }

    /**
     * Восстанавливает хранилище из NBT.
     * <p>Recreates the storage from an NBT compound.</p>
     */
    public static PathStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        PathStorage storage = new PathStorage();
        NbtList list = tag.getList(PATHS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            String from = entry.getString(FROM_KEY);
            String to = entry.getString(TO_KEY);
            String key = KeyUtil.pathKey(from, to);
            NbtList posList = entry.getList(POS_KEY, NbtElement.LONG_TYPE);
            List<BlockPos> positions = new ArrayList<>();
            for (NbtElement nbtElement : posList) {
                positions.add(BlockPos.fromLong(((NbtLong) nbtElement).longValue()));
            }
            storage.paths.put(key, positions);
            if (entry.contains(WATER_KEY, NbtElement.BYTE_ARRAY_TYPE)) {
                byte[] arr = entry.getByteArray(WATER_KEY);
                boolean[] mask = new boolean[arr.length];
                for (int j = 0; j < arr.length; j++) {
                    mask[j] = arr[j] != 0;
                }
                storage.waterMasks.put(key, mask);
            }
            if (entry.contains(BUOYS_KEY, NbtElement.LIST_TYPE)) {
                NbtList bList = entry.getList(BUOYS_KEY, NbtElement.LONG_TYPE);
                List<BlockPos> buoys = new ArrayList<>();
                for (NbtElement nbtElement : bList) {
                    buoys.add(BlockPos.fromLong(((NbtLong) nbtElement).longValue()));
                }
                storage.buoyPositions.put(key, buoys);
            }
            Status status = Status.READY;
            if (entry.contains(STATUS_KEY, NbtElement.STRING_TYPE)) {
                try {
                    status = Status.valueOf(entry.getString(STATUS_KEY));
                } catch (IllegalArgumentException ignore) {
                    // keep READY
                }
            }
            storage.statuses.put(key, status);
        }
        return storage;
    }

    /**
     * Сохраняет все пути в NBT.
     * <p>Serializes all paths into an NBT compound.</p>
     */
    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String key = entry.getKey();
            String[] ids = key.split("\\|", 2);
            if (ids.length != 2) continue;
            NbtCompound elem = new NbtCompound();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            NbtList posList = new NbtList();
            for (BlockPos pos : entry.getValue()) {
                posList.add(NbtLong.of(pos.asLong()));
            }
            elem.put(POS_KEY, posList);
            boolean[] mask = waterMasks.get(key);
            if (mask != null) {
                byte[] arr = new byte[mask.length];
                for (int i = 0; i < mask.length; i++) {
                    arr[i] = (byte) (mask[i] ? 1 : 0);
                }
                elem.putByteArray(WATER_KEY, arr);
            }
            List<BlockPos> buoys = buoyPositions.get(key);
            if (buoys != null && !buoys.isEmpty()) {
                NbtList bList = new NbtList();
                for (BlockPos pos : buoys) {
                    bList.add(NbtLong.of(pos.asLong()));
                }
                elem.put(BUOYS_KEY, bList);
            }
            Status st = statuses.getOrDefault(key, Status.PENDING);
            elem.putString(STATUS_KEY, st.name());
            list.add(elem);
        }
        tag.put(PATHS_KEY, list);
        return tag;
    }

    /**
     * Сохраняет путь между двумя узлами.
     * <p>Stores a path connecting two nodes.</p>
     */
    public void putPath(String from, String to, List<BlockPos> path, Status status) {
        String key = KeyUtil.pathKey(from, to);
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        waterMasks.remove(key);
        buoyPositions.remove(key);
        markDirty();
    }

    public void updatePath(String key, List<BlockPos> path, Status status) {
        updatePath(key, path, null, null, status);
    }

    public void updatePath(String key, List<BlockPos> path, boolean[] waterMask,
                           List<BlockPos> buoys, Status status) {
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
        if (waterMask != null) {
            waterMasks.put(key, waterMask.clone());
        } else {
            waterMasks.remove(key);
        }
        if (buoys != null) {
            buoyPositions.put(key, List.copyOf(buoys));
        } else {
            buoyPositions.remove(key);
        }
        markDirty();
    }

    /**
     * Возвращает сохраненный путь между узлами.
     * <p>Returns the stored path between the two nodes.</p>
     */
    public List<BlockPos> getPath(String from, String to) {
        return paths.getOrDefault(KeyUtil.pathKey(from, to), List.of());
    }

    public List<BlockPos> getPath(String key) {
        return paths.getOrDefault(key, List.of());
    }

    public boolean[] getWaterMask(String key) {
        boolean[] mask = waterMasks.get(key);
        return mask == null ? new boolean[0] : mask.clone();
    }

    public List<BlockPos> getBuoys(String key) {
        return buoyPositions.getOrDefault(key, List.of());
    }

    public Status getStatus(String key) {
        return statuses.getOrDefault(key, Status.PENDING);
    }

    public void setStatus(String key, Status status) {
        statuses.put(key, status);
        markDirty();
    }

    public Map<String, Status> allStatuses() {
        return Map.copyOf(statuses);
    }

    public boolean tryMarkProcessing(String key) {
        synchronized (statuses) {
            Status cur = statuses.get(key);
            if (cur != Status.PENDING) {
                return false;
            }
            statuses.put(key, Status.PROCESSING);
            markDirty();
            return true;
        }
    }

    public List<String> getPendingForChunk(ChunkPos chunk) {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<BlockPos>> e : paths.entrySet()) {
            if (getStatus(e.getKey()) != Status.PENDING) continue;
            for (BlockPos p : e.getValue()) {
                if (new ChunkPos(p).equals(chunk)) {
                    out.add(e.getKey());
                    break;
                }
            }
        }
        return out;
    }

    public enum Status {
        PENDING,
        PROCESSING,
        READY,
        FAILED
    }
}