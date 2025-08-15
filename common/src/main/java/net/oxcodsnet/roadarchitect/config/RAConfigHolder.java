package net.oxcodsnet.roadarchitect.config;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Синглтон-холдер активного конфига + простые слушатели «перепривязки».
 */
public final class RAConfigHolder {
    private static volatile RAConfig INSTANCE = new Defaults();
    private static final CopyOnWriteArrayList<Consumer<RAConfig>> LISTENERS = new CopyOnWriteArrayList<>();

    private RAConfigHolder() {
    }

    public static RAConfig get() {
        return INSTANCE;
    }

    /**
     * Привязывает платформенную реализацию и уведомляет слушателей.
     */
    public static void set(RAConfig impl) {
        INSTANCE = Objects.requireNonNull(impl, "impl");
        for (var l : LISTENERS) l.accept(INSTANCE);
    }

    /**
     * Подписка на смену реализации (например, после перезагрузки конфига).
     */
    public static AutoCloseable listen(Consumer<RAConfig> listener) {
        LISTENERS.add(Objects.requireNonNull(listener));
        listener.accept(INSTANCE); // сразу отдать текущее
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Запасные значения по умолчанию, если платформа не подвязана.
     */
    private static final class Defaults implements RAConfig {
        @Override
        public int initScanRadius() {
            return 125;
        }

        @Override
        public int chunkGenerateScanRadius() {
            return 20;
        }

        @Override
        public int maxConnectionDistance() {
            return 715;
        }

        @Override
        public int pipelineIntervalSeconds() {
            return 120;
        }

        @Override
        public List<String> structureSelectors() {
            return List.of("#minecraft:village");
        }
    }
}
