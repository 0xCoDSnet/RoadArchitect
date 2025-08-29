package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.TriggerStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core addon API entry and trigger service facade exposed to other mods.
 */
public final class RoadAddons {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/addons");

    private static final Map<Identifier, TriggerType> TRIGGER_TYPES = new ConcurrentHashMap<>();
    private static final List<RoadAddon> ADDONS = new ArrayList<>();

    private RoadAddons() {}

    /**
     * Registers an addon implementation. Call during your mod init.
     */
    public static void registerAddon(RoadAddon addon) {
        try {
            ADDONS.add(addon);
            addon.onRegister();
            LOGGER.debug("Registered addon {}", addon.id());
        } catch (Throwable t) {
            LOGGER.error("Addon registration failed: {}", addon.id(), t);
        }
    }

    /**
     * Registers a trigger type. Addons should call this from {@link RoadAddon#onRegister()}.
     */
    public static void registerTriggerType(TriggerType type) {
        TRIGGER_TYPES.put(type.id(), type);
        LOGGER.debug("Registered trigger type {} (r={})", type.id(), type.radius());
    }

    /**
     * Places a trigger marker in the world. Stored in persistent state until triggered or removed.
     *
     * @param world  server world
     * @param pos    marker position
     * @param typeId trigger type id
     * @param data   optional data (nullable)
     */
    public static void placeMarker(ServerWorld world, BlockPos pos, Identifier typeId, NbtCompound data) {
        TriggerType type = TRIGGER_TYPES.get(typeId);
        if (type == null) {
            LOGGER.warn("Attempt to place marker with unknown type {} at {}", typeId, pos);
            return;
        }
        TriggerStorage storage = TriggerStorage.get(world);
        storage.addMarker(pos, typeId, type.radius(), data == null ? new NbtCompound() : data.copy());
        storage.markDirty();
        LOGGER.debug("Placed marker {} at {} (r={})", typeId, pos, type.radius());
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
     * Called from platform events each server tick. Checks proximity and fires markers.
     */
    public static void onServerTick(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            World w = player.getWorld();
            if (!(w instanceof ServerWorld sw)) continue;
            processPlayer(sw, player);
        }
    }

    /**
     * Called from platform events on chunk load.
     */
    public static void onChunkLoad(ServerWorld world, ChunkPos pos) {
        // Optional quick pass: pre-filter to this chunk markers.
        // Full proximity check still happens on ticks.
        TriggerStorage storage = TriggerStorage.get(world);
        if (storage.hasMarkersInChunk(pos)) {
            LOGGER.debug("Chunk {} loaded with {} markers", pos, storage.markersInChunk(pos));
        }
    }

    private static void processPlayer(ServerWorld world, ServerPlayerEntity player) {
        TriggerStorage storage = TriggerStorage.get(world);
        List<TriggerStorage.Marker> nearby = storage.findMarkersNear(player.getBlockPos());
        if (nearby.isEmpty()) return;

        List<UUID> toRemove = new ArrayList<>();
        for (TriggerStorage.Marker m : nearby) {
            TriggerType type = TRIGGER_TYPES.get(m.type());
            if (type == null) continue; // stale type, skip
            if (!world.isChunkLoaded(m.pos().getX() >> 4, m.pos().getZ() >> 4)) continue; // ensure generated/loaded

            try {
                type.handler().onTrigger(world, m.pos(), player, m.data());
                toRemove.add(m.id());
                LOGGER.debug("Fired marker {} at {} for {}", m.type(), m.pos(), player.getName().getString());
            } catch (Throwable t) {
                LOGGER.error("Trigger handler {} failed at {}", m.type(), m.pos(), t);
            }
        }
        if (!toRemove.isEmpty()) {
            storage.removeAll(toRemove);
            storage.markDirty();
        }
    }

    /**
     * Internal bootstrap for built-in addons. Call from loader init once.
     */
    public static void initBuiltins() {
        // no built-ins; external addons should register themselves
    }
}
