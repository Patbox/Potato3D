package eu.pb4.potato3d.blaze3d.shader;

import eu.pb4.potato3d.RGBA;

public record EndShader(SampledTexture texture, int color, int time) implements SoftShader {
    @Override
    public int sample(int x, int y, float u, float v, int layer, int mip) {
        return RGBA.multiply(this.texture.sampleRawWrap((short) (x + time / 15), (short) (y + time / 25), layer, mip), color);
    }
}
