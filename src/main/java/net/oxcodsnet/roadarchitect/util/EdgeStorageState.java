package net.oxcodsnet.roadarchitect.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentState.Type;
import net.minecraft.world.PersistentStateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Persistent state holder for {@link EdgeStorage}. */
public class EdgeStorageState extends PersistentState {
    private static final Logger LOGGER = LoggerFactory.getLogger("EdgeStorageState");
    private static final String EDGES_KEY = "edges";
    private static final String NAME = "roadarchitect_edges";
    private static final Type<EdgeStorageState> TYPE =
            new Type<>(EdgeStorageState::new, EdgeStorageState::fromNbt, DataFixTypes.SAVED_DATA_RANDOM_SEQUENCES);

    private final EdgeStorage storage;

    public EdgeStorageState() {
        this(new EdgeStorage());
    }

    private EdgeStorageState(EdgeStorage storage) {
        this.storage = storage;
    }

    public EdgeStorage getStorage() {
        return this.storage;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        NbtList list = new NbtList();
        for (EdgeStorage.Edge edge : this.storage.asEdgeSet()) {
            NbtCompound entry = new NbtCompound();
            entry.putLong("a", edge.a().asLong());
            entry.putLong("b", edge.b().asLong());
            list.add(entry);
        }
        nbt.put(EDGES_KEY, list);
        return nbt;
    }

    public static EdgeStorageState fromNbt(NbtCompound nbt, WrapperLookup registryLookup) {
        EdgeStorage storage = new EdgeStorage();
        NbtList list = nbt.getList(EDGES_KEY, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            BlockPos a = BlockPos.fromLong(entry.getLong("a"));
            BlockPos b = BlockPos.fromLong(entry.getLong("b"));
            storage.add(a, b);
        }
        return new EdgeStorageState(storage);
    }

    /** Obtains the persistent state for the given world, creating it if needed. */
    public static EdgeStorageState get(ServerWorld world) {
        PersistentStateManager manager = world.getPersistentStateManager();
        return manager.getOrCreate(TYPE, NAME);
    }

    /** Registers a listener saving the state on world unload. */
    public static void init() {
        ServerWorldEvents.UNLOAD.register(EdgeStorageState::onUnload);
    }

    private static void onUnload(MinecraftServer server, ServerWorld world) {
        EdgeStorageState.get(world).markDirty();
    }
}
