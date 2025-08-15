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
}
