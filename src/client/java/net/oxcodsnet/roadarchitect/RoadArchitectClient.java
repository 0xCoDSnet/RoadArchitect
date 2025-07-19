package net.oxcodsnet.roadarchitect;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreen;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Клиентская сторона мода.
 * <p>Client side entry point of the mod.</p>
 */
public class RoadArchitectClient implements ClientModInitializer {

    private static KeyBinding debugKey;

    @Override
    /**
     * Точка входа на клиенте. Регистрирует клавишу отладки и обработчик тиков.
     * <p>Client entry point that registers a debug key and tick handler.</p>
     */
    public void onInitializeClient() {
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
                } else {
                    ServerWorld world = client.getServer() == null ? null : client.getServer().getOverworld();
                    if (world != null) {
                        RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());
                        List<Node> nodes = new ArrayList<>(state.nodes().all().values());
                        client.setScreen(new RoadGraphDebugScreen(nodes, state.edges().allWithStatus()));
                    }
                }
            }
        });
    }
}