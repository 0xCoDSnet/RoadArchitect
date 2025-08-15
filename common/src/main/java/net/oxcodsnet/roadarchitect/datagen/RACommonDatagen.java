package net.oxcodsnet.roadarchitect.datagen;

import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;

/**
 * Платформо-независимый хелпер для регистрации динамических реестров в датагене.
 * Вызывается из Fabric/NeoForge-адаптеров.
 */
public final class RACommonDatagen {
    private RACommonDatagen() {}

    public static void buildRegistries(RegistryBuilder builder) {
        builder.addRegistry(RegistryKeys.CONFIGURED_FEATURE, RoadFeatureRegistry::bootstrapConfigured);
        builder.addRegistry(RegistryKeys.PLACED_FEATURE, RoadFeatureRegistry::bootstrapPlaced);
    }
}
