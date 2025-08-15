package net.oxcodsnet.roadarchitect.worldgen.style;

import net.oxcodsnet.roadarchitect.worldgen.style.decoration.Decoration;

/**
 * Bundles together block palette and decoration for a biome.
 */
public record RoadStyle(BlockPalette palette, Decoration decoration) {
}

