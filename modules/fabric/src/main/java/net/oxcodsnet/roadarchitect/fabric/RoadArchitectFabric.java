package net.oxcodsnet.roadarchitect.fabric;

import net.fabricmc.api.ModInitializer;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.fabric.config.RAConfigFabricBridge;
import net.oxcodsnet.roadarchitect.fabric.events.FabricEventBridge;
import net.oxcodsnet.roadarchitect.fabric.events.RoadFeatureRegistryFabric;
import net.oxcodsnet.roadarchitect.fabric.events.RoadGraphStateFabricEvents;
import net.oxcodsnet.roadarchitect.fabric.events.RoadPipelineFabricEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoadArchitectFabric implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    @Override
    public void onInitialize() {
        // 1) Поднять мост конфига (Cloth Config → common)
        RAConfigFabricBridge.bootstrap();
        RoadArchitect.init();
        RoadFeatureRegistryFabric.register();
        // 2) Зарегистрировать события Fabric → хуки common
        FabricEventBridge.register();
        RoadGraphStateFabricEvents.register();
        RoadPipelineFabricEvents.register();
    }
}
