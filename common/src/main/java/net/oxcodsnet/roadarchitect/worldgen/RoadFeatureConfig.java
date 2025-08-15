package net.oxcodsnet.roadarchitect.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/**
 * Configuration for {@link RoadFeature}.
 */
public record RoadFeatureConfig(int orthWidth, int forwardLength) implements FeatureConfig {

    /**
     * Codec for serialising the configuration.
     */
    public static final Codec<RoadFeatureConfig> CODEC = RecordCodecBuilder.create(
            i -> i.group(
                    Codec.INT.fieldOf("orth_width").forGetter(RoadFeatureConfig::orthWidth),
                    Codec.INT.fieldOf("forward_length").forGetter(RoadFeatureConfig::forwardLength)
            ).apply(i, RoadFeatureConfig::new)
    );
}
