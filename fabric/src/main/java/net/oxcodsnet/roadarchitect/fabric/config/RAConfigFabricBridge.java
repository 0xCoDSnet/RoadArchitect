package net.oxcodsnet.roadarchitect.fabric.config;

import io.wispforest.owo.config.ConfigWrapper;
import net.oxcodsnet.roadarchitect.fabric.RoadArchitectFabric;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.fabric.config.RoadArchitectConfig;

import java.util.List;

/**
 * Бридж: читаем owo-config и отдаём значения в общий RAConfigHolder (common).
 * Важно: никаких прямых ссылок на owo в common.
 */
public final class RAConfigFabricBridge {

    private RAConfigFabricBridge() {}

    public static void bootstrap() {
        // создаём/поднимаем обёртку, чтобы при первом обращении файл конфигурации был готов
        RoadArchitectConfig cfg = RoadArchitectConfig.createAndLoad();

        // наполняем общий holder (далее common код обращается к RoadArchitect.CONFIG)
        RAConfigHolder.set(new RAConfig() {
            @Override public int initScanRadius()          { return cfg.initScanRadius(); }
            @Override public int chunkGenerateScanRadius() { return cfg.chunkGenerateScanRadius(); }
            @Override public int maxConnectionDistance()   { return cfg.maxConnectionDistance(); }
            @Override public int pipelineIntervalSeconds() { return cfg.pipelineIntervalSeconds(); }
            @Override public List<String> structureSelectors() { return cfg.structureSelectors(); }
        });

        // если конфиг перезагружают в рантайме, перекидываем новые значения в common
        ((ConfigWrapper<?>) cfg).load(newCfg -> {
            RoadArchitectConfig updated = (RoadArchitectConfig) newCfg;
            RAConfigHolder.set(new RAConfig() {
                @Override public int initScanRadius()          { return updated.initScanRadius(); }
                @Override public int chunkGenerateScanRadius() { return updated.chunkGenerateScanRadius(); }
                @Override public int maxConnectionDistance()   { return updated.maxConnectionDistance(); }
                @Override public int pipelineIntervalSeconds() { return updated.pipelineIntervalSeconds(); }
                @Override public List<String> structureSelectors() { return updated.structureSelectors(); }
            });
            RoadArchitectFabric.LOGGER.info("[RoadArchitect] OWO config reloaded");
        });

        RoadArchitectFabric.LOGGER.info("[RoadArchitect] OWO config bridge initialized");
    }
}
