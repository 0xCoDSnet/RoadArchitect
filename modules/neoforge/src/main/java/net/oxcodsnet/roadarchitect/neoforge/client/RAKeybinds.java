package net.oxcodsnet.roadarchitect.neoforge.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = RoadArchitect.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class RAKeybinds {
    public static KeyBinding OPEN;


    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent e) {
        OPEN = new KeyBinding("key.roadarchitect.debug",
                InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H,
                "category.roadarchitect");
        e.register(OPEN);
    }
}
