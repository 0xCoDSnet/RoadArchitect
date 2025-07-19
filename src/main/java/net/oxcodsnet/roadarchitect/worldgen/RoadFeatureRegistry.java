package net.oxcodsnet.roadarchitect.worldgen;

import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.oxcodsnet.roadarchitect.RoadArchitect;

/**
 * Registers the {@link RoadFeature} type.
 */
public class RoadFeatureRegistry {
    public static final RoadFeature ROAD_FEATURE = new RoadFeature(Blocks.GLASS.getDefaultState());

    private static Identifier id() {
        return Identifier.of(RoadArchitect.MOD_ID, "road");
    }

    public static void register() {
        Registry.register(Registries.FEATURE, id(), ROAD_FEATURE);
    }
}
