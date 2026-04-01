package eu.pb4.softwaregl.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(VideoSettingsScreen.class)
public class VideoSettingsScreenMixin {
    @WrapOperation(method = "addOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V"))
    private void hideTheseOptions(OptionsList instance, OptionInstance<?> option, Operation<Void> original) {}


    @ModifyReturnValue(method = "displayOptions", at = @At("RETURN"))
    private static OptionInstance<?>[] hideBrokenDisplayOptions(OptionInstance<?>[] original, @Local(argsOnly = true) Options options) {
        var list = new ArrayList<>(List.of(original));
        return list.toArray(OptionInstance[]::new);
    }

    @ModifyReturnValue(method = "qualityOptions", at = @At("RETURN"))
    private static OptionInstance<?>[] hideBrokenQualityOptions(OptionInstance<?>[] original, @Local(argsOnly = true) Options options) {
        var list = new ArrayList<>(List.of(original));
        list.remove(options.mipmapLevels());
        list.remove(options.menuBackgroundBlurriness());
        list.remove(options.cloudRange());
        list.remove(options.improvedTransparency());
        list.remove(options.textureFiltering());
        list.remove(options.maxAnisotropyBit());
        return list.toArray(OptionInstance[]::new);
    }

    @ModifyReturnValue(method = "preferenceOptions", at = @At("RETURN"))
    private static OptionInstance<?>[] hideBrokenPreferenceOptions(OptionInstance<?>[] original, @Local(argsOnly = true) Options options) {
        var list = new ArrayList<>(List.of(original));
        list.remove(options.vignette());
        list.remove(options.chunkSectionFadeInTime());
        return list.toArray(OptionInstance[]::new);
    }
}
