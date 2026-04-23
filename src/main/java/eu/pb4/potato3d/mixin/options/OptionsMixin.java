package eu.pb4.potato3d.mixin.options;

import eu.pb4.potato3d.Potato3D;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Options.class)
public class OptionsMixin {
    @Shadow
    @Nullable
    public String fullscreenVideoModeString;

    @Shadow
    @Final
    private OptionInstance<Integer> mipmapLevels;

    @Shadow
    @Final
    private OptionInstance<Integer> renderDistance;

    @Shadow
    @Final
    private OptionInstance<GraphicsPreset> graphicsPreset;

    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Ljava/lang/Object;Lnet/minecraft/client/OptionInstance$ValueUpdateListener;)V"))
    private void modifyOptions(Args args) {
        if (!Potato3D.MODIFY_CLIENT_BEHAVIOUR) {
            return;
        }
        var key = args.get(0);
        if (key.equals("options.renderDistance")) {
            args.set(3, new OptionInstance.IntRange(2, 6, false));
            args.set(4, 3);
        }
    }

    @Inject(method = "load", at = @At("HEAD"))
    private void modifyInitialOptions(CallbackInfo ci) {
        if (!Potato3D.MODIFY_CLIENT_BEHAVIOUR) {
            return;
        }
        this.graphicsPreset.set(GraphicsPreset.FAST);
        this.renderDistance.set(3);
    }

    @Inject(method = "load", at = @At("RETURN"))
    private void modifyLoadedOptions(CallbackInfo ci) {
        if (!Potato3D.MODIFY_CLIENT_BEHAVIOUR) {
            return;
        }
        this.fullscreenVideoModeString = null;
        this.mipmapLevels.set(1);
        this.graphicsPreset.set(GraphicsPreset.CUSTOM);
    }
}
