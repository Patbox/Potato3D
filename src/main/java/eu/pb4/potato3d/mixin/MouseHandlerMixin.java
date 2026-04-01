package eu.pb4.potato3d.mixin;

import com.mojang.blaze3d.platform.Window;
import eu.pb4.potato3d.Potato3D;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(method = "getScaledXPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("HEAD"), cancellable = true)
    private static void replaceScalingX(Window window, double x, CallbackInfoReturnable<Double> cir) {
        if (window.getWidth() == Potato3D.framebufferWidth) return;
        cir.setReturnValue(Potato3D.remapMouseX(window, x));
    }

    @Inject(method = "getScaledYPos(Lcom/mojang/blaze3d/platform/Window;D)D", at = @At("HEAD"), cancellable = true)
    private static void replaceScalingY(Window window, double x, CallbackInfoReturnable<Double> cir) {
        if (window.getWidth() == Potato3D.framebufferHeight) return;
        cir.setReturnValue(Potato3D.remapMouseY(window, x));
    }
}
