package net.oxcodsnet.roadarchitect.api.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.Optional;
import java.util.Set;

/**
 * Minimal key-value persistent storage for addons, scoped per world and addon id.
 * Values are stored as NBT compounds keyed by {@link Identifier}.
 */
public interface PersistentStore {
    Optional<NbtCompound> get(Identifier key);

    void put(Identifier key, NbtCompound value);

    void remove(Identifier key);

    Set<Identifier> keys();
}

