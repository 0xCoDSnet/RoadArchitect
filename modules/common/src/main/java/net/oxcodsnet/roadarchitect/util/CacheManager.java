package net.oxcodsnet.roadarchitect.util;

//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
//import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.CacheStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Centralized, thread-safe caches for world generation data.
 * Reused across multiple PathFinder instances to avoid cache warm-up on each search.
 */
public final class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + CacheManager.class.getSimpleName());

    private static final Map<RegistryKey<World>, CacheStorage> STATES = new ConcurrentHashMap<>();

    private CacheManager() {
        // no-op
    }

    /**
     * Called by platform hooks when a server world loads (server side).
     * Ensures the cache state is allocated and attached to the world.
     *
     * @param world server world
     */
    public static void onWorldLoad(ServerWorld world) {
        if (world.isClient()) return;
        load(world);
    }

    /**
     * Called by platform hooks when a server world unloads.
     * Flushes and detaches the cache state from the internal map.
     *
     * @param world server world
     */
    public static void onWorldUnload(ServerWorld world) {
        if (world.isClient()) return;
        save(world);
    }

    /**
     * Called by platform hooks when the server is stopping.
     * Flushes all cached states for all worlds.
     *
     * @param server minecraft server
     */
    public static void onServerStopping(MinecraftServer server) {
        for (ServerWorld world : server.getWorlds()) {
            save(world);
        }
    }

    private static CacheStorage state(ServerWorld world) {
        return STATES.computeIfAbsent(world.getRegistryKey(), k -> CacheStorage.get(world));
    }

    private static void load(ServerWorld world) {
        CacheStorage storage = CacheStorage.get(world);
        STATES.put(world.getRegistryKey(), storage);
        LOGGER.debug("Cache loaded for world {}", world.getRegistryKey().getValue());
    }

    private static void save(ServerWorld world) {
        CacheStorage storage = STATES.remove(world.getRegistryKey());
        if (storage != null) {
            storage.markDirty();
            LOGGER.debug("Cache saved for world {}", world.getRegistryKey().getValue());
        }
    }

    /**
     * Prefills height and biome caches asynchronously over the given XZ area.
     *
     * @param world server world
     * @param minX  min X (blocks)
     * @param minZ  min Z (blocks)
     * @param maxX  max X (blocks)
     * @param maxZ  max Z (blocks)
     */
    public static void prefill(ServerWorld world, int minX, int minZ, int maxX, int maxZ) {
        int step = PathFinder.GRID_STEP;
        CacheStorage storage = state(world);
        AsyncExecutor.execute(() -> {
            ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
            NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
            MultiNoiseUtil.MultiNoiseSampler sampler = cfg.getMultiNoiseSampler();
            BiomeSource bsrc = gen.getBiomeSource();

            for (int x = minX; x <= maxX; x += step) {
                for (int z = minZ; z <= maxZ; z += step) {
                    long key = hash(x, z);
                    int finalX = x;
                    int finalZ = z;
                    AsyncExecutor.execute(() -> {
                        int h = gen.getHeight(finalX, finalZ, Heightmap.Type.WORLD_SURFACE_WG, world, cfg);
                        RegistryEntry<Biome> biome = bsrc.getBiome(
                                BiomeCoords.fromBlock(finalX), 316,
                                BiomeCoords.fromBlock(finalZ), sampler);
                        storage.heights().put(key, h);
                        storage.biomes().put(key, biome);
                    });
                }
            }
            LOGGER.debug("Prefill complete [{}..{}]×[{}..{}]",
                    minX, maxX, minZ, maxZ);
        });
    }

    /**
     * Gets or computes world surface height for the cached key.
     *
     * @param world  server world
     * @param key    cache key (hash of x,z)
     * @param loader fallback loader if value missing
     */
    public static int getHeight(ServerWorld world, long key, IntSupplier loader) {
        return state(world).heights().computeIfAbsent(key, k -> loader.getAsInt());
    }

    /**
     * Gets or computes world surface height at block coordinates.
     */
    public static int getHeight(ServerWorld world, int x, int z) {
        ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
        NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
        long key = hash(x, z);
        return getHeight(world, key, () -> gen.getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world, cfg));
    }

    /**
     * Gets or computes terrain stability metric for the key.
     */
    public static double getStability(ServerWorld world, long key, DoubleSupplier loader) {
        return state(world).stabilities().computeIfAbsent(key, k -> loader.getAsDouble());
    }

    /**
     * Gets or computes biome entry for the key.
     */
    public static RegistryEntry<Biome> getBiome(ServerWorld world, long key, Supplier<RegistryEntry<Biome>> loader) {
        return state(world).biomes().computeIfAbsent(key, k -> loader.get());
    }

    /**
     * Packs X and Z into a single long key.
     */
    public static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    /**
     * Unpacks X and Z from a long key into {@link BlockPos} (Y=0).
     */
    public static BlockPos keyToPos(long k) {
        return new BlockPos((int) (k >> 32), 0, (int) k);
    }
}
