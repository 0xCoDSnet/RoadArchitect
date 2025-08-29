package net.oxcodsnet.roadarchitect.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryWrapper;
import net.oxcodsnet.roadarchitect.datagen.RACommonDatagen;
import net.oxcodsnet.roadarchitect.fabric.worldgen.RoadWorldgenProvider;

import java.util.concurrent.CompletableFuture;

public class RoadArchitectDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void buildRegistry(RegistryBuilder registryBuilder) {
        RACommonDatagen.buildRegistries(registryBuilder);
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(RoadWorldgenProvider::new);
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "en_us")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "ru_ru")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "es_es")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "fr_fr")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "de_de")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "zh_cn")
        );
        pack.addProvider((FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries) ->
                        new RoadLanguageProvider(output, registries, "uk_ua")
        );
    }
}
