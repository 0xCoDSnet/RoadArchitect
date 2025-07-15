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
    /**
     * Structure selectors used by {@code StructureLocator} to limit what
     * structures are discovered. Entries may either be a structure ID or a tag
     * prefixed with {@code '#'}.
     */
    public List<String> structureSelectors = List.of(
            "#minecraft:village",
            "minecraft:village_plains"
    );
}
