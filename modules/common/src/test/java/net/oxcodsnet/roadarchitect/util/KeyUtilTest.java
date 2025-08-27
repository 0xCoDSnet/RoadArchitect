package net.oxcodsnet.roadarchitect.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyUtilTest {

    @Test
    void edgeKey_isOrderIndependent_andUsesPlusDelimiter() {
        String a = "nodeA";
        String b = "nodeB";

        String ab = KeyUtil.edgeKey(a, b);
        String ba = KeyUtil.edgeKey(b, a);

        assertEquals(ab, ba, "edgeKey must be order-independent");
        assertTrue(ab.contains("+"), "edgeKey must use '+' delimiter");

        // Deterministic ordering
        assertEquals("nodeA+nodeB", ab);
    }

    @Test
    void pathKey_isOrderIndependent_andUsesPipeDelimiter() {
        String a = "u1";
        String b = "u2";

        String ab = KeyUtil.pathKey(a, b);
        String ba = KeyUtil.pathKey(b, a);

        assertEquals(ab, ba, "pathKey must be order-independent");
        assertTrue(ab.contains("|"), "pathKey must use '|' delimiter");
        assertEquals("u1|u2", ab);
    }
}

