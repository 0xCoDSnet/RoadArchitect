package net.oxcodsnet.roadarchitect.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.option.KeyBinding;
import net.oxcodsnet.roadarchitect.fabric.client.hook.DebugGraphScreenHook;
import net.oxcodsnet.roadarchitect.fabric.client.hook.LoadingOverlayHook;

/**
 * Клиентская сторона мода.
 * <p>Client side entry point of the mod.</p>
 */
public class RoadArchitectClientQuilt implements ClientModInitializer {

    private static KeyBinding debugKey;

    @Override
    public void onInitializeClient() {
        LoadingOverlayHook.init();
        DebugGraphScreenHook.init();
    }
}
