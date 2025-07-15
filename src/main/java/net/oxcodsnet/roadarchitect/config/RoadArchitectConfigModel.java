package net.oxcodsnet.roadarchitect.config;

import io.wispforest.owo.config.annotation.Config;

@Config(name = "roadarchitect", wrapperName = "RoadArchitectConfig")
public class RoadArchitectConfigModel {
    public int playerScanRadius = 100;
    public int chunkLoadScanRadius = 50;
}
