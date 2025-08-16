package net.oxcodsnet.roadarchitect.neoforge.events;

import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.GenerationStep;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * NeoForge bridge for road feature registration and biome injection.
 */
public final class RoadFeatureRegistryNeoForge {
    private RoadFeatureRegistryNeoForge() {
    }

    /**
     * Registers the road feature and injects the placed feature into all biomes.
     */
    public static void register(RegisterEvent event) {
        event.register(RegistryKeys.FEATURE,
                helper -> helper.register(RoadFeatureRegistry.ROAD_FEATURE_KEY, RoadFeatureRegistry.ROAD_FEATURE));
    }
}