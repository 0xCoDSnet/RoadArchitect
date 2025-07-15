package net.oxcodsnet.roadarchitect.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.oxcodsnet.roadarchitect.RoadArchitect;

/**
 * Utility that logs potential structure locations around a player.
 */
public final class StructureLocator {
    private static final int CHUNK_RADIUS = 100;

    private StructureLocator() {
    }

    /**
     * Registers event listeners that trigger structure scanning when a player
     * joins or changes dimension.
     */
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register(StructureLocator::onPlayerJoin);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(StructureLocator::onPlayerChangeWorld);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler,  PacketSender sender,  MinecraftServer server) {
        ServerWorld world = handler.getPlayer().getServerWorld();
        if (world.getRegistryKey() == World.OVERWORLD) {
            locateStructures(world, handler.getPlayer().getBlockPos());
        }
    }

    private static void onPlayerChangeWorld( ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        if (destination.getRegistryKey() == World.OVERWORLD) {
            locateStructures(destination, player.getBlockPos());
        }
    }

    /**
     * Performs a seed-based scan for structures around the given position
     * without loading chunks. Any found structures are logged once.
     */
    private static void locateStructures(ServerWorld world, BlockPos originPos) {
        // 1) Создаём калькулятор структур и инициализируем его
        StructurePlacementCalculator calculator = world.getChunkManager().getStructurePlacementCalculator();
        calculator.tryCalculate();

        // 2) Берём генератор чанков и конфигурацию шума из калькулятора
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = calculator.getNoiseConfig();

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        ChunkPos originChunk = new ChunkPos(originPos);
        Set<String> logged = new HashSet<>();

        for (RegistryEntry<Structure> entry : registry.streamEntries().toList()) {
            List<StructurePlacement> placements = calculator.getPlacements(entry);
            if (placements.isEmpty()) continue;

            for (StructurePlacement placement : placements) {
                for (int dx = -CHUNK_RADIUS; dx <= CHUNK_RADIUS; dx++) {
                    for (int dz = -CHUNK_RADIUS; dz <= CHUNK_RADIUS; dz++) {
                        int chunkX = originChunk.x + dx;
                        int chunkZ = originChunk.z + dz;
                        if (!placement.shouldGenerate(calculator, chunkX, chunkZ)) continue;

                        // 3) Получаем предварительную позицию (Y=0)
                        BlockPos roughPos = placement.getLocatePos(new ChunkPos(chunkX, chunkZ));

                        // 4) Запрашиваем реальную высоту ландшафта через getHeight
                        int surfaceY = generator.getHeight(
                                roughPos.getX(), roughPos.getZ(),
                                Heightmap.Type.WORLD_SURFACE_WG,
                                world,
                                noiseConfig
                        );

                        BlockPos realPos = new BlockPos(roughPos.getX(), surfaceY, roughPos.getZ());
                        String key = registry.getId(entry.value())
                                + "@" + realPos.getX()
                                + "," + realPos.getY()
                                + "," + realPos.getZ();
                        if (logged.add(key)) {
                            RoadArchitect.LOGGER.info(
                                    "Found structure {} at ({}, {}, {}) in {}",
                                    registry.getId(entry.value()),
                                    realPos.getX(), realPos.getY(), realPos.getZ(),
                                    world.getRegistryKey().getValue()
                            );
                        }
                    }
                }
            }
        }
    }
}
