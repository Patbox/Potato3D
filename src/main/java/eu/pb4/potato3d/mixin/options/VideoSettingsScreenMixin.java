package eu.pb4.potato3d.mixin.options;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import eu.pb4.potato3d.Potato3D;
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
    @WrapWithCondition(method = "addOptions", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/OptionsList;addBig(Lnet/minecraft/client/OptionInstance;)V"))
    private boolean hideTheseOptions(OptionsList instance, OptionInstance<?> option) {
        return !Potato3D.MODIFY_CLIENT_BEHAVIOUR;
    }


    @ModifyReturnValue(method = "displayOptions", at = @At("RETURN"))
    private static OptionInstance<?>[] hideBrokenDisplayOptions(OptionInstance<?>[] original, @Local(argsOnly = true) Options options) {
        if (Potato3D.MODIFY_CLIENT_BEHAVIOUR) return original;

        var list = new ArrayList<>(List.of(original));
        return list.toArray(OptionInstance[]::new);
    }

    @ModifyReturnValue(method = "qualityOptions", at = @At("RETURN"))
    private static OptionInstance<?>[] hideBrokenQualityOptions(OptionInstance<?>[] original, @Local(argsOnly = true) Options options) {
        if (Potato3D.MODIFY_CLIENT_BEHAVIOUR) return original;

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
        if (Potato3D.MODIFY_CLIENT_BEHAVIOUR) return original;

        var list = new ArrayList<>(List.of(original));
        list.remove(options.vignette());
        list.remove(options.chunkSectionFadeInTime());
        return list.toArray(OptionInstance[]::new);
    }
}
