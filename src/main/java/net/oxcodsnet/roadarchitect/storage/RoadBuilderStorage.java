package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Хранит очереди сегментов дорог для каждого чанка.
 * <p>Stores pending road-building segments per chunk.</p>
 */
public class RoadBuilderStorage extends PersistentState {
    private static final String KEY = "road_builder_tasks";
    private static final String SEGMENTS_KEY = "segments";
    private static final String CHUNK_KEY = "chunk";
    private static final String PATH_KEY = "path";
    private static final String START_KEY = "start";
    private static final String END_KEY = "end";

    public static final PersistentStateType<RoadBuilderStorage> TYPE = new PersistentStateType<>(
            KEY,
            ctx -> new RoadBuilderStorage(),
            ctx -> NbtCompound.CODEC.xmap(
                    tag -> fromNbt(tag, ctx.world().getRegistryManager()),
                    storage -> storage.writeNbt(new NbtCompound(), ctx.world().getRegistryManager())
            ),
            DataFixTypes.SAVED_DATA_SCOREBOARD
    );
    private final Map<ChunkPos, List<SegmentEntry>> segments = new ConcurrentHashMap<>();

    /**
     * Получает хранилище задач для указанного мира.
     * <p>Retrieves the storage of building tasks for the given world.</p>
     */
    public static RoadBuilderStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE);
    }

    /**
     * Загружает хранилище из NBT.
     * <p>Loads the storage from NBT.</p>
     */
    public static RoadBuilderStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        RoadBuilderStorage storage = new RoadBuilderStorage();
        NbtList list = tag.getListOrEmpty(SEGMENTS_KEY);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompoundOrEmpty(i);
            ChunkPos chunk = new ChunkPos(entry.getLong(CHUNK_KEY, 0L));
            String path = entry.getString(PATH_KEY, "");
            int start = entry.getInt(START_KEY, 0);
            int end = entry.getInt(END_KEY, 0);
            storage.segments.computeIfAbsent(chunk, c -> new CopyOnWriteArrayList<>())
                    .add(new SegmentEntry(path, start, end));
        }
        return storage;
    }

    /**
     * Сохраняет все сегменты в NBT.
     * <p>Serializes all segments into an NBT compound.</p>
     */
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<ChunkPos, List<SegmentEntry>> entry : segments.entrySet()) {
            long pos = entry.getKey().toLong();
            for (SegmentEntry segment : entry.getValue()) {
                NbtCompound elem = new NbtCompound();
                elem.putLong(CHUNK_KEY, pos);
                elem.putString(PATH_KEY, segment.pathKey());
                elem.putInt(START_KEY, segment.start());
                elem.putInt(END_KEY, segment.end());
                list.add(elem);
            }
        }
        tag.put(SEGMENTS_KEY, list);
        return tag;
    }

    /**
     * Добавляет новый сегмент для строительства в указанном чанке.
     * <p>Adds a new segment to be built within the given chunk.</p>
     */
    public void addSegment(ChunkPos chunk, String key, int start, int end) {
        segments.computeIfAbsent(chunk, c -> new CopyOnWriteArrayList<>())
                .add(new SegmentEntry(key, start, end));
        markDirty();
    }

    /**
     * Возвращает список сегментов, ожидающих постройки в чанке.
     * <p>Returns the list of queued segments for the chunk.</p>
     */
    public List<SegmentEntry> getSegments(ChunkPos chunk) {
        return segments.getOrDefault(chunk, List.of());
    }

    /**
     * Удаляет указанный сегмент из очереди чанка.
     * <p>Removes the given segment from the chunk queue.</p>
     */
    public void removeSegment(ChunkPos chunk, SegmentEntry entry) {
        List<SegmentEntry> list = segments.get(chunk);
        if (list != null && list.remove(entry)) {
            if (list.isEmpty()) {
                segments.remove(chunk);
            }
            markDirty();
        }
    }

    /**
     * Один сегмент пути, находящийся внутри чанка.
     * <p>A single path segment contained within a chunk.</p>
     */
    public record SegmentEntry(String pathKey, int start, int end) {
    }
}
