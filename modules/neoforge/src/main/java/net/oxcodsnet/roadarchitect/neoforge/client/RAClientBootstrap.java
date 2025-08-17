package net.oxcodsnet.roadarchitect.neoforge.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.neoforge.config.RAConfigNeoForgeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

public final class RAClientBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RAClientBootstrap.class.getSimpleName());
    public static void init(ModContainer container) {
        LOGGER.info("I'll try to register the configuration screen");
        try {
            Class<?> factoryClass = Class.forName("net.neoforged.neoforge.client.gui.IConfigScreenFactory");
            Object factory = Proxy.newProxyInstance(factoryClass.getClassLoader(), new Class[]{factoryClass}, (proxy, method, args) -> RAConfigNeoForgeBridge.createScreen(args[1]));
            ModLoadingContext.get().registerExtensionPoint((Class) factoryClass, (java.util.function.Supplier) () -> factory);
            LOGGER.info("The configuration screen is successfully registered");
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("The configuration screen is not registered due to error:", e);
        }

    }
}