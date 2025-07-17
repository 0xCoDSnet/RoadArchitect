package net.oxcodsnet.roadarchitect.config;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.Modmenu;
import net.oxcodsnet.roadarchitect.RoadArchitect;

import java.util.List;

@Modmenu(modId = RoadArchitect.MOD_ID)
@Config(name = "roadarchitect", wrapperName = "RoadArchitectConfig")
public class RoadArchitectConfigModel {
    public int playerScanRadius = 100;
    public int chunkLoadScanRadius = 50;
    public int maxConnectionDistance = 2048;
    public List<String> structureSelectors = List.of(
            "#minecraft:village",
            "minecraft:village_plains"
    );
}
