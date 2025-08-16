package net.oxcodsnet.roadarchitect.neoforge.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.oxcodsnet.roadarchitect.handlers.PipelineRunner;

public final class LoadingBarOverlay {
    private LoadingBarOverlay() {}

    // размеры бара
    private static final int BAR_W = 260;
    private static final int BAR_H = 8;
    private static final int GLOW_W = 80;

    // анимация
    private static final int PERIOD_MS = 2400; // полный «проезд» сегмента

    /** Рисуем поверх стандартного LevelLoadingScreen */
    public static void onRender(final ScreenEvent.Render.Post e) {
        final Screen screen = e.getScreen();
        if (!(screen instanceof LevelLoadingScreen)) return; // целимся только в экран загрузки мира (Yarn: LevelLoadingScreen)

        // В NeoForge событии лежит контекст отрисовки.
        // В Yarn это DrawContext (см. список методов fill/fillGradient/drawCenteredTextWithShadow).
        final DrawContext ctx = e.getGuiGraphics();

        final MinecraftClient mc = MinecraftClient.getInstance();
        final int sw = mc.getWindow().getScaledWidth();
        final int sh = mc.getWindow().getScaledHeight();

        final int x = (sw - BAR_W) / 2;
        final int y = sh - 24;

        // ── дорожка
        ctx.fillGradient(x, y, x + BAR_W, y + BAR_H, 0xFF20242A, 0xFF2A2F36);

        // ── обводка (1 px)
        final int bc = 0xFFB0B8C0;
        ctx.drawHorizontalLine(x, x + BAR_W - 1, y, bc);
        ctx.drawHorizontalLine(x, x + BAR_W - 1, y + BAR_H - 1, bc);
        ctx.drawVerticalLine(x, y, y + BAR_H - 1, bc);
        ctx.drawVerticalLine(x + BAR_W - 1, y, y + BAR_H - 1, bc);

        // ── «бегущий» сегмент (индетерминированный)
        final long now = System.currentTimeMillis();
        final float tLin = (now % PERIOD_MS) / (float) PERIOD_MS;
        final float t = tLin * tLin * (3f - 2f * tLin); // smoothstep
        final int path = Math.round(-GLOW_W + t * (BAR_W + GLOW_W));
        final int segL = x + Math.max(0, path);
        final int segR = x + Math.min(BAR_W, path + GLOW_W);
        if (segR > segL) {
            ctx.fillGradient(segL, y + 1, segR, y + BAR_H - 1, 0xFF34C3FF, 0xFF78C8FF);
        }

        // ── подпись текущей стадии, как в твоём Fabric-экране
        // (в оригинале ты обновляешь stageLabel из PipelineRunner.getCurrentStage().label()).
        final Text stage = PipelineRunner.getCurrentStage().label();
        final TextRenderer font = mc.textRenderer;
        ctx.drawCenteredTextWithShadow(font, stage, sw / 2, y - 12, 0xFFFFFFFF);
    }
}
