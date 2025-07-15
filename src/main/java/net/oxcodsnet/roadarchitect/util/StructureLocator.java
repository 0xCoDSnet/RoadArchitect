package net.oxcodsnet.roadarchitect.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
    private static Set<Identifier> allowedIds;

    /** Executor for asynchronous structure scanning. */
    private static final ExecutorService SCAN_EXECUTOR =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "structure-locator");
                t.setDaemon(true);
                return t;
            });

    /** Executor handling pathfinding and road planning. */
    private static final ExecutorService PATH_EXECUTOR =
            Executors.newFixedThreadPool(2, r -> {
                Thread t = new Thread(r, "roadarchitect-planner");
                t.setDaemon(true);
                return t;
            });

    private StructureLocator() {
    }

    /**
     * Registers event listeners that trigger structure scanning when a player
     * joins or changes dimension.
     */
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register(StructureLocator::onPlayerJoin);
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(StructureLocator::onPlayerChangeWorld);
        // TODO: Нужно придумать другой способ динамически собирать новые структуры, этот не работает и из-за него не грузит чанки
        //ServerChunkEvents.CHUNK_LOAD.register(StructureLocator::onChunkLoad);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            SCAN_EXECUTOR.shutdownNow();
            PATH_EXECUTOR.shutdownNow();
        });
    }

    private static void onPlayerJoin(ServerPlayNetworkHandler handler,  PacketSender sender,  MinecraftServer server) {
        ServerWorld world = handler.getPlayer().getServerWorld();
        if (world.getRegistryKey() == World.OVERWORLD) {
            locateStructuresAsync(world, handler.getPlayer().getBlockPos(), RoadArchitect.CONFIG.playerScanRadius());
        }
    }

    private static void onPlayerChangeWorld(ServerPlayerEntity player, ServerWorld origin, ServerWorld destination) {
        if (destination.getRegistryKey() == World.OVERWORLD) {
            locateStructuresAsync(destination, player.getBlockPos(), RoadArchitect.CONFIG.playerScanRadius());
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
            locateStructuresAsync(world, pos, RoadArchitect.CONFIG.chunkLoadScanRadius());
        });
    }


    /**
     * Returns the set of structure identifiers allowed by the configured
     * structure selectors.
     */
    private static Set<Identifier> getAllowedIds(ServerWorld world) {
        if (allowedIds != null) {
            return allowedIds;
        }

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Identifier> result = new HashSet<>();
        for (String selector : RoadArchitect.CONFIG.structureSelectors()) {
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
     * Scans for structures around the given origin without storing the results.
     */
    private static Set<NodeStorage.Node> scanStructures(ServerWorld world, BlockPos originPos,
            int chunkRadius) {
        StructurePlacementCalculator calculator = world.getChunkManager().getStructurePlacementCalculator();
        calculator.tryCalculate();

        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        NoiseConfig noiseConfig = calculator.getNoiseConfig();

        Registry<Structure> registry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        Set<Identifier> allowed = getAllowedIds(world);
        ChunkPos originChunk = new ChunkPos(originPos);

        Set<NodeStorage.Node> result = new HashSet<>();
        for (RegistryEntry<Structure> entry : registry.streamEntries().toList()) {
            Identifier entryId = registry.getId(entry.value());
            if (!allowed.contains(entryId)) {
                continue;
            }

            List<StructurePlacement> placements = calculator.getPlacements(entry);
            if (placements.isEmpty()) {
                continue;
            }

            for (StructurePlacement placement : placements) {
                for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                    for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                        int chunkX = originChunk.x + dx;
                        int chunkZ = originChunk.z + dz;
                        if (!placement.shouldGenerate(calculator, chunkX, chunkZ)) {
                            continue;
                        }

                        BlockPos roughPos = placement.getLocatePos(new ChunkPos(chunkX, chunkZ));
                        int surfaceY = generator.getHeight(
                                roughPos.getX(),
                                roughPos.getZ(),
                                Heightmap.Type.WORLD_SURFACE_WG,
                                world,
                                noiseConfig);

                        BlockPos realPos = new BlockPos(roughPos.getX(), surfaceY, roughPos.getZ());
                        String id = registry.getId(entry.value()).toString();
                        result.add(new NodeStorage.Node(realPos, id));
                    }
                }
            }
        }
        return result;
    }

    /**
     * Stores the given nodes and logs them if they were newly added.
     */
    private static void storeAndLog(ServerWorld world, Set<NodeStorage.Node> nodes) {
        NodeStorageState nodeState = NodeStorageState.get(world);
        NodeStorage nodeStorage = nodeState.getStorage();
        EdgeStorageState edgeState = EdgeStorageState.get(world);
        EdgeStorage edges = edgeState.getStorage();

        Set<NodeStorage.Node> newNodes = new HashSet<>();
        for (NodeStorage.Node node : nodes) {
            if (nodeStorage.add(node.pos(), node.structure())) {
                newNodes.add(node);
                RoadArchitect.LOGGER.info(
                        "Found structure {} at ({}, {}, {}) in {}",
                        node.structure(),
                        node.pos().getX(),
                        node.pos().getY(),
                        node.pos().getZ(),
                        world.getRegistryKey().getValue());
            }
        }

        if (newNodes.isEmpty()) {
            return;
        }

        PathFinder.Environment env = new PathFinder.WorldEnvironment(world);

        for (NodeStorage.Node start : newNodes) {
            for (NodeStorage.Node target : nodeStorage.asNodeSet()) {
                if (start.equals(target)) {
                    continue;
                }
                if (edges.contains(start.pos(), target.pos())) {
                    continue;
                }

                PATH_EXECUTOR.submit(() -> {
                    List<BlockPos> path = PathFinder.findPath(start.pos(), target.pos(), env);
                    if (path.isEmpty()) {
                        RoadArchitect.LOGGER.info(
                                "No path found between {} and {}",
                                start.pos(),
                                target.pos());
                        return;
                    }

                    world.getServer().execute(() -> {
                        if (edges.add(start.pos(), target.pos())) {
                            edgeState.markDirty();
                        }
                        RoadArchitect.LOGGER.info(
                                "Planned road of {} blocks between {} and {}",
                                path.size(),
                                start.pos(),
                                target.pos());
                        RoadPlanner.planRoad(world, path);
                    });
                });
            }
        }

        nodeState.markDirty();
    }

    private static void locateStructuresAsync(ServerWorld world, BlockPos originPos, int chunkRadius) {
        SCAN_EXECUTOR.submit(() -> {
            Set<NodeStorage.Node> nodes = scanStructures(world, originPos, chunkRadius);
            if (!nodes.isEmpty()) {
                world.getServer().execute(() -> storeAndLog(world, nodes));
            }
        });
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
        Set<NodeStorage.Node> nodes = scanStructures(world, originPos, chunkRadius);
        storeAndLog(world, nodes);
    }
}