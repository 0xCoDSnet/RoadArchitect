package net.oxcodsnet.roadarchitect.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AsyncExecutorTest {

    @Test
    void submit_returnsResult() throws Exception {
        CompletableFuture<Integer> f = AsyncExecutor.submit(() -> 2 + 3);
        assertEquals(5, f.get(2, TimeUnit.SECONDS));
    }
}

