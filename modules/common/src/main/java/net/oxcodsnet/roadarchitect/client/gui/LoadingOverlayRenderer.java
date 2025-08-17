package net.oxcodsnet.roadarchitect.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

// Если у тебя PipelineRunner в common — удобно подтягивать прямо отсюда.
// Иначе пробрасывай Text stage параметром из «клея».
import net.oxcodsnet.roadarchitect.handlers.PipelineRunner;

public final class LoadingOverlayRenderer {
    private LoadingOverlayRenderer() {}

    // размеры и анимация
    private static final int BAR_W = 260, BAR_H = 8, GLOW_W = 80, PERIOD_MS = 2400;

    /** Отрисовать прогресс-бар и подпись стадии внизу экрана */
    public static void render(DrawContext ctx, int screenWidth, int screenHeight) {
        Text stage = resolveStageLabel();
        render(ctx, screenWidth, screenHeight, stage);
    }

    /** Перегрузка, если хочешь подставлять свой текст стадии извне */
    public static void render(DrawContext ctx, int screenWidth, int screenHeight, Text stage) {
        final int x = (screenWidth - BAR_W) / 2;
        final int y = screenHeight - 24;

        // трек
        ctx.fill(x, y, x + BAR_W, y + BAR_H, 0xFF20242A);
        ctx.drawBorder(x, y, BAR_W, BAR_H, 0xFFB0B8C0); // тонкая рамка (в DrawContext есть drawBorder) :contentReference[oaicite:0]{index=0}

        // "бегущий" сегмент (индетерминированный)
        long now = System.currentTimeMillis();
        float tLin = (now % PERIOD_MS) / (float) PERIOD_MS;
        float t = tLin * tLin * (3f - 2f * tLin); // smoothstep
        int path = Math.round(-GLOW_W + t * (BAR_W + GLOW_W));
        int segL = x + Math.max(0, path);
        int segR = x + Math.min(BAR_W, path + GLOW_W);
        if (segR > segL) {
            // можно делать и градиентом (есть fillGradient), но для надёжности достаточно сплошного "glow"
            ctx.fill(segL, y + 1, segR, y + BAR_H - 1, 0xFF54C8FF);
        }

        // подпись стадии по центру
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer font = mc.textRenderer;
        int textX = (screenWidth - font.getWidth(stage)) / 2;
        ctx.drawText(font, stage, textX, y - 12, 0xFFFFFFFF, true);
    }

    private static Text resolveStageLabel() {
        try {
            var stage = PipelineRunner.getCurrentStage();
            if (stage != null) return stage.label();
        } catch (Throwable ignored) {}
        return Text.literal("Loading…");
    }
}
