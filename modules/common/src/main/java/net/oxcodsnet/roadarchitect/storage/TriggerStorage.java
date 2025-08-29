package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent storage of addon trigger markers per-dimension.
 */
public final class TriggerStorage extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/TriggerStorage");
    private static final String KEY = "road_triggers";

    private static final String LIST = "markers";
    private static final String ID = "id";
    private static final String POS = "pos"; // long
    private static final String TYPE = "type"; // string
    private static final String RADIUS = "radius"; // int
    private static final String DATA = "data"; // compound

    public static final Type<TriggerStorage> TYPE_DEF = new Type<>(TriggerStorage::new, TriggerStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final Map<UUID, Marker> byId = new ConcurrentHashMap<>();
    private final Map<ChunkPos, Set<UUID>> byChunk = new ConcurrentHashMap<>();

    public static TriggerStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE_DEF, KEY);
    }

    public static TriggerStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        TriggerStorage storage = new TriggerStorage();
        NbtList list = tag.getList(LIST, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            UUID id = e.getUuid(ID);
            BlockPos pos = BlockPos.fromLong(e.getLong(POS));
            Identifier type = Identifier.tryParse(e.getString(TYPE));
            int radius = e.getInt(RADIUS);
            NbtCompound data = e.getCompound(DATA);
            if (type == null) continue;
            storage.index(new Marker(id, pos, type, radius, data.copy()));
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Marker m : byId.values()) {
            NbtCompound e = new NbtCompound();
            e.putUuid(ID, m.id);
            e.putLong(POS, m.pos.asLong());
            e.putString(TYPE, m.type.toString());
            e.putInt(RADIUS, m.radius);
            e.put(DATA, m.data.copy());
            list.add(e);
        }
        tag.put(LIST, list);
        return tag;
    }

    public void addMarker(BlockPos pos, Identifier type, int radius, NbtCompound data) {
        UUID id = UUID.randomUUID();
        index(new Marker(id, pos.toImmutable(), type, radius, data == null ? new NbtCompound() : data.copy()));
        markDirty();
    }

    private void index(Marker m) {
        byId.put(m.id, m);
        byChunk.computeIfAbsent(new ChunkPos(m.pos), c -> ConcurrentHashMap.newKeySet()).add(m.id);
    }

    public boolean hasMarkersInChunk(ChunkPos pos) {
        return byChunk.containsKey(pos) && !byChunk.get(pos).isEmpty();
    }

    public int markersInChunk(ChunkPos pos) {
        return byChunk.getOrDefault(pos, Set.of()).size();
    }

    /**
     * Returns markers whose center is within their radius of the given position.
     * Performs a cheap chunk-range prefilter and then exact distance in XZ.
     */
    public List<Marker> findMarkersNear(BlockPos pos) {
        if (byId.isEmpty()) return List.of();
        int cx = pos.getX() >> 4;
        int cz = pos.getZ() >> 4;

        List<Marker> out = new ArrayList<>();
        // Collect candidate chunks in taxicab range covering max radius 6 chunks (~96m); iterate a small square
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                ChunkPos cp = new ChunkPos(cx + dx, cz + dz);
                Set<UUID> ids = byChunk.get(cp);
                if (ids == null || ids.isEmpty()) continue;
                for (UUID id : ids) {
                    Marker m = byId.get(id);
                    if (m == null) continue;
                    int rx = m.pos.getX() - pos.getX();
                    int rz = m.pos.getZ() - pos.getZ();
                    if ((long) rx * rx + (long) rz * rz <= (long) m.radius * m.radius) {
                        out.add(m);
                    }
                }
            }
        }
        return out;
    }

    public void removeAll(Collection<UUID> ids) {
        for (UUID id : ids) remove(id);
    }

    public void remove(UUID id) {
        Marker m = byId.remove(id);
        if (m == null) return;
        Set<UUID> s = byChunk.get(new ChunkPos(m.pos));
        if (s != null) s.remove(id);
    }

    public record Marker(UUID id, BlockPos pos, Identifier type, int radius, NbtCompound data) { }
}

