package net.oxcodsnet.roadarchitect.api.core;

/**
 * Immutable edge snapshot.
 */
public record EdgeView(String id, String nodeA, String nodeB, EdgeStatus status) {}
