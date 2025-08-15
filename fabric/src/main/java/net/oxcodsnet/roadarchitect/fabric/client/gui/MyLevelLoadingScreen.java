package net.oxcodsnet.roadarchitect.fabric.client.gui;

import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.BoxComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.StackLayout;
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

    // размеры
    private static final int BAR_W = 260;
    private static final int BAR_H = 8;
    private static final int GLOW_W = 80;

    // анимация
    private static final int PERIOD_MS  = 2400; // полный проезд слева-направо
    // контейнер и слои
    private StackLayout   barRoot;
    private BoxComponent  barGlow;

    // UI
    private LabelComponent stageLabel;



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




    // ───────────────────────── build ─────────────────────────
    @Override
    protected void build(FlowLayout root) {
        root.surface(Surface.BLANK)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.BOTTOM)
                .padding(Insets.of(8));

        // Заголовок стадии
        this.stageLabel = (LabelComponent) Components.label(PipelineStage.INITIALISATION.label())
                .horizontalTextAlignment(HorizontalAlignment.CENTER)
                .margins(Insets.bottom(6));

        // Контейнер баров (стек)
        this.barRoot = Containers.stack(Sizing.fixed(BAR_W), Sizing.fixed(BAR_H));

        // Фон дорожки
        var barTrack = Components.box(Sizing.fill(), Sizing.fill())
                .fill(true)
                .startColor(Color.ofRgb(0x20242A))
                .endColor(Color.ofRgb(0x2A2F36))
                .direction(BoxComponent.GradientDirection.LEFT_TO_RIGHT);

        // Обводка
        var barOutline = Components.box(Sizing.fill(), Sizing.fill())
                .fill(false)
                .startColor(Color.ofRgb(0xB0B8C0));

        // Бегущий сегмент
        this.barGlow = (BoxComponent) Components.box(Sizing.fixed(GLOW_W), Sizing.fill())
                .fill(true)
                .startColor(Color.ofRgb(0x34C3FF))
                .endColor(Color.ofRgb(0x78C8FF))
                .direction(BoxComponent.GradientDirection.LEFT_TO_RIGHT)
                .positioning(Positioning.absolute(0, 0));

        // Слои: фон → свечение → обводка
        this.barRoot.child(barTrack);
        this.barRoot.child(this.barGlow);
        this.barRoot.child(barOutline);

        root.child(this.stageLabel);
        root.child(this.barRoot);
    }

    // ---------------------------------------------------------------------
    // Rendering & Updates
    // ---------------------------------------------------------------------

    // ───────────────────────── render ─────────────────────────
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {

        this.renderPanoramaBackground(ctx, delta);

        // 1) сначала ванильная «карта чанков» + процент
        parent.render(ctx, mouseX, mouseY, delta);

        // 2) обновляем текст стадии и анимацию бара
        this.stageLabel.text(PipelineRunner.getCurrentStage().label());

        final long now = System.currentTimeMillis();
        final float tLinear = (now % PERIOD_MS) / (float) PERIOD_MS;
        final float t = tLinear * tLinear * (3f - 2f * tLinear); // smoothstep
        final float path = -GLOW_W + t * (BAR_W + GLOW_W);
        int left = Math.max(0, Math.round(path));
        int right = Math.min(BAR_W, Math.round(path) + GLOW_W);
        int visible = Math.max(0, right - left);

        this.barGlow.positioning(Positioning.absolute(left, 0));
        this.barGlow.horizontalSizing(Sizing.fixed(visible));

        // 3) рисуем owo-компоненты поверх ванилы
        super.render(ctx, mouseX, mouseY, delta);
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
