package eu.pb4.potato3d.blaze3d.texture;

public record DepthTexture(float[] data, int width, int height) implements TextureLike {
    public DepthTexture(int width, int height) {
        this(new float[width * height], width, height);
    }

    public float get(int x, int y) {
        return this.data[x + y * width];
    }

    public void set(int x, int y, float color) {
        this.data[x + y * width] = color;
    }
}
