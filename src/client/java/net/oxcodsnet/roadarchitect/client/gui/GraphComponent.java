package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Component.FocusSource;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.jetbrains.annotations.Nullable;

/**
 * Компонент для отрисовки графа дорог в debug-экране (фикс под 1.21.7).
 * - не используем drawText(..., scale) из oωo; масштабируем матрицей
 * - кружки рисуем полилинией (обход фан/стрип пайплайнов)
 * - draw(...) соответствует сигнатуре BaseComponent для 1.21.7 (delta, tickDelta)
 * - все цвета текста — с непрозрачной альфой (Colors.WHITE = 0xFFFFFFFF)
 */
public class GraphComponent extends BaseComponent {

    private static final int RADIUS_PX = 4;
    private static final int PADDING = 20;
    private static final int TARGET_GRID_PX = 80;

    private final List<Node> nodes;
    private final Collection<EdgeStorage.Edge> edges;

    private final Map<EdgeStorage.Status, Color> statusColors = Map.of(
            EdgeStorage.Status.NEW, Color.ofRgb(0xF2C94C),
            EdgeStorage.Status.SUCCESS, Color.ofRgb(0x27AE60),
            EdgeStorage.Status.FAILURE, Color.ofRgb(0xAE162B)
    );

    private final Map<String, Color> typeColors = new HashMap<>();
    private final Map<String, ScreenPos> screenPositions = new HashMap<>();

    private int minX, maxX, minZ, maxZ;
    private double baseScale = 1.0;
    private double offsetX = 0.0;
    private double offsetY = 0.0;
    private double zoom = 1.0;
    private boolean invertPan = false; // true — инверсия направления перетаскивания
    private double dragPrevX = 0.0, dragPrevY = 0.0;

    private boolean dragging = false;
    private boolean firstLayout = true;

    public GraphComponent(List<Node> nodes, Collection<EdgeStorage.Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
        this.horizontalSizing(Sizing.fill());
        this.verticalSizing(Sizing.fill());

        if (!nodes.isEmpty()) {
            this.minX = nodes.stream().mapToInt(n -> n.pos().getX()).min().orElse(0);
            this.maxX = nodes.stream().mapToInt(n -> n.pos().getX()).max().orElse(0);
            this.minZ = nodes.stream().mapToInt(n -> n.pos().getZ()).min().orElse(0);
            this.maxZ = nodes.stream().mapToInt(n -> n.pos().getZ()).max().orElse(0);
        }
        for (Node node : nodes) {
            this.typeColors.computeIfAbsent(
                    node.type(),
                    t -> Color.ofHsv(Math.abs(t.hashCode() % 360) / 360f, 0.6f, 0.9f)
            );
        }
    }

    // ───────────────────────── BaseComponent overrides ─────────────────────────

    @Override
    public boolean canFocus(FocusSource source) {
        return source == FocusSource.MOUSE_CLICK;
    }

    @Override public void onFocusGained(FocusSource src) {}
    @Override public void onFocusLost() {}

    @Override protected int determineHorizontalContentSize(Sizing s) { return 100; }
    @Override protected int determineVerticalContentSize(Sizing s) { return 100; }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float delta, float tickDelta) {
        computeLayout();

        // фон/рамка
        context.drawPanel(this.x(), this.y(), this.width(), this.height(), true);
        context.drawRectOutline(this.x(), this.y(), this.width(), this.height(), 0xFFFFFFFF);

        // сетка
        drawGrid(context);

        // рёбра
        for (EdgeStorage.Edge e : edges) {
            ScreenPos a = screenPositions.get(e.nodeA());
            ScreenPos b = screenPositions.get(e.nodeB());
            if (a == null || b == null) continue;
            Color col = statusColors.getOrDefault(e.status(), Color.WHITE);
            context.drawLine(a.x, a.y, b.x, b.y, 1.0, col);
        }

        // узлы + подсказка
        @Nullable Node hovered = null;
        for (Node n : nodes) {
            ScreenPos p = screenPositions.get(n.id());
            if (p == null) continue;

            Color col = typeColors.getOrDefault(n.type(), Color.WHITE);
            UiGfx.drawCircleApprox(context, p.x, p.y, 24, RADIUS_PX, 1.5, col);
            UiGfx.drawCircleApprox(context, p.x, p.y, 24, RADIUS_PX - 1, 1.0, Color.BLACK);

            if (distance(p.x, p.y, mouseX, mouseY) <= RADIUS_PX) {
                hovered = n;
            }
        }
        if (hovered != null) {
            context.drawTooltip(
                    MinecraftClient.getInstance().textRenderer,
                    Text.literal(hovered.pos().toShortString() + " • " + hovered.type()),
                    mouseX, mouseY
            );
        }

        // линейка масштаба
        drawScale(context);

        // легенда
        drawLegend(context);
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        dragging = true;
        dragPrevX = mouseX;
        dragPrevY = mouseY;
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double _dx, double _dy, int button) {
        if (!dragging || button != 0) return false;

        // Сами считаем сдвиг — это устойчиво к любым «особенностям» deltaX/deltaY
        double dx = mouseX - dragPrevX;
        double dy = mouseY - dragPrevY;

        double sign = invertPan ? -1.0 : 1.0;
        offsetX += sign * dx;
        offsetY += sign * dy;

        dragPrevX = mouseX;
        dragPrevY = mouseY;
        return true;
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        double oldZoom = zoom;
        zoom = amount > 0 ? zoom * 1.1 : zoom / 1.1;

        double sOld = baseScale * oldZoom;
        double sNew = baseScale * zoom;

        // масштабирование относительно точки курсора
        offsetX = (offsetX - mouseX + PADDING) * (sNew / sOld) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (sNew / sOld) + mouseY - PADDING;
        return true;
    }

    // ───────────────────────── helpers ─────────────────────────

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void computeLayout() {
        if (nodes.isEmpty()) return;

        int w = Math.max(1, this.width() - PADDING * 2);
        int h = Math.max(1, this.height() - PADDING * 2);

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
        double s = baseScale * zoom;
        for (Node n : nodes) {
            int sx = PADDING + (int) Math.round((n.pos().getX() - minX) * s + offsetX);
            int sy = PADDING + (int) Math.round((n.pos().getZ() - minZ) * s + offsetY);
            screenPositions.put(n.id(), new ScreenPos(sx, sy));
        }
    }

    private void drawGrid(OwoUIDrawContext ctx) {
        int w = Math.max(1, this.width() - PADDING * 2);
        int h = Math.max(1, this.height() - PADDING * 2);
        double s = baseScale * zoom;

        // Видимый диапазон мира
        double worldX0 = minX + (-offsetX) / s;
        double worldZ0 = minZ + (-offsetY) / s;
        double worldX1 = minX + (w - offsetX) / s;
        double worldZ1 = minZ + (h - offsetY) / s;

        int spacing = computeGridSpacing();
        int startWX = (int) (Math.floor(worldX0 / spacing) * spacing);
        int startWZ = (int) (Math.floor(worldZ0 / spacing) * spacing);

        for (int x = startWX; x <= worldX1 + 1; x += spacing) {
            int sx = PADDING + (int) Math.round((x - minX) * s + offsetX);
            ctx.drawLine(sx, PADDING, sx, PADDING + h, 1.0, Color.ofArgb(0x60444444));
            UiGfx.drawTextScaled(ctx, Text.literal(Integer.toString(x)), sx + 2, PADDING + 2, 0.6f, Colors.WHITE);
        }
        for (int z = startWZ; z <= worldZ1 + 1; z += spacing) {
            int sz = PADDING + (int) Math.round((z - minZ) * s + offsetY);
            ctx.drawLine(PADDING, sz, PADDING + w, sz, 1.0, Color.ofArgb(0x60444444));
            UiGfx.drawTextScaled(ctx, Text.literal(Integer.toString(z)), PADDING + 2, sz + 2, 0.6f, Colors.WHITE);
        }
    }

    private int computeGridSpacing() {
        double s = baseScale * zoom;
        double worldPerTarget = TARGET_GRID_PX / s;
        double pow10 = Math.pow(10, Math.floor(Math.log10(worldPerTarget)));
        int[] nice = {1, 2, 5};
        for (int n : nice) {
            double candidate = n * pow10;
            if (candidate >= worldPerTarget) return (int) Math.round(candidate);
        }
        return (int) Math.round(10 * pow10);
    }

    private void drawScale(OwoUIDrawContext ctx) {
        int spacing = computeGridSpacing();
        int lengthPx = (int) Math.round(spacing * baseScale * zoom);
        int x = this.width() - PADDING - lengthPx - 10;
        int y = this.height() - PADDING - 8;

        ctx.drawLine(x, y, x + lengthPx, y, 2.0, Color.WHITE);
        ctx.drawLine(x, y - 3, x, y + 3, 2.0, Color.WHITE);
        ctx.drawLine(x + lengthPx, y - 3, x + lengthPx, y + 3, 2.0, Color.WHITE);
        UiGfx.drawTextScaled(ctx, Text.literal(spacing + " m"), x, y - 10, 0.7f, Colors.WHITE);
    }

    private void drawLegend(OwoUIDrawContext ctx) {
        int x = PADDING;
        int y = this.height() - PADDING - typeColors.size() * 12;
        for (Map.Entry<String, Color> e : typeColors.entrySet()) {
            ctx.fill(x, y, x + 8, y + 8, e.getValue().argb());
            ctx.drawRectOutline(x, y, 8, 8, 0xFFFFFFFF);
            UiGfx.drawTextScaled(ctx, Text.literal(e.getKey()), x + 10, y, 0.7f, Colors.WHITE);
            y += 12;
        }
    }

    private boolean clickOnNode(double mouseX, double mouseY) {
        for (Node node : nodes) {
            ScreenPos pos = screenPositions.get(node.id());
            if (pos != null && distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS_PX) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.getServer() != null && client.player != null) {
                        ServerPlayerEntity sp =
                                client.getServer().getPlayerManager().getPlayer(client.player.getUuid());
                        if (sp != null) {
                            sp.requestTeleport(node.pos().getX() + 0.5, node.pos().getY(), node.pos().getZ() + 0.5);
                        }
                    }
                });
                return true;
            }
        }
        return false;
    }

    /** Экранные координаты узла. */
    private record ScreenPos(int x, int y) {}
}
