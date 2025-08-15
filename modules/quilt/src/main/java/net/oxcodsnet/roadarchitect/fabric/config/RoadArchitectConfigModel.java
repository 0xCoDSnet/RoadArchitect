package net.oxcodsnet.roadarchitect.fabric.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import io.wispforest.owo.config.annotation.Hook; // ← добавили
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;

@Modmenu(modId = RoadArchitect.MOD_ID)
@Config(name = "roadarchitect", wrapperName = "RoadArchitectConfig")
public class RoadArchitectConfigModel {
    @Hook public int initScanRadius = 125;
    @Hook public int chunkGenerateScanRadius = 20;
    @Hook public int maxConnectionDistance = 715;
    @Hook public int pipelineIntervalSeconds = 120;
    @Hook public List<String> structureSelectors = List.of("#minecraft:village");
}
