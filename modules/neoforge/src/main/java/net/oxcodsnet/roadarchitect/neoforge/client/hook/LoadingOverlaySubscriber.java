package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import net.oxcodsnet.roadarchitect.client.gui.LoadingOverlayRenderer;

@EventBusSubscriber(modid = "roadarchitect", value = Dist.CLIENT)
public final class LoadingOverlaySubscriber {
    private LoadingOverlaySubscriber() {}

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post e) {
        if (!(e.getScreen() instanceof LevelLoadingScreen)) return;

        var mc = MinecraftClient.getInstance();
        var ctx = e.getGuiGraphics(); // в Yarn это DrawContext
        LoadingOverlayRenderer.render(
                ctx,
                mc.getWindow().getScaledWidth(),
                mc.getWindow().getScaledHeight()
        );
    }
}
