package net.oxcodsnet.roadarchitect.config;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import java.util.List;

/**
 * Data model for Road Architect configuration.
 */
@Config(name = "roadarchitect")
public final class RoadArchitectConfigData implements ConfigData {
    @ConfigEntry.Gui.Tooltip
    public int initScanRadius = 125; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int chunkGenerateScanRadius = 20; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int maxConnectionDistance = 715; // numeric field

    @ConfigEntry.Gui.Tooltip
    public int pipelineIntervalSeconds = 120; // numeric field (seconds)

    @ConfigEntry.Gui.Tooltip
    public int lampInterval = 30; // numeric field (blocks)

    @ConfigEntry.Gui.Tooltip
    public int sideDecorationInterval = 12; // numeric field (blocks)

    @ConfigEntry.Gui.Tooltip
    public int buoyInterval = 18; // numeric field (blocks)

    // Small discrete range — keep slider for convenience (0..8)
    @ConfigEntry.BoundedDiscrete(min = 0, max = 8)
    @ConfigEntry.Gui.Tooltip
    public int maskErosion = 1;

    // Boolean toggle (drop-down/toggle, not a slider)
    @ConfigEntry.Gui.Tooltip
    public boolean deterministicDecorations = true;

    @ConfigEntry.Gui.Tooltip
    public List<String> structureSelectors = List.of("#minecraft:village");
}
