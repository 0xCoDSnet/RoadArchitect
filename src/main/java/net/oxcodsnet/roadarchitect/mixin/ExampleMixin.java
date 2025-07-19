package net.oxcodsnet.roadarchitect.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Пример миксина сервера.
 * <p>Example server mixin.</p>
 */
@Mixin(MinecraftServer.class)
public class ExampleMixin {
    /**
     * Пример внедрения в начало метода {@code loadWorld}.
     * <p>Example injection at the start of {@code loadWorld}.</p>
     */
    @Inject(at = @At("HEAD"), method = "loadWorld")
    private void init(CallbackInfo info) {
        // This code is injected into the start of MinecraftServer.loadWorld()V
    }
}