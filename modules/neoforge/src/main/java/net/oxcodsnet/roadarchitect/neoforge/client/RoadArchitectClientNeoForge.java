package net.oxcodsnet.roadarchitect.neoforge.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.neoforge.client.gui.LoadingBarOverlay;

@EventBusSubscriber(modid = RoadArchitect.MOD_ID, value = Dist.CLIENT)
public final class RoadArchitectClientNeoForge {
    private RoadArchitectClientNeoForge() {}

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        LoadingBarOverlay.onRender(e);
    }
}
