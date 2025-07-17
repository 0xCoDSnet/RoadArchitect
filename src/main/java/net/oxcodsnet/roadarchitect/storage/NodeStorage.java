package net.oxcodsnet.roadarchitect.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Хранилище всех узлов в мире.
 */
public class NodeStorage {
    private final Map<String, Node> nodes = new ConcurrentHashMap<>();

    /**
     * Создает новый узел по указанной позиции и добавляет его в хранилище.
     *
     * @param pos координаты узла
     * @return созданный узел
     */
    public Node add(BlockPos pos) {
        String id = UUID.randomUUID().toString();
        Node node = new Node(id, pos);
        nodes.put(id, node);
        return node;
    }

    /**
     * Удаляет узел по его идентификатору.
     *
     * @param id идентификатор узла
     * @return {@code true}, если узел был удален
     */
    public boolean remove(String id) {
        return nodes.remove(id) != null;
    }

    /**
     * Возвращает неизменяемое представление всех узлов.
     *
     * @return неизменяемая карта узлов
     */
    public Map<String, Node> all() {
        return Collections.unmodifiableMap(nodes);
    }

    /**
     * Очищает все сохраненные узлы.
     */
    public void clear() {
        nodes.clear();
    }

    public NbtList toNbt() {
        NbtList list = new NbtList();
        for (Node node : nodes.values()) {
            NbtCompound tag = new NbtCompound();
            tag.putString("id", node.id());
            tag.putLong("pos", node.pos().asLong());
            list.add(tag);
        }
        return list;
    }

    public static NodeStorage fromNbt(NbtList list) {
        NodeStorage storage = new NodeStorage();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound tag = list.getCompound(i);
            String id = tag.getString("id");
            BlockPos pos = BlockPos.fromLong(tag.getLong("pos"));
            storage.nodes.put(id, new Node(id, pos));
        }
        return storage;
    }
}
