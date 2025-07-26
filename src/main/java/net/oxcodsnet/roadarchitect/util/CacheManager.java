package net.oxcodsnet.roadarchitect.util;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ForkJoinPool;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * Centralized, thread-safe caches for world generation data.
 * Reused across multiple PathFinder instances to avoid cache warm-up on each search.
 */
public final class CacheManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + CacheManager.class);
    private static final Long2IntMap HEIGHT_CACHE = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
    private static final Long2DoubleMap STABILITY_CACHE = Long2DoubleMaps.synchronize(new Long2DoubleOpenHashMap());
    private static final Long2ObjectMap<RegistryEntry<Biome>> BIOME_CACHE = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

    static {
        HEIGHT_CACHE.defaultReturnValue(Integer.MIN_VALUE);
        STABILITY_CACHE.defaultReturnValue(Double.MAX_VALUE);
    }

    private CacheManager() {
        // no-op
    }

    /**
     * Prefills height and biome caches asynchronously over the given area.
     */
    public static void prefill(ServerWorld world, int minX, int minZ, int maxX, int maxZ) {
        int step = PathFinder.GRID_STEP;
        ForkJoinPool.commonPool().submit(() -> {
            ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
            NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
            MultiNoiseUtil.MultiNoiseSampler sampler = cfg.getMultiNoiseSampler();
            BiomeSource bsrc = gen.getBiomeSource();

            for (int x = minX; x <= maxX; x += step) {
                for (int z = minZ; z <= maxZ; z += step) {
                    long key = hash(x, z);
                    int finalX = x;
                    int finalZ = z;
                    ForkJoinPool.commonPool().execute(() -> {
                        int h = gen.getHeight(finalX, finalZ, Heightmap.Type.WORLD_SURFACE, world, cfg);
                        HEIGHT_CACHE.put(key, h);
                    });
                    ForkJoinPool.commonPool().execute(() -> {
                        RegistryEntry<Biome> biome = bsrc.getBiome(
                                BiomeCoords.fromBlock(finalX),
                                316,
                                BiomeCoords.fromBlock(finalZ),
                                sampler
                        );
                        BIOME_CACHE.put(key, biome);
                    });
                }
            }
            LOGGER.debug("CacheManager: Prefill complete [{}..{}]×[{}..{}]", minX, maxX, minZ, maxZ);
        });
    }

    public static int getHeight(long key, IntSupplier loader) {
        int val = HEIGHT_CACHE.get(key);
        if (val != Integer.MIN_VALUE) {
            return val;
        }
        int h = loader.getAsInt();
        HEIGHT_CACHE.put(key, h);
        return h;
    }

    public static int getHeight(ServerWorld world, int x, int z) {
        ChunkGenerator gen = world.getChunkManager().getChunkGenerator();
        NoiseConfig cfg = world.getChunkManager().getNoiseConfig();
        long key = hash(x, z);
        return getHeight(key, () -> gen.getHeight(x, z, Heightmap.Type.WORLD_SURFACE, world, cfg));
    }

    public static double getStability(long key, DoubleSupplier loader) {
        double val = STABILITY_CACHE.get(key);
        if (val != Double.MAX_VALUE) {
            return val;
        }
        double s = loader.getAsDouble();
        STABILITY_CACHE.put(key, s);
        return s;
    }

    public static RegistryEntry<Biome> getBiome(long key, Supplier<RegistryEntry<Biome>> loader) {
        RegistryEntry<Biome> entry = BIOME_CACHE.get(key);
        if (entry != null) {
            return entry;
        }
        RegistryEntry<Biome> b = loader.get();
        BIOME_CACHE.put(key, b);
        return b;
    }

    public static long hash(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFF_FFFFL);
    }

    public static BlockPos keyToPos(long k) {
        return new BlockPos((int) (k >> 32), 0, (int) k);
    }
}
