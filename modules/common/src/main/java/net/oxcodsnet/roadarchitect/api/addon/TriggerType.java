package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.util.Identifier;

/**
 * Describes a trigger type provided by an addon.
 */
public final class TriggerType {
    private final Identifier id;
    private final int radius;
    private final TriggerHandler handler;

    public TriggerType(Identifier id, int radius, TriggerHandler handler) {
        this.id = id;
        this.radius = radius;
        this.handler = handler;
    }

    public Identifier id() {
        return id;
    }

    public int radius() {
        return radius;
    }

    public TriggerHandler handler() {
        return handler;
    }
}

