package kotlinx.io.core

import kotlinx.io.pool.*
import org.khronos.webgl.*

actual class BufferView internal constructor(
        private var content: ArrayBuffer,
        private actual val origin: BufferView?
) {
    private var refCount = 0

    private var readPosition = 0
    private var writePosition = 0
    private var limit = content.byteLength

    private var view = if (content === EmptyBuffer) EmptyDataView else DataView(content)
    private var i8 = if (content === EmptyBuffer) Empty8 else Int8Array(content, 0, limit)

    private var littleEndian = false

    actual var attachment: Any? = null
    actual var next: BufferView? = null

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
        val value = i8[readPosition]
        readPosition++
        return value
    }

    actual fun writeByte(v: Byte) {
        i8[writePosition] = v
        writePosition++
    }

    actual fun readShort(): Short {
        val value = view.getInt16(readPosition, littleEndian)
        readPosition += 2
        return value
    }

    actual fun writeShort(v: Short) {
        view.setInt16(writePosition, v, littleEndian)
        writePosition += 2
    }

    actual fun readInt(): Int {
        val value = view.getInt32(readPosition, littleEndian)
        readPosition += 4
        return value
    }

    actual fun writeInt(v: Int) {
        view.setInt32(writePosition, v, littleEndian)
        writePosition += 4
    }

    actual fun readFloat(): Float {
        val value = view.getFloat32(readPosition, littleEndian)
        readPosition += 4
        return value
    }

    actual fun writeFloat(v: Float) {
        view.setFloat32(writePosition, v, littleEndian)
        writePosition += 4
    }

    actual fun readDouble(): Double {
        val value = view.getFloat64(readPosition, littleEndian)
        readPosition += 8
        return value
    }

    actual fun writeDouble(v: Double) {
        view.setFloat64(writePosition, v, littleEndian)
        writePosition += 8
    }

    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        val rp = readPosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            dst[offset + idx] = i8[rp + idx]
        }

        readPosition += length
    }

    fun read(dst: Array<Byte>, offset: Int, length: Int) {
        val rp = readPosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            dst[offset + idx] = i8[rp + idx]
        }

        readPosition += length
    }

    fun read(dst: ArrayBuffer, offset: Int, length: Int) {
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
        val wp = writePosition
        val i8 = i8

        for (idx in 0 .. length - 1) {
            i8[wp + idx] = array[offset + idx]
        }
    }

    fun write(src: Int8Array, offset: Int, length: Int) {
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
        val a = readInt()
        val b = readInt()

        return if (littleEndian) {
            (b.toLong() shl 32) or a.toLong()
        } else {
            (a.toLong() shl 32) or b.toLong()
        }
    }

    actual fun writeLong(v: Long) {
        val a = (v and Int.MAX_VALUE.toLong()).toInt()
        val b = (v shr 32).toInt()

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
        readPosition = 0
        writePosition = 0
    }

    actual fun resetForRead() {
        readPosition = 0
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

    actual fun writeBuffer(other: BufferView, length: Int): Int {
        require(length <= other.readRemaining)
        require(length <= writeRemaining)

        val otherEnd = other.readPosition + length
        val sub = other.i8.subarray(other.readPosition, otherEnd)
        i8.set(sub, writePosition)
        other.readPosition = otherEnd
        writePosition += length

        return length
    }

    internal actual fun writeBufferPrepend(other: BufferView) {
        val size = other.readRemaining
        require(size <= startGap)

        val otherEnd = other.readPosition + size
        val sub = other.i8.subarray(other.readPosition, otherEnd)

        i8.set(sub, readPosition - size)
        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: BufferView, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap)

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
        limit = 0
        content = EmptyBuffer
        i8 = Empty8
        view = EmptyDataView
    }

    private fun acquire() {
        val v = refCount
        if (v == 0) throw IllegalStateException("buffer has been already released")
        refCount = v + 1
    }

    private fun release(): Boolean {
        val v = refCount
        if (v == 0) throw IllegalStateException("buffer has been already released")
        val newCount = v - 1
        refCount = newCount
        return newCount == 0
    }

    actual companion object {
        private val EmptyBuffer = ArrayBuffer(0)
        private val EmptyDataView = DataView(EmptyBuffer)
        private val Empty8 = Int8Array(0)

        actual val Empty = BufferView(EmptyBuffer, null)
        actual val Pool: ObjectPool<BufferView> = DefaultBufferViewPool
        actual val NoPool: ObjectPool<BufferView> = object : NoPoolImpl<BufferView>() {
            override fun borrow(): BufferView {
                return BufferView(ArrayBuffer(4096), null)
            }
        }
    }
}
