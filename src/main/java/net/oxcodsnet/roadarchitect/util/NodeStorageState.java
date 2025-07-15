package net.oxcodsnet.roadarchitect.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.PersistentState.Type;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Persistent state holder for {@link NodeStorage}.
 */
public class NodeStorageState extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger("NodeStorageState");
    private static final String NODES_KEY = "nodes";
    private static final String NAME = "roadarchitect_nodes";
    private static final Type<NodeStorageState> TYPE = new Type<>(NodeStorageState::new, NodeStorageState::fromNbt, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final NodeStorage storage;

    public NodeStorageState() {
        this(new NodeStorage());
    }

    private NodeStorageState(NodeStorage storage) {
        this.storage = storage;
    }

    public NodeStorage getStorage() {
        return this.storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        nbt.putLongArray(NODES_KEY, this.storage.asLongArray());
        return nbt;
    }

    public static NodeStorageState fromNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        NodeStorage storage = new NodeStorage();
        for (long packed : nbt.getLongArray(NODES_KEY)) {
            storage.add(BlockPos.fromLong(packed));
        }
        return new NodeStorageState(storage);
    }

    /**
     * Obtains the persistent state for the given world, creating it if needed.
     */
    public static NodeStorageState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, NAME);
    }

    /**
     * Registers a listener saving the state on world unload.
     */
    public static void init() {
        ServerWorldEvents.UNLOAD.register(NodeStorageState::onUnload);
    }

    private static void onUnload(MinecraftServer server, ServerWorld world) {
        NodeStorageState.get(world).markDirty();
    }
}
