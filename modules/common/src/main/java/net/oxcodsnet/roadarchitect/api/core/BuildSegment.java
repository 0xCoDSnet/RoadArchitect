package net.oxcodsnet.roadarchitect.api.core;

/** A queued build segment for a path subrange within a chunk. */
public record BuildSegment(String pathKey, int start, int end) {}

