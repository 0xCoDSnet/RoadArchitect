package net.oxcodsnet.roadarchitect.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.core.Insets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.handlers.PipelineRunner;
import net.oxcodsnet.roadarchitect.handlers.PipelineStage;
import org.jetbrains.annotations.NotNull;



/**
 * Настраиваемый экран загрузки мира.
 * Отображает текущую стадию пайплайна {@link PipelineStage} и
 * индетерминированный прогресс-бар вместо прежней кнопки.
 */
public class MyLevelLoadingScreen extends BaseOwoScreen<FlowLayout> {

    private final LevelLoadingScreen parent;
    private OwoUIAdapter<FlowLayout> adapter;

    private LabelComponent stageLabel;
    private BoxComponent progressBar;

    private int barProgress;

    public MyLevelLoadingScreen(LevelLoadingScreen parent) {
        super(Text.literal("Road Architect – worldgen"));
        this.parent = parent;
    }

    // ---------------------------------------------------------------------
    // UI Construction
    // ---------------------------------------------------------------------

    @Override
    protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
        this.adapter = OwoUIAdapter.create(this, Containers::verticalFlow);
        return this.adapter;
    }

    @Override
    protected void build(FlowLayout root) {
        // Настройки корневого контейнера
        root.surface(Surface.VANILLA_TRANSLUCENT)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .padding(Insets.of(8));

        // Подпись стадии
        this.stageLabel = Components.label(Text.literal(PipelineStage.INITIALISATION.toString()))
                .horizontalTextAlignment(HorizontalAlignment.CENTER);

        // Индетерминированный прогресс‑бар. Его ширина
        // изменяется в {@link #render(DrawContext, int, int, float)}
        this.progressBar = (BoxComponent) Components.box(Sizing.fixed(0), Sizing.fixed(4))
                .fill(true)
                .color(Color.ofRgb(0xFFFFFF))
                .margins(Insets.vertical(4));

        root.child(stageLabel);
        root.child(progressBar);
    }

    // ---------------------------------------------------------------------
    // Rendering & Updates
    // ---------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Обновляем надпись стадии
        PipelineStage stage = PipelineRunner.getCurrentStage();
        this.stageLabel.text(stage.label());

        // Анимация "бегущей" полосы
        int maxWidth = Math.max(this.width - 40, 100); // запас на края
        this.barProgress = (this.barProgress + 4) % maxWidth;
        this.progressBar.horizontalSizing(Sizing.fixed(this.barProgress));

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
