package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.biome.Biome;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CacheStorage extends PersistentState {
    private static final String KEY = "road_cache";
    private static final String HEIGHTS_KEY = "heights";
    private static final String STABILITIES_KEY = "stabilities";
    private static final String BIOMES_KEY = "biomes";
    private static final String ENTRY_KEY = "k";
    private static final String ENTRY_VALUE = "v";

    public static final PersistentStateType<CacheStorage> TYPE = new PersistentStateType<>(
            KEY,
            ctx -> new CacheStorage(),
            ctx -> NbtCompound.CODEC.xmap(
                    tag -> fromNbt(tag, ctx.world().getRegistryManager()),
                    storage -> storage.writeNbt(new NbtCompound(), ctx.world().getRegistryManager())
            ),
            DataFixTypes.SAVED_DATA_SCOREBOARD
    );

    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RegistryEntry<Biome>> biomes = new ConcurrentHashMap<>();

    public static CacheStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE);
    }

    public static CacheStorage fromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        CacheStorage storage = new CacheStorage();
        NbtList hList = tag.getListOrEmpty(HEIGHTS_KEY);
        for (int i = 0; i < hList.size(); i++) {
            NbtCompound entry = hList.getCompoundOrEmpty(i);
            long k = entry.getLong(ENTRY_KEY, 0L);
            int v = entry.getInt(ENTRY_VALUE, 0);
            storage.heights.put(k, v);
        }
        NbtList sList = tag.getListOrEmpty(STABILITIES_KEY);
        for (int i = 0; i < sList.size(); i++) {
            NbtCompound entry = sList.getCompoundOrEmpty(i);
            long k = entry.getLong(ENTRY_KEY, 0L);
            double v = entry.getDouble(ENTRY_VALUE, 0.0);
            storage.stabilities.put(k, v);
        }
        NbtList bList = tag.getListOrEmpty(BIOMES_KEY);
        RegistryWrapper.Impl<Biome> registry = lookup == null ? null : lookup.getOrThrow(RegistryKeys.BIOME);
        for (int i = 0; i < bList.size(); i++) {
            NbtCompound entry = bList.getCompoundOrEmpty(i);
            Identifier id = Identifier.tryParse(entry.getString(ENTRY_VALUE, ""));
            if (id == null || registry == null) {
                continue;
            }
            RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, id);
            registry.getOptional(key).ifPresent(e -> storage.biomes.put(entry.getLong(ENTRY_KEY, 0L), e));
        }
        return storage;
    }

    public NbtCompound writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        NbtList hList = new NbtList();
        for (Map.Entry<Long, Integer> entry : heights.entrySet()) {
            NbtCompound elem = new NbtCompound();
            elem.putLong(ENTRY_KEY, entry.getKey());
            elem.putInt(ENTRY_VALUE, entry.getValue());
            hList.add(elem);
        }
        tag.put(HEIGHTS_KEY, hList);

        NbtList sList = new NbtList();
        for (Map.Entry<Long, Double> entry : stabilities.entrySet()) {
            NbtCompound elem = new NbtCompound();
            elem.putLong(ENTRY_KEY, entry.getKey());
            elem.putDouble(ENTRY_VALUE, entry.getValue());
            sList.add(elem);
        }
        tag.put(STABILITIES_KEY, sList);

        NbtList bList = new NbtList();
        for (Map.Entry<Long, RegistryEntry<Biome>> entry : biomes.entrySet()) {
            Identifier id = entry.getValue().getKey().map(RegistryKey::getValue).orElse(null);
            if (id == null) {
                continue;
            }
            NbtCompound elem = new NbtCompound();
            elem.putLong(ENTRY_KEY, entry.getKey());
            elem.putString(ENTRY_VALUE, id.toString());
            bList.add(elem);
        }
        tag.put(BIOMES_KEY, bList);
        return tag;
    }

    public ConcurrentMap<Long, Integer> heights() {
        return heights;
    }

    public ConcurrentMap<Long, Double> stabilities() {
        return stabilities;
    }

    public ConcurrentMap<Long, RegistryEntry<Biome>> biomes() {
        return biomes;
    }
}

