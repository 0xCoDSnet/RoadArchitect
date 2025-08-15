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

    public static final Type<CacheStorage> TYPE = new Type<>(CacheStorage::new, CacheStorage::fromNbt, DataFixTypes.SAVED_DATA_SCOREBOARD);

    private final ConcurrentMap<Long, Integer> heights = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Double> stabilities = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, RegistryEntry<Biome>> biomes = new ConcurrentHashMap<>();

    public static CacheStorage get(ServerWorld world) {
        return PersistentStateUtil.get(world, TYPE, KEY);
    }

    public static CacheStorage fromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        CacheStorage storage = new CacheStorage();
        NbtList hList = tag.getList(HEIGHTS_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < hList.size(); i++) {
            NbtCompound entry = hList.getCompound(i);
            storage.heights.put(entry.getLong(ENTRY_KEY), entry.getInt(ENTRY_VALUE));
        }
        NbtList sList = tag.getList(STABILITIES_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < sList.size(); i++) {
            NbtCompound entry = sList.getCompound(i);
            storage.stabilities.put(entry.getLong(ENTRY_KEY), entry.getDouble(ENTRY_VALUE));
        }
        NbtList bList = tag.getList(BIOMES_KEY, NbtElement.COMPOUND_TYPE);
        RegistryWrapper.Impl<Biome> registry = lookup.getWrapperOrThrow(RegistryKeys.BIOME);
        for (int i = 0; i < bList.size(); i++) {
            NbtCompound entry = bList.getCompound(i);
            Identifier id = Identifier.tryParse(entry.getString(ENTRY_VALUE));
            if (id == null) {
                continue;
            }
            RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, id);
            registry.getOptional(key).ifPresent(e -> storage.biomes.put(entry.getLong(ENTRY_KEY), e));
        }
        return storage;
    }

    @Override
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

