package net.oxcodsnet.roadarchitect.api.addon;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Handler for a trigger type. Invoked when a marker of this type fires.
 */
@FunctionalInterface
public interface TriggerHandler {
    /**
     * Called when a marker of the associated type is triggered.
     *
     * @param world  server world
     * @param pos    marker position
     * @param player triggering player in proximity
     * @param data   optional addon data; never null (empty if none)
     */
    void onTrigger(ServerWorld world, BlockPos pos, ServerPlayerEntity player, NbtCompound data);
}

