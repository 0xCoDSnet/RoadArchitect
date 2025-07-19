package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtLong;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.datafixer.DataFixTypes;
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
    private static final Logger LOGGER = LoggerFactory.getLogger("PathStorage");
    private static final String KEY = "road_paths";
    private static final String PATHS_KEY = "paths";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String POS_KEY = "pos";

    public static final Type<PathStorage> TYPE = new Type<>(PathStorage::new, PathStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final Map<String, List<BlockPos>> paths = new ConcurrentHashMap<>();


    public static PathStorage get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, KEY);
    }

    public static PathStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        PathStorage storage = new PathStorage();
        NbtList list = tag.getList(PATHS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            String from = entry.getString(FROM_KEY);
            String to = entry.getString(TO_KEY);
            String key = makeKey(from, to);
            NbtList posList = entry.getList(POS_KEY, NbtElement.LONG_TYPE);
            List<BlockPos> positions = new ArrayList<>();
            for (NbtElement nbtElement : posList) {
                positions.add(BlockPos.fromLong(((NbtLong) nbtElement).longValue()));
            }
            storage.paths.put(key, positions);
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<String, List<BlockPos>> entry : paths.entrySet()) {
            String[] ids = entry.getKey().split("\\|");
            if (ids.length != 2) continue;
            NbtCompound elem = new NbtCompound();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            NbtList posList = new NbtList();
            for (BlockPos pos : entry.getValue()) {
                posList.add(NbtLong.of(pos.asLong()));
            }
            elem.put(POS_KEY, posList);
            list.add(elem);
        }
        tag.put(PATHS_KEY, list);
        return tag;
    }

    public void putPath(String from, String to, List<BlockPos> path) {
        paths.put(makeKey(from, to), List.copyOf(path));
        markDirty();
    }

    public List<BlockPos> getPath(String from, String to) {
        return paths.getOrDefault(makeKey(from, to), List.of());
    }

    private static String makeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}