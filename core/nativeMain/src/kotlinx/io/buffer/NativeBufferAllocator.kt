package kotlinx.io.buffer

import kotlinx.cinterop.*

actual object PlatformBufferAllocator : BufferAllocator by NativeBufferAllocator(nativeHeap)

internal inline class NativeBufferAllocator(private val placement: NativeFreeablePlacement) : BufferAllocator {
    override fun allocate(size: Int): Buffer = Buffer(placement.allocArray(size), size)

    override fun free(instance: Buffer) {
        placement.free(instance.pointer)
    }
}
