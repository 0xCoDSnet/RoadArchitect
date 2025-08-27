package net.oxcodsnet.roadarchitect.handlers;

import net.minecraft.text.Text;

public enum PipelineStage {
    INITIALISATION(Text.translatable("roadarchitect.stage.initialisation")),
    SCANNING_STRUCTURES(Text.translatable("roadarchitect.stage.scanning")),
    PATH_FINDING(Text.translatable("roadarchitect.stage.pathfinding")),
    POST_PROCESSING(Text.translatable("roadarchitect.stage.postprocess")),
    COMPLETE(Text.translatable("roadarchitect.stage.complete"));

    private final Text label;

    PipelineStage(Text label) {
        this.label = label;
    }

    public Text label() {
        return label;
    }
}
