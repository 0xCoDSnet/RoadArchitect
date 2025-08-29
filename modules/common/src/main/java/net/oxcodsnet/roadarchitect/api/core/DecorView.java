package net.oxcodsnet.roadarchitect.api.core;

/**
 * Read-only access to decoration metrics and masks per path.
 */
public interface DecorView {
    long checksum(String pathKey);

    double[] prefix(String pathKey);

    byte[] groundMask(String pathKey);

    byte[] waterInteriorMask(String pathKey);
}

