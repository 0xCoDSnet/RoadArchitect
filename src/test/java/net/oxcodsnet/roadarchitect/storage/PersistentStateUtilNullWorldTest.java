package net.oxcodsnet.roadarchitect.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies storage classes delegate to {@code PersistentStateUtil} by observing
 * a {@link NullPointerException} when the world is {@code null}.
 */
class PersistentStateUtilNullWorldTest {
    @Test
    void pathStorageDelegates() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> PathStorage.get(null));
        assertTrue(stackContainsUtil(ex));
    }

    @Test
    void roadBuilderStorageDelegates() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> RoadBuilderStorage.get(null));
        assertTrue(stackContainsUtil(ex));
    }

    @Test
    void cacheStorageDelegates() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CacheStorage.get(null));
        assertTrue(stackContainsUtil(ex));
    }

    @Test
    void roadGraphStateDelegates() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> RoadGraphState.get(null));
        assertTrue(stackContainsUtil(ex));
    }

    private boolean stackContainsUtil(Throwable ex) {
        for (StackTraceElement el : ex.getStackTrace()) {
            if (el.getClassName().endsWith("PersistentStateUtil")) {
                return true;
            }
        }
        return false;
    }
}
