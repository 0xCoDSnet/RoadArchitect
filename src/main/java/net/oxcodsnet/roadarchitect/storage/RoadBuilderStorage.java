package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores pending road building segments per chunk.
 */
public class RoadBuilderStorage extends PersistentState {
    private static final String KEY = "road_builder_tasks";
    private static final String SEGMENTS_KEY = "segments";
    private static final String CHUNK_KEY = "chunk";
    private static final String PATH_KEY = "path";
    private static final String START_KEY = "start";
    private static final String END_KEY = "end";

    public static final Type<RoadBuilderStorage> TYPE = new Type<>(RoadBuilderStorage::new,
            RoadBuilderStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    /**
     * Single segment of a path that lies within a chunk.
     */
    public record SegmentEntry(String pathKey, int start, int end) {
    }

    private final Map<ChunkPos, List<SegmentEntry>> segments = new ConcurrentHashMap<>();

    public static RoadBuilderStorage get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, KEY);
    }

    public static RoadBuilderStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        RoadBuilderStorage storage = new RoadBuilderStorage();
        NbtList list = tag.getList(SEGMENTS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            ChunkPos chunk = new ChunkPos(entry.getLong(CHUNK_KEY));
            String path = entry.getString(PATH_KEY);
            int start = entry.getInt(START_KEY);
            int end = entry.getInt(END_KEY);
            storage.segments.computeIfAbsent(chunk, c -> new ArrayList<>())
                    .add(new SegmentEntry(path, start, end));
        }
        return storage;
    }

    @Override
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

    /** Adds a new segment to be built within the given chunk. */
    public void addSegment(ChunkPos chunk, String key, int start, int end) {
        segments.computeIfAbsent(chunk, c -> new ArrayList<>())
                .add(new SegmentEntry(key, start, end));
        markDirty();
    }

    /** Returns the list of segments queued for the chunk. */
    public List<SegmentEntry> getSegments(ChunkPos chunk) {
        return segments.getOrDefault(chunk, List.of());
    }

    /** Removes the specified segment from the chunk list. */
    public void removeSegment(ChunkPos chunk, SegmentEntry entry) {
        List<SegmentEntry> list = segments.get(chunk);
        if (list != null && list.remove(entry)) {
            if (list.isEmpty()) {
                segments.remove(chunk);
            }
            markDirty();
        }
    }

    /** Utility to build deterministic keys from node ids. */
    public static String makeKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }
}
