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

    public RoadLanguageProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registries, String code) {
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
                break;
            case "ru_ru":
                builder.add("key.roadarchitect.debug", "Отладка графа дорог");
                builder.add("category.roadarchitect", "Архитектор дорог");
                builder.add("roadarchitect.stage.intialisation", "Инициализация");
                builder.add("roadarchitect.stage.scanning", "Сканирование структур");
                builder.add("roadarchitect.stage.pathfinding", "Поиск пути");
                builder.add("roadarchitect.stage.postprocess", "Постобработка");
                builder.add("roadarchitect.stage.complete", "Завершено");
                break;
            case "es_es":
                builder.add("key.roadarchitect.debug", "Depuración del grafo de carreteras");
                builder.add("category.roadarchitect", "Arquitecto de Carreteras");
                builder.add("roadarchitect.stage.intialisation", "Inicialización");
                builder.add("roadarchitect.stage.scanning", "Escaneando estructuras");
                builder.add("roadarchitect.stage.pathfinding", "Búsqueda de rutas");
                builder.add("roadarchitect.stage.postprocess", "Postprocesamiento");
                builder.add("roadarchitect.stage.complete", "Completado");
                break;
            case "fr_fr":
                builder.add("key.roadarchitect.debug", "Débogage du graphe routier");
                builder.add("category.roadarchitect", "Architecte routier");
                builder.add("roadarchitect.stage.intialisation", "Initialisation");
                builder.add("roadarchitect.stage.scanning", "Analyse des structures");
                builder.add("roadarchitect.stage.pathfinding", "Recherche de chemin");
                builder.add("roadarchitect.stage.postprocess", "Post-traitement");
                builder.add("roadarchitect.stage.complete", "Terminé");
                break;
            case "de_de":
                builder.add("key.roadarchitect.debug", "Straßengraph-Debug");
                builder.add("category.roadarchitect", "Straßenarchitekt");
                builder.add("roadarchitect.stage.intialisation", "Initialisierung");
                builder.add("roadarchitect.stage.scanning", "Strukturen scannen");
                builder.add("roadarchitect.stage.pathfinding", "Wegfindung");
                builder.add("roadarchitect.stage.postprocess", "Nachbearbeitung");
                builder.add("roadarchitect.stage.complete", "Abgeschlossen");
                break;
            case "zh_cn":
                builder.add("key.roadarchitect.debug", "道路网络调试");
                builder.add("category.roadarchitect", "道路架构师");
                builder.add("roadarchitect.stage.intialisation", "初始化");
                builder.add("roadarchitect.stage.scanning", "扫描结构");
                builder.add("roadarchitect.stage.pathfinding", "路径搜索");
                builder.add("roadarchitect.stage.postprocess", "后期处理");
                builder.add("roadarchitect.stage.complete", "完成");
                break;
            default:
                break;
        }
    }
}

