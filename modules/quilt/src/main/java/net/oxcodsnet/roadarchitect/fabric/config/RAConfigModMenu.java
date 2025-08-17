package net.oxcodsnet.roadarchitect.fabric.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Registers the config screen with ModMenu.
 */
public final class RAConfigModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return RAConfigQuiltBridge::createScreen;
    }
}
