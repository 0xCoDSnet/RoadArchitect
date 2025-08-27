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
import net.minecraft.world.PersistentStateType;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + PathStorage.class.getSimpleName());
    private static final String KEY = "road_paths";
    private static final String PATHS_KEY = "paths";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String POS_KEY = "pos";
    private static final String STATUS_KEY = "status";

    public static final PersistentStateType<PathStorage> TYPE = new PersistentStateType<>(
            KEY,
            ctx -> new PathStorage(),
            ctx -> NbtCompound.CODEC.xmap(
                    tag -> fromNbt(tag, ctx.world().getRegistryManager()),
                    storage -> storage.writeNbt(new NbtCompound(), ctx.world().getRegistryManager())
            ),
            DataFixTypes.SAVED_DATA_SCOREBOARD
    );
    private final Map<String, List<BlockPos>> paths = new ConcurrentHashMap<>();
    private final Map<String, Status> statuses = new ConcurrentHashMap<>();

    /**
     * Возвращает экземпляр хранилища путей для указанного мира.
     * <p>Gets the {@code PathStorage} instance for the given world.</p>
     *
     * @param world серверный мир / server world
     * @return хранилище путей / path storage instance
     */
    public static PathStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE);
    }

    /**
     * Восстанавливает хранилище из NBT.
     * <p>Recreates the storage from an NBT compound.</p>
     */
    public static PathStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        PathStorage storage = new PathStorage();
        NbtList list = tag.getListOrEmpty(PATHS_KEY);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompoundOrEmpty(i);
            String from = entry.getString(FROM_KEY, "");
            String to = entry.getString(TO_KEY, "");
            String key = KeyUtil.pathKey(from, to);
            NbtList posList = entry.getListOrEmpty(POS_KEY);
            List<BlockPos> positions = new ArrayList<>();
            for (int j = 0; j < posList.size(); j++) {
                NbtElement elem = posList.get(j);
                if (elem instanceof NbtLong l) {
                    positions.add(BlockPos.fromLong(l.longValue()));
                }
            }
            storage.paths.put(key, positions);
            Status status = net.oxcodsnet.roadarchitect.util.NbtUtils.getEnumOrDefault(entry, STATUS_KEY, Status.class, Status.READY);
            storage.statuses.put(key, status);
        }
        return storage;
    }

    /**
     * Сохраняет все пути в NBT.
     * <p>Serializes all paths into an NBT compound.</p>
     */
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String[] ids = KeyUtil.parsePathKey(entry.getKey());
            if (ids.length != 2) continue;
            NbtCompound elem = new NbtCompound();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            NbtList posList = new NbtList();
            for (BlockPos pos : entry.getValue()) {
                posList.add(NbtLong.of(pos.asLong()));
            }
            elem.put(POS_KEY, posList);
            Status st = statuses.getOrDefault(entry.getKey(), Status.PENDING);
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
        markDirty();
    }

    public void updatePath(String key, List<BlockPos> path, Status status) {
        paths.put(key, List.copyOf(path));
        statuses.put(key, status);
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
