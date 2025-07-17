package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GraphComponent extends BaseComponent {

    private final List<Node> nodes;
    private final Map<String, Set<String>> edges;
    private final Map<String, ScreenPos> screenPositions = new HashMap<>();

    private static final int RADIUS = 4;
    private static final int PADDING = 20;

    public GraphComponent(List<Node> nodes, Map<String, Set<String>> edges) {
        this.nodes = nodes;
        this.edges = edges;
        this.horizontalSizing(Sizing.fill());
        this.verticalSizing(Sizing.fill());
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
            context.drawCircle(pos.x, pos.y, RADIUS, 1, Color.WHITE);
            context.drawCircle(pos.x, pos.y, RADIUS - 1, 1, Color.BLACK);

            if (distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS) {
                Text tip = Text.literal(node.pos().toShortString() + " \u2022 " + node.type());
                context.drawTooltip(MinecraftClient.getInstance().textRenderer, tip, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        for (Node node : nodes) {
            ScreenPos pos = screenPositions.get(node.id());
            if (pos != null && distance(pos.x, pos.y, mouseX, mouseY) <= RADIUS) {
                var client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.requestTeleport(node.pos().getX() + 0.5, node.pos().getY(), node.pos().getZ() + 0.5);
                }
                return true;
            }
        }
        return false;
    }

    private void computeLayout() {
        if (nodes.isEmpty()) return;
        int minX = nodes.stream().mapToInt(n -> n.pos().getX()).min().orElse(0);
        int maxX = nodes.stream().mapToInt(n -> n.pos().getX()).max().orElse(0);
        int minZ = nodes.stream().mapToInt(n -> n.pos().getZ()).min().orElse(0);
        int maxZ = nodes.stream().mapToInt(n -> n.pos().getZ()).max().orElse(0);

        int w = Math.max(1, this.width() - PADDING * 2);
        int h = Math.max(1, this.height() - PADDING * 2);

        double scaleX = (double) w / Math.max(1, maxX - minX);
        double scaleZ = (double) h / Math.max(1, maxZ - minZ);

        for (Node node : nodes) {
            int x = PADDING + (int) ((node.pos().getX() - minX) * scaleX);
            int y = PADDING + (int) ((node.pos().getZ() - minZ) * scaleZ);
            screenPositions.put(node.id(), new ScreenPos(x, y));
        }
    }

    private static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    private record ScreenPos(int x, int y) {}
}
