package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Экран отладки, отображающий граф дорог.
 * <p>Debug screen displaying the road graph.</p>
 */
public class RoadGraphDebugScreen extends BaseOwoScreen<FlowLayout> {

    private final List<Node> nodes;
    private final Collection<EdgeStorage.Edge> edges;

    /**
     * Создает новый экран отладки графа.
     * <p>Creates a new road graph debug screen.</p>
     */
    public RoadGraphDebugScreen(List<Node> nodes, Collection<EdgeStorage.Edge> edges) {
        super(Text.literal("Road Graph Debug"));
        this.nodes = nodes;
        this.edges = edges;
    }

    @Override
    /**
     * Создает UI-адаптер для корневого контейнера.
     * <p>Creates the UI adapter for the root container.</p>
     */
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, (w, h) -> Containers.verticalFlow(Sizing.fill(), Sizing.fill()));
    }


    /**
     * Собирает содержимое экрана.
     * <p>Builds the screen contents.</p>
     */
    @Override
    protected void build(FlowLayout root) {
        root.child(new GraphComponent(nodes, edges));
    }

    @Override
    /**
     * Позволяет закрывать экран клавишей ESC.
     * <p>Allows closing the screen with the ESC key.</p>
     */
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
