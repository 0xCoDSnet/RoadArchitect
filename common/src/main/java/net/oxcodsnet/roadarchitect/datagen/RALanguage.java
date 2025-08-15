package net.oxcodsnet.roadarchitect.datagen;

import java.util.function.BiConsumer;

/**
 * Все ключи/значения локализаций лежат в common.
 * Платформенный провайдер просто вызывает fill(locale, builder::add).
 */
public final class RALanguage {
    private RALanguage() {
    }

    public static void fill(String code, BiConsumer<String, String> add) {
        switch (code) {
            case "en_us": {
                add.accept("key.roadarchitect.debug", "Road Graph Debug");
                add.accept("category.roadarchitect", "Road Architect");
                add.accept("roadarchitect.stage.intialisation", "Initialising");
                add.accept("roadarchitect.stage.scanning", "Scanning Structures");
                add.accept("roadarchitect.stage.pathfinding", "Path Finding");
                add.accept("roadarchitect.stage.postprocess", "Post Processing");
                add.accept("roadarchitect.stage.complete", "Complete");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Initial Scan Radius");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radius in blocks to scan for structures when the world is first loaded.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius", "Chunk Generation Scan Radius");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radius in chunks scanned when new chunks generate.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance", "Max Connection Distance");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Maximum distance in blocks between two structures to connect them.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds", "Pipeline Interval Seconds");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Delay in seconds between pipeline runs.");
                add.accept("text.config.roadarchitect.option.structureSelectors", "Structure Selectors");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "List of structure selectors that roads will connect.");
                break;
            }
            case "ru_ru": {
                add.accept("key.roadarchitect.debug", "Отладка графа дорог");
                add.accept("category.roadarchitect", "Архитектор дорог");
                add.accept("roadarchitect.stage.intialisation", "Инициализация");
                add.accept("roadarchitect.stage.scanning", "Сканирование структур");
                add.accept("roadarchitect.stage.pathfinding", "Поиск пути");
                add.accept("roadarchitect.stage.postprocess", "Постобработка");
                add.accept("roadarchitect.stage.complete", "Завершено");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Начальный радиус сканирования");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Радиус в блоках для поиска структур при первом запуске мира.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Радиус сканирования при генерации чанков");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Радиус в чанках, который сканируется при генерации новых чанков.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Максимальная дистанция соединения");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Максимальное расстояние в блоках между двумя структурами для соединения дорогой.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Интервал конвейера (сек)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Задержка в секундах между запусками конвейера.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Селекторы структур");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Список селекторов структур, которые будут соединяться дорогами.");
                break;
            }
            case "es_es": {
                add.accept("key.roadarchitect.debug", "Depuración del grafo de carreteras");
                add.accept("category.roadarchitect", "Arquitecto de Carreteras");
                add.accept("roadarchitect.stage.intialisation", "Inicialización");
                add.accept("roadarchitect.stage.scanning", "Escaneando estructuras");
                add.accept("roadarchitect.stage.pathfinding", "Búsqueda de rutas");
                add.accept("roadarchitect.stage.postprocess", "Postprocesamiento");
                add.accept("roadarchitect.stage.complete", "Completado");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Radio de exploración inicial");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radio en bloques para buscar estructuras cuando se carga el mundo por primera vez.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Radio de exploración al generar chunks");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radio en chunks que se examina al generar nuevos chunks.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Distancia máxima de conexión");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Distancia máxima en bloques entre dos estructuras para conectarlas.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalo del pipeline (segundos)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Retraso en segundos entre ejecuciones del pipeline.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Selectores de estructuras");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Lista de selectores de estructuras que se conectarán con carreteras.");
                break;
            }
            case "fr_fr": {
                add.accept("key.roadarchitect.debug", "Débogage du graphe routier");
                add.accept("category.roadarchitect", "Architecte routier");
                add.accept("roadarchitect.stage.intialisation", "Initialisation");
                add.accept("roadarchitect.stage.scanning", "Analyse des structures");
                add.accept("roadarchitect.stage.pathfinding", "Recherche de chemin");
                add.accept("roadarchitect.stage.postprocess", "Post-traitement");
                add.accept("roadarchitect.stage.complete", "Terminé");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Rayon de balayage initial");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Rayon en blocs pour rechercher des structures lors du premier chargement du monde.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Rayon de balayage de génération de chunks");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Rayon en chunks analysé lors de la génération de nouveaux chunks.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Distance maximale de connexion");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Distance maximale en blocs entre deux structures à relier.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Intervalle du pipeline (secondes)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Délai en secondes entre les exécutions du pipeline.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Sélecteurs de structures");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Liste des sélecteurs de structures que les routes relieront.");
                break;
            }
            case "de_de": {
                add.accept("key.roadarchitect.debug", "Straßengraph-Debug");
                add.accept("category.roadarchitect", "Straßenarchitekt");
                add.accept("roadarchitect.stage.intialisation", "Initialisierung");
                add.accept("roadarchitect.stage.scanning", "Strukturen scannen");
                add.accept("roadarchitect.stage.pathfinding", "Wegfindung");
                add.accept("roadarchitect.stage.postprocess", "Nachbearbeitung");
                add.accept("roadarchitect.stage.complete", "Abgeschlossen");
                add.accept("text.config.roadarchitect.option.initScanRadius", "Anfänglicher Scanradius");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "Radius in Blöcken zum Suchen nach Strukturen beim ersten Laden der Welt.");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius",
                        "Scanradius bei Chunk-Generierung");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "Radius in Chunks, der beim Generieren neuer Chunks durchsucht wird.");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance",
                        "Maximale Verbindungsdistanz");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "Maximaler Abstand in Blöcken zwischen zwei Strukturen, die verbunden werden.");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds",
                        "Pipeline-Intervall (Sekunden)");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "Verzögerung in Sekunden zwischen Pipeline-Durchläufen.");
                add.accept("text.config.roadarchitect.option.structureSelectors",
                        "Strukturauswahlen");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "Liste von Strukturauswahlen, die Straßen verbinden.");
                break;
            }
            case "zh_cn": {
                add.accept("key.roadarchitect.debug", "道路网络调试");
                add.accept("category.roadarchitect", "道路架构师");
                add.accept("roadarchitect.stage.intialisation", "初始化");
                add.accept("roadarchitect.stage.scanning", "扫描结构");
                add.accept("roadarchitect.stage.pathfinding", "路径搜索");
                add.accept("roadarchitect.stage.postprocess", "后期处理");
                add.accept("roadarchitect.stage.complete", "完成");
                add.accept("text.config.roadarchitect.option.initScanRadius", "初始扫描半径");
                add.accept("text.config.roadarchitect.option.initScanRadius.tooltip",
                        "在世界首次加载时扫描结构的方块半径。");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius", "区块生成扫描半径");
                add.accept("text.config.roadarchitect.option.chunkGenerateScanRadius.tooltip",
                        "在新生成区块时扫描的区块半径。");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance", "最大连接距离");
                add.accept("text.config.roadarchitect.option.maxConnectionDistance.tooltip",
                        "两结构间允许连接的最大方块距离。");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds", "管线间隔（秒）");
                add.accept("text.config.roadarchitect.option.pipelineIntervalSeconds.tooltip",
                        "每次管线运行之间的秒数。");
                add.accept("text.config.roadarchitect.option.structureSelectors", "结构选择器");
                add.accept("text.config.roadarchitect.option.structureSelectors.tooltip",
                        "将被道路连接的结构选择器列表。");
                break;
            }
            default: {
                break;
            }
        }
    }
}
