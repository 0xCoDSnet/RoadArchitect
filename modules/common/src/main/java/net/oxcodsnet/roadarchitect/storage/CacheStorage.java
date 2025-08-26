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
import net.oxcodsnet.roadarchitect.util.NbtUtils;

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
        NbtUtils.fillLongIntMap(hList, storage.heights);

        NbtList sList = tag.getList(STABILITIES_KEY, NbtElement.COMPOUND_TYPE);
        NbtUtils.fillLongDoubleMap(sList, storage.stabilities);
        NbtList bList = tag.getList(BIOMES_KEY, NbtElement.COMPOUND_TYPE);
        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(bList.size());
        NbtUtils.fillLongStringMap(bList, biomeIds);
        RegistryWrapper.Impl<Biome> registry = lookup.getWrapperOrThrow(RegistryKeys.BIOME);
        for (java.util.Map.Entry<Long, String> e : biomeIds.entrySet()) {
            Identifier id = Identifier.tryParse(e.getValue());
            if (id == null) continue;
            RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, id);
            registry.getOptional(key).ifPresent(entry -> storage.biomes.put(e.getKey(), entry));
        }
        return storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.put(HEIGHTS_KEY, NbtUtils.toLongIntList(heights));

        tag.put(STABILITIES_KEY, NbtUtils.toLongDoubleList(stabilities));

        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(biomes.size());
        for (Map.Entry<Long, RegistryEntry<Biome>> entry : biomes.entrySet()) {
            Identifier id = entry.getValue().getKey().map(RegistryKey::getValue).orElse(null);
            if (id != null) {
                biomeIds.put(entry.getKey(), id.toString());
            }
        }
        tag.put(BIOMES_KEY, NbtUtils.toLongStringList(biomeIds));
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
