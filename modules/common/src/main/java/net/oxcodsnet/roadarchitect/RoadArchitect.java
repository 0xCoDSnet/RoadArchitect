package net.oxcodsnet.roadarchitect;

import net.oxcodsnet.roadarchitect.config.RAConfig;
import net.oxcodsnet.roadarchitect.config.RAConfigHolder;

public final class RoadArchitect {
    public static final String MOD_ID = "roadarchitect";

    // Фасад, делегирующий в актуальный провайдер из Holder
    public static final RAConfig CONFIG = new RAConfig() {
        @Override
        public int initScanRadius() {
            return RAConfigHolder.get().initScanRadius();
        }

        @Override
        public int chunkGenerateScanRadius() {
            return RAConfigHolder.get().chunkGenerateScanRadius();
        }

        @Override
        public int maxConnectionDistance() {
            return RAConfigHolder.get().maxConnectionDistance();
        }

        @Override
        public int pipelineIntervalSeconds() {
            return RAConfigHolder.get().pipelineIntervalSeconds();
        }

        @Override
        public int lampInterval() {
            return RAConfigHolder.get().lampInterval();
        }

        @Override
        public int buoyInterval() {
            return RAConfigHolder.get().buoyInterval();
        }

        @Override
        public int sideDecorationInterval() {
            return RAConfigHolder.get().sideDecorationInterval();
        }

        @Override
        public int maskErosion() {
            return RAConfigHolder.get().maskErosion();
        }

        @Override
        public boolean deterministicDecorations() {
            return RAConfigHolder.get().deterministicDecorations();
        }


        @Override
        public java.util.List<String> structureSelectors() {
            return RAConfigHolder.get().structureSelectors();
        }
    };

    private RoadArchitect() {
    }

    public static void init() {
        // Common init: bootstrap built-in addons
        net.oxcodsnet.roadarchitect.api.addon.RoadAddons.initBuiltins();
    }
}
