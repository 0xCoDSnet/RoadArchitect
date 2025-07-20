package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * Configuration for {@link RoadFeature}.
 */
public record RoadFeatureConfig(int width) implements FeatureConfig {
    //...
}
