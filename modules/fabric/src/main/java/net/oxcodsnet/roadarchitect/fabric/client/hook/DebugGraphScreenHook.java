package net.oxcodsnet.roadarchitect.fabric.client.hook;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.world.ServerWorld;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class DebugGraphScreenHook {
    private DebugGraphScreenHook() {}

    public static void init() {
        KeyBinding openDebugKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.roadarchitect.debug",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.roadarchitect"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openDebugKey.wasPressed()) {
                if (mc.currentScreen instanceof RoadGraphDebugScreenVanilla) {
                    mc.setScreen(null);
                    continue;
                }

                ServerWorld world = mc.getServer() == null ? null : mc.getServer().getOverworld();
                if (world == null) continue;

                RoadGraphState state = RoadGraphState.get(world);

                List<Node> nodes  = new ArrayList<>(state.nodes().all().values());
                Collection<EdgeStorage.Edge> edges = state.edges().all().values();
                MinecraftClient.getInstance().setScreen(new RoadGraphDebugScreenVanilla(nodes, edges));
            }
        });
    }
}
