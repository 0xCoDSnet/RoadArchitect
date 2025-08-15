package net.oxcodsnet.roadarchitect.fabric.config;

import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.fabric.config.RoadArchitectConfig;

import java.util.List;

/**
 * Поднимает owo-конфиг и маппит его на общий RAConfig.
 * Вызвать один раз из Fabric-инициализатора.
 */
public final class RAConfigFabricBridge {

    private static RoadArchitectConfig OWOCFG;

    /** Вызвать из RoadArchitectFabric#onInitialize(). */
    public static void bootstrap() {
        // Создаёт/загружает файл конфига и обёртку (метод генерится owo-аннотациями)
        OWOCFG = RoadArchitectConfig.createAndLoad(); // см. owo docs
        // Привязка к общему API (значения читаются «на лету» из обёртки)
        RAConfigHolder.set(new RAConfig() {
            @Override public int initScanRadius() { return OWOCFG.initScanRadius(); }
            @Override public int chunkGenerateScanRadius() { return OWOCFG.chunkGenerateScanRadius(); }
            @Override public int maxConnectionDistance() { return OWOCFG.maxConnectionDistance(); }
            @Override public int pipelineIntervalSeconds() { return OWOCFG.pipelineIntervalSeconds(); }
            @Override public List<String> structureSelectors() { return OWOCFG.structureSelectors(); }
        });
    }

    private RAConfigFabricBridge() {}
}
