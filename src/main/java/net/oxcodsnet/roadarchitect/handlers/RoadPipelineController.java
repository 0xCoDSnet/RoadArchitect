package net.oxcodsnet.roadarchitect.handlers;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controls execution of the road generation pipeline.
 */
public final class RoadPipelineController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/RoadPipelineController");

    /**
     * Tracks worlds that have completed initial pipeline setup.
     * Cleared on server stop to allow fresh initialization next run.
     */
    private static final Set<RegistryKey<World>> INITIALIZED = ConcurrentHashMap.newKeySet();
    private static final int INTERVAL_TICKS = RoadArchitect.CONFIG.pipelineIntervalSeconds() * 20;
    // Cached selectors for fast matching
    private static final Set<Identifier> TARGET_IDS = new HashSet<>();
    private static final Set<TagKey<Structure>> TARGET_TAGS = new HashSet<>();
    private static int tickCounter = 0;

    private RoadPipelineController() {
    }

    /**
     * Registers event hooks for pipeline triggers.
     */
    public static void register() {
        cacheStructureSelectors();

        // ───── First‑chunk generation trigger ────────────────────────────────────
        // Fires only when the spawn chunk is generated for the first time.
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) {
                return;
            }

            ChunkPos spawnChunk = new ChunkPos(world.getSpawnPos());
            if (!chunk.getPos().equals(spawnChunk)) {
                return; // Not the spawn chunk – ignore.
            }

            if (INITIALIZED.add(world.getRegistryKey())) {
                LOGGER.debug("Spawn chunk {} generated in {}, starting pipeline", chunk.getPos(),
                        world.getRegistryKey().getValue());
                PipelineRunner.runPipeline(world, world.getSpawnPos(), PipelineRunner.PipelineMode.INIT);
            }
        });

        // ───── Chunk generation containing a target structure ───────────────────
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            if (world.getRegistryKey() != World.OVERWORLD) {
                return;
            }
            if (!containsTargetStructure(world, chunk)) {
                return;
            }
            LOGGER.debug("Chunk {} generated with target structure, starting pipeline", chunk.getPos());
            PipelineRunner.runPipeline(world, chunk.getPos().getCenterAtY(0),
                    PipelineRunner.PipelineMode.CHUNK);
        });

        // ───── Player‑join trigger – run as early as possible for the player ────
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ServerWorld world = (ServerWorld) player.getWorld();
            if (world.getRegistryKey() != World.OVERWORLD) {
                return;
            }
            BlockPos pos = player.getBlockPos();
            LOGGER.debug("Player {} joined at {}, starting pipeline", player.getName().getString(), pos);
            // Using PERIODIC mode as the closest existing mode; it uses small scan radius.
            PipelineRunner.runPipeline(world, pos, PipelineRunner.PipelineMode.PERIODIC);
        });

        // ───── Periodic trigger every INTERVAL_TICKS ────────────────────────────
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter < INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                World world = player.getWorld();
                if (world.getRegistryKey() != World.OVERWORLD) {
                    continue;
                }
                BlockPos pos = player.getBlockPos();
                LOGGER.debug("Periodic trigger at player {} pos {}, starting pipeline", player.getName().getString(), pos);
                PipelineRunner.runPipeline((ServerWorld) world, pos, PipelineRunner.PipelineMode.PERIODIC);
            }
        });

        // ───── Clear state on server stop ───────────────────────────────────────
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> INITIALIZED.clear());
    }

    /**
     * Pre‑caches identifiers and tags from config for O(1) matching.
     */
    private static void cacheStructureSelectors() {
        TARGET_IDS.clear();
        TARGET_TAGS.clear();
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        for (String sel : selectors) {
            if (sel.startsWith("#")) {
                TARGET_TAGS.add(TagKey.of(RegistryKeys.STRUCTURE, Identifier.of(sel.substring(1))));
            } else {
                TARGET_IDS.add(Identifier.of(sel));
            }
        }
    }

    /* ─────────────────────────── Structure matching ─────────────────────────── */

    private static boolean containsTargetStructure(ServerWorld world, Chunk chunk) {
        if (!chunk.hasStructureReferences()) {
            return false;
        }

        Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);
        for (StructureStart start : chunk.getStructureStarts().values()) {
            Structure structure = start.getStructure();
            Identifier id = registry.getId(structure);
            if (id != null && TARGET_IDS.contains(id)) {
                return true;
            }
            RegistryEntry<Structure> entry = registry.getEntry(structure);
            if (entry != null) {
                for (TagKey<Structure> tag : TARGET_TAGS) {
                    if (entry.isIn(tag)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

