package eu.pb4.potato3d.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.resources.SplashManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Locale;
import java.util.stream.Stream;

@Mixin(SplashManager.class)
public class SplashManagerMixin {
    @ModifyExpressionValue(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Ljava/util/List;", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private Stream<String> thereIsNoOpenGl(Stream<String> original) {
        return original.map(x -> x.toLowerCase(Locale.ROOT).contains("opengl") ? "Software Rendered!" : x);
    }
}
