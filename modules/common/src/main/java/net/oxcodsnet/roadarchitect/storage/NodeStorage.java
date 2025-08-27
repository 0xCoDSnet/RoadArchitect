package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import net.oxcodsnet.roadarchitect.storage.components.Node;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище всех узлов в мире.
 * <p>Storage for all nodes in the world.</p>
 */
public class NodeStorage {
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();

    /**
     * Загружает хранилище узлов из списка NBT.
     * <p>Loads node storage from the given NBT list.</p>
     */
    public static NodeStorage fromNbt(NbtList list) {
        NodeStorage storage = new NodeStorage();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompoundOrEmpty(i);
            String id = tag.getString("id", "");
            if (id.isEmpty()) continue;
            long rawPos = tag.getLong("pos", 0L);
            BlockPos pos = BlockPos.fromLong(rawPos);
            String type = tag.getString("type", "");
            storage.nodes.put(id, new Node(id, pos, type));
        }
        return storage;
    }

    /**
     * Создает новый узел по указанной позиции и добавляет его в хранилище.
     * <p>Creates a new node at the given position and adds it to this storage.</p>
     *
     * @param pos  координаты узла / node coordinates
     * @param type идентификатор структуры / structure id
     * @return созданный узел / created node
     */
    public Node add(BlockPos pos, String type) {
        String id = UUID.randomUUID().toString();
        Node node = new Node(id, pos, type);
        nodes.put(id, node);
        return node;
    }

    /**
     * Удаляет узел по его идентификатору.
     * <p>Removes the node with the given identifier.</p>
     *
     * @param id идентификатор узла / node identifier
     * @return {@code true} если узел был удален / {@code true} if the node was removed
     */
    public boolean remove(String id) {
        return nodes.remove(id) != null;
    }

    /**
     * Возвращает неизменяемое представление всех узлов.
     * <p>Returns an unmodifiable view of all stored nodes.</p>
     *
     * @return неизменяемая карта узлов / unmodifiable map of nodes
     */
    public Map<String, Node> all() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Очищает все сохраненные узлы.
     * <p>Clears all stored nodes.</p>
     */
    public void clear() {
        nodes.clear();
    }

    /**
     * Сериализует все узлы в список NBT.
     * <p>Serializes all nodes into an NBT list.</p>
     */
    public NbtList toNbt() {
        NbtList list = new NbtList();
        for (Node node : nodes.values()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", node.id());
            tag.putLong("pos", node.pos().asLong());
            tag.putString("type", node.type());
            list.add(tag);
        }
        return list;
    }
}
