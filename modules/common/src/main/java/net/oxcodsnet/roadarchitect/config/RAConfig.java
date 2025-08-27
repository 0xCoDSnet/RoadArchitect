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

    int lampInterval();

    /**
     * Interval (in blocks along the path metric) for placing buoys on water segments.
     */
    int buoyInterval();

    /**
     * Interval (in blocks along the path metric) for placing side decorations (fences, shrubs, etc.) on land.
     */
    int sideDecorationInterval();

    /**
     * Erosion in points/blocks for suitability masks near water/land transitions.
     * 0 disables erosion; 1 removes immediate transition points, etc.
     */
    int maskErosion();

    /**
     * Whether to use deterministic, chunk-agnostic placement for decorations.
     */
    boolean deterministicDecorations();

    List<String> structureSelectors();
}
