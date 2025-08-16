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
    @ConfigEntry.BoundedDiscrete(min = 0, max = 1000)
    @ConfigEntry.Gui.Tooltip
    public int initScanRadius = 125;

    @ConfigEntry.BoundedDiscrete(min = 0, max = 512)
    @ConfigEntry.Gui.Tooltip
    public int chunkGenerateScanRadius = 20;

    @ConfigEntry.BoundedDiscrete(min = 0, max = 2048)
    @ConfigEntry.Gui.Tooltip
    public int maxConnectionDistance = 715;

    @ConfigEntry.BoundedDiscrete(min = 1, max = 3600)
    @ConfigEntry.Gui.Tooltip
    public int pipelineIntervalSeconds = 120;

    @ConfigEntry.Gui.Tooltip
    public List<String> structureSelectors = List.of("#minecraft:village");
}
