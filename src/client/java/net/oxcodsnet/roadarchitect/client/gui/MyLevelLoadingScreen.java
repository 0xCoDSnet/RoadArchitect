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
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.jetbrains.annotations.NotNull;

public class MyLevelLoadingScreen extends BaseOwoScreen<FlowLayout> {
    private final LevelLoadingScreen parent;
    private OwoUIAdapter<FlowLayout> adapter;

    public MyLevelLoadingScreen(LevelLoadingScreen parent) {
        super(Text.literal("Custom WorldGen"));
        this.parent = parent;
    }

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        this.adapter = OwoUIAdapter.create(this, Containers::verticalFlow);
        return this.adapter;
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
        parent.render(context, mouseX, mouseY, delta);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (this.adapter.mouseClicked(x, y, button)) return true;
        return parent.mouseClicked(x, y, button);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (this.adapter.mouseReleased(x, y, button)) return true;
        return parent.mouseReleased(x, y, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.adapter.keyPressed(keyCode, scanCode, modifiers)) return true;
        return parent.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (this.adapter.charTyped(chr, modifiers)) return true;
        return parent.charTyped(chr, modifiers);
    }
}
