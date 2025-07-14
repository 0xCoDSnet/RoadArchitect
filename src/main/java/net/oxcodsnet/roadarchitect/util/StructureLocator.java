package net.oxcodsnet.roadarchitect.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.structure.StructureStart;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registry;

import net.oxcodsnet.roadarchitect.RoadArchitect;

public final class StructureLocator {
    private StructureLocator() {}

    public static void init() {
        ServerChunkEvents.CHUNK_LOAD.register(StructureLocator::onChunkLoad);
        ServerChunkEvents.CHUNK_GENERATE.register(StructureLocator::onChunkGenerate);
    }

    private static void onChunkLoad(ServerWorld world, WorldChunk chunk) {
        logStructures(world, chunk);
    }

    private static void onChunkGenerate(ServerWorld world, WorldChunk chunk) {
        logStructures(world, chunk);
    }

    private static void logStructures(ServerWorld world, WorldChunk chunk) {
        StructureAccessor accessor = world.getStructureAccessor();
        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        ChunkSectionPos sectionPos = ChunkSectionPos.from(chunk.getPos(), world.getBottomSectionCoord());

        for (Structure structure : registry) {
            for (StructureStart start : accessor.getStructureStarts(sectionPos, structure)) {
                if (start != null && start.hasChildren()) {
                    BlockBox box = start.getBoundingBox();
                    BlockPos center = box.getCenter();
                    RoadArchitect.LOGGER.info(
                            "Found structure {} at ({}, {}, {}) in {}",
                            registry.getId(structure),
                            center.getX(), center.getY(), center.getZ(),
                            world.getRegistryKey().getValue());
                }
            }
        }
    }
}
