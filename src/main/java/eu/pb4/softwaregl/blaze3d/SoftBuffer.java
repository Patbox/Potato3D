package eu.pb4.softwaregl.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SoftBuffer extends GpuBuffer implements GpuBuffer.MappedView {
    private final ByteBuffer buffer;
    private boolean closed = false;

    public SoftBuffer(String label, @Usage int usage, long size) {
        super(usage, size);
        this.buffer = ByteBuffer.allocateDirect((int) size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    public SoftBuffer(String label, @Usage int usage, ByteBuffer data) {
        this(label, usage, data.remaining());
        this.buffer.put(0, data, 0, data.remaining());
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public ByteBuffer data() {
        return this.buffer;
    }

    @Override
    public void close() {
        this.buffer.clear();
        this.closed = true;
    }

    public record MappedView(ByteBuffer data) implements GpuBuffer.MappedView {
        @Override
        public void close() {
            data.position(0);
        }
    }
}
