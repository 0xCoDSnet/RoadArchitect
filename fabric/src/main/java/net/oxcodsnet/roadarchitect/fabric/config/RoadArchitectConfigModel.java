package net.oxcodsnet.roadarchitect.fabric.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;

/**
 * Настройки мода Road Architect (owo-config, видны в ModMenu).
 */
@Modmenu(modId = RoadArchitect.MOD_ID) // Регистрация экрана в ModMenu через аннотацию
@Config(name = "roadarchitect", wrapperName = "RoadArchitectConfig") // генерит wrapper-класс
public class RoadArchitectConfigModel {
    public int initScanRadius = 125;
    public int chunkGenerateScanRadius = 20;
    public int maxConnectionDistance = 715;
    public int pipelineIntervalSeconds = 120;
    public List<String> structureSelectors = List.of(
            "#minecraft:village"
    );
}
