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
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.world.gen.structure.Structure;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Улучшенный локатор структур.
 * <p>
 * Изменения по сравнению с предыдущей ревизией:
 * <ul>
 *   <li>Вся сетка <strong>обрабатывается одним</strong> асинхронным заданием вместо тысяч мелких тасков –
 *   это снижает накладные расходы планировщика и упрощает ожидание завершения.</li>
 *   <li>По‑прежнему выполняется <em>вне тика сервера</em> (через {@link AsyncExecutor}), а результаты
 *   сохраняются на главном потоке через {@code world.getServer().execute(...)}.</li>
 *   <li>Продолжает переиспользовать {@link Mutable} для минимизации выделений памяти.</li>
 * </ul>
 */
public final class StructureLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + StructureLocator.class.getSimpleName());

    private static final DynamicCommandExceptionType INVALID_STRUCTURE_EXCEPTION = new DynamicCommandExceptionType(id -> Text.translatable("commands.locate.structure.invalid", id));

    private StructureLocator() {
    }

    /* ───────────────────────────── Public API ───────────────────────────── */

    /**
     * Асинхронно сканирует область квадратичной сеткой.
     * <p>
     * Вернёт {@link CompletableFuture}, который завершится, когда скан и последующая
     * сериализация узлов будут полностью обработаны.
     *
     * @param overallRadius радиус (в чанках) всей области поиска
     * @param scanRadius    радиус (в чанках) одного locate-запроса
     */
    public static List<Pair<BlockPos, String>> scanGridAsync(ServerWorld world, BlockPos origin, int overallRadius, int scanRadius, List<String> structureSelectors) {
        List<Pair<BlockPos, String>> list = performGridScan(world, origin, overallRadius, scanRadius, structureSelectors);
        schedulePersistence(world, list);
        return list;
    }

    /* ───────────────────────────── Internal logic ───────────────────────────── */

    private static List<Pair<BlockPos, String>> performGridScan(ServerWorld world, BlockPos origin, int overallRadius, int scanRadius, List<String> structureSelectors) {
        int step = scanRadius * 2 + 1;
        int originChunkX = origin.getX() >> 4;
        int originChunkZ = origin.getZ() >> 4;

        Set<BlockPos> seen = ConcurrentHashMap.newKeySet();
        List<Pair<BlockPos, String>> allFound = Collections.synchronizedList(new ArrayList<>());

        for (int dx = -overallRadius; dx <= overallRadius; dx += step) {
            for (int dz = -overallRadius; dz <= overallRadius; dz += step) {
                int chunkX = originChunkX + dx;
                int chunkZ = originChunkZ + dz;
                scanCell(world, origin, chunkX, chunkZ, scanRadius, structureSelectors, seen, allFound);
            }
        }
        return allFound;
    }

    private static void scanCell(ServerWorld world, BlockPos origin, int chunkX, int chunkZ, int scanRadius, List<String> structureSelectors, Set<BlockPos> seen, List<Pair<BlockPos, String>> out) {
        Mutable scanOrigin = new Mutable((chunkX << 4) + 8, origin.getY(), (chunkZ << 4) + 8);
        List<Pair<BlockPos, String>> local = findStructures(world, scanOrigin.toImmutable(), scanRadius, structureSelectors);

        Mutable tmp = new Mutable();
        for (Pair<BlockPos, String> pair : local) {
            BlockPos p = pair.getFirst();
            int y = CacheManager.getHeight(world, p.getX(), p.getZ());
            tmp.set(p.getX(), y, p.getZ());
            BlockPos key = tmp.toImmutable();
            if (seen.add(key)) {
                out.add(Pair.of(key, pair.getSecond()));
            }
        }
    }

    /**
     * Находит ближайшую структуру для каждого селектора вокруг заданной точки.
     */
    private static List<Pair<BlockPos, String>> findStructures(ServerWorld world, BlockPos origin, int radius, List<String> structureSelectors) {
        List<Pair<BlockPos, String>> foundPositions = new ArrayList<>();
        Registry<Structure> registry = world.getRegistryManager().getOrThrow(RegistryKeys.STRUCTURE);

        for (String selector : structureSelectors) {
            try {
                RegistryPredicateArgumentType<Structure> argType = new RegistryPredicateArgumentType<>(RegistryKeys.STRUCTURE);
                RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate = argType.parse(new StringReader(selector));

                RegistryEntryList<Structure> structList = getStructureList(predicate, registry).orElseThrow(() -> INVALID_STRUCTURE_EXCEPTION.create(selector));

                Pair<BlockPos, RegistryEntry<Structure>> located = world.getChunkManager().getChunkGenerator().locateStructure(world, structList, origin, radius, true);

                if (located != null) {
                    Identifier id = registry.getId(located.getSecond().value());
                    foundPositions.add(Pair.of(located.getFirst(), String.valueOf(id)));
                }
            } catch (CommandSyntaxException ex) {
                LOGGER.error("Selector '{}' failed: {}", selector, ex.getMessage());
            }
        }
        return foundPositions;
    }

    /**
     * Сохраняем найденные узлы и рёбра на главном потоке.
     */
    private static void schedulePersistence(ServerWorld world, List<Pair<BlockPos, String>> found) {
        RoadGraphState graph = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
        for (Pair<BlockPos, String> pair : found) {
            Node node = graph.addNodeWithEdges(pair.getFirst(), pair.getSecond());
            LOGGER.debug("Added node {} at {}", node.id(), node.pos());
        }
        graph.markDirty();
    }

    private static Optional<? extends RegistryEntryList<Structure>> getStructureList(RegistryPredicateArgumentType.RegistryPredicate<Structure> predicate, Registry<Structure> registry) {
        return predicate.getKey().map(
                key -> registry.getOptionalValue(key).map(registry::getEntry).map(RegistryEntryList::of),
                tag -> registry.streamTags()
                        .filter(named -> named.getTag().equals(tag))
                        .findFirst()
                        .map(named -> (RegistryEntryList<Structure>) named)
        );
    }
}
