package net.oxcodsnet.roadarchitect.worldgen;

//import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
//import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;

import net.minecraft.registry.Registerable;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.oxcodsnet.roadarchitect.RoadArchitect;

/**
 * Holds registry keys and bootstrap logic for road worldgen features.
 */
public final class RoadFeatureRegistry {
    /**
     * The raw feature used for road placement.
     */
    public static final RoadFeature ROAD_FEATURE = new RoadFeature(RoadFeatureConfig.CODEC);

    /**
     * Key for the road feature instance.
     */
    public static final RegistryKey<Feature<?>> ROAD_FEATURE_KEY = RegistryKey.of(RegistryKeys.FEATURE,
            Identifier.of(RoadArchitect.MOD_ID, "road"));

    /**
     * Key for the configured road feature.
     */
    public static final RegistryKey<ConfiguredFeature<?, ?>> ROAD_CONFIGURED_FEATURE_KEY = RegistryKey.of(
            RegistryKeys.CONFIGURED_FEATURE, Identifier.of(RoadArchitect.MOD_ID, "road"));

    /**
     * Key for the placed road feature.
     */
    public static final RegistryKey<PlacedFeature> ROAD_PLACED_FEATURE_KEY = RegistryKey.of(
            RegistryKeys.PLACED_FEATURE, Identifier.of(RoadArchitect.MOD_ID, "road"));

    private RoadFeatureRegistry() {
    }

    /**
     * Bootstrap for configured features used during data generation.
     *
     * @param ctx registerable context for configured features
     */
    public static void bootstrapConfigured(Registerable<ConfiguredFeature<?, ?>> ctx) {
        ctx.register(ROAD_CONFIGURED_FEATURE_KEY, new ConfiguredFeature<>(ROAD_FEATURE, new RoadFeatureConfig(3, 1)));
    }

    /**
     * Bootstrap for placed features used during data generation.
     *
     * @param ctx registerable context for placed features
     */
    public static void bootstrapPlaced(net.minecraft.registry.Registerable<PlacedFeature> ctx) {
        RegistryEntryLookup<ConfiguredFeature<?, ?>> lookup = ctx.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);
        ctx.register(ROAD_PLACED_FEATURE_KEY, new PlacedFeature(lookup.getOrThrow(ROAD_CONFIGURED_FEATURE_KEY), java.util.List.of(SquarePlacementModifier.of())));
    }
}
