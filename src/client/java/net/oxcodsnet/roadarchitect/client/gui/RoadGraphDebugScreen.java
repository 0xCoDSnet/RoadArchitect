package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.container.Containers;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RoadGraphDebugScreen extends BaseOwoScreen<FlowLayout> {

    private final List<Node> nodes;
    private final Map<String, Set<String>> edges;

    public RoadGraphDebugScreen(List<Node> nodes, Map<String, Set<String>> edges) {
        super(Text.literal("Road Graph Debug"));
        this.nodes = nodes;
        this.edges = edges;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, (w, h) -> Containers.verticalFlow(Sizing.fill(), Sizing.fill()));
    }

    @Override
    protected void build(FlowLayout root) {
        root.child(new GraphComponent(nodes, edges));
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
