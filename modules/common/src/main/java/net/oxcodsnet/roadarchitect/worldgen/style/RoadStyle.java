package net.oxcodsnet.roadarchitect.worldgen.style;

import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;

import java.util.List;

/**
 * Bundles together block palette and decorations for a biome.
 */
public record RoadStyle(BlockPalette palette, List<Decoration> decorations) {
    public RoadStyle(BlockPalette palette, Decoration... decorations) {
        this(palette, List.of(decorations));
    }
}

