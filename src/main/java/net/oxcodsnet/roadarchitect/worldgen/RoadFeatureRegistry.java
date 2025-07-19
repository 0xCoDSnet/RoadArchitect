package net.oxcodsnet.roadarchitect.worldgen;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registerable;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;

/**
 * Central place for registering the custom {@link RoadFeature} and all of its configured & placed variants.
 * <p>
 * <b>Why it is split into two methods</b>
 * <ul>
 *     <li>{@link #bootstrapFeature(Registerable)} – called <em>only</em> by the built‑in Fabric data‑generator (via
 *     {@code RegistryBuilder.addRegistry}) and therefore receives a {@link Registerable} that is still mutable.</li>
 *     <li>{@link #register()} – called from {@code RoadArchitect.onInitialize()} when the game starts normally.
 *     It uses the static {@link Registry} helpers because registries have not been frozen yet in that phase.</li>
 * </ul>
 * This guarantees we never try to write to a frozen registry again, fixing the
 * <code>IllegalStateException: Registry is already frozen</code> you encountered.
 */
public final class RoadFeatureRegistry {

    /* ------------------------------------------------------------------------- */
    /* Keys & Identifiers                                                        */
    /* ------------------------------------------------------------------------- */

    // ---- Raw feature ---------------------------------------------------------

    public static final RegistryKey<Feature<?>> ROAD_KEY = RegistryKey.of(RegistryKeys.FEATURE, Identifier.of(RoadArchitect.MOD_ID, "road"));
    public static final RoadFeature ROAD_FEATURE = new RoadFeature(Blocks.GLASS.getDefaultState());

    // ---- Configured features --------------------------------------------------

    public static final RegistryKey<ConfiguredFeature<?, ?>> GLASS_ROAD = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"glass_road"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> WOOD_ROAD  = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"wood_road"));
    public static final RegistryKey<ConfiguredFeature<?, ?>> STONE_ROAD = RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"stone_road"));

    // ---- Placed features ------------------------------------------------------

    public static final RegistryKey<PlacedFeature> GLASS_ROAD_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"glass_road"));
    public static final RegistryKey<PlacedFeature> WOOD_ROAD_PLACED  = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"wood_road"));
    public static final RegistryKey<PlacedFeature> STONE_ROAD_PLACED = RegistryKey.of(RegistryKeys.PLACED_FEATURE, Identifier.of(RoadArchitect.MOD_ID,"stone_road"));

    /* ------------------------------------------------------------------------- */
    /* Datagen bootstrap                                                         */
    /* ------------------------------------------------------------------------- */

    /**
     * Called by Fabric's data‑generator. Uses the {@link Registerable} parameter instead of the global
     * {@link Registry} so we write to a fresh, still‑mutable registry snapshot.
     */
    public static void bootstrapFeature(Registerable<Feature<?>> context) {
        context.register(ROAD_KEY, ROAD_FEATURE);
    }

    /**
     * Bootstraps the configured features used during datagen.
     */
    public static void bootstrapConfigured(Registerable<ConfiguredFeature<?, ?>> context) {
        context.register(GLASS_ROAD, new ConfiguredFeature<>(ROAD_FEATURE, new RoadFeatureConfig(3)));
        context.register(WOOD_ROAD, new ConfiguredFeature<>(new RoadFeature(Blocks.OAK_PLANKS.getDefaultState()), new RoadFeatureConfig(3)));
        context.register(STONE_ROAD, new ConfiguredFeature<>(new RoadFeature(Blocks.STONE.getDefaultState()), new RoadFeatureConfig(3)));
    }

    /**
     * Bootstraps the placed features used during datagen.
     */
    public static void bootstrapPlaced(Registerable<PlacedFeature> context) {
        var lookup = context.getRegistryLookup(RegistryKeys.CONFIGURED_FEATURE);
        context.register(GLASS_ROAD_PLACED, new PlacedFeature(lookup.getOrThrow(GLASS_ROAD), List.of(SquarePlacementModifier.of())));
        context.register(WOOD_ROAD_PLACED, new PlacedFeature(lookup.getOrThrow(WOOD_ROAD), List.of(SquarePlacementModifier.of())));
        context.register(STONE_ROAD_PLACED, new PlacedFeature(lookup.getOrThrow(STONE_ROAD), List.of(SquarePlacementModifier.of())));
    }

    /* ------------------------------------------------------------------------- */
    /* Runtime initialisation                                                    */
    /* ------------------------------------------------------------------------- */

    /**
     * Registers the raw feature in a normal (non‑datagen) run and injects the placed versions into biomes.
     * <p>Must be called from {@code RoadArchitect.onInitialize()} <em>before</em> any registry freeze occurs.</p>
     */
    public static void register() {
        // Raw feature
        Registry.register(Registries.FEATURE, ROAD_KEY.getValue(), ROAD_FEATURE);

        // Inject placed features into all Overworld biomes
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.SURFACE_STRUCTURES, GLASS_ROAD_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.SURFACE_STRUCTURES, WOOD_ROAD_PLACED);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Feature.SURFACE_STRUCTURES, STONE_ROAD_PLACED);
    }

    /* ------------------------------------------------------------------------- */
    /* Helpers                                                                   */
    /* ------------------------------------------------------------------------- */

    /** Utility method shared by all {@code *WorldgenProvider}s for a simple “just place it in every chunk” rule. */
    public static List<net.minecraft.world.gen.placementmodifier.PlacementModifier> defaultHorizontalPlacements() {
        return List.of(SquarePlacementModifier.of());
    }

    private RoadFeatureRegistry() { /* no‑instantiation */ }
}
