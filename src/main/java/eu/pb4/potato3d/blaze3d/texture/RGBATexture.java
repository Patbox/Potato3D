package eu.pb4.potato3d.blaze3d.texture;

import java.util.Arrays;

public record RGBATexture(int[] data, int width, int height, int layerHeight) implements TextureLike {
    public RGBATexture(int width, int height, int layerHeight) {
        this(new int[width * layerHeight], width, height, layerHeight);
        Arrays.fill(data, 0x0);
    }

    public int get(int x, int y) {
        return this.data[x + y * width];
    }

    public void set(int x, int y, int color) {
        this.data[x + y * width] = color;
    }

    public int get(int depth, int x, int y) {
        return this.data[x + (y + depth * height) * width];
    }

    public void set(int depth, int x, int y, int color) {
        this.data[x + (y + depth * height) * width] = color;
    }
}
