package net.oxcodsnet.roadarchitect.util;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    @Test
    void hashAndKeyToPos_roundTrip() {
        int[] xs = {-123456, -1, 0, 1, 123456};
        int[] zs = {-98765, -1, 0, 1, 98765};
        for (int x : xs) {
            for (int z : zs) {
                long k = CacheManager.hash(x, z);
                BlockPos p = CacheManager.keyToPos(k);
                assertEquals(x, p.getX());
                assertEquals(z, p.getZ());
            }
        }
    }
}

