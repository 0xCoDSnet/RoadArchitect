package net.oxcodsnet.roadarchitect.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Global asynchronous executor backed by a single {@link ForkJoinPool}.
 * Provides helper methods for submitting tasks without creating
 * additional thread pools across the project.
 */
public final class AsyncExecutor {
    private static final ForkJoinPool POOL = new ForkJoinPool(
            Math.max(2, Runtime.getRuntime().availableProcessors() - 1)
    );

    private AsyncExecutor() {
    }

    /**
     * Submits a value-producing task to the shared pool.
     */
    public static <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, POOL);
    }

    /**
     * Executes a fire-and-forget task on the shared pool.
     */
    public static void execute(Runnable task) {
        POOL.execute(task);
    }
}

