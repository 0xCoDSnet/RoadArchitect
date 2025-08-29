package net.oxcodsnet.roadarchitect.api.storage;

import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.oxcodsnet.roadarchitect.util.PersistentStateUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-addon key-value persistent storage (world scoped), storing values as NBT compounds keyed by Identifier.
 */
public final class AddonPersistentStorage extends PersistentState {
    private static final String ROOT = "entries";
    private static final String KEY = "key";
    private static final String VAL = "val";

    private final Map<Identifier, NbtCompound> data = new ConcurrentHashMap<>();

    private final Identifier addonId;

    private AddonPersistentStorage() {
        this.addonId = Identifier.of("roadarchitect", "unknown");
    }

    private AddonPersistentStorage(Identifier addonId) {
        this.addonId = addonId;
    }

    public static AddonPersistentStorage get(ServerWorld world, Identifier addonId) {
        String storageKey = storageKey(addonId);
        PersistentStateType<AddonPersistentStorage> type = new PersistentStateType<>(
                storageKey,
                ctx -> new AddonPersistentStorage(addonId),
                ctx -> NbtCompound.CODEC.xmap(
                        tag -> fromNbt(tag, ctx.world().getRegistryManager(), addonId),
                        storage -> storage.writeNbt(new NbtCompound(), ctx.world().getRegistryManager())
                ),
                // Use the same fixer domain as other storages for consistency
                DataFixTypes.SAVED_DATA_SCOREBOARD
        );
        return PersistentStateUtil.get(world, type);
    }

    private static String storageKey(Identifier addonId) {
        // One state per addon id
        String safePath = addonId.getPath().replace('/', '_');
        return "ra_addon_" + addonId.getNamespace() + "_" + safePath;
    }

    public static AddonPersistentStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        return fromNbt(tag, lookup, Identifier.of("roadarchitect", "unknown"));
    }

    private static AddonPersistentStorage fromNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup, Identifier addonId) {
        AddonPersistentStorage s = new AddonPersistentStorage(addonId);
        NbtList list = tag.getListOrEmpty(ROOT);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompoundOrEmpty(i);
            String keyStr = e.getString(KEY, "");
            if (keyStr.isEmpty()) continue;
            Identifier id = Identifier.tryParse(keyStr);
            if (id == null) continue;
            NbtCompound val = e.getCompoundOrEmpty(VAL);
            s.data.put(id, val.copy());
        }
        return s;
    }


    public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup lookup) {
        NbtList list = new NbtList();
        for (Map.Entry<Identifier, NbtCompound> en : data.entrySet()) {
            NbtCompound e = new NbtCompound();
            e.putString(KEY, en.getKey().toString());
            e.put(VAL, en.getValue().copy());
            list.add(e);
        }
        tag.put(ROOT, list);
        return tag;
    }

    public Optional<NbtCompound> get(Identifier key) {
        return Optional.ofNullable(data.get(key)).map(NbtCompound::copy);
    }

    public void put(Identifier key, NbtCompound value) {
        data.put(key, value == null ? new NbtCompound() : value.copy());
        markDirty();
    }

    public void remove(Identifier key) {
        if (data.remove(key) != null) {
            markDirty();
        }
    }

    public Set<Identifier> keys() { return Collections.unmodifiableSet(data.keySet()); }

    /** Exposes dirty mark for helpers. */
    public void touch() { markDirty(); }
}
