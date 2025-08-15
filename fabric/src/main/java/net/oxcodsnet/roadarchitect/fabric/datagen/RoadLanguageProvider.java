package net.oxcodsnet.roadarchitect.fabric.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import net.oxcodsnet.roadarchitect.datagen.RALanguage;

import java.util.concurrent.CompletableFuture;

public class RoadLanguageProvider extends FabricLanguageProvider {
    private final String code;

    public RoadLanguageProvider(
            FabricDataOutput output,
            CompletableFuture<RegistryWrapper.WrapperLookup> registries,
            String code
    ) {
        super(output, code, registries);
        this.code = code;
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup registries, TranslationBuilder builder) {
        // вся таблица ключей/значений теперь в common:
        RALanguage.fill(this.code, builder::add);
    }
}
