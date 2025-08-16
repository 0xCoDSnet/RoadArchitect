package net.oxcodsnet.roadarchitect.neoforge.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;
import net.oxcodsnet.roadarchitect.config.RoadArchitectConfigData;
import net.oxcodsnet.roadarchitect.handlers.RoadPipelineController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridges Cloth Config with the common {@link RAConfigHolder} on NeoForge.
 */
public final class RAConfigNeoForgeBridge {
    private static final Logger LOG = LoggerFactory.getLogger("RoadArchitect/ConfigBridge");
    private static ConfigHolder<RoadArchitectConfigData> holder;

    private RAConfigNeoForgeBridge() {}

    public static void bootstrap() {
        holder = AutoConfig.register(RoadArchitectConfigData.class, GsonConfigSerializer::new);
        RAConfigHolder.set(new RAConfig() {
            @Override
            public int initScanRadius() {
                return holder.getConfig().initScanRadius;
            }

            @Override
            public int chunkGenerateScanRadius() {
                return holder.getConfig().chunkGenerateScanRadius;
            }

            @Override
            public int maxConnectionDistance() {
                return holder.getConfig().maxConnectionDistance;
            }

            @Override
            public int pipelineIntervalSeconds() {
                return holder.getConfig().pipelineIntervalSeconds;
            }

            @Override
            public java.util.List<String> structureSelectors() {
                return holder.getConfig().structureSelectors;
            }
        });
        RoadPipelineController.refreshStructureSelectorCache();
        LOG.info("[RoadArchitect] cloth-config bridge initialized");
    }
}
