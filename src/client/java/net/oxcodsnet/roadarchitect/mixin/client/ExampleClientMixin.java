package net.oxcodsnet.roadarchitect.mixin.client;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
/**
 * Пример клиентского миксина.
 * <p>Example client mixin.</p>
 */
public class ExampleClientMixin {
        @Inject(at = @At("HEAD"), method = "run")
        private void init(CallbackInfo info) {
                // This code is injected into the start of MinecraftClient.run()V
        }
}