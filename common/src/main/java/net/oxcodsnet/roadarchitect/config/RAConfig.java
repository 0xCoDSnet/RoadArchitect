package net.oxcodsnet.roadarchitect.config;

import java.util.List;

/**
 * Кроссплатформенный интерфейс конфига.
 * Платформенные слои (Fabric / NeoForge) должны предоставить реализацию
 * и вызвать RAConfigHolder#set(...) при инициализации.
 */
public interface RAConfig {
    int initScanRadius();

    int chunkGenerateScanRadius();

    int maxConnectionDistance();

    int pipelineIntervalSeconds();

    List<String> structureSelectors();
}