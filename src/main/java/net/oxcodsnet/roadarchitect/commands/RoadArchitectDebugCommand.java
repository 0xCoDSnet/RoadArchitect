package net.oxcodsnet.roadarchitect.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.PlacedFeature;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureConfig;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Registers a simple debug command to manually invoke the road feature.
 */
public class RoadArchitectDebugCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/DebugCommand");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) -> register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("roadarchitect")
                .requires(src -> src.hasPermissionLevel(2))
                .then(CommandManager.literal("place")
                        .then(CommandManager.argument("width", IntegerArgumentType.integer(1))
                                .executes(ctx -> execute(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "width"))))));
    }

    private static int execute(ServerCommandSource source, int width) {
        ServerWorld world = source.getWorld();
        BlockPos pos = BlockPos.ofFloored(source.getPosition());
        ChunkGenerator generator = world.getChunkManager().getChunkGenerator();
        PlacedFeature feature = new PlacedFeature(
                RegistryEntry.of(new ConfiguredFeature<>(RoadFeatureRegistry.ROAD_FEATURE, new RoadFeatureConfig(width))),
                List.of());
        feature.generateUnregistered(world, generator, world.getRandom(), pos);
        LOGGER.info("Placed debug road feature at {} with width {}", pos, width);
        return 1;
    }
}