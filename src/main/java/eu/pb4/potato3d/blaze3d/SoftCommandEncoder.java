package eu.pb4.potato3d.blaze3d;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.buffers.TransientMemory;
import com.mojang.renderpearl.api.commands.GpuFence;
import com.mojang.renderpearl.api.commands.GpuQueryPool;
import com.mojang.renderpearl.api.commands.RenderPassDescriptor;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.backend.api.CommandEncoderBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import eu.pb4.potato3d.RGBA;
import net.minecraft.util.ARGB;
import org.joml.Vector4fc;

import java.nio.ByteBuffer;

public class SoftCommandEncoder implements CommandEncoderBackend {
    private final SoftDevice device;
    public boolean isInRenderPass = false;
    public final SoftTransientMemory memory = new SoftTransientMemory();

    public SoftCommandEncoder(SoftDevice device) {
        this.device = device;
    }

    @Override
    public void submit() {

    }

    @Override
    public TransientMemory transientMemory() {
        return this.memory;
    }

    @Override
    public RenderPassBackend createRenderPass(RenderPassDescriptor descriptor) {
        SoftTextureView colorTexture = null;
        SoftTextureView depthTexture = null;

        for (var color : descriptor.colorAttachments()) {
            if (color.clearValue().isPresent()) {
                ((SoftTexture) color.textureView().texture()).clear(color.textureView().baseMipLevel(), RGBA.fromVector4f(color.clearValue().get()));
            }
            colorTexture = (SoftTextureView) color.textureView();
        }

        if (descriptor.depthAttachment() != null) {
            if (descriptor.depthAttachment().clearValue().isPresent()) {
                ((SoftTexture) descriptor.depthAttachment().textureView().texture()).clear(descriptor.depthAttachment().textureView().baseMipLevel(),
                        descriptor.depthAttachment().clearValue().getAsDouble());
            }
            depthTexture = (SoftTextureView) descriptor.depthAttachment().textureView();
        }


        return new SoftRenderPass(this, descriptor.label().get(), colorTexture, depthTexture, descriptor.renderArea());
    }

    @Override
    public void submitRenderPass() {

    }

    @Override
    public void clearColorTexture(GpuTexture colorTexture, Vector4fc clearColor) {
        ((SoftTexture) colorTexture).clear(RGBA.fromVector4f(clearColor));
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth) {
        ((SoftTexture) colorTexture).clear(RGBA.fromVector4f(clearColor));
        ((SoftTexture) depthTexture).clear(clearDepth);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, Vector4fc clearColor, GpuTexture depthTexture, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight, int mipLevel) {
        ((SoftTexture) colorTexture).clear(mipLevel, RGBA.fromVector4f(clearColor), regionX, regionY, regionWidth, regionHeight);
        ((SoftTexture) depthTexture).clear(mipLevel, clearDepth, regionX, regionY, regionWidth, regionHeight);
    }

    @Override
    public void clearDepthTexture(GpuTexture depthTexture, double clearDepth) {
        ((SoftTexture) depthTexture).clear(clearDepth);
    }

    @Override
    public void writeToBuffer(GpuBufferSlice destination, ByteBuffer data) {
        ((SoftBuffer) destination.buffer()).data().put(Math.toIntExact(destination.offset()), data, 0, data.remaining());
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        var src = (SoftBuffer) source.buffer();
        var dest = (SoftBuffer) target.buffer();

        dest.data().put((int) target.offset(), src.data(), (int) source.offset(), (int) source.length());
    }


    @Override
    public void writeToTexture(GpuTexture destination, ByteBuffer source, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height) {
        var dest = ((SoftTexture) destination);

        switch (destination.getFormat()) {
            case RGBA8_UNORM -> {
                for (int y = destY; y < destY + height; y++) {
                    for (int x = destX; x < destX + width; x++) {
                        dest.setRGBA(depthOrLayer, mipLevel, x, y, Integer.reverseBytes(source.getInt()));
                    }
                }
            }
            case RGB8_UNORM -> {
                for (int y = destY; y < destY + height; y++) {
                    for (int x = destX; x < destX + width; x++) {
                        dest.setRGBA(depthOrLayer, mipLevel, x, y, Integer.reverseBytes(
                                Byte.toUnsignedInt(source.get()) << 24
                                        | Byte.toUnsignedInt(source.get()) << 16
                                        | Byte.toUnsignedInt(source.get()) << 8
                                        | 0xFF)
                        );
                    }
                }
            }
            case R8_UNORM -> {
                for (int y = destY; y < destY + height; y++) {
                    for (int x = destX; x < destX + width; x++) {
                        dest.setRGBA(depthOrLayer, mipLevel, x, y,
                                ARGB.setBrightness(0xFFFFFFFF, Byte.toUnsignedInt(source.get()))
                        );
                    }
                }
            }
            case RG8_UNORM -> {
                for (int y = destY; y < destY + height; y++) {
                    for (int x = destX; x < destX + width; x++) {
                        var base = ARGB.setBrightness(0xFFFFFFFF, Byte.toUnsignedInt(source.get()));
                        dest.setRGBA(depthOrLayer, mipLevel, x, y,
                                ARGB.color(Byte.toUnsignedInt(source.get()), base)
                        );
                    }
                }
            }
        }
    }

    @Override
    public void copyBufferToTexture(GpuBufferSlice source, int sourceX, int sourceY, int sourceWidth, int sourceHeight, GpuTexture destination, int destX, int destY, int width, int height, int mipLevel, int depthOrLayer) {
        var dest = ((SoftTexture) destination);

        var buf = ((SoftBuffer) source.buffer()).data();

        switch (destination.getFormat()) {
            case RGBA8_UNORM -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        var i = ((x + sourceX) + (y + sourceY) * sourceWidth) * 4;

                        dest.setRGBA(depthOrLayer, mipLevel, destX + x, destY + y, Integer.reverseBytes(buf.getInt(i)));
                    }
                }
            }
            case RGB8_UNORM -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        var i = (x + sourceX + (y + sourceY) * sourceWidth) * 3;

                        dest.setRGBA(depthOrLayer, mipLevel, destX + x, destY + y, Integer.reverseBytes(buf.getInt(i) & 0xFFFFFF) << 8 | 0xFF);
                    }
                }
            }
            case R8_UNORM -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        var i = (x + sourceX + (y + sourceY) * sourceWidth);
                        var c = Byte.toUnsignedInt(buf.get(i));
                        dest.setRGBA(depthOrLayer, mipLevel, destX + x, destY + y, RGBA.colorARGB(0xFF, c, c, c));
                    }
                }
            }
            case RG8_UNORM -> {
                // Todo
            }
        }
    }


    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel) {
        var src = ((SoftTexture) source).rgba[mipLevel].data();
        var dest = ((SoftBuffer) destination).data();

        switch (source.getFormat().name().split("_")[0]) {
            case "RGBA8" -> {
                for (int i = 0; i < src.length; i++) {
                    dest.putInt((int) (offset + i * 4), Integer.reverseBytes(src[i]));
                }
            }
            case "RGB8" -> {
                for (int i = 0; i < src.length; i++) {
                    dest.putInt((int) (offset + i * 3), Integer.reverseBytes(src[i]));
                }
            }
            case "R8" -> {
                for (int i = 0; i < src.length; i++) {
                    var color = src[i];
                    dest.put((int) (offset + i), (byte) ARGB.red(color));
                }
            }
            case "RG8" -> {
                for (int i = 0; i < src.length; i++) {
                    var color = src[i];
                    dest.put((int) (offset + i * 2), (byte) ARGB.red(color));
                    dest.put((int) (offset + i * 2 + 1), (byte) ARGB.alpha(color));
                }
            }
            case "D32" -> {
                var srcd = ((SoftTexture) source).depth[mipLevel].data();

                for (int i = 0; i < src.length; i++) {
                    dest.putFloat((int) (offset + i * 4), srcd[i]);
                }
            }
        }

        callback.run();
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel, int x, int y, int width, int height) {
        callback.run();
    }

    @Override
    public void copyTextureToTexture(GpuTexture source, GpuTexture destination, int mipLevel, int destX, int destY, int sourceX, int sourceY, int width, int height) {
        var dest = ((SoftTexture) destination);
        var src = ((SoftTexture) source);

        for (int x = destX; x < destX + width; x++) {
            for (int y = destY; y < destY + height; y++) {
                dest.setRGBA(mipLevel, x, y, src.getRGBA(0, sourceX + x - destX, sourceY + y - destY));
            }
        }

        if (dest.depth.length != 0 && src.depth.length != 0) {
            for (int x = destX; x < destX + width; x++) {
                for (int y = destY; y < destY + height; y++) {
                    dest.setDepth(mipLevel, x, y, src.getDepth(0, sourceX + x - destX, sourceY + y - destY));
                }
            }
        }
    }

    @Override
    public GpuFence createFence() {
        return new GpuFence() {
            @Override
            public void close() {

            }

            @Override
            public boolean awaitCompletion(long timeoutMs) {
                return true;
            }
        };
    }

    @Override
    public void writeTimestamp(GpuQueryPool pool, int index) {

    }
}
