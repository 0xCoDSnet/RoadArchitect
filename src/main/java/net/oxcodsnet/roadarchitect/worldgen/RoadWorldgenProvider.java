package net.oxcodsnet.roadarchitect.worldgen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.minecraft.world.gen.placementmodifier.SquarePlacementModifier;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data provider for Road Architect world generation features.
 */
public class RoadWorldgenProvider extends FabricDynamicRegistryProvider {
    public RoadWorldgenProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) {
        super(output, registries);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {

        entries.add(RoadFeatureRegistry.GLASS_ROAD, new ConfiguredFeature<>(RoadFeatureRegistry.ROAD_FEATURE, new RoadFeatureConfig(3)));
        entries.add(RoadFeatureRegistry.WOOD_ROAD, new ConfiguredFeature<>(new RoadFeature(Blocks.OAK_PLANKS.getDefaultState()), new RoadFeatureConfig(3)));
        entries.add(RoadFeatureRegistry.STONE_ROAD, new ConfiguredFeature<>(new RoadFeature(Blocks.STONE.getDefaultState()), new RoadFeatureConfig(3)));

        entries.add(RoadFeatureRegistry.GLASS_ROAD_PLACED, new PlacedFeature(entries.ref(RoadFeatureRegistry.GLASS_ROAD), List.of(SquarePlacementModifier.of())));
        entries.add(RoadFeatureRegistry.WOOD_ROAD_PLACED, new PlacedFeature(entries.ref(RoadFeatureRegistry.WOOD_ROAD), List.of(SquarePlacementModifier.of())));
        entries.add(RoadFeatureRegistry.STONE_ROAD_PLACED, new PlacedFeature(entries.ref(RoadFeatureRegistry.STONE_ROAD), List.of(SquarePlacementModifier.of())));
    }

    @Override
    public String getName() {
        return "RoadArchitect Worldgen";
    }
}
