package net.oxcodsnet.roadarchitect.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
/**
 * Пример миксина сервера.
 * <p>Example server mixin.</p>
 */
public class ExampleMixin {
        @Inject(at = @At("HEAD"), method = "loadWorld")
        private void init(CallbackInfo info) {
                // This code is injected into the start of MinecraftServer.loadWorld()V
        }
}