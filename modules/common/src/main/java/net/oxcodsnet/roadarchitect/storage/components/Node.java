package net.oxcodsnet.roadarchitect.storage.components;

import net.minecraft.util.math.BlockPos;

/**
 * Представляет узел точки интереса.
 * <p>Represents a point of interest node.</p>
 *
 * @param id   уникальный идентификатор узла / unique node id
 * @param pos  позиция узла в мире / node position in the world
 * @param type идентификатор структуры / structure identifier
 */
public record Node(String id, BlockPos pos, String type) {
}
