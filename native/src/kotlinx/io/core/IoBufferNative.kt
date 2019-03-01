@file:Suppress("DEPRECATION_ERROR", "DEPRECATION", "RedundantModalityModifier")
package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.bits.*
import kotlinx.io.core.internal.*
import kotlinx.io.pool.*
import platform.posix.*
import kotlin.contracts.*
import kotlin.native.concurrent.*

@PublishedApi
internal val MAX_SIZE: size_t = size_t.MAX_VALUE

@Suppress("DIFFERENT_NAMES_FOR_THE_SAME_PARAMETER_IN_SUPERTYPES")
@Deprecated("Use Buffer instead.", replaceWith = ReplaceWith("Buffer", "kotlinx.io.core.Buffer"))
actual class IoBuffer internal constructor(
    internal var content: CPointer<ByteVar>,
    private val contentCapacity: Int,
    origin: ChunkBuffer?
) : Input, Output, ChunkBuffer(Memory.of(content, contentCapacity), origin) {
    override fun discard(n: Long): Long {
        return (this as Buffer).discard(n)
    }

    internal var refCount = 1

    constructor(content: CPointer<ByteVar>, contentCapacity: Int) : this(content, contentCapacity, null)

    override val endOfInput: Boolean get() = !canRead()

    init {
        require(contentCapacity >= 0) { "contentCapacity shouln't be negative: $contentCapacity" }
        require(this !== origin) { "origin shouldn't point to itself" }
    }

    @Deprecated(
        "Not supported anymore. All operations are big endian by default.",
        level = DeprecationLevel.ERROR
    )
    final override var byteOrder: ByteOrder get() = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            if (newOrder != ByteOrder.BIG_ENDIAN) {
                throw IllegalArgumentException("Only BIG_ENDIAN is supported")
            }
        }

    final override fun peekTo(destination: Buffer, offset: Int, min: Int, max: Int): Int {
        return (this as Buffer).peekTo(destination, offset, min, max)
    }

    final override fun tryPeek(): Int {
        return tryPeekByte()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readShort(): Short {
        return (this as Buffer).readShort()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeShort(v: Short) {
        (this as Buffer).writeShort(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readInt(): Int {
        return (this as Buffer).readInt()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeInt(v: Int) {
        (this as Buffer).writeInt(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFloat(): Float {
        return (this as Buffer).readFloat()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFloat(v: Float) {
        (this as Buffer).writeFloat(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readDouble(): Double {
        return (this as Buffer).readDouble()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeDouble(v: Double) {
        (this as Buffer).writeDouble(v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        (this as Buffer).readFully(dst, offset, length.toIntOrFail("length"))
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long) {
        (this as Buffer).writeFully(src, offset, length.toIntOrFail("length"))
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: ByteArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: ShortArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: IntArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: LongArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)

    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: FloatArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        (this as Buffer).readFully(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readFully(dst: IoBuffer, length: Int) {
        (this as Buffer).readFully(dst, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: IoBuffer, length: Int): Int {
        return (this as Buffer).readAvailable(dst, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
        return (this as Buffer).readAvailable(dst, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long {
        return (this as Buffer).readAvailable(dst, offset, length.toIntOrFail("length")).toLong()
    }

    /**
     * Apply [block] to a native pointer for writing to the buffer. Lambda should return number of bytes were written.
     * @return number of bytes written
     */
    fun writeDirect(block: (CPointer<ByteVar>) -> Int): Int {
        val rc = block((content + writePosition)!!)
        check(rc >= 0) { "block function should return non-negative results: $rc" }
        check(rc <= writeRemaining)
        commitWritten(rc)
        return rc
    }

    /**
     * Apply [block] to a native pointer for reading from the buffer. Lambda should return number of bytes were read.
     * @return number of bytes read
     */
    fun readDirect(block: (CPointer<ByteVar>) -> Int): Int {
        val rc = block((content + readPosition)!!)
        check(rc >= 0) { "block function should return non-negative results: $rc" }
        check(rc <= readRemaining) { "result value is too large: $rc > $readRemaining" }
        discard(rc)
        return rc
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: ByteArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: ShortArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: IntArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: LongArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: FloatArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: DoubleArray, offset: Int, length: Int) {
        (this as Buffer).writeFully(src, offset, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeFully(src: IoBuffer, length: Int) {
        (this as Buffer).writeFully(src, length)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun fill(n: Long, v: Byte) {
        (this as Buffer).fill(n, v)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun readLong(): Long {
        return (this as Buffer).readLong()
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun writeLong(v: Long) {
        (this as Buffer).writeLong(v)
    }

    final override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        val idx = appendChars(csq ?: "null", start, end)
        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    final override fun append(csq: CharSequence?): Appendable {
        return if (csq == null) append("null") else append(csq, 0, csq.length)
    }

    final override fun append(csq: CharArray, start: Int, end: Int): Appendable {
        val idx = appendChars(csq, start, end)

        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    override fun append(c: Char): Appendable {
        (this as Buffer).append(c)
        return this
    }

    fun appendChars(csq: CharArray, start: Int, end: Int): Int {
        return (this as Buffer).appendChars(csq, start, end)
    }

    fun appendChars(csq: CharSequence, start: Int, end: Int): Int {
        return (this as Buffer).appendChars(csq, start, end)
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    final override fun peekTo(buffer: IoBuffer): Int {
        return (this as Buffer).peekTo(buffer)
    }

    fun makeView(): IoBuffer {
        return duplicate()
    }

    override fun duplicate(): IoBuffer = (origin ?: this).let { newOrigin ->
        newOrigin.acquire()
        IoBuffer(memory.pointer, contentCapacity, newOrigin).also { copy ->
            duplicateTo(copy)
        }
    }

    actual final override fun flush() {
    }

    actual override fun close() {
        throw UnsupportedOperationException("close for buffer view is not supported")
    }

    override fun toString(): String =
        "Buffer[readable = $readRemaining, writable = $writeRemaining, startGap = $startGap, endGap = $endGap]"

    actual companion object {
        /**
         * Number of bytes usually reserved in the end of chunk
         * when several instances of [IoBuffer] are connected into a chain (usually inside of [ByteReadPacket]
         * or [BytePacketBuilder])
         */
        @Deprecated("Use Buffer.ReservedSize instead.", ReplaceWith("Buffer.ReservedSize"))
        actual val ReservedSize: Int get() = Buffer.ReservedSize

        internal val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        actual val Empty = IoBuffer(EmptyBuffer, 0, null)

        /**
         * The default buffer pool
         */
        actual val Pool: ObjectPool<IoBuffer> get() = NoPool // BufferPoolNativeWorkaround

        actual val NoPool: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                val content = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
                return IoBuffer(content, BUFFER_VIEW_SIZE, null)
            }

            override fun recycle(instance: IoBuffer) {
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }
                nativeHeap.free(instance.content)
                instance.content = EmptyBuffer
            }
        }

        internal val NoPoolForManaged: ObjectPool<IoBuffer> = object : NoPoolImpl<IoBuffer>() {
            override fun borrow(): IoBuffer {
                error("You can't borrow an instance from this pool: use it only for manually created")
            }

            override fun recycle(instance: IoBuffer) {
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }
                instance.content = EmptyBuffer
            }
        }

        actual val EmptyPool: ObjectPool<IoBuffer> = EmptyBufferPoolImpl
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Short): Short = (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Int): Int =
    (swap((s and 0xffff).toShort()).toInt() shl 16) or (swap((s ushr 16).toShort()).toInt() and 0xffff)

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Long): Long =
    (swap((s and 0xffffffff).toInt()).toLong() shl 32) or (swap((s ushr 32).toInt()).toLong() and 0xffffffff)

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Float): Float = Float.fromBits(swap(s.toRawBits()))

@Suppress("NOTHING_TO_INLINE")
internal inline fun swap(s: Double): Double = Double.fromBits(swap(s.toRawBits()))

@ThreadLocal
private object BufferPoolNativeWorkaround : DefaultPool<ChunkBuffer>(BUFFER_VIEW_POOL_SIZE) {
    override fun produceInstance(): ChunkBuffer {
        val buffer = nativeHeap.allocMemory(BUFFER_VIEW_SIZE)
        return ChunkBuffer(buffer, null)
    }

    override fun clearInstance(instance: ChunkBuffer): ChunkBuffer {
        return super.clearInstance(instance).apply {
            instance.resetForWrite()
            instance.next = null
            instance.unpark()
        }
    }

    override fun validateInstance(instance: ChunkBuffer) {
        super.validateInstance(instance)

        require(instance.referenceCount == 0) { "unable to recycle buffer: buffer view is in use (refCount = ${instance.referenceCount})" }
        require(instance.origin == null) { "Unable to recycle buffer view: view copy shouldn't be recycled" }
    }

    override fun disposeInstance(instance: ChunkBuffer) {
        require(instance.referenceCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.referenceCount}" }
        nativeHeap.free(instance.memory)
    }
}

fun Buffer.readFully(pointer: CPointer<ByteVar>, offset: Int, length: Int) {
    readFully(pointer, offset.toLong(), length)
}

fun Buffer.readFully(pointer: CPointer<ByteVar>, offset: Long, length: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")
    readExact(length, "content") { memory, start ->
        memory.copyTo(pointer, start.toLong(), length.toLong(), offset)
    }
}

fun Buffer.readAvailable(pointer: CPointer<ByteVar>, offset: Int, length: Int): Int {
    return readAvailable(pointer, offset.toLong(), length)
}

fun Buffer.readAvailable(pointer: CPointer<ByteVar>, offset: Long, length: Int): Int {
    val available = readRemaining
    if (available == 0) return -1
    val resultSize = minOf(available, length)
    readFully(pointer, offset, resultSize)
    return resultSize
}

fun Buffer.writeFully(pointer: CPointer<ByteVar>, offset: Int, length: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")

    writeExact(length, "content") { memory, start ->
        pointer.copyTo(memory, offset, length, start)
    }
}

fun Buffer.writeFully(pointer: CPointer<ByteVar>, offset: Long, length: Int) {
    requirePositiveIndex(offset, "offset")
    requirePositiveIndex(length, "length")

    writeExact(length, "content") { memory, start ->
        pointer.copyTo(memory, offset, length.toLong(), start.toLong())
    }
}

inline fun Buffer.readDirect(block: (CPointer<ByteVar>) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return read { memory, start, _ ->
        block(memory.pointer.plus(start)!!)
    }
}

inline fun Buffer.writeDirect(block: (CPointer<ByteVar>) -> Int): Int {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return write { memory, start, _ ->
        block(memory.pointer.plus(start)!!)
    }
}

fun ChunkBuffer(ptr: CPointer<*>, lengthInBytes: Int, origin: ChunkBuffer?): ChunkBuffer {
    return ChunkBuffer(Memory.of(ptr, lengthInBytes), origin)
}

fun ChunkBuffer(ptr: CPointer<*>, lengthInBytes: Long, origin: ChunkBuffer?): ChunkBuffer {
    return ChunkBuffer(Memory.of(ptr, lengthInBytes), origin)
}
