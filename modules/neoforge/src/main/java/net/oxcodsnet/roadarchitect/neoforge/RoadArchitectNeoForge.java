package net.oxcodsnet.roadarchitect.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.neoforge.datagen.RoadArchitectDataGenerator;
import net.oxcodsnet.roadarchitect.neoforge.events.NeoForgeEventBridge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadFeatureRegistryNeoForge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadGraphStateNeoForgeEvents;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadPipelineNeoForgeEvents;
import net.oxcodsnet.roadarchitect.neoforge.config.RAConfigNeoForgeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoadArchitect.MOD_ID)
public final class RoadArchitectNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    public RoadArchitectNeoForge(IEventBus modBus) {
        modBus.addListener(RoadFeatureRegistryNeoForge::register); // v
        // modBus.addListener(RoadArchitectDataGenerator::gatherData); // Disabled: NeoForge datagen breaks on Yarn mappings

        NeoForgeEventBridge.register(); // v
        RoadGraphStateNeoForgeEvents.register(); // v
        RoadPipelineNeoForgeEvents.register(); // v

        RAConfigNeoForgeBridge.bootstrap();
        RoadArchitect.init();
        LOGGER.info("Initialized Road Architect on NeoForge");
    }
}
