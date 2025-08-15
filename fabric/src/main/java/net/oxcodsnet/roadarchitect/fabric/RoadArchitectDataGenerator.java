package net.oxcodsnet.roadarchitect.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.oxcodsnet.roadarchitect.fabric.datagen.RoadLanguageProvider;
import net.oxcodsnet.roadarchitect.fabric.worldgen.RoadFeatureRegistry;
import net.oxcodsnet.roadarchitect.fabric.worldgen.RoadWorldgenProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryWrapper;
import java.util.concurrent.CompletableFuture;

/**
 * Генератор данных для мода.
 * <p>Data generation entry point for the mod.</p>
 */
public class RoadArchitectDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void buildRegistry(RegistryBuilder registryBuilder) {
        registryBuilder.addRegistry(RegistryKeys.CONFIGURED_FEATURE, RoadFeatureRegistry::bootstrapConfigured);
        registryBuilder.addRegistry(RegistryKeys.PLACED_FEATURE, RoadFeatureRegistry::bootstrapPlaced);
    }

    @Override
    /**
     * Точка входа генератора данных.
     * <p>Entry point for Fabric data generation.</p>
     */
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(RoadWorldgenProvider::new);
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "en_us")
        );
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "ru_ru")
        );
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "es_es")
        );
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "fr_fr")
        );
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "de_de")
        );
        pack.addProvider(
                (FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "zh_cn")
        );
    }
}
