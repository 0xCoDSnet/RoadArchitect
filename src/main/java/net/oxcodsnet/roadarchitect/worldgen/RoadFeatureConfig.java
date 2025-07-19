package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * Configuration for {@link RoadFeature}.
 */
public record RoadFeatureConfig(int width) implements FeatureConfig {
    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.intRange(1, 64).fieldOf("width").forGetter(RoadFeatureConfig::width)
            ).apply(instance, RoadFeatureConfig::new)
    );
}
