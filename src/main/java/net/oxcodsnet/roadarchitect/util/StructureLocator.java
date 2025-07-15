package net.oxcodsnet.roadarchitect.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
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
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.util.math.BlockBox;
import net.minecraft.structure.StructureStart;
/**
 * Utility that logs potential structure locations around a player.
 */
public final class StructureLocator {
    /**
     * List of structure selectors. Each entry can be either a structure ID or a
     * tag prefixed with '#'.
     */
    private static final List<String> SELECTORS = List.of(
            "#minecraft:village",
            "minecraft:village_plains"
    );

    private static Set<Identifier> allowedIds;

    private StructureLocator() {
    }

    /**
     * Registers event listeners that trigger structure scanning when a player
     * joins or changes dimension.
     */
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register(StructureLocator::onPlayerJoin);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(StructureLocator::onPlayerChangeWorld);
        ServerChunkEvents.CHUNK_LOAD.register(StructureLocator::onChunkLoad);
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler,  PacketSender sender,  MinecraftServer server) {
        ServerWorld world = handler.getPlayer().getServerWorld();
        if (world.getRegistryKey() == World.OVERWORLD) {
            locateStructures(world, handler.getPlayer().getBlockPos(), RoadArchitect.CONFIG.playerScanRadius());
        }
    }

    private static void onPlayerChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        if (destination.getRegistryKey() == World.OVERWORLD) {
            locateStructures(destination, player.getBlockPos(), RoadArchitect.CONFIG.playerScanRadius());
        }
    }

    private static void onChunkLoad(ServerWorld world,  WorldChunk chunk) {
        if (world.getRegistryKey() != World.OVERWORLD) {
            return;
        }
        chunk.getStructureStarts().forEach((key, start) -> {
            if (start == null || start.equals(StructureStart.DEFAULT)) {
                return;
            }
            if (!start.hasChildren()) {
                return;
            }
            BlockBox box = start.getBoundingBox();
            int x = (box.getMinX() + box.getMaxX()) >> 1;
            int z = (box.getMinZ() + box.getMaxZ()) >> 1;
            BlockPos pos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(x, 0, z));
            locateStructures(world, pos, RoadArchitect.CONFIG.chunkLoadScanRadius());
        });
    }

    /**
     * Returns the set of structure identifiers allowed by {@link #SELECTORS}.
     */
    private static Set<Identifier> getAllowedIds(ServerWorld world) {
        if (allowedIds != null) {
            return allowedIds;
        }

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Identifier> result = new HashSet<>();
        for (String selector : SELECTORS) {
            if (selector.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(selector.substring(1));
                if (tagId != null) {
                    TagKey<Structure> tagKey = TagKey.of(RegistryKeys.STRUCTURE, tagId);
                    registry.getEntryList(tagKey).ifPresent(list ->
                            list.stream().map(entry -> registry.getId(entry.value())).forEach(result::add));
                }
            } else {
                Identifier id = Identifier.tryParse(selector);
                if (id != null && registry.containsId(id)) {
                    result.add(id);
                }
            }
        }

        if (result.isEmpty()) {
            // Fallback to all structures if nothing matched
            result.addAll(registry.streamEntries().map(entry -> registry.getId(entry.value())).collect(Collectors.toSet()));
        }
        allowedIds = result;
        return allowedIds;
    }

    /**
     * Performs a seed-based scan for structures around the given position
     * without loading chunks. Any found structures are logged once and stored
     * for pathfinding.
     *
     * @param world       the world to scan
     * @param originPos   center position of the scan
     * @param chunkRadius radius in chunks to search around the origin
     */
    private static void locateStructures(ServerWorld world, BlockPos originPos, int chunkRadius) {
        // 1) Создаём калькулятор структур и инициализируем его
        StructurePlacementCalculator calculator = world.getChunkManager().getStructurePlacementCalculator();
        calculator.tryCalculate();

        // 2) Берём генератор чанков и конфигурацию шума из калькулятора
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = calculator.getNoiseConfig();

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Identifier> allowed = getAllowedIds(world);
        ChunkPos originChunk = new ChunkPos(originPos);
        Set<String> logged = new HashSet<>();

        for (RegistryEntry<Structure> entry : registry.streamEntries().toList()) {
            Identifier entryId = registry.getId(entry.value());
            if (!allowed.contains(entryId)) {
                continue;
            }
            List<StructurePlacement> placements = calculator.getPlacements(entry);
            if (placements.isEmpty()) continue;

            for (StructurePlacement placement : placements) {
                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
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
                        String id = registry.getId(entry.value()).toString();
                        if (NodeStorageState.get(world).getStorage().add(realPos, id)) {
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