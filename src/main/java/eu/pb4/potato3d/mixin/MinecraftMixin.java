package eu.pb4.potato3d.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.renderpearl.api.device.GpuBackend;
import eu.pb4.potato3d.blaze3d.SoftBackend;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/PreferredGraphicsApi;getBackendsToTry()[Lcom/mojang/renderpearl/api/device/GpuBackend;"))
    private GpuBackend[] t(GpuBackend[] original){
        return new GpuBackend[] { new SoftBackend() };
    }
}
