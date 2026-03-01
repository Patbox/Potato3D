package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.GpuFence;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoderBackend;
import com.mojang.blaze3d.systems.GpuQuery;
import com.mojang.blaze3d.systems.RenderPassBackend;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import eu.pb4.softwaregl.RGBA;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Supplier;

public class SoftCommandEncoder implements CommandEncoderBackend {
    public boolean isInRenderPass = false;

    @Override
    public RenderPassBackend createRenderPass(Supplier<String> label, GpuTextureView colorTexture, OptionalInt clearColor) {
        return createRenderPass(label, colorTexture, clearColor, null, OptionalDouble.empty());
    }

    @Override
    public RenderPassBackend createRenderPass(Supplier<String> label, GpuTextureView colorTexture, OptionalInt clearColor, @Nullable GpuTextureView depthTexture, OptionalDouble clearDepth) {
        if (clearColor.isPresent()) {
            ((SoftTexture) colorTexture.texture()).clear(colorTexture.baseMipLevel(), RGBA.colorARGB(clearColor.getAsInt()));
        }

        if (depthTexture != null && clearDepth.isPresent()) {
            ((SoftTexture) depthTexture.texture()).clear(depthTexture.baseMipLevel(), clearDepth.getAsDouble());
        }


        return new SoftRenderPass(this, label != null ? label.get() : null, (SoftTextureView) colorTexture, (SoftTextureView) depthTexture);
    }

    @Override
    public boolean isInRenderPass() {
        return this.isInRenderPass;
    }

    @Override
    public void clearColorTexture(GpuTexture colorTexture, int clearColor) {
        ((SoftTexture) colorTexture).clear(RGBA.colorARGB(clearColor));
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth) {
        ((SoftTexture) colorTexture).clear(RGBA.colorARGB(clearColor));
        ((SoftTexture) depthTexture).clear(clearDepth);
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture colorTexture, int clearColor, GpuTexture depthTexture, double clearDepth, int regionX, int regionY, int regionWidth, int regionHeight) {
        ((SoftTexture) colorTexture).clear(RGBA.colorARGB(clearColor), regionX, regionY, regionWidth, regionHeight);
        ((SoftTexture) depthTexture).clear(clearDepth, regionX, regionY, regionWidth, regionHeight);
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
    public GpuBuffer.MappedView mapBuffer(GpuBufferSlice buffer, boolean read, boolean write) {
        var buf = ((SoftBuffer) buffer.buffer()).data().slice((int) buffer.offset(), (int) (buffer.length()));
        buf.order(((SoftBuffer) buffer.buffer()).data().order());
        return new SoftBuffer.MappedView(buf);
    }

    @Override
    public void copyToBuffer(GpuBufferSlice source, GpuBufferSlice target) {
        var src = (SoftBuffer) source.buffer();
        var dest = (SoftBuffer) target.buffer();

        src.data().put((int) source.offset(), dest.data(), (int) target.offset(), (int) target.length());
    }

    @Override
    public void writeToTexture(GpuTexture destination, NativeImage source, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height, int sourceX, int sourceY) {
        var dest = ((SoftTexture) destination);

        switch (source.format()) {
            case RGBA -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        dest.setRGBA(mipLevel, destX + x, destY + y, Integer.reverseBytes(ARGB.toABGR(source.getPixel(x + sourceX, y + sourceY))));
                    }
                }
            }
            case RGB -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        dest.setRGBA(mipLevel, destX + x, destY + y, Integer.reverseBytes(ARGB.toABGR(source.getPixel(x + sourceX, y + sourceY)) << 8 | 0xFF));
                    }
                }
            }
            case LUMINANCE -> {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        var c = Byte.toUnsignedInt(source.getLuminanceOrAlpha(x + sourceX, y + sourceY));
                        dest.setRGBA(mipLevel, destX + x, destY + y, RGBA.colorARGB(0xFF, c, c, c));
                    }
                }
            }
            case LUMINANCE_ALPHA -> {
                // Todo
            }
        }
    }

    @Override
    public void writeToTexture(GpuTexture destination, ByteBuffer source, NativeImage.Format format, int mipLevel, int depthOrLayer, int destX, int destY, int width, int height) {
        var dest = ((SoftTexture) destination);

        switch (format) {
            case RGBA -> {
                for (int x = destX; x < destX + width; x++) {
                    for (int y = destY; y < destY + height; y++) {
                        dest.setRGBA(mipLevel, x, y, source.getInt());
                    }
                }
            }
            case RGB -> {
                for (int x = destX; x < destX + width; x++) {
                    for (int y = destY; y < destY + height; y++) {
                        dest.setRGBA(mipLevel, x, y,
                                Byte.toUnsignedInt(source.get()) << 24
                                        | Byte.toUnsignedInt(source.get()) << 16
                                        | Byte.toUnsignedInt(source.get()) << 8
                                        | 0xFF
                        );
                    }
                }
            }
            case LUMINANCE -> {
                for (int x = destX; x < destX + width; x++) {
                    for (int y = destY; y < destY + height; y++) {
                        dest.setRGBA(mipLevel, x, y,
                                ARGB.setBrightness(0xFFFFFFFF, Byte.toUnsignedInt(source.get()))
                        );
                    }
                }
            }
            case LUMINANCE_ALPHA -> {
                for (int x = destX; x < destX + width; x++) {
                    for (int y = destY; y < destY + height; y++) {
                        var base = ARGB.setBrightness(0xFFFFFFFF, Byte.toUnsignedInt(source.get()));
                        dest.setRGBA(mipLevel, x, y,
                                ARGB.color(Byte.toUnsignedInt(source.get()), base)
                        );
                    }
                }
            }
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture source, GpuBuffer destination, long offset, Runnable callback, int mipLevel) {
        var src = ((SoftTexture) source).buffers[mipLevel];
        var dest = ((SoftBuffer) destination).data();

        switch (source.getFormat()) {
            case RGBA8 -> {
                for (int i = 0; i < src.length; i++) {
                    dest.putInt((int) (offset + i * 4), Integer.reverseBytes(src[i]));
                }
            }
            case RED8 -> {
                for (int i = 0; i < src.length; i++) {
                    var color = src[i];
                    dest.put((int) (offset + i), (byte) ARGB.red(color));
                }
            }
            case RED8I -> {
                for (int i = 0; i < src.length; i++) {
                    var color = src[i];
                    dest.put((int) (offset + i * 2), (byte) ARGB.red(color));
                    dest.put((int) (offset + i * 2 + 1), (byte) ARGB.alpha(color));
                }
            }
            case DEPTH32 -> {
                var srcd = ((SoftTexture) source).depth[mipLevel];

                for (int i = 0; i < src.length; i++) {
                    dest.putFloat((int) (offset + i * 4), (float) (srcd[i]));
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
                dest.setDepth(mipLevel, x, y, src.getDepth(0, sourceX + x - destX, sourceY + y - destY));
            }
        }
    }

    @Override
    public void presentTexture(GpuTextureView texture) {
        var width = texture.getWidth(0);
        var height = texture.getHeight(0);
        var txt = (SoftTexture) texture.texture();
        GL11.glViewport(0, 0, width, height);
        GL11.glClearColor(0, 0, 0, 0);
        var swap = new int[txt.buffers[0].length];

        for (int i = 0; i < swap.length; i++) {
            var color = txt.buffers[0][i];
            swap[i] = Integer.reverseBytes(color | 0xFF);
        }

        GL11.glClearColor(0, 0, 0,0);
        GL11.glDrawPixels(width, height, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, swap);
    }

    @Override
    public GpuFence createFence() {
        return new GpuFence() {
            @Override
            public void close() {

            }

            @Override
            public boolean awaitCompletion(long timeoutMs) {
                return false;
            }
        };
    }

    @Override
    public GpuQuery timerQueryBegin() {
        return new GpuQuery() {
            @Override
            public OptionalLong getValue() {
                return OptionalLong.empty();
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public void timerQueryEnd(GpuQuery query) {

    }
}
