package net.oxcodsnet.roadarchitect;

import net.fabricmc.api.ModInitializer;

import net.oxcodsnet.roadarchitect.config.RoadArchitectConfig;
import net.oxcodsnet.roadarchitect.handlers.StructureScanManager;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoadArchitect implements ModInitializer {
    public static final String MOD_ID = "roadarchitect";

    public static final RoadArchitectConfig CONFIG = RoadArchitectConfig.createAndLoad();


    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {


        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        LOGGER.info("Hello Fabric world!");

        StructureScanManager.register();
        ServerWorldEvents.LOAD.register((server, world) -> RoadGraphState.get(world, CONFIG.maxConnectionDistance()));
    }
}

