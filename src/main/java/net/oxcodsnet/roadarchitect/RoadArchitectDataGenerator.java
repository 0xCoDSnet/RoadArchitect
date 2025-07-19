package net.oxcodsnet.roadarchitect;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.oxcodsnet.roadarchitect.worldgen.RoadWorldgenProvider;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Генератор данных для мода.
 * <p>Data generation entry point for the mod.</p>
 */
public class RoadArchitectDataGenerator implements DataGeneratorEntrypoint {


        @Override
        public void buildRegistry(RegistryBuilder registryBuilder) {
            registryBuilder.addRegistry(RegistryKeys.FEATURE, RoadFeatureRegistry::bootstrap);
        }

        @Override
        /**
         * Точка входа генератора данных.
         * <p>Entry point for Fabric data generation.</p>
         */
        public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
            FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
            pack.addProvider(RoadWorldgenProvider::new);
        }
}
