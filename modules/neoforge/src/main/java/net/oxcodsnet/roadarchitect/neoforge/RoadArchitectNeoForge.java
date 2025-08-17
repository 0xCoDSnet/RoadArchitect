package net.oxcodsnet.roadarchitect.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.neoforge.client.RAClientBootstrap;
import net.oxcodsnet.roadarchitect.neoforge.config.RAConfigNeoForgeBridge;
import net.oxcodsnet.roadarchitect.neoforge.events.NeoForgeEventBridge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadFeatureRegistryNeoForge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadGraphStateNeoForgeEvents;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadPipelineNeoForgeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(RoadArchitect.MOD_ID)
public final class RoadArchitectNeoForge {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    public RoadArchitectNeoForge(IEventBus modBus, ModContainer container, Dist dist) {
        modBus.addListener(RoadFeatureRegistryNeoForge::register); // v
        // modBus.addListener(RoadArchitectDataGenerator::gatherData); // Disabled: NeoForge datagen breaks on Yarn mappings

        NeoForgeEventBridge.register(); // v
        RoadGraphStateNeoForgeEvents.register(); // v
        RoadPipelineNeoForgeEvents.register(); // v
        RAConfigNeoForgeBridge.bootstrap();

        // --- client-only ---
        if (dist.isClient()) {
            RAClientBootstrap.init(container);
        }

        RoadArchitect.init();
        LOGGER.info("Initialized Road Architect on NeoForge");
    }
}
