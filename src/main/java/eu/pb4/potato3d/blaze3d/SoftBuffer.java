package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class SoftBuffer extends GpuBuffer  {
    private ByteBuffer buffer;
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

    public ByteBuffer data() {
        return this.buffer;
    }

    @Override
    public void close() {
        this.buffer = null;
        this.closed = true;
    }

    @Override
    public GpuBufferSlice.MappedView map(long offset, long length, boolean read, boolean write) {
        var buf = this.buffer.slice((int) offset, (int) length);
        buf.order(this.buffer.order());
        return new GpuBufferSlice.MappedView(new GpuBufferSlice(this, offset, length), buf, () -> {});
    }
}
