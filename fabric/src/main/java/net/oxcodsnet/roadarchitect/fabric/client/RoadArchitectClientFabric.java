package net.oxcodsnet.roadarchitect.fabric.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.fabric.client.gui.MyLevelLoadingScreen;
import net.oxcodsnet.roadarchitect.fabric.client.gui.RoadGraphDebugScreen;
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
public class RoadArchitectClientFabric implements ClientModInitializer {

    private static KeyBinding debugKey;

    @Override
    public void onInitializeClient() {

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof LevelLoadingScreen vanilla) {
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

                RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());

                List<Node> nodes  = new ArrayList<>(state.nodes().all().values());
                Collection<EdgeStorage.Edge> edges = state.edges().all().values();

                client.setScreen(new RoadGraphDebugScreen(nodes, edges));
            }
        });
    }
}
