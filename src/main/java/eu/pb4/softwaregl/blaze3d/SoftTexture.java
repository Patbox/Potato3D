package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import eu.pb4.softwaregl.RGBA;
import net.minecraft.util.RandomSource;

import java.util.Arrays;

public class SoftTexture extends GpuTexture {
    private final int width;
    public int[][] buffers;
    public float[][] depth;

    public SoftTexture(@Usage int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        this.buffers = new int[mipLevels][];
        this.depth = new float[mipLevels][];
        var r = RandomSource.create();
        int rand = 0;//RGBA.colorARGB(0xFF, r.nextInt(255), r.nextInt(255), r.nextInt(255));
        for (int i = 0; i < mipLevels; i++) {
            this.buffers[i] = new int[(width >> i) * (height >> i)];
            Arrays.fill(this.buffers[i], rand);
            this.depth[i] = new float[(width >> i) * (height >> i)];
            Arrays.fill(this.depth[i], 0);
        }
        this.width = width;
    }

    public int getRGBA(int mip, int x, int y) {
        return this.buffers[mip][x + y * getWidth(mip)];
    }

    public void setRGBA(int mip, int x, int y, int color) {
        this.buffers[mip][x + y * getWidth(mip)] = color;
    }

    public void setDepth(int mip, int x, int y, double color) {
        this.depth[mip][x + y * getWidth(mip)] = (float) color;
    }

    public float getDepth(int mip, int x, int y) {
        return this.depth[mip][x + y * getWidth(mip)];
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    public void clear(int clearColor) {
        for (int i = 0; i < this.getMipLevels(); i++) {
            Arrays.fill(this.buffers[i], clearColor);
        }
    }

    public void clear(int mip, int clearColor) {
        Arrays.fill(this.buffers[mip], clearColor);
    }

    public void clear(int mip, double clearDepth) {
        clear(mip, (int) clearDepth * 1024);
    }

    public void clear(double clearDepth) {
        for (int i = 0; i < this.getMipLevels(); i++) {
            Arrays.fill(this.depth[i], (float) clearDepth);
        }
    }

    public void clear(int clearColor, int regionX, int regionY, int regionWidth, int regionHeight) {
        for (int i = 0; i < this.getMipLevels(); i++) {
            clear(i, clearColor, regionX, regionY, regionWidth, regionHeight);
        }
    }
    public void clear(int mip, int clearColor, int regionX, int regionY, int regionWidth, int regionHeight) {
        for (int x = regionX << mip; x < (regionX + regionWidth) << mip; x++) {
            for (int y = regionY << mip; y < (regionY + regionHeight) << mip; y++) {
                this.setRGBA(mip, x, y, clearColor);
            }
        }
    }

    public void clear(double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight) {
        for (int i = 0; i < this.getMipLevels(); i++) {
            clear(i, clearDepth, regionX, regionY, regionWidth, regionHeight);
        }
    }
    public void clear(int mip, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight) {
        for (int x = regionX << mip; x < (regionX + regionWidth) << mip; x++) {
            for (int y = regionY << mip; y < (regionY + regionHeight) << mip; y++) {
                this.setDepth(mip, x, y, clearDepth);
            }
        }
    }
}
