package eu.pb4.softwaregl.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.opengl.GlBackend;
import com.mojang.blaze3d.systems.GpuBackend;
import eu.pb4.softwaregl.blaze3d.SoftBackend;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Definition(id = "GpuBackend", type = GpuBackend.class)
    @Definition(id = "GlBackend", type = GlBackend.class)
    @Expression("new GpuBackend[]{ new GlBackend() }")
    @ModifyExpressionValue(method = "<init>", at = @At("MIXINEXTRAS:EXPRESSION"))
    private GpuBackend[] t(GpuBackend[] original){
        return new GpuBackend[] { new SoftBackend() };
    }
}
