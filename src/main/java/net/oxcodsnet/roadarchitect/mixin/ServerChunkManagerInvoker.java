package net.oxcodsnet.roadarchitect.mixin;

import net.minecraft.server.world.OptionalChunk;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.concurrent.CompletableFuture;

/**
 * Пример миксина сервера.
 * <p>Example server mixin.</p>
 */
@Mixin(ServerChunkManager .class)
public interface ServerChunkManagerInvoker {
    @Invoker("getChunkFuture")
    CompletableFuture<OptionalChunk<Chunk>> invokeGetChunkFuture(
            int chunkX, int chunkZ, ChunkStatus leastStatus, boolean create);
}

