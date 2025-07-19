package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphComponent extends BaseComponent {

    private final List<Node> nodes;
    private final Map<String, Set<String>> edges;
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

    private static final int RADIUS = 4;
    private static final int PADDING = 20;
    private static final int GRID_SPACING = 100;

    public GraphComponent(List<Node> nodes, Map<String, Set<String>> edges) {
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

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return 100;
    }

    @Override
    public void draw(OwoUIDrawContext context, int mouseX, int mouseY, float delta, float tickDelta) {
        computeLayout();

        context.drawPanel(this.x(), this.y(), this.width(), this.height(), false);
        context.drawRectOutline(this.x(), this.y(), this.width(), this.height(), 0xFFFFFFFF);

        drawGrid(context);

        // draw edges
        for (Map.Entry<String, Set<String>> entry : edges.entrySet()) {
            ScreenPos from = screenPositions.get(entry.getKey());
            if (from == null) continue;
            for (String id : entry.getValue()) {
                ScreenPos to = screenPositions.get(id);
                if (to != null) {
                    context.drawLine(from.x, from.y, to.x, to.y, 1, Color.WHITE);
                }
            }
        }

        // draw nodes
        for (Node node : nodes) {
            ScreenPos pos = screenPositions.get(node.id());
            if (pos == null) continue;
            Color color = typeColors.getOrDefault(node.type(), Color.WHITE);
            context.drawCircle(pos.x, pos.y, 16, RADIUS, color);
            context.drawCircle(pos.x, pos.y, 16, RADIUS - 1, Color.BLACK);
            context.drawText(Text.literal(node.type()), pos.x + RADIUS + 2, pos.y - 4, 0.7f, 0xFFFFFF);

            if (distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS) {
                Text tip = Text.literal(node.pos().toShortString() + " • " + node.type());
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, tip, mouseX, mouseY);
            }
        }

        drawScale(context);
        drawLegend(context);
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (Node node : nodes) {
                ScreenPos pos = screenPositions.get(node.id());
                if (pos != null && distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS) {
                    var client = MinecraftClient.getInstance();
                    if (client.player != null) {
                        client.player.setPosition(node.pos().getX() + 0.5, node.pos().getY(), node.pos().getZ() + 0.5);
                    }
                    return true;
                }
            }
        } else return button == 1;
        return false;
    }

    @Override
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

    private void computeLayout() {
        if (nodes.isEmpty()) return;
        int w = Math.max(1, this.width() - PADDING * 2);
        int h = Math.max(1, this.height() - PADDING * 2);

        double scaleX = (double) w / Math.max(1, maxX - minX);
        double scaleZ = (double) h / Math.max(1, maxZ - minZ);
        baseScale = Math.min(scaleX, scaleZ);

        for (Node node : nodes) {
            double x = (node.pos().getX() - minX) * baseScale * zoom + offsetX;
            double y = (node.pos().getZ() - minZ) * baseScale * zoom + offsetY;
            int sx = PADDING + (int) x;
            int sy = PADDING + (int) y;
            screenPositions.put(node.id(), new ScreenPos(sx, sy));
        }
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private void drawGrid(OwoUIDrawContext context) {
        int w = this.width() - PADDING * 2;
        int h = this.height() - PADDING * 2;
        int startX = (int) (Math.floor(minX / (double) GRID_SPACING) * GRID_SPACING);
        int startZ = (int) (Math.floor(minZ / (double) GRID_SPACING) * GRID_SPACING);

        for (int x = startX; x <= maxX; x += GRID_SPACING) {
            int sx = PADDING + (int) ((x - minX) * baseScale * zoom + offsetX);
            context.drawLine(sx, PADDING, sx, PADDING + h, 1, Color.ofRgb(0x444444));
            context.drawText(Text.literal(Integer.toString(x)), sx + 2, PADDING + 2, 0.6f, 0xFFFFFF);
        }

        for (int z = startZ; z <= maxZ; z += GRID_SPACING) {
            int sz = PADDING + (int) ((z - minZ) * baseScale * zoom + offsetY);
            context.drawLine(PADDING, sz, PADDING + w, sz, 1, Color.ofRgb(0x444444));
            context.drawText(Text.literal(Integer.toString(z)), PADDING + 2, sz + 2, 0.6f, 0xFFFFFF);
        }
    }

    private void drawScale(OwoUIDrawContext context) {
        int length = (int) (GRID_SPACING * baseScale * zoom);
        int x = this.width() - PADDING - length - 10;
        int y = this.height() - PADDING - 8;
        context.drawLine(x, y, x + length, y, 2, Color.WHITE);
        context.drawLine(x, y - 3, x, y + 3, 2, Color.WHITE);
        context.drawLine(x + length, y - 3, x + length, y + 3, 2, Color.WHITE);
        context.drawText(Text.literal(GRID_SPACING + "m"), x, y - 10, 0.7f, 0xFFFFFF);
    }

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

    private record ScreenPos(int x, int y) {}
}