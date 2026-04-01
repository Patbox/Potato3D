package eu.pb4.potato3d.blaze3d.shader;

import com.mojang.blaze3d.textures.AddressMode;
import eu.pb4.potato3d.blaze3d.SoftSampler;
import eu.pb4.potato3d.blaze3d.SoftTextureView;
import net.minecraft.util.Mth;

public record SampledTexture(SoftTextureView texture, SoftSampler sampler) implements SoftShader {
    public int sample(int _x, int _y, float u, float v, int layer, int mip) {

        var width = texture.texture().getWidth(texture.baseMipLevel() + mip);
        var height = texture.texture().getHeight(texture.baseMipLevel() + mip);

        var x = (int) (u * width);
        var y = (int) (v * height);

        if (sampler.getAddressModeU() == AddressMode.REPEAT) {
            x = x % width;
            if (x < 0) {
                x = width + x;
            }
        } else {
            x = Mth.clamp(x, 0, width - 1);
        }

        if (sampler.getAddressModeV() == AddressMode.REPEAT) {
            y = y % height;
            if (y < 0) {
                y = height + y;
            }
        } else {
            y = Mth.clamp(y, 0, height - 1);
        }

        return texture.texture().getRGBA(layer, texture.baseMipLevel() + mip, x, y);
    }

    public int sampleRaw(short u, short v, int layer, int mip) {
        var width = texture.texture().getWidth(texture.baseMipLevel());
        var height = texture.texture().getHeight(texture.baseMipLevel());

        var x = Mth.clamp(u, 0, width - 1);
        var y = Mth.clamp(v, 0, height - 1);

        return texture.texture().getRGBA(layer, texture.baseMipLevel() + mip, x, y);
    }

    public int sampleRawWrap(short u, short v, int layer, int mip) {
        var width = texture.texture().getWidth(texture.baseMipLevel());
        var height = texture.texture().getHeight(texture.baseMipLevel());

        var x = Math.abs(u % width);
        var y = Math.abs(v % height);

        return texture.texture().getRGBA(layer, texture.baseMipLevel() + mip, x, y);
    }
}
