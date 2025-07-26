package net.oxcodsnet.roadarchitect.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;


/**
 * Настройки мода Road Architect.
 * <p>Configuration model for the Road Architect mod.</p>
 */
@Modmenu(modId = RoadArchitect.MOD_ID)
@Config(name = "roadarchitect", wrapperName = "RoadArchitectConfig")
public class RoadArchitectConfigModel {
    public int initScanRadius = 100;
    public int chunkGenerateScanRadius = 25;
    public int maxConnectionDistance = 715;
    public int pipelineIntervalSeconds = 120;
    public List<String> structureSelectors = List.of(
            "#minecraft:village"
    );
}
