package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.jetbrains.annotations.NotNull;

public class MyWorldGenScreen extends BaseOwoScreen<FlowLayout> {
    private final CreateWorldScreen parent;
    private OwoUIAdapter<FlowLayout> adapter;

    public MyWorldGenScreen(CreateWorldScreen parent) {
        super(Text.literal("Custom WorldGen"));
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        return OwoUIAdapter.create(this, Containers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        root.child(Components.button(Text.literal("My Owo Button"), b ->
                RoadArchitect.LOGGER.info("Clicked owo button in world gen!")
        ));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Отрисовываем оригинальный экран мирогенерации
        parent.render(context, mouseX, mouseY, delta);

        // Затем рисуем owo-ui
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        // Сначала owo-ui
        if (adapter.mouseClicked(x, y, button)) return true;
        // Иначе передаём родительскому экрану
        return parent.mouseClicked(x, y, button);
    }

    // По аналогии добавьте keyPressed, charTyped и т.д.
}
