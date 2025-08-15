package net.oxcodsnet.roadarchitect.fabric.config;

import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Читает owo-config и экспортирует значения в общий RAConfigHolder.
 * Подписывается на изменения нужных опций (эффект "onConfigReloaded").
 */
public final class RAConfigFabricBridge {

    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");

    private static net.oxcodsnet.roadarchitect.fabric.config.RoadArchitectConfig CFG;

    private RAConfigFabricBridge() {}

    public static void bootstrap() {
        // 1) Загружаем wrapper (создаст файл при первом запуске)
        CFG = net.oxcodsnet.roadarchitect.fabric.config.RoadArchitectConfig.createAndLoad();

        // 2) Отдаем common-слою "живой" интерфейс — методы читают напрямую из CFG
        RAConfigHolder.set(new RAConfig() {
            @Override public int initScanRadius()          { return CFG.initScanRadius(); }
            @Override public int chunkGenerateScanRadius() { return CFG.chunkGenerateScanRadius(); }
            @Override public int maxConnectionDistance()   { return CFG.maxConnectionDistance(); }
            @Override public int pipelineIntervalSeconds() { return CFG.pipelineIntervalSeconds(); }
            @Override public List<String> structureSelectors() { return CFG.structureSelectors(); }
        });

        // 3) Подписка на изменения — здесь делаем всё, что в игре надо "пересчитать"
        wireLiveReload();

        LOG.info("[RoadArchitect] owo-config bridge initialized");
    }

    /** Колбэки "на изменение опций" — это и есть наш onConfigReloaded-путь. */
    private static void wireLiveReload() {
        // При смене набора селекторов — обновляем кэш матчеров структур (common)
        CFG.subscribeToStructureSelectors(list -> {
            RoadPipelineController.refreshStructureSelectorCache();
            LOG.debug("structureSelectors changed: {}", list);
        });

        // Ниже — просто логируем (если нужно — добавим действия)
        CFG.subscribeToPipelineIntervalSeconds(v -> LOG.debug("pipelineIntervalSeconds changed: {}", v));
        CFG.subscribeToMaxConnectionDistance(v -> LOG.debug("maxConnectionDistance changed: {}", v));
        CFG.subscribeToInitScanRadius(v -> LOG.debug("initScanRadius changed: {}", v));
        CFG.subscribeToChunkGenerateScanRadius(v -> LOG.debug("chunkGenerateScanRadius changed: {}", v));
    }

    /** Опционально: если когда-то захочешь вручную дергать "перезагрузку" — вызывай это. */
    public static void onConfigReloaded() {
        // Здесь ничего дополнительно не нужно — наши subscribe-колбэки уже стоят.
        // Если вручную перечитываешь файл и wrapper обновляет значения,
        // подписки сработают, а кэши мы обновляем через refreshStructureSelectorCache().
        RoadPipelineController.refreshStructureSelectorCache();
        LOG.info("[RoadArchitect] config reload handled");
    }
}
