package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.api.storage.PersistentStore;
import net.oxcodsnet.roadarchitect.api.storage.AddonPersistentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Core addon API entry and trigger service facade exposed to other mods.
 */
public final class RoadAddons {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/addons");

    private static final List<RoadAddon> ADDONS = new ArrayList<>();

    private RoadAddons() {}

    /**
     * Registers an addon implementation. Call during your mod init.
     */
    public static void registerAddon(RoadAddon addon) {
        try {
            ADDONS.add(addon);
            addon.onRegister(new ContextImpl(addon.id()));
            LOGGER.debug("Registered addon {}", addon.id());
        } catch (Throwable t) {
            LOGGER.error("Addon registration failed: {}", addon.id(), t);
        }
    }

    // ===== Pipeline hooks (invoked by common when path becomes READY) =====

    public static void onPathReady(ServerWorld world, String pathKey, List<BlockPos> refinedPath) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onPathReady(world, pathKey, refinedPath);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onPathReady failure", addon.id(), t);
            }
        }
    }

    // ===== Runtime hooks driven by platform event bridges =====

    /**
     * Called from platform events each server tick. Forwards to addons.
     */
    public static void onServerTick(MinecraftServer server) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onServerTick(server);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onServerTick failure", addon.id(), t);
            }
        }
    }

    /**
     * Called from platform events on chunk load. Forwards to addons.
     */
    public static void onChunkLoad(ServerWorld world, ChunkPos pos) {
        for (RoadAddon addon : ADDONS) {
            try {
                addon.onChunkLoad(world, pos);
            } catch (Throwable t) {
                LOGGER.error("Addon {} onChunkLoad failure at {}", addon.id(), pos, t);
            }
        }
    }

    /**
     * Internal bootstrap for built-in addons. Call from loader init once.
     */
    public static void initBuiltins() {
        // no built-ins; external addons should register themselves
    }

    // ===== Addon-scoped persistent storage =====

    /**
     * Returns the persistent store for the given addon in the given world.
     */
    public static PersistentStore persistent(Identifier addonId, ServerWorld world) {
        AddonPersistentStorage state = AddonPersistentStorage.get(world, addonId);
        return new StoreView(state);
    }

    private record ContextImpl(Identifier addonId) implements AddonContext {
        private static Logger makeLogger(Identifier id) {
            return LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/addons/" + id);
        }

        @Override
        public PersistentStore persistent(ServerWorld world) {
            return RoadAddons.persistent(addonId, world);
        }

        @Override
        public Logger logger() {
            return makeLogger(addonId);
        }
    }

    private static final class StoreView implements PersistentStore {
        private final AddonPersistentStorage state;

        private StoreView(AddonPersistentStorage state) {
            this.state = state;
        }

        @Override
        public Optional<NbtCompound> get(Identifier key) {
            return state.get(key);
        }

        @Override
        public void put(Identifier key, NbtCompound value) {
            state.put(key, value);
            state.markDirty();
        }

        @Override
        public void remove(Identifier key) {
            state.remove(key);
            state.markDirty();
        }

        @Override
        public Set<Identifier> keys() {
            return state.keys();
        }
    }
}
