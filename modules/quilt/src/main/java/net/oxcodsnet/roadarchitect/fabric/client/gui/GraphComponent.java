package net.oxcodsnet.roadarchitect.fabric.client.gui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Компонент UI для отображения графа дорог на экране отладки.
 * <p>UI component used to render the road graph in the debug screen.</p>
 */
public class GraphComponent extends BaseComponent {

    private static final int RADIUS = 4;
    private static final int PADDING = 20;
    private static final int GRID_SPACING = 100;
    private static final int TARGET_GRID_PX = 80;
    private final List<Node> nodes;
    private final Collection<EdgeStorage.Edge> edges;
    private final Map<EdgeStorage.Status, Color> statusColors = Map.of(
            EdgeStorage.Status.NEW, Color.ofRgb(0xF2C94C),
            EdgeStorage.Status.SUCCESS, Color.ofRgb(0x27AE60),
            EdgeStorage.Status.FAILURE, Color.ofRgb(0xAE162B)
    );
    private final Map<String, ScreenPos> screenPositions = new HashMap<>();
    private final Map<String, Color> typeColors = new HashMap<>();
    private int minX;
    private int maxX;
    private int minZ;
    private int maxZ;
    private double baseScale;
    private double offsetX;
    private double offsetY;
    private double zoom = 1.0;
    private boolean dragging;
    private boolean firstLayout = true;

    /**
     * Создает компонент для отрисовки графа.
     * <p>Creates a component for rendering the graph.</p>
     */
    public GraphComponent(List<Node> nodes, Collection<EdgeStorage.Edge> edges) {
        this.nodes = nodes;
        this.edges = edges;
        this.horizontalSizing(Sizing.fill());
        this.verticalSizing(Sizing.fill());

        if (!nodes.isEmpty()) {
            minX = nodes.stream().mapToInt(n -> n.pos().getX()).min().orElse(0);
            maxX = nodes.stream().mapToInt(n -> n.pos().getX()).max().orElse(0);
            minZ = nodes.stream().mapToInt(n -> n.pos().getZ()).min().orElse(0);
            maxZ = nodes.stream().mapToInt(n -> n.pos().getZ()).max().orElse(0);
        }

        for (Node node : nodes) {
            typeColors.computeIfAbsent(node.type(), t ->
                    Color.ofHsv(Math.abs(t.hashCode() % 360) / 360f, 0.6f, 0.9f));
        }
    }

    /**
     * Вычисляет расстояние между двумя точками.
     * <p>Calculates the distance between two points.</p>
     */
    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2, dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    public boolean canFocus(FocusSource source) {
        // Разрешаем фокус только по клику мышью,
        // чтобы Tab‑навигация не «заезжала» на граф
        return source == FocusSource.MOUSE_CLICK;
    }

    @Override
    public void onFocusGained(FocusSource src) {
    }   // ничего особого

    @Override
    public void onFocusLost() {
    }                     // тоже пусто

    @Override
    /**
     * Возвращает базовый размер по горизонтали для расчёта размещения.
     * <p>Returns the base horizontal size used for layout.</p>
     */
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    /**
     * Возвращает базовый размер по вертикали для расчёта размещения.
     * <p>Returns the base vertical size used for layout.</p>
     */
    protected int determineVerticalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    /**
     * Отрисовывает граф, сетку и легенду.
     * <p>Renders the graph, grid and legend.</p>
     */
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float delta, float tickDelta) {
        computeLayout();

        context.drawPanel(this.x(), this.y(), this.width(), this.height(), true);
        context.drawRectOutline(this.x(), this.y(), this.width(), this.height(), 0xFFFFFFFF);

        drawGrid(context);

        /* ----- draw edges ----- */
        for (EdgeStorage.Edge edge : edges) {
            ScreenPos a = screenPositions.get(edge.nodeA());
            ScreenPos b = screenPositions.get(edge.nodeB());
            if (a == null || b == null) continue;

            Color col = statusColors.getOrDefault(edge.status(), Color.WHITE);
            context.drawLine(a.x, a.y, b.x, b.y, 1, col);
        }

        /* ----- draw nodes ----- */
        for (Node node : nodes) {
            ScreenPos p = screenPositions.get(node.id());
            if (p == null) continue;

            Color col = typeColors.getOrDefault(node.type(), Color.WHITE);
            context.drawCircle(p.x, p.y, 16, RADIUS, col);
            context.drawCircle(p.x, p.y, 16, RADIUS - 1, Color.BLACK);

            if (distance(p.x, p.y, mouseX, mouseY) <= RADIUS) {
                context.drawTooltip(
                        MinecraftClient.getInstance().textRenderer,
                        Text.literal(node.pos().toShortString() + " • " + node.type()),
                        mouseX, mouseY
                );
            }
        }

        drawScale(context);
        drawLegend(context);
    }

    @Override
    /**
     * Обработчик нажатий мыши.
     * <p>Handles mouse button presses.</p>
     */
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (clickOnNode(mouseX, mouseY))
            return true;

        dragging = true;
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (dragging && button == 0) {
            offsetX += deltaX;
            offsetY += deltaY;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    private boolean clickOnNode(double mouseX, double mouseY) {
        for (Node node : nodes) {
            ScreenPos pos = screenPositions.get(node.id());
            if (pos != null && distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS) {
                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    if (client.getServer() != null && client.player != null) {
                        ServerPlayerEntity sp = client.getServer()
                                .getPlayerManager()
                                .getPlayer(client.player.getUuid());
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

    @Override
    /**
     * Обработчик прокрутки колёсика мыши для масштабирования.
     * <p>Handles mouse wheel scrolling to zoom the view.</p>
     */
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        double oldZoom = zoom;
        if (amount > 0) {
            zoom *= 1.1;
        } else {
            zoom /= 1.1;
        }
        offsetX = (offsetX - mouseX + PADDING) * (zoom / oldZoom) + mouseX - PADDING;
        offsetY = (offsetY - mouseY + PADDING) * (zoom / oldZoom) + mouseY - PADDING;
        return true;
    }

    /**
     * Пересчитывает расположение узлов и масштаб отображения.
     * <p>Recomputes node positions and scaling factors.</p>
     */
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
            offsetX = (w - graphW) / 2;
            offsetY = (h - graphH) / 2;
            firstLayout = false;
        }

        for (Node node : nodes) {
            double x = (node.pos().getX() - minX) * baseScale * zoom + offsetX;
            double y = (node.pos().getZ() - minZ) * baseScale * zoom + offsetY;
            int sx = PADDING + (int) x;
            int sy = PADDING + (int) y;
            screenPositions.put(node.id(), new ScreenPos(sx, sy));
        }
    }

    /**
     * Рисует сетку координат на фоне графа.
     * <p>Draws the coordinate grid behind the graph.</p>
     */
    private void drawGrid(OwoUIDrawContext context) {
        int w = width() - PADDING * 2;
        int h = height() - PADDING * 2;

        double worldX0 = minX + (-offsetX) / (baseScale * zoom);
        double worldZ0 = minZ + (-offsetY) / (baseScale * zoom);
        double worldX1 = minX + (w - offsetX) / (baseScale * zoom);
        double worldZ1 = minZ + (h - offsetY) / (baseScale * zoom);

        int spacing = computeGridSpacing();

        int startWX = (int) (Math.floor(worldX0 / spacing) * spacing);
        int startWZ = (int) (Math.floor(worldZ0 / spacing) * spacing);

        for (int x = startWX; x <= worldX1; x += spacing) {
            int sx = PADDING + (int) ((x - worldX0) * baseScale * zoom);
            context.drawLine(sx, PADDING, sx, PADDING + h, 1, Color.ofArgb(0x60444444));
            context.drawText(Text.literal(Integer.toString(x)), sx + 2, PADDING + 2, .6f, 0xFFFFFF);
        }
        for (int z = startWZ; z <= worldZ1; z += spacing) {
            int sz = PADDING + (int) ((z - worldZ0) * baseScale * zoom);
            context.drawLine(PADDING, sz, PADDING + w, sz, 1, Color.ofArgb(0x60444444));
            context.drawText(Text.literal(Integer.toString(z)), PADDING + 2, sz + 2, .6f, 0xFFFFFF);
        }

    }

    /**
     * Рисует линейку масштаба в правом нижнем углу.
     * <p>Draws the scale ruler in the bottom-right corner.</p>
     */
    private void drawScale(OwoUIDrawContext ctx) {
        int spacing = computeGridSpacing();
        int lengthPx = (int) (spacing * baseScale * zoom);
        int x = width() - PADDING - lengthPx - 10;
        int y = height() - PADDING - 8;
        ctx.drawLine(x, y, x + lengthPx, y, 2, Color.WHITE);
        ctx.drawLine(x, y - 3, x, y + 3, 2, Color.WHITE);
        ctx.drawLine(x + lengthPx, y - 3, x + lengthPx, y + 3, 2, Color.WHITE);
        ctx.drawText(Text.literal(spacing + "m"), x, y - 10, .7f, 0xFFFFFF);
    }

    /**
     * Рисует легенду типов структур.
     * <p>Renders the legend of structure types.</p>
     */
    private void drawLegend(OwoUIDrawContext context) {
        int x = PADDING;
        int y = this.height() - PADDING - typeColors.size() * 12;
        for (Map.Entry<String, Color> entry : typeColors.entrySet()) {
            context.fill(x, y, x + 8, y + 8, entry.getValue().argb());
            context.drawRectOutline(x, y, 8, 8, 0xFFFFFFFF);
            context.drawText(Text.literal(entry.getKey()), x + 10, y, 0.7f, 0xFFFFFF);
            y += 12;
        }
    }

    /**
     * Возвращает «красивый» шаг сетки в мировых координатах
     */
    private int computeGridSpacing() {
        double unitsPerPixel = 1.0 / (baseScale * zoom);
        double raw = TARGET_GRID_PX * unitsPerPixel;          // желаемый шаг в блоках
        double pow10 = Math.pow(10, Math.floor(Math.log10(raw)));
        for (int n : new int[]{1, 2, 5}) {
            double candidate = n * pow10;
            if (candidate >= raw) return (int) candidate;
        }
        return (int) (10 * pow10); // fallback = 10*10^k
    }

    /**
     * Экранные координаты узла.
     * <p>Screen coordinates of a node.</p>
     */
    private record ScreenPos(int x, int y) {
    }
}