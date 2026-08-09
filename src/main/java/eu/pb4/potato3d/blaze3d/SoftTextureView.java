package eu.pb4.potato3d.blaze3d;


import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.common.BaseGpuTextureView;

public class SoftTextureView extends BaseGpuTextureView {
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
