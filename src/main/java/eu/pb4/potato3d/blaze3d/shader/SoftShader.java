package eu.pb4.potato3d.blaze3d.shader;

public interface SoftShader {
    int sample(int x, int y, float u, float v, int layer, int mip);
}