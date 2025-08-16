package net.oxcodsnet.roadarchitect.neoforge.datagen;

import net.minecraft.data.DataOutput;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.datagen.RACommonDatagen;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Generates configured and placed features for world generation.
 */
public class RoadWorldgenProvider extends DatapackBuiltinEntriesProvider {
    public RoadWorldgenProvider(DataOutput output, CompletableFuture<WrapperLookup> registries) {
        super(output, registries, createBuilder(), Set.of(RoadArchitect.MOD_ID));
    }

    private static RegistryBuilder createBuilder() {
        RegistryBuilder builder = new RegistryBuilder();
        RACommonDatagen.buildRegistries(builder);
        return builder;
    }
}

