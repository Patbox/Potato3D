package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import eu.pb4.potato3d.blaze3d.texture.DepthTexture;
import eu.pb4.potato3d.blaze3d.texture.RGBATexture;
import eu.pb4.potato3d.blaze3d.texture.TextureLike;

import java.util.Arrays;

public class SoftTexture extends GpuTexture {
    public final TextureLike[] texture;
    public final RGBATexture[] rgba;
    public final DepthTexture[] depth;

    public SoftTexture(@Usage int usage, String label, TextureFormat format, int width, int height, int depthOrLayers, int mipLevels) {
        super(usage, label, format, width, height, depthOrLayers, mipLevels);
        var layerHeight = height * depthOrLayers;

        this.texture = new TextureLike[mipLevels];
        if (format == TextureFormat.DEPTH32) {
            this.depth = new DepthTexture[mipLevels];
            this.rgba = new RGBATexture[0];
            for (int i = 0; i < mipLevels; i++) {
                this.texture[i] = this.depth[i] = new DepthTexture(width >>> i, height >>> i);
            }
        } else {
            this.rgba = new RGBATexture[mipLevels];
            this.depth = new DepthTexture[0];
            for (int i = 0; i < mipLevels; i++) {
                this.texture[i] = this.rgba[i] = new RGBATexture(width >>> i, height >>> i, layerHeight >>> i);
            }
        }

    }

    public int getRGBA(int mip, int x, int y) {
        return this.rgba[mip].get(x, y);
    }

    public int getRGBA(int depth, int mip, int x, int y) {
        return this.rgba[mip].get(depth, x, y);
    }

    public void setRGBA(int mip, int x, int y, int color) {
        this.rgba[mip].set(x, y, color);
    }

    public void setRGBA(int depth, int mip, int x, int y, int color) {
        this.rgba[mip].set(depth, x, y, color);
    }

    public void setDepth(int mip, int x, int y, double color) {
        this.depth[mip].set(x, y, (float) color);
    }

    public float getDepth(int mip, int x, int y) {
        return this.depth[mip].get(x, y);
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
            Arrays.fill(this.rgba[i].data(), clearColor);
        }
    }

    public void clear(int mip, int clearColor) {
        Arrays.fill(this.rgba[mip].data(), clearColor);
    }

    public void clear(int mip, double clearDepth) {
        Arrays.fill(this.depth[mip].data(), (float) clearDepth);
    }

    public void clear(double clearDepth) {
        for (int i = 0; i < this.getMipLevels(); i++) {
            Arrays.fill(this.depth[i].data(), (float) clearDepth);
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
