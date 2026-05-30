package eu.pb4.potato3d.blaze3d;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.TransientMemory;
import com.mojang.blaze3d.util.TransientBlockAllocator;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;

public class SoftTransientMemory implements TransientMemory {
    private final TransientBlockAllocator<Long> cpuBlockAllocator = new TransientBlockAllocator<>(524288L, 16L, TransientBlockAllocator.Allocator.create(MemoryUtil::nmemAlloc, MemoryUtil::nmemFree));

    @Override
    public ByteBuffer allocateCpu(final long size, final long alignment, final long minimumAllocation, final long elementSize) {
        assert size <= 2147483647L;

        TransientBlockAllocator.Allocation<Long> alloc = this.cpuBlockAllocator.allocate(size, alignment, minimumAllocation, elementSize);
        return MemoryUtil.memByteBuffer(alloc.block() + alloc.offset(), (int) alloc.size());
    }

    public void rotate() {
        this.cpuBlockAllocator.rotate().run();
    }


    @Override
    public GpuBufferSlice.MappedView allocateStaging(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        return new SoftBuffer("AllocateStaging", usage, size).slice().map(true, true);
    }

    @Override
    public GpuBufferSlice allocateGpu(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        return new SoftBuffer("AllocateGpu", usage, size).slice();
    }

    @Override
    public GpuBufferSlice.MappedView allocateGpuMapped(long size, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        return new SoftBuffer("AllocateGpuMapped", usage, size).map(0, elementSize, true, true);
    }

    @Override
    public GpuBufferSlice uploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        var size = 0l;
        for (var x : data) {
            size += x.remaining();
        }

        var buf = new SoftBuffer("UploadStaging", usage, size);
        for (var x : data) {
            buf.data().put(x);
        }
        buf.data().position(0);

        return buf.slice();
    }

    @Override
    public GpuBufferSlice uploadGpu(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage, long minimumAllocation, long elementSize) {
        var size = 0l;
        for (var x : data) {
            size += x.remaining();
        }

        var buf = new SoftBuffer("UploadStaging", usage, size);
        for (var x : data) {
            buf.data().put(x);
        }
        buf.data().position(0);

        return buf.slice();
    }

    @Override
    public List<GpuBufferSlice> multiUploadStaging(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
        return data.stream().map(x -> new SoftBuffer("MultiUpload", usage, x).slice()).toList();
    }

    @Override
    public List<GpuBufferSlice> multiUploadGpu(List<ByteBuffer> data, long alignment, @GpuBuffer.Usage int usage) {
        return data.stream().map(x -> new SoftBuffer("MultiUpload", usage, x).slice()).toList();
    }
}
