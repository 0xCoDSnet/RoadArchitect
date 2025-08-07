package net.oxcodsnet.roadarchitect.datagen;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

/**
 * Generates language files for multiple locales.
 */
public class RoadLanguageProvider extends FabricLanguageProvider {
    private final String code;

    public RoadLanguageProvider(
            FabricDataOutput output,
            CompletableFuture<RegistryWrapper.WrapperLookup> registries,
            String code
    ) {
        super(output, code, registries);
        this.code = code;
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup registries, TranslationBuilder builder) {
        switch (this.code) {
            case "en_us":
                builder.add("key.roadarchitect.debug", "Road Graph Debug");
                builder.add("category.roadarchitect", "Road Architect");
                builder.add("roadarchitect.stage.intialisation", "Initialising");
                builder.add("roadarchitect.stage.scanning", "Scanning Structures");
                builder.add("roadarchitect.stage.pathfinding", "Path Finding");
                builder.add("roadarchitect.stage.postprocess", "Post Processing");
                builder.add("roadarchitect.stage.complete", "Complete");
                builder.add("text.config.roadarchitect.option.initScanRadius", "Initial Scan Radius");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radius in blocks to scan for structures when the world is first loaded."
                );
                builder.add("text.config.roadarchitect.option.chunkGenerateScanRadius", "Chunk Generation Scan Radius");
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radius in chunks scanned when new chunks generate."
                );
                builder.add("text.config.roadarchitect.option.maxConnectionDistance", "Max Connection Distance");
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Maximum distance in blocks between two structures to connect them."
                );
                builder.add("text.config.roadarchitect.option.pipelineIntervalSeconds", "Pipeline Interval Seconds");
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Delay in seconds between pipeline runs."
                );
                builder.add("text.config.roadarchitect.option.structureSelectors", "Structure Selectors");
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "List of structure selectors that roads will connect."
                );
                break;
            case "ru_ru":
                builder.add("key.roadarchitect.debug", "Отладка графа дорог");
                builder.add("category.roadarchitect", "Архитектор дорог");
                builder.add("roadarchitect.stage.intialisation", "Инициализация");
                builder.add("roadarchitect.stage.scanning", "Сканирование структур");
                builder.add("roadarchitect.stage.pathfinding", "Поиск пути");
                builder.add("roadarchitect.stage.postprocess", "Постобработка");
                builder.add("roadarchitect.stage.complete", "Завершено");
                builder.add("text.config.roadarchitect.option.initScanRadius", "Начальный радиус сканирования");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Радиус в блоках для поиска структур при первом запуске мира."
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Радиус сканирования при генерации чанков"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Радиус в чанках, который сканируется при генерации новых чанков."
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance",
                        "Максимальная дистанция соединения"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Максимальное расстояние в блоках между двумя структурами для соединения дорогой."
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Интервал конвейера (сек)"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Задержка в секундах между запусками конвейера."
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors",
                        "Селекторы структур"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Список селекторов структур, которые будут соединяться дорогами."
                );
                break;
            case "es_es":
                builder.add("key.roadarchitect.debug", "Depuración del grafo de carreteras");
                builder.add("category.roadarchitect", "Arquitecto de Carreteras");
                builder.add("roadarchitect.stage.intialisation", "Inicialización");
                builder.add("roadarchitect.stage.scanning", "Escaneando estructuras");
                builder.add("roadarchitect.stage.pathfinding", "Búsqueda de rutas");
                builder.add("roadarchitect.stage.postprocess", "Postprocesamiento");
                builder.add("roadarchitect.stage.complete", "Completado");
                builder.add("text.config.roadarchitect.option.initScanRadius", "Radio de exploración inicial");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radio en bloques para buscar estructuras cuando se carga el mundo por primera vez."
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Radio de exploración al generar chunks"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radio en chunks que se examina al generar nuevos chunks."
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance",
                        "Distancia máxima de conexión"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Distancia máxima en bloques entre dos estructuras para conectarlas."
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalo del pipeline (segundos)"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Retraso en segundos entre ejecuciones del pipeline."
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors",
                        "Selectores de estructuras"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Lista de selectores de estructuras que se conectarán con carreteras."
                );
                break;
            case "fr_fr":
                builder.add("key.roadarchitect.debug", "Débogage du graphe routier");
                builder.add("category.roadarchitect", "Architecte routier");
                builder.add("roadarchitect.stage.intialisation", "Initialisation");
                builder.add("roadarchitect.stage.scanning", "Analyse des structures");
                builder.add("roadarchitect.stage.pathfinding", "Recherche de chemin");
                builder.add("roadarchitect.stage.postprocess", "Post-traitement");
                builder.add("roadarchitect.stage.complete", "Terminé");
                builder.add("text.config.roadarchitect.option.initScanRadius", "Rayon de balayage initial");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Rayon en blocs pour rechercher des structures lors du premier chargement du monde."
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Rayon de balayage de génération de chunks"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Rayon en chunks analysé lors de la génération de nouveaux chunks."
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance",
                        "Distance maximale de connexion"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Distance maximale en blocs entre deux structures à relier."
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalle du pipeline (secondes)"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Délai en secondes entre les exécutions du pipeline."
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors",
                        "Sélecteurs de structures"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Liste des sélecteurs de structures que les routes relieront."
                );
                break;
            case "de_de":
                builder.add("key.roadarchitect.debug", "Straßengraph-Debug");
                builder.add("category.roadarchitect", "Straßenarchitekt");
                builder.add("roadarchitect.stage.intialisation", "Initialisierung");
                builder.add("roadarchitect.stage.scanning", "Strukturen scannen");
                builder.add("roadarchitect.stage.pathfinding", "Wegfindung");
                builder.add("roadarchitect.stage.postprocess", "Nachbearbeitung");
                builder.add("roadarchitect.stage.complete", "Abgeschlossen");
                builder.add("text.config.roadarchitect.option.initScanRadius", "Anfänglicher Scanradius");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radius in Blöcken zum Suchen nach Strukturen beim ersten Laden der Welt."
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Scanradius bei Chunk-Generierung"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radius in Chunks, der beim Generieren neuer Chunks durchsucht wird."
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance",
                        "Maximale Verbindungsdistanz"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Maximaler Abstand in Blöcken zwischen zwei Strukturen, die verbunden werden."
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Pipeline-Intervall (Sekunden)"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Verzögerung in Sekunden zwischen Pipeline-Durchläufen."
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors",
                        "Strukturauswahlen"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Liste von Strukturauswahlen, die Straßen verbinden."
                );
                break;
            case "zh_cn":
                builder.add("key.roadarchitect.debug", "道路网络调试");
                builder.add("category.roadarchitect", "道路架构师");
                builder.add("roadarchitect.stage.intialisation", "初始化");
                builder.add("roadarchitect.stage.scanning", "扫描结构");
                builder.add("roadarchitect.stage.pathfinding", "路径搜索");
                builder.add("roadarchitect.stage.postprocess", "后期处理");
                builder.add("roadarchitect.stage.complete", "完成");
                builder.add("text.config.roadarchitect.option.initScanRadius", "初始扫描半径");
                builder.add(
                        "text.config.roadarchitect.option.initScanRadius.tooltip",
                        "在世界首次加载时扫描结构的方块半径。"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "区块生成扫描半径"
                );
                builder.add(
                        "text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "在新生成区块时扫描的区块半径。"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance",
                        "最大连接距离"
                );
                builder.add(
                        "text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "两结构间允许连接的最大方块距离。"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "管线间隔（秒）"
                );
                builder.add(
                        "text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "每次管线运行之间的秒数。"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors",
                        "结构选择器"
                );
                builder.add(
                        "text.config.roadarchitect.option.structureSelectors.tooltip",
                        "将被道路连接的结构选择器列表。"
                );
                break;
            default:
                break;
        }
    }
}

