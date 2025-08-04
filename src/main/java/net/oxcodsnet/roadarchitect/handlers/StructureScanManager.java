package net.oxcodsnet.roadarchitect.handlers;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.util.StructureLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Менеджер запуска сканирования структур.
 * <p>Manager responsible for initiating structure scans.</p>
 */
public class StructureScanManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    static void scan(ServerWorld world, String approach, BlockPos center) {
        scan(world, approach, center, 1);
    }
    static void scan(ServerWorld world, String approach, BlockPos center, int overallRadius) {
        int scanRadius = 1;
        List<String> selectors = RoadArchitect.CONFIG.structureSelectors();
        LOGGER.info("[{}] Scan launch: overallRadius={}, scanRadius={}, selectors={}", approach, overallRadius, scanRadius, selectors);
        List<Pair<BlockPos, String>> found = StructureLocator.scanGridAsync(world, center, overallRadius, scanRadius, selectors);
        LOGGER.info("[{}] Scanning is completed. Found structures: {}", approach, found.size());
    }
}
