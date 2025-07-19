package net.oxcodsnet.roadarchitect;

import net.fabricmc.api.ModInitializer;
import net.oxcodsnet.roadarchitect.commands.RoadArchitectDebugCommand;
import net.oxcodsnet.roadarchitect.config.RoadArchitectConfig;
import net.oxcodsnet.roadarchitect.handlers.RoadBuilderManager;
import net.oxcodsnet.roadarchitect.handlers.RoadGraphStateManager;
import net.oxcodsnet.roadarchitect.handlers.StructureScanManager;
import net.oxcodsnet.roadarchitect.handlers.PathFinderManager;
import net.oxcodsnet.roadarchitect.worldgen.RoadFeatureRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Главный класс мода Road Architect.
 * <p>Main class of the Road Architect mod.</p>
 */
public class RoadArchitect implements ModInitializer {
    public static final String MOD_ID = "roadarchitect";

    public static final RoadArchitectConfig CONFIG = RoadArchitectConfig.createAndLoad();


    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Точка входа Fabric. Выполняет инициализацию сервера.
     * <p>Fabric entry point used to initialise the mod.</p>
     */
    @Override
    public void onInitialize() {


        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Hello Fabric world!");

        // Регистрация сканирования структур и загрузки состояния
        StructureScanManager.register();
        RoadGraphStateManager.register();
        PathFinderManager.register();
        RoadFeatureRegistry.register();
        RoadBuilderManager.register();
        RoadArchitectDebugCommand.register();
        LOGGER.info("Road Architect initialization complete!");
    }
}
