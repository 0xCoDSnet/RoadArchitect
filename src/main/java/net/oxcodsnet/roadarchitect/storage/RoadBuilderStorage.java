package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtInt;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores unfinished road building tasks as a {@link PersistentState}.
 */
public class RoadBuilderStorage extends PersistentState {
    private static final String KEY = "road_builder_tasks";
    private static final String TASKS_KEY = "tasks";
    private static final String FROM_KEY = "from";
    private static final String TO_KEY = "to";
    private static final String INDEX_KEY = "index";

    public static final Type<RoadBuilderStorage> TYPE = new Type<>(RoadBuilderStorage::new,
            RoadBuilderStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final Map<String, Integer> tasks = new ConcurrentHashMap<>();

    public static RoadBuilderStorage get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, KEY);
    }

    public static RoadBuilderStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        RoadBuilderStorage storage = new RoadBuilderStorage();
        NbtList list = tag.getList(TASKS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            String from = entry.getString(FROM_KEY);
            String to = entry.getString(TO_KEY);
            int index = entry.getInt(INDEX_KEY);
            storage.tasks.put(makeKey(from, to), index);
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<String, Integer> entry : tasks.entrySet()) {
            String[] ids = entry.getKey().split("\\|");
            if (ids.length != 2) continue;
            NbtCompound elem = new NbtCompound();
            elem.putString(FROM_KEY, ids[0]);
            elem.putString(TO_KEY, ids[1]);
            elem.put(INDEX_KEY, NbtInt.of(entry.getValue()));
            list.add(elem);
        }
        tag.put(TASKS_KEY, list);
        return tag;
    }

    public Set<Map.Entry<String, Integer>> tasks() {
        return Set.copyOf(tasks.entrySet());
    }

    public int getProgress(String key) {
        return tasks.getOrDefault(key, 0);
    }

    public void addTask(String from, String to, int index) {
        tasks.put(makeKey(from, to), index);
        markDirty();
    }

    public void updateProgress(String key, int index) {
        tasks.put(key, index);
        markDirty();
    }

    public void removeTask(String key) {
        if (tasks.remove(key) != null) {
            markDirty();
        }
    }

    public boolean hasTask(String key) {
        return tasks.containsKey(key);
    }

    public static String makeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}
