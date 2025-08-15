package net.oxcodsnet.roadarchitect.neoforge;

import net.neoforged.fml.common.Mod;

import net.oxcodsnet.roadarchitect.RoadArchitect;

@Mod(RoadArchitect.MOD_ID)
public final class ExampleModNeoForge {
    public ExampleModNeoForge() {
        // Run our common setup.
        RoadArchitect.init();
    }
}
