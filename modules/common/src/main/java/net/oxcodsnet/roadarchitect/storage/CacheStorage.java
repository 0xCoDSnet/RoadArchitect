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
import net.oxcodsnet.roadarchitect.util.NbtUtils;

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
        net.oxcodsnet.roadarchitect.util.NbtUtils.fillLongIntMap(hList, storage.heights);

        NbtList sList = tag.getListOrEmpty(STABILITIES_KEY);
        net.oxcodsnet.roadarchitect.util.NbtUtils.fillLongDoubleMap(sList, storage.stabilities);

        NbtList bList = tag.getListOrEmpty(BIOMES_KEY);
        java.util.HashMap<Long, String> biomeIds = new java.util.HashMap<>(bList.size());
        net.oxcodsnet.roadarchitect.util.NbtUtils.fillLongStringMap(bList, biomeIds);
        RegistryWrapper.Impl<Biome> registry = lookup == null ? null : lookup.getOrThrow(RegistryKeys.BIOME);
        if (registry != null) {
            for (java.util.Map.Entry<Long, String> entry : biomeIds.entrySet()) {
                Identifier id = Identifier.tryParse(entry.getValue());
                if (id == null) continue;
                RegistryKey<Biome> key = RegistryKey.of(RegistryKeys.BIOME, id);
                registry.getOptional(key).ifPresent(regEntry -> storage.biomes.put(entry.getKey(), regEntry));
            }
        }
        return storage;
    }

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
