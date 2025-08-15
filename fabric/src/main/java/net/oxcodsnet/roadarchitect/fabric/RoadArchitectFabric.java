package net.oxcodsnet.roadarchitect.fabric;

import io.wispforest.owo.command.EnumArgumentType;
import net.fabricmc.api.ModInitializer;
//import net.oxcodsnet.roadarchitect.config.RoadArchitectConfig;
import net.oxcodsnet.roadarchitect.handlers.RoadGraphStateManager;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import net.oxcodsnet.roadarchitect.handlers.RoadPostProcessor;
import net.oxcodsnet.roadarchitect.util.CacheManager;
//import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс мода Road Architect.
 * <p>Main class of the Road Architect mod.</p>
 */
public class RoadArchitectFabric implements ModInitializer {
//    public static final String MOD_ID = "roadarchitect";
//    public static final RoadArchitectConfig CONFIG = RoadArchitectConfig.createAndLoad();
//    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "/Init");
//
//    /**
//     * Точка входа Fabric. Выполняет инициализацию сервера.
//     * <p>Fabric entry point used to initialise the mod.</p>
//     */
//    @Override
//    public void onInitialize() {
//
//        try {
//            EnumArgumentType.create(org.slf4j.event.Level.class);
//            LOGGER.debug("Registered OWO enum argument for SLF4J Level");
//        } catch (IllegalStateException already) {
//            // если кто-то уже зарегистрировал — это ок, просто логируем по-тихому
//            LOGGER.debug("OWO enum argument for SLF4J Level already registered");
//        } catch (Throwable t) {
//            LOGGER.warn("Failed to register OWO enum argument for SLF4J Level", t);
//        }
//
//        LOGGER.info("Road Architect initialization...");
//        // Регистрация хранилищ и пайплайна
//        RoadGraphStateManager.register();
//        RoadPipelineController.register();
//        RoadPostProcessor.register();
//        CacheManager.register();
//        RoadFeatureRegistry.register();
//        LOGGER.info("Road Architect initialization complete!");
//    }

    @Override
    public void onInitialize() {
        net.oxcodsnet.roadarchitect.fabric.config.RAConfigFabricBridge.bootstrap();
    }
}
