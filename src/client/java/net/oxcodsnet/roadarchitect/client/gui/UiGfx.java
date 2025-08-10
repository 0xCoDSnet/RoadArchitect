package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;
import net.minecraft.util.math.ColorHelper;

/**
 * Утилиты безопасного рендера для 1.21.6:
 * - масштабирование текста через 2D-матрицы (Matrix3x2fStack)
 * - аппроксимация окружностей полилинией (без TRIANGLE_FAN/STRIP)
 * - автодобавление непрозрачной альфы для текста (во многих местах раньше передавался 0xFFFFFF)
 */
public final class UiGfx {

    private UiGfx() {}

    /** Если альфа отсутствует (0), делаем цвет полностью непрозрачным. */
    private static int ensureOpaque(int argb) {
        return ColorHelper.getAlpha(argb) == 0 ? ColorHelper.fullAlpha(argb) : argb;
    }

    /** Текст с масштабированием через матрицу. */
    public static void drawTextScaled(OwoUIDrawContext ctx, Text text, float x, float y, float scale, int color) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);      // Matrix3x2fStack: 2D translate
        ctx.getMatrices().scale(scale, scale);  // Matrix3x2fStack: 2D scale
        ctx.drawText(tr, text, 0, 0, ensureOpaque(color), false);
        ctx.getMatrices().popMatrix();
    }

    /** Контур круга как полилиния. */
    public static void drawCircleApprox(OwoUIDrawContext ctx, int cx, int cy, int segments,
                                        double radius, double thickness, Color color) {
        int seg = Math.max(8, segments);
        double step = (Math.PI * 2.0) / seg;
        int px = cx + (int) Math.round(radius);
        int py = cy;

        for (int i = 1; i <= seg; i++) {
            double a = i * step;
            int x = cx + (int) Math.round(Math.cos(a) * radius);
            int y = cy + (int) Math.round(Math.sin(a) * radius);
            ctx.drawLine(px, py, x, y, thickness, color);
            px = x;
            py = y;
        }
    }
}
