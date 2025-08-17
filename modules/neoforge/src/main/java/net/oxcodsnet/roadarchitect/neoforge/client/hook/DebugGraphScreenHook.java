package net.oxcodsnet.roadarchitect.neoforge.client.hook;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.server.world.ServerWorld;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.client.gui.RoadGraphDebugScreenVanilla;
import net.oxcodsnet.roadarchitect.storage.EdgeStorage;
import net.oxcodsnet.roadarchitect.storage.RoadGraphState;
import net.oxcodsnet.roadarchitect.storage.components.Node;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@EventBusSubscriber(modid = "roadarchitect", value = Dist.CLIENT)
public final class DebugGraphScreenHook {
    private static KeyBinding OPEN;

    @SubscribeEvent
    public static void keys(RegisterKeyMappingsEvent e) {
        OPEN = new KeyBinding("key.roadarchitect.debug", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.roadarchitect");
        e.register(OPEN);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key e) {
        if (OPEN != null && OPEN.wasPressed()) {
            MinecraftClient mc = MinecraftClient.getInstance();

            if (mc.currentScreen instanceof RoadGraphDebugScreenVanilla) {
                mc.setScreen(null);
                return;
            }

            ServerWorld world = mc.getServer() == null ? null : mc.getServer().getOverworld();
            if (world == null) return;

            RoadGraphState state = RoadGraphState.get(world, RoadArchitect.CONFIG.maxConnectionDistance());

            List<Node> nodes  = new ArrayList<>(state.nodes().all().values());
            Collection<EdgeStorage.Edge> edges = state.edges().all().values();
            mc.setScreen(new RoadGraphDebugScreenVanilla(nodes, edges));
        }
    }
}
