package kotlinx.io.core

import kotlinx.io.js.*
import kotlinx.io.pool.*
import org.khronos.webgl.*

actual class BufferView internal constructor(
        private var content: ArrayBuffer,
        internal actual val origin: BufferView?
) {
    private var refCount = 1

    private var readPosition = 0
    private var writePosition = 0
    private var limit = content.byteLength

    private var view = if (content === EmptyBuffer) EmptyDataView else DataView(content)
    private var i8 = if (content === EmptyBuffer) Empty8 else Int8Array(content, 0, limit)

    private var littleEndian = false

    actual var attachment: Any? = null
    actual var next: BufferView? = null

    /**
     * Backing buffer capacity. Value for released buffer is unspecified
     */
    actual val capacity: Int get() = content.byteLength
    actual val readRemaining get() = writePosition - readPosition
    actual val writeRemaining get() = limit - writePosition

    actual fun canRead() = writePosition > readPosition
    actual fun canWrite() = writePosition < limit

    actual fun reserveStartGap(n: Int) {
        if (readPosition > 0) throw IllegalStateException("Start gap is already reserved")
        if (writePosition > 0) throw IllegalStateException("Start gap is already reserved")
        writePosition = n
        readPosition = n
    }

    actual fun reserveEndGap(n: Int) {
        if (limit != content.byteLength) throw IllegalStateException("End gap is already reserved")
        limit -= n
    }

    actual val startGap: Int get() = readPosition
    actual val endGap: Int get() = content.byteLength - limit

    actual var byteOrder: ByteOrder
        get() = if (littleEndian) ByteOrder.LITTLE_ENDIAN else  ByteOrder.BIG_ENDIAN
        set(value) {
            littleEndian = when (value) {
                ByteOrder.BIG_ENDIAN -> false
                ByteOrder.LITTLE_ENDIAN -> true
            }
        }

    actual fun readByte(): Byte {
        if (readRemaining < 0) throw IllegalStateException("No bytes available for read")
        val value = i8[readPosition]
        readPosition++
        return value
    }

    actual fun writeByte(v: Byte) {
        if (writeRemaining < 1) throw IllegalStateException("No space left for writing")
        i8[writePosition] = v
        writePosition++
    }

    actual fun readShort(): Short {
        if (readRemaining < 2) throw IllegalStateException("Not enough bytes available to read a short")
        val value = view.getInt16(readPosition, littleEndian)
        readPosition += 2
        return value
    }

    actual fun writeShort(v: Short) {
        if (writeRemaining < 2) throw IllegalStateException("Not enough space left to write a short")
        view.setInt16(writePosition, v, littleEndian)
        writePosition += 2
    }

    actual fun readInt(): Int {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read an int")
        val value = view.getInt32(readPosition, littleEndian)
        readPosition += 4
        return value
    }

    actual fun writeInt(v: Int) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write an int")
        view.setInt32(writePosition, v, littleEndian)
        writePosition += 4
    }

    actual fun readFloat(): Float {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read a float")
        val value = view.getFloat32(readPosition, littleEndian)
        readPosition += 4
        return value
    }

    actual fun writeFloat(v: Float) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write a float")
        view.setFloat32(writePosition, v, littleEndian)
        writePosition += 4
    }

    actual fun readDouble(): Double {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a double")
        val value = view.getFloat64(readPosition, littleEndian)
        readPosition += 8
        return value
    }

    actual fun writeDouble(v: Double) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough space left to write a double")
        view.setFloat64(writePosition, v, littleEndian)
        writePosition += 8
    }

    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        if (readRemaining < length) throw IllegalStateException("Not enough bytes available ($readRemaining) to read $length bytes")
        val rp = readPosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            dst[offset + idx] = i8[rp + idx]
        }

        readPosition += length
    }

    fun read(dst: Array<Byte>, offset: Int, length: Int) {
        if (readRemaining < length) throw IllegalStateException("Not enough bytes available ($readRemaining) to read $length bytes")
        val rp = readPosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            dst[offset + idx] = i8[rp + idx]
        }

        readPosition += length
    }

    fun read(dst: ArrayBuffer, offset: Int, length: Int) {
        if (readRemaining < length) throw IllegalStateException("Not enough bytes available ($readRemaining) to read $length bytes")
        val to = Int8Array(dst, offset, length)

        val rp = readPosition
        val rem = writePosition - rp
        val i8 = i8

        if (rp == 0 && length == rem) {
            to.set(i8, offset)
        } else if (length < 100) {
            for (i in 0 .. length - 1) {
                to[offset + i] = i8[rp + i]
            }
        } else {
            val from = Int8Array(content, rp, length)
            to.set(from)
        }

        readPosition = rp + length
    }

    fun read(dst: Int8Array, offset: Int, length: Int) {
        if (readRemaining < length) throw IllegalStateException("Not enough bytes available ($readRemaining) to read $length bytes")
        val rp = readPosition
        val rem = writePosition - rp
        val i8 = i8

        if (rp == 0 && rem == length) {
            dst.set(i8, offset)
        } else if (length < 100) {
            for (i in 0 .. length - 1) {
                dst[offset + i] = i8[rp + i]
            }
        } else {
            val from = Int8Array(content, rp, length)
            dst.set(from, offset)
        }

        readPosition = rp + length
    }

    actual fun write(array: ByteArray, offset: Int, length: Int) {
        if (writeRemaining < length) throw IllegalStateException("Not enough space left ($writeRemaining) to write $length bytes")
        val wp = writePosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            i8[wp + idx] = array[offset + idx]
        }

        writePosition = wp + length
    }

    fun write(src: Int8Array, offset: Int, length: Int) {
        if (writeRemaining < length) throw IllegalStateException("Not enough space left ($writeRemaining) to write $length bytes")
        val wp = writePosition
        val rem = limit - wp
        val i8 = i8

        if (length > rem) throw IndexOutOfBoundsException()
        if (offset == 0 && length == src.length) {
            i8.set(src, wp)
        } else if (length < 100) {
            for (i in 0 .. length - 1) {
                i8[wp + i] = src[offset + i]
            }
        } else {
            val from = Int8Array(src.buffer, src.byteOffset + offset, length)
            i8.set(from, wp)
        }

        writePosition = wp + length
    }

    actual fun readLong(): Long {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a long")
        val m = 0xffffffff
        val a = readInt().toLong() and m
        val b = readInt().toLong() and m

        return if (littleEndian) {
            (b shl 32) or a
        } else {
            (a shl 32) or b
        }
    }

    actual fun writeLong(v: Long) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough space left to write a long")
        val m = 0xffffffff
        val a = (v shr 32).toInt()
        val b = (v and m).toInt()

        if (littleEndian) {
            writeInt(b)
            writeInt(a)
        } else {
            writeInt(a)
            writeInt(b)
        }
    }

    actual fun discardExact(n: Int) {
        val rem = readRemaining
        if (n > rem) throw IllegalArgumentException("Can't discard $n bytes: only $rem bytes available")
        readPosition += n
    }

    actual fun pushBack(n: Int) {
        if (readPosition == 0) throw IllegalStateException("Nothing to push back")
        readPosition--
    }

    actual fun resetForWrite() {
        resetForWrite(content.byteLength)
    }

    actual fun resetForWrite(limit: Int) {
        require(limit <= content.byteLength) { "Limit shouldn't be bigger than buffer size: limit = $limit, size = ${content.byteLength}"}
        readPosition = 0
        writePosition = 0
        this.limit = limit
    }

    actual fun resetForRead() {
        readPosition = 0
        limit = content.byteLength
        writePosition = limit
    }

    actual fun isExclusivelyOwned(): Boolean = refCount == 1

    actual fun makeView(): BufferView {
        val o = origin ?: this
        o.acquire()

        val view = BufferView(content, o)
        view.attachment = attachment
        view.readPosition = readPosition
        view.writePosition = writePosition
        view.limit = limit

        return view
    }

    actual fun release(pool: ObjectPool<BufferView>) {
        if (release()) {
            resetForWrite()

            if (origin != null) {
                unlink()
                origin.release(pool)
            } else {
                pool.recycle(this)
            }
        }
    }

    actual fun writeBuffer(src: BufferView, length: Int): Int {
        require(length <= src.readRemaining) { "length is too large: not enough bytes to read $length > ${src.readRemaining}"}
        require(length <= writeRemaining) { "length is too large: not enough room to write $length > $writeRemaining" }

        val otherEnd = src.readPosition + length
        val sub = src.i8.subarray(src.readPosition, otherEnd)
        i8.set(sub, writePosition)
        src.readPosition = otherEnd
        writePosition += length

        return length
    }

    internal fun readText(decoder: TextDecoder, out: Appendable, lastBuffer: Boolean, max: Int = Int.MAX_VALUE): Int {
        require(max >= 0) { "max shouldn't be negative: $max" }

        if (readRemaining == 0) return 0

        val rawResult = decoder.decodeStream(i8.subarray(readPosition, writePosition), !lastBuffer)
        val result = if (rawResult.length <= max) {
            readPosition = writePosition
            rawResult
        } else {
            val actual = rawResult.substring(0, max)

            // as js's text decoder is too stupid, let's guess new readPosition
            val subDecoder = TextDecoderFatal(decoder.encoding, true)
            val subArray = Int8Array(1)
            var subDecoded = 0

            for (i in readPosition until writePosition) {
                subArray[0] = i8[i]
                subDecoded += subDecoder.decodeStream(subArray, true).length

                if (subDecoded >= max) {
                    readPosition = i + 1
                    break
                }
            }

            if (subDecoded < max) {
                subDecoded += subDecoder.decode().length

                if (subDecoded >= max) {
                    readPosition = writePosition
                } else {
                    throw IllegalStateException("Failed to readText: don't know how to update read position")
                }
            }

            actual
        }

        out.append(result)

        return result.length
    }

    internal actual fun writeBufferPrepend(other: BufferView) {
        val size = other.readRemaining
        require(size <= startGap) { "size should be greater than startGap (size = $size, startGap = $startGap)" }

        val otherEnd = other.readPosition + size
        val sub = other.i8.subarray(other.readPosition, otherEnd)

        i8.set(sub, readPosition - size)
        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: BufferView, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap) { "should should be greater than write space + end gap (size = $size, " +
                "writeRemaining = $writeRemaining, endGap = $endGap, rem+gap = ${writeRemaining + endGap}" }

        val otherEnd = other.readPosition + size
        val sub = other.i8.subarray(other.readPosition, otherEnd)

        i8.set(sub, writePosition)
        writePosition += size
        if (writePosition > limit) {
            limit = writePosition
        }
        other.readPosition += size
    }

    internal fun unlink() {
        if (refCount != 0) throw IllegalStateException("Unable to unlink buffers: buffer view is in use")
        content = EmptyBuffer
        i8 = Empty8
        view = EmptyDataView
        resetForWrite()
    }

    private fun acquire() {
        val v = refCount
        if (v == 0) throw IllegalStateException("Failed to acquire buffer: buffer has been already released")
        refCount = v + 1
    }

    private fun release(): Boolean {
        if (this === Empty) throw IllegalStateException("attempted to release BufferView.Empty")

        val v = refCount
        if (v == 0) throw IllegalStateException("Unable to release: buffer has been already released")
        val newCount = v - 1
        refCount = newCount
        return newCount == 0
    }

    actual companion object {
        private val EmptyBuffer = ArrayBuffer(0)
        private val EmptyDataView = DataView(EmptyBuffer)
        private val Empty8 = Int8Array(0)

        actual val Empty = BufferView(EmptyBuffer, null)
        actual val Pool: ObjectPool<BufferView> = object: DefaultPool<BufferView>(BUFFER_VIEW_POOL_SIZE) {
            override fun produceInstance(): BufferView {
                return BufferView(ArrayBuffer(BUFFER_VIEW_SIZE), null)
            }

            override fun clearInstance(instance: BufferView): BufferView {
                return super.clearInstance(instance).apply {
                    instance.resetForWrite()
                    instance.next = null
                    instance.attachment = null

                    if (instance.refCount != 0) throw IllegalStateException("Unable to clear instance: refCount is ${instance.refCount} != 0")
                    instance.refCount = 1
                }
            }

            override fun validateInstance(instance: BufferView) {
                super.validateInstance(instance)

                require(instance.refCount == 0) { "unable to recycle buffer: buffer view is in use (refCount = ${instance.refCount})"}
                require(instance.origin == null) { "Unable to recycle buffer view: view copy shouldn't be recycled" }
            }

            override fun disposeInstance(instance: BufferView) {
                instance.unlink()
            }
        }

        actual val NoPool: ObjectPool<BufferView> = object : NoPoolImpl<BufferView>() {
            override fun borrow(): BufferView {
                return BufferView(ArrayBuffer(4096), null)
            }
        }
    }
}
