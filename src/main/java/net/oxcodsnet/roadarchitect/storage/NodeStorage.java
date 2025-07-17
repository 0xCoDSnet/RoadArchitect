package net.oxcodsnet.roadarchitect.storage;

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
}
