package net.oxcodsnet.roadarchitect.util;

/**
 * Utility methods for building deterministic keys for edges and paths.
 */
public final class KeyUtil {
    private KeyUtil() {
    }

    /**
     * Constructs a deterministic edge key using "+" as delimiter.
     * Order of arguments does not affect the result.
     */
    public static String edgeKey(String a, String b) {
        return a.compareTo(b) < 0 ? a + "+" + b : b + "+" + a;
    }

    /**
     * Constructs a deterministic path key using "|" as delimiter.
     * Order of arguments does not affect the result.
     */
    public static String pathKey(String a, String b) {
        return a.compareTo(b) <= 0 ? a + "|" + b : b + "|" + a;
    }

    /**
     * Attempts to parse an edge key formatted as "A+B". Returns a two-element
     * array [A,B] when valid; otherwise returns an empty array.
     */
    public static String[] parseEdgeKey(String key) {
        if (key == null) return new String[0];
        String[] parts = key.split("\\+", 2);
        return parts.length == 2 ? parts : new String[0];
    }

    /**
     * Attempts to parse a path key formatted as "A|B". Returns a two-element
     * array [A,B] when valid; otherwise returns an empty array.
     */
    public static String[] parsePathKey(String key) {
        if (key == null) return new String[0];
        String[] parts = key.split("\\|", 2);
        return parts.length == 2 ? parts : new String[0];
    }
}
