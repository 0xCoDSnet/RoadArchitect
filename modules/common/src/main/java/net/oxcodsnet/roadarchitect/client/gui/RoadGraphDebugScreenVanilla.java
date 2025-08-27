package net.oxcodsnet.roadarchitect.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.*;
import java.util.function.Supplier;

/**
 * Ванильный экран отладки графа (без owo-lib).
 * Поддерживает: сетка, пан/зум, легенда, тултипы, клик для телепорта (в одиночке).
 */
public class RoadGraphDebugScreenVanilla extends Screen {

    private static final int RADIUS = 4;
    private static final int PADDING = 20;
    private static final int TARGET_GRID_PX = 80;

    private final List<Node> nodes;
    private final Collection<EdgeStorage.Edge> edges;
    private final Supplier<Text> titleSupplier;

    private final Map<String, ScreenPos> screenPositions = new HashMap<>();
    private final Map<String, Integer> typeColors = new HashMap<>();
    private final Map<EdgeStorage.Status, Integer> statusColors = Map.of(
            EdgeStorage.Status.NEW,     0xFFF2C94C,
            EdgeStorage.Status.SUCCESS, 0xFF27AE60,
            EdgeStorage.Status.FAILURE, 0xFFAE162B
    );

    private boolean dragging = false;
    private boolean firstLayout = true;
    private double zoom = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private double baseScale = 1.0;
    private int minX, maxX, minZ, maxZ;

    public RoadGraphDebugScreenVanilla(List<Node> nodes, Collection<EdgeStorage.Edge> edges) {
        this(nodes, edges, () -> Text.literal("Road Graph Debug"));
    }

    public RoadGraphDebugScreenVanilla(List<Node> nodes, Collection<EdgeStorage.Edge> edges, Supplier<Text> titleSupplier) {
        super(titleSupplier.get());
        this.nodes = nodes;
        this.edges = edges;
        this.titleSupplier = titleSupplier;

        if (!nodes.isEmpty()) {
            minX = nodes.stream().mapToInt(n -> n.pos().getX()).min().orElse(0);
            maxX = nodes.stream().mapToInt(n -> n.pos().getX()).max().orElse(0);
            minZ = nodes.stream().mapToInt(n -> n.pos().getZ()).min().orElse(0);
            maxZ = nodes.stream().mapToInt(n -> n.pos().getZ()).max().orElse(0);
        }
        for (Node node : nodes) {
            typeColors.computeIfAbsent(node.type(), t -> hsvToArgb(Math.abs(t.hashCode() % 360), 0.6f, 0.9f));
        }
    }

    // ---------- жизненный цикл ----------

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta); // затемнение фона
        computeLayout();

        // панель + рамка
        ctx.fill(PADDING, PADDING, width - PADDING, height - PADDING, 0xA0101010);
        ctx.drawBorder(PADDING, PADDING, width - 2 * PADDING, height - 2 * PADDING, 0xFFFFFFFF);

        drawGrid(ctx);

        // рёбра
        for (EdgeStorage.Edge e : edges) {
            ScreenPos a = screenPositions.get(e.nodeA());
            ScreenPos b = screenPositions.get(e.nodeB());
            if (a == null || b == null) continue;
            int col = statusColors.getOrDefault(e.status(), 0xFFFFFFFF);
            drawLine(ctx, a.x, a.y, b.x, b.y, col);
        }

        // узлы + тултип
        Node hovered = null;
        for (Node n : nodes) {
            ScreenPos p = screenPositions.get(n.id());
            if (p == null) continue;
            int fill = typeColors.getOrDefault(n.type(), 0xFFFFFFFF);
            fillCircle(ctx, p.x, p.y, RADIUS, fill);
            drawCircleOutline(ctx, p.x, p.y, RADIUS, 0xFF000000);

            if (dist2(p.x, p.y, mouseX, mouseY) <= RADIUS * RADIUS) hovered = n;
        }
        if (hovered != null) {
            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            ctx.drawTooltip(font, Text.literal(hovered.pos().toShortString() + " • " + hovered.type()), mouseX, mouseY);
        }

        drawPlayerMarker(ctx);

        // линейка масштаба + легенда
        drawScale(ctx);
        drawLegend(ctx);

        // заголовок
        drawCenteredTitle(ctx);

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override public boolean shouldCloseOnEsc() { return true; }

    // ---------- ввод ----------

//    @Override
//    protected void applyBlur(float delta) {
//        // Отключаем блюр для этого экрана
//    }
    @Override
    protected void applyBlur(){

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // клик по узлу = телепорт (только одиночка, на сервер — TODO: отправить пакет)
        Node clicked = findClickedNode(mouseX, mouseY);
        if (clicked != null) {
            teleportTo(clicked);
            return true;
        }
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            offsetX += deltaX;
            offsetY += deltaY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) { dragging = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double old = zoom;
        zoom = vertical > 0 ? zoom * 1.1 : zoom / 1.1;
        offsetX = (offsetX - mouseX + PADDING) * (zoom / old) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (zoom / old) + mouseY - PADDING;
        return true;
    }

    // ---------- отрисовка частей ----------

    private void drawCenteredTitle(DrawContext ctx) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        Text t = titleSupplier.get();
        int tw = font.getWidth(t);
        ctx.drawText(font, t, (width - tw) / 2, PADDING - 12, 0xFFFFFFFF, true);
    }

    private void drawGrid(DrawContext ctx) {
        int w = width - PADDING * 2, h = height - PADDING * 2;

        double worldX0 = minX + (-offsetX) / (baseScale * zoom);
        double worldZ0 = minZ + (-offsetY) / (baseScale * zoom);
        double worldX1 = minX + (w - offsetX) / (baseScale * zoom);
        double worldZ1 = minZ + (h - offsetY) / (baseScale * zoom);

        int spacing = computeGridSpacing();

        int startWX = (int)Math.floor(worldX0 / spacing) * spacing;
        int startWZ = (int)Math.floor(worldZ0 / spacing) * spacing;

        for (int x = startWX; x <= worldX1; x += spacing) {
            int sx = PADDING + (int)((x - worldX0) * baseScale * zoom);
            fillV(ctx, sx, PADDING, PADDING + h, 0x60444444);
            drawSmallLabel(ctx, String.valueOf(x), sx + 2, PADDING + 2);
        }
        for (int z = startWZ; z <= worldZ1; z += spacing) {
            int sz = PADDING + (int)((z - worldZ0) * baseScale * zoom);
            fillH(ctx, PADDING, PADDING + w, sz, 0x60444444);
            drawSmallLabel(ctx, String.valueOf(z), PADDING + 2, sz + 2);
        }
    }

    private void drawScale(DrawContext ctx) {
        int spacing = computeGridSpacing();
        int lengthPx = (int)(spacing * baseScale * zoom);
        int x = width - PADDING - lengthPx - 10;
        int y = height - PADDING - 8;

        fillH(ctx, x, x + lengthPx, y, 0xFFFFFFFF);
        fillV(ctx, x, y - 3, y + 3, 0xFFFFFFFF);
        fillV(ctx, x + lengthPx, y - 3, y + 3, 0xFFFFFFFF);
        drawSmallLabel(ctx, spacing + "m", x, y - 10);
    }

    private void drawLegend(DrawContext ctx) {
        int x = PADDING;
        int y = height - PADDING - typeColors.size() * 12;
        for (Map.Entry<String, Integer> e : typeColors.entrySet()) {
            ctx.fill(x, y, x + 8, y + 8, e.getValue());
            ctx.drawBorder(x, y, 8, 8, 0xFFFFFFFF);
            drawSmallLabel(ctx, e.getKey(), x + 10, y);
            y += 12;
        }
    }

    // ---------- вычисления/утилиты ----------

    private void computeLayout() {
        if (nodes.isEmpty()) return;
        int w = Math.max(1, width - PADDING * 2);
        int h = Math.max(1, height - PADDING * 2);

        double scaleX = (double) w / Math.max(1, maxX - minX);
        double scaleZ = (double) h / Math.max(1, maxZ - minZ);
        baseScale = Math.min(scaleX, scaleZ);

        if (firstLayout) {
            double graphW = (maxX - minX) * baseScale * zoom;
            double graphH = (maxZ - minZ) * baseScale * zoom;
            offsetX = (w - graphW) / 2.0;
            offsetY = (h - graphH) / 2.0;
            firstLayout = false;
        }

        screenPositions.clear();
        for (Node node : nodes) {
            double sx = (node.pos().getX() - minX) * baseScale * zoom + offsetX;
            double sy = (node.pos().getZ() - minZ) * baseScale * zoom + offsetY;
            screenPositions.put(node.id(), new ScreenPos(PADDING + (int)sx, PADDING + (int)sy));
        }
    }

    private int computeGridSpacing() {
        double unitsPerPixel = 1.0 / (baseScale * zoom);
        double raw = TARGET_GRID_PX * unitsPerPixel;
        double pow10 = Math.pow(10, Math.floor(Math.log10(raw)));
        for (int n : new int[]{1, 2, 5}) {
            double candidate = n * pow10;
            if (candidate >= raw) return (int) candidate;
        }
        return (int)(10 * pow10);
    }

    private Node findClickedNode(double mouseX, double mouseY) {
        for (Node node : nodes) {
            ScreenPos p = screenPositions.get(node.id());
            if (p != null && dist2(p.x, p.y, mouseX, mouseY) <= RADIUS * RADIUS) {
                return node;
            }
        }
        return null;
    }

    private void teleportTo(Node node) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            if (mc.getServer() != null && mc.player != null) {
                ServerPlayerEntity sp = mc.getServer().getPlayerManager().getPlayer(mc.player.getUuid());
                if (sp != null) {
                    sp.requestTeleport(node.pos().getX() + 0.5, node.pos().getY(), node.pos().getZ() + 0.5);
                }
            }
            // Иначе: на сервере — отправить пакет с позицией (реализуется платформенно)
        });
    }

    private static double dist2(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2; return dx*dx + dy*dy;
    }

    private void drawSmallLabel(DrawContext ctx, String s, int x, int y) {
        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        ctx.drawText(font, Text.literal(s), x, y, 0xFFFFFFFF, true);
    }

    // --- примитивы (без GL-шейдеров): достаточно для отладки ---

    private static void fillH(DrawContext ctx, int x0, int x1, int y, int argb) {
        if (x1 < x0) { int t = x0; x0 = x1; x1 = t; }
        ctx.fill(x0, y, x1, y + 1, argb);
    }

    private static void fillV(DrawContext ctx, int x, int y0, int y1, int argb) {
        if (y1 < y0) { int t = y0; y0 = y1; y1 = t; }
        ctx.fill(x, y0, x + 1, y1, argb);
    }

    private static void drawLine(DrawContext ctx, int x0, int y0, int x1, int y1, int argb) {
        // Брезенхем, 1px
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        int x = x0, y = y0;
        while (true) {
            ctx.fill(x, y, x + 1, y + 1, argb);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x += sx; }
            if (e2 <= dx) { err += dx; y += sy; }
        }
    }

    private static void fillCircle(DrawContext ctx, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int span = (int)Math.round(Math.sqrt(r * r - dy * dy));
            ctx.fill(cx - span, cy + dy, cx + span + 1, cy + dy + 1, argb);
        }
    }

    private static void drawCircleOutline(DrawContext ctx, int cx, int cy, int r, int argb) {
        int x = r, y = 0; int err = 0;
        while (x >= y) {
            plot8(ctx, cx, cy, x, y, argb);
            y++;
            if (err <= 0) err += 2*y + 1;
            if (err > 0) { x--; err -= 2*x + 1; }
        }
    }

    private static void plot8(DrawContext ctx, int cx, int cy, int x, int y, int argb) {
        ctx.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, argb);
        ctx.fill(cx + y, cy + x, cx + y + 1, cy + x + 1, argb);
        ctx.fill(cx - y, cy + x, cx - y + 1, cy + x + 1, argb);
        ctx.fill(cx - x, cy + y, cx - x + 1, cy + y + 1, argb);
        ctx.fill(cx - x, cy - y, cx - x + 1, cy - y + 1, argb);
        ctx.fill(cx - y, cy - x, cx - y + 1, cy - x + 1, argb);
        ctx.fill(cx + y, cy - x, cx + y + 1, cy - x + 1, argb);
        ctx.fill(cx + x, cy - y, cx + x + 1, cy - y + 1, argb);
    }

    private static int hsvToArgb(float hDeg, float s, float v) {
        float h = (hDeg % 360 + 360) % 360 / 60f;
        int i = (int)Math.floor(h);
        float f = h - i;
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        float r=0,g=0,b=0;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            case 5, -1 -> { r = v; g = p; b = q; }
        }
        int ri = Math.round(r * 255), gi = Math.round(g * 255), bi = Math.round(b * 255);
        return (0xFF << 24) | (ri << 16) | (gi << 8) | bi;
    }

    private ScreenPos worldToScreen(double wx, double wz) {
        int sx = PADDING + (int)((wx - minX) * baseScale * zoom + offsetX);
        int sy = PADDING + (int)((wz - minZ) * baseScale * zoom + offsetY);
        return new ScreenPos(sx, sy);
    }

    private void drawPlayerMarker(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null || nodes.isEmpty()) return;

        double px = mc.player.getX();
        double pz = mc.player.getZ();

        ScreenPos p = worldToScreen(px, pz);

        // Красная «точка» в стилистике: заливка + чёрный контур
        final int r = RADIUS + 2;
        final int fill = 0xFFE74C3C;     // насыщенно-красный
        final int outline = 0xFF000000;  // чёрный

        fillCircle(ctx, p.x, p.y, r, fill);
        drawCircleOutline(ctx, p.x, p.y, r, outline);

        // (опционально) маленькая «носик-стрелка» по направлению взгляда:
        // В Minecraft yaw = 0 смотрит на +Z, углы по часовой, поэтому разворачиваем чуть-чуть.
        float yaw = mc.player.getYaw();
        double a = Math.toRadians(-yaw) + Math.PI / 2.0;
        int tx = p.x + (int)Math.round(Math.cos(a) * (r + 3));
        int ty = p.y + (int)Math.round(Math.sin(a) * (r + 3));
        drawLine(ctx, p.x, p.y, tx, ty, 0xFFFFFFFF);
    }


    private record ScreenPos(int x, int y) {}
}

