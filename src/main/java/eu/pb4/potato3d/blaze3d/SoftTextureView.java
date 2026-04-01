package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;

public class SoftTextureView extends GpuTextureView {
    private final SoftTexture texturex;

    protected SoftTextureView(GpuTexture texture, int baseMipLevel, int mipLevels) {
        super(texture, baseMipLevel, mipLevels);
        this.texturex = (SoftTexture) texture;
    }

    @Override
    public SoftTexture texture() {
        return this.texturex;
    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }
}
