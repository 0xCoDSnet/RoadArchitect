package net.oxcodsnet.roadarchitect.neoforge.datagen;

import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataOutput;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.oxcodsnet.roadarchitect.datagen.RACommonDatagen;
//import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class RoadArchitectDataGenerator {
    private RoadArchitectDataGenerator() {}

    public static void gatherData(GatherDataEvent event) {

        // Disabled on Yarn: NeoForge’s datagen uses reflection on vanilla fields
        // and crashes under Yarn mappings (see Loom/NeoForge issues).
        // Use manual JSON files instead.
        event.getGenerator().getPackOutput(); // no-op to keep class referenced


//        RegistryBuilder builder = new RegistryBuilder();
//        RACommonDatagen.buildRegistries(builder);
//        event.createDatapackRegistryObjects(builder);
//
//        DataGenerator generator = event.getGenerator();
//        DataOutput output = generator.getPackOutput();
//        CompletableFuture<WrapperLookup> lookup = event.getLookupProvider();
//        ExistingFileHelper helper = event.getExistingFileHelper();
//
//        generator.addProvider(event.includeServer(), new RoadWorldgenProvider(output, lookup));
//        generator.addProvider(event.includeServer(), new RoadBiomeModifierProvider(output, lookup, helper));
//
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "en_us"));
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "ru_ru"));
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "es_es"));
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "fr_fr"));
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "de_de"));
//        generator.addProvider(event.includeClient(), new RoadLanguageProvider(output, "zh_cn"));

    }
}