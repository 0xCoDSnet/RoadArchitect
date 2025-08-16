package net.oxcodsnet.roadarchitect.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.neoforge.config.RAConfigNeoForgeBridge;
import net.oxcodsnet.roadarchitect.neoforge.events.NeoForgeEventBridge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadFeatureRegistryNeoForge;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadGraphStateNeoForgeEvents;
import net.oxcodsnet.roadarchitect.neoforge.events.RoadPipelineNeoForgeEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

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
        try {
            Class<?> factoryClass = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Object factory = Proxy.newProxyInstance(factoryClass.getClassLoader(), new Class[]{factoryClass},
                    (proxy, method, args) -> RAConfigNeoForgeBridge.createScreen(args[1]));
            ModLoadingContext.get().registerExtensionPoint((Class) factoryClass,
                    (java.util.function.Supplier) () -> factory);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("Failed to register config screen", e);
        }
        RoadArchitect.init();
        LOGGER.info("Initialized Road Architect on NeoForge");
    }
}
