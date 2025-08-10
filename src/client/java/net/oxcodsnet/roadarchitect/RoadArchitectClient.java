package net.oxcodsnet.roadarchitect;

import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.HorizontalAlignment;
import io.wispforest.owo.ui.core.OwoUIAdapter;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.oxcodsnet.roadarchitect.client.gui.MyLevelLoadingScreen;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreen;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Клиентская сторона мода.
 * <p>Client side entry point of the mod.</p>
 */
public class RoadArchitectClient implements ClientModInitializer {

    private static KeyBinding debugKey;

    @Override
    public void onInitializeClient() {

        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            // 1) Не трогаем наш же экран
            if (screen instanceof MyLevelLoadingScreen) return;

            // 2) Подменяем только ванильный загрузочный
            if (screen instanceof LevelLoadingScreen vanilla) {
                // ВАЖНО: не вызываем init() вручную — setScreen сам всё сделает
                // Дополнительно завернём в client.execute, чтобы выйти из текущего init-колбэка
                client.execute(() -> client.setScreen(new MyLevelLoadingScreen(vanilla)));
            }
        });


        debugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.roadarchitect.debug",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.roadarchitect"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (debugKey.wasPressed()) {

                if (client.currentScreen instanceof RoadGraphDebugScreen) {
                    client.setScreen(null);
                    continue;
                }

                ServerWorld world = client.getServer() == null ? null : client.getServer().getOverworld();
                if (world == null) continue;

                RoadGraphState state = RoadGraphState.get(world);

                List<Node> nodes  = new ArrayList<>(state.nodes().all().values());
                Collection<EdgeStorage.Edge> edges = state.edges().all().values();

                client.setScreen(new RoadGraphDebugScreen(nodes, edges));
            }
        });
    }
}
