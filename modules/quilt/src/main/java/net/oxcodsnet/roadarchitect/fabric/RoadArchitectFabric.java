package net.oxcodsnet.roadarchitect.fabric;

import io.wispforest.owo.command.EnumArgumentType;
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
        // =============== Для синхранизации между сервером и клиентом ===============
        try {
            EnumArgumentType.create(org.slf4j.event.Level.class);
            LOGGER.debug("Registered OWO enum argument for SLF4J Level");
        } catch (IllegalStateException already) {
            // если кто-то уже зарегистрировал — это ок, просто логируем по-тихому
            LOGGER.debug("OWO enum argument for SLF4J Level already registered");
        } catch (Throwable t) {
            LOGGER.warn("Failed to register OWO enum argument for SLF4J Level", t);
        }

        // 1) Поднять мост конфига (owo → common)
        RAConfigFabricBridge.bootstrap();
        RoadFeatureRegistryFabric.register();
        // 2) Зарегистрировать события Fabric → хуки common
        FabricEventBridge.register();
        RoadGraphStateFabricEvents.register();
        RoadPipelineFabricEvents.register();
    }
}
