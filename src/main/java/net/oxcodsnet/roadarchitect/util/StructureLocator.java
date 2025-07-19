package net.oxcodsnet.roadarchitect.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.command.argument.RegistryPredicateArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Утилитарный класс для поиска структур по тегам/ID внутри заданного радиуса чанков.
 * Utility class for finding structures by tags/IDs within a chunk radius.
 */
public class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID+"/StructureLocator");
    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.invalid", id)
    );
    private static final DynamicCommandExceptionType STRUCTURE_NOT_FOUND_EXCEPTION = new DynamicCommandExceptionType(
            id -> Text.translatable("commands.locate.structure.not_found", id)
    );

    /**
     * Ищет ближайшие структуры для каждого селектора в радиусе вокруг origin.
     * Finds nearest structures for each selector within the specified radius around origin.
     *
     * @param world              мир сервера / server world
     * @param origin             центральная позиция (координаты блока) / center position (block coordinates)
     * @param radius             радиус поиска в чанках / search radius in chunks
     * @param structureSelectors список селекторов ("minecraft:village" или "#minecraft:village") / list of selectors ("minecraft:village" or "#minecraft:village")
     * @return список найденных позиций структур / list of found structure positions
     */
    public static List<Pair<BlockPos, String>> findStructures(ServerWorld world,
                                                              BlockPos origin,
                                                              int radius,
                                                              List<String> structureSelectors) {
        List<Pair<BlockPos, String>> foundPositions = new ArrayList<>();
        Registry<Structure> structureRegistry = world.getRegistryManager().get(RegistryKeys.STRUCTURE);

        for (String selector : structureSelectors) {
            try {
                // Парсим селектор в предикат / Parse selector into predicate
                RegistryPredicateArgumentType<Structure> argumentType = new RegistryPredicateArgumentType<>(RegistryKeys.STRUCTURE);
                RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate = argumentType.parse(new StringReader(selector));

                // Получаем список структур по предикату / Resolve registry list for predicate
                RegistryEntryList<Structure> structureList = getStructureList(predicate, structureRegistry)
                        .orElseThrow(() -> INVALID_STRUCTURE_EXCEPTION.create(selector));

                // Ищем структуру / Locate structure
                Pair<BlockPos, RegistryEntry<Structure>> located = world.getChunkManager()
                        .getChunkGenerator()
                        .locateStructure(world, structureList, origin, radius, true);

                if (located != null) {
                    BlockPos pos = located.getFirst();
                    Identifier id = structureRegistry.getId(located.getSecond().value());
                    LOGGER.debug("Found structure '{}' at {}", selector, pos);
                    foundPositions.add(Pair.of(pos, String.valueOf(id)));
                } else {
                    LOGGER.debug("No structure '{}' found within radius {} around {}", selector, radius, origin);
                }

            } catch (CommandSyntaxException e) {
                LOGGER.error("Failed to parse or locate structure selector '{}': {}", selector, e.getMessage());
            }
        }

        return foundPositions;
    }

    /**
     * Помощник для получения списка записей структур по предикату.
     * Helper to get a list of structure entries for the given predicate.
     *
     * @param predicate предикат структуры / structure predicate
     * @param registry  реестр структур / structure registry
     * @return Optional списка структур / Optional of structure entry list
     */
    private static Optional<? extends RegistryEntryList.ListBacked<Structure>> getStructureList(
            RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate,
            Registry<Structure> registry) {
        return predicate.getKey()
                .map(key -> registry.getEntry(key).map(RegistryEntryList::of), registry::getEntryList);
    }

    /**
     * Просканировать область вокруг origin сеткой вызовов locateStructure.
     * Scans the area around origin in a grid pattern using locateStructure calls.
     *
     * @param world              мир сервера / server world
     * @param origin             центр сканирования (координаты блока) / scan center (block coordinates)
     * @param overallRadius      общий радиус сканирования в чанках / overall scan radius in chunks
     * @param scanRadius         радиус одного locateStructure вызова в чанках / radius per locateStructure call in chunks
     * @param structureSelectors список селекторов структур / list of structure selectors
     * @return список уникальных найденных позиций структур / unique list of found structure positions
     */
    public static List<Pair<BlockPos, String>> scanGrid(ServerWorld world,
                                                        BlockPos origin,
                                                        int overallRadius,
                                                        int scanRadius,
                                                        List<String> structureSelectors) {
        List<Pair<BlockPos, String>> allFound = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();


        int step = scanRadius * 2 + 1;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        for (int dx = -overallRadius; dx <= overallRadius; dx += step) {
            for (int dz = -overallRadius; dz <= overallRadius; dz += step) {
                int chunkX = originChunkX + dx;
                int chunkZ = originChunkZ + dz;
                BlockPos scanOrigin = new BlockPos((chunkX << 4) + 8, origin.getY(), (chunkZ << 4) + 8);

                List<Pair<BlockPos, String>> found = findStructures(world, scanOrigin, scanRadius, structureSelectors);
                for (Pair<BlockPos, String> pair : found) {
                    BlockPos pos = pair.getFirst();
                    if (seen.add(pos)) {
                        allFound.add(pair);
                        LOGGER.debug("Grid scan found new structure at {}", pos);
                    }
                }
            }
        }
        // Сохранение узлов
        RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());

        for (Pair<BlockPos, String> pair : allFound) {
            Node node = state.addNodeWithEdges(pair.getFirst(), pair.getSecond());
            LOGGER.info("Added node {} on {} ", node.id(), node.pos());
        }
        state.markDirty();
        LOGGER.info("All nodes are preserved in a persistent state.");

        return allFound;
    }
}
