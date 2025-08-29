package net.oxcodsnet.roadarchitect.fabric;

import net.fabricmc.api.ModInitializer;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.fabric.config.RAConfigQuiltBridge;
import net.oxcodsnet.roadarchitect.fabric.events.QuiltEventBridge;
import net.oxcodsnet.roadarchitect.fabric.events.RoadFeatureRegistryQuilt;
import net.oxcodsnet.roadarchitect.fabric.events.RoadGraphStateQuiltEvents;
import net.oxcodsnet.roadarchitect.fabric.events.RoadPipelineQuiltEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RoadArchitectQuilt implements ModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID);

    @Override
    public void onInitialize() {
        // 1) Поднять мост конфига (Cloth Config → common)
        RAConfigQuiltBridge.bootstrap();
        RoadArchitect.init();
        RoadFeatureRegistryQuilt.register();
        // 2) Зарегистрировать события Fabric → хуки common
        QuiltEventBridge.register();
        RoadGraphStateQuiltEvents.register();
        RoadPipelineQuiltEvents.register();
    }
}
