package net.oxcodsnet.roadarchitect.worldgen.style;

import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;
import net.oxcodsnet.roadarchitect.worldgen.style.decoration.FenceDecoration;

import java.util.Map;

/**
 * Provides biome specific road styles.
 */
public final class RoadStyles {
    private static final RoadStyle DEFAULT = new RoadStyle(
            BlockPalette.builder()
                    .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                    .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                    .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                    .add(Blocks.GRAVEL.getDefaultState(), 1)
                    .build(),
            Decoration.NONE);

    private static final Map<RegistryKey<Biome>, RoadStyle> STYLES = Map.ofEntries(

            Map.entry(BiomeKeys.RIVER, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.REDSTONE_BLOCK.getDefaultState(), 1)
                            .build(),
                    Decoration.NONE)
            ),

            Map.entry(BiomeKeys.STONY_SHORE, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.STONE.getDefaultState(), 7)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.GRAVEL.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.OAK_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.BEACH, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.SAND.getDefaultState(), 7)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.GRAVEL.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.OAK_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.OLD_GROWTH_PINE_TAIGA, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.PODZOL.getDefaultState(), 7)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.GRAVEL.getDefaultState(), 1)
                            .add(Blocks.TUFF.getDefaultState(), 1)
                            .add(Blocks.ANDESITE.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.SPRUCE_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.OLD_GROWTH_SPRUCE_TAIGA, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.PODZOL.getDefaultState(), 7)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 2)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.GRAVEL.getDefaultState(), 1)
                            .add(Blocks.TUFF.getDefaultState(), 1)
                            .add(Blocks.ANDESITE.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.SPRUCE_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.TAIGA, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                            .add(Blocks.STONE_BRICKS.getDefaultState(), 1)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.MOSSY_STONE_BRICKS.getDefaultState(), 1)
                            .add(Blocks.MOSS_BLOCK.getDefaultState(), 1)
                            .add(Blocks.STONE.getDefaultState(), 1)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.SPRUCE_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.SWAMP, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                            .add(Blocks.TUFF.getDefaultState(), 1)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.MOSSY_STONE_BRICKS.getDefaultState(), 1)
                            .add(Blocks.CRACKED_STONE_BRICKS.getDefaultState(), 1)
                            .add(Blocks.MOSS_BLOCK.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.MOSSY_COBBLESTONE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.PLAINS, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                            .add(Blocks.STONE.getDefaultState(), 1)
                            .add(Blocks.ANDESITE.getDefaultState(), 1)
                            .add(Blocks.CRACKED_STONE_BRICKS.getDefaultState(), 1)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.MOSSY_STONE_BRICKS.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.OAK_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.DARK_FOREST, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                            .add(Blocks.MOSSY_COBBLESTONE.getDefaultState(), 1)
                            .add(Blocks.MOSS_BLOCK.getDefaultState(), 1)
                            .add(Blocks.STONE.getDefaultState(), 1)
                            .add(Blocks.COBBLESTONE.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.DARK_OAK_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.SAVANNA, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.GRASS_BLOCK.getDefaultState(), 7)
                            .add(Blocks.COARSE_DIRT.getDefaultState(), 1)
                            .add(Blocks.ANDESITE.getDefaultState(), 1)
                            .add(Blocks.GRAVEL.getDefaultState(), 1)
                            .build(),
                    new FenceDecoration(Blocks.ACACIA_FENCE.getDefaultState()))
            ),
            Map.entry(BiomeKeys.DESERT, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.SMOOTH_SANDSTONE.getDefaultState(), 7)
                            .add(Blocks.SUSPICIOUS_SAND.getDefaultState(), 2)
                            .add(Blocks.PACKED_MUD.getDefaultState(), 2)
                            .build(),
                    new FenceDecoration(Blocks.SANDSTONE_WALL.getDefaultState()))
            ),
            Map.entry(BiomeKeys.BADLANDS, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.RED_SANDSTONE.getDefaultState(), 7)
                            .add(Blocks.RED_SAND.getDefaultState(), 2)
                            .add(Blocks.PACKED_MUD.getDefaultState(), 2)
                            .build(),
                    new FenceDecoration(Blocks.RED_SANDSTONE_WALL.getDefaultState()))
            ),
            Map.entry(BiomeKeys.WOODED_BADLANDS, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.RED_SANDSTONE.getDefaultState(), 7)
                            .add(Blocks.RED_SAND.getDefaultState(), 2)
                            .add(Blocks.PACKED_MUD.getDefaultState(), 2)
                            .build(),
                    new FenceDecoration(Blocks.RED_SANDSTONE_WALL.getDefaultState()))
            ),
            Map.entry(BiomeKeys.ERODED_BADLANDS, new RoadStyle(
                    BlockPalette.builder()
                            .add(Blocks.RED_SANDSTONE.getDefaultState(), 7)
                            .add(Blocks.RED_SAND.getDefaultState(), 2)
                            .add(Blocks.PACKED_MUD.getDefaultState(), 2)
                            .build(),
                    new FenceDecoration(Blocks.RED_SANDSTONE_WALL.getDefaultState()))
            )
    );

    private RoadStyles() {
    }

    public static RoadStyle forBiome(RegistryEntry<Biome> biomeEntry) {
        return biomeEntry.getKey().map(STYLES::get).orElse(DEFAULT);
    }
}

