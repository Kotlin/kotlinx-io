package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy
import platform.posix.memset

actual class BufferView internal constructor(
        private var content: CPointer<ByteVar>,
        private val contentCapacity: Int,
        internal actual val origin: BufferView?
) : Input, Output {
    private var refCount = 1

    private var readPosition = 0
    private var writePosition = 0
    private var limit = contentCapacity

    private var platformEndian = ByteOrder.BIG_ENDIAN === ByteOrder.nativeOrder()

    actual var attachment: Any? = null
    actual var next: BufferView? = null

    actual val capacity: Int get() = contentCapacity
    actual val readRemaining: Int get() = writePosition - readPosition
    actual val writeRemaining: Int get() = limit - writePosition

    actual fun canRead() = writePosition > readPosition
    actual fun canWrite() = writePosition < limit

    actual final override var byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN
        set(newOrder) {
            field = newOrder
            platformEndian = newOrder === ByteOrder.nativeOrder()
        }

    actual fun reserveStartGap(n: Int) {
        if (readPosition > 0) throw IllegalStateException("Start gap is already reserved")
        if (writePosition > 0) throw IllegalStateException("Start gap is already reserved")
        writePosition = n
        readPosition = n
    }

    actual fun reserveEndGap(n: Int) {
        if (limit != contentCapacity) throw IllegalStateException("End gap is already reserved")
        limit -= n
    }

    actual val startGap: Int get() = readPosition
    actual val endGap: Int get() = contentCapacity - limit


    actual final override fun readByte(): Byte {
        if (readRemaining < 0) throw IllegalStateException("No bytes available for read")
        val value = content[readPosition]
        readPosition++
        return value
    }

    actual final override fun writeByte(v: Byte) {
        if (writeRemaining < 1) throw IllegalStateException("No space left for writing")
        content[writePosition] = v
        writePosition++
    }

    actual final override fun readShort(): Short {
        if (readRemaining < 2) throw IllegalStateException("Not enough bytes available to read a short")
        var value = (content + readPosition)!!.reinterpret<ShortVar>()[0]
        if (!platformEndian) value = swap(value)
        readPosition += 2
        return value
    }

    actual final override fun writeShort(v: Short) {
        if (writeRemaining < 2) throw IllegalStateException("Not enough space left to write a short")
        var value = v
        if (!platformEndian) value = swap(value)
        (content + writePosition)!!.reinterpret<ShortVar>()[0] = value
        writePosition += 2
    }

    actual final override fun readInt(): Int {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read an int")
        var value = (content + readPosition)!!.reinterpret<IntVar>()[0]
        if (!platformEndian) value = swap(value)
        readPosition += 4
        return value
    }

    actual final override fun writeInt(v: Int) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write an int")
        var value = if (platformEndian) v else swap(v)
        (content + writePosition)!!.reinterpret<IntVar>()[0] = value
        writePosition += 4
    }

    actual final override fun readFloat(): Float {
        if (readRemaining < 4) throw IllegalStateException("Not enough bytes available to read a float")

        val f = (content + readPosition)!!.reinterpret<FloatVar>()[0]
        readPosition += 4
        return if (platformEndian) f else swap(f)
    }

    actual final override fun writeFloat(v: Float) {
        if (writeRemaining < 4) throw IllegalStateException("Not enough space left to write a float")
        val b = if (platformEndian) v else swap(v)
        (content + writePosition)!!.reinterpret<FloatVar>()[0] = b

        writePosition += 4
    }

    actual final override fun readDouble(): Double {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a double")

        val b = (content + readPosition)!!.reinterpret<DoubleVar>()[0]
        readPosition += 8
        return if (platformEndian) b else swap(b)
    }

    actual final override fun writeDouble(v: Double) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough space left to write a double")
        (content + writePosition)!!.reinterpret<DoubleVar>()[0] = if (platformEndian) v else swap(v)
        writePosition += 8
    }

    final override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        
        memcpy(dst + offset, content + readPosition, length.toLong())
        readPosition += length
    }

    fun writeFully(array: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= writeRemaining) { "Not enough space available to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(content + writePosition, array + offset, length.toLong())
        writePosition += length
    }

    @Deprecated("Use readFully instead", ReplaceWith("readFully(dst, offset, length)"))
    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        readFully(dst, offset, length)
    }

    actual final override fun readFully(dst: ByteArray, offset: Int, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        dst.usePinned {
            val address = it.addressOf(offset)
            memcpy(address, content + readPosition, length.toLong())
        }

        readPosition += length
    }

    actual final override fun readFully(dst: ShortArray, offset: Int, length: Int) {
        require(length * 2 <= readRemaining) { "Not enough bytes available to read $length short int numbers (16)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, length.toLong() * 2L)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<ShortVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 2
    }

    actual final override fun readFully(dst: IntArray, offset: Int, length: Int) {
        require(length * 4 <= readRemaining) { "Not enough bytes available to read $length int numbers (32)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, length.toLong() * 4L)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<IntVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 4
    }

    actual final override fun readFully(dst: LongArray, offset: Int, length: Int) {
        require(length * 8 <= readRemaining) { "Not enough bytes available to read $length long int numbers (64)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, length.toLong() * 8L)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<LongVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 8
    }

    actual final override fun readFully(dst: FloatArray, offset: Int, length: Int) {
        require(length * 4 <= readRemaining) { "Not enough bytes available to read $length float numbers" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, length.toLong() * 4L)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<FloatVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 4
    }

    actual final override fun readFully(dst: DoubleArray, offset: Int, length: Int) {
        require(length * 8 <= readRemaining) { "Not enough bytes available to read $length double numbers" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, length.toLong() * 8L)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<DoubleVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 8
    }

    actual final override fun readFully(dst: BufferView, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length <= dst.writeRemaining) { "Not enough space in the destination buffer to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(dst.content + dst.writePosition, content + readPosition, length.toLong())
        readPosition += length
        dst.writePosition += length
    }


    actual final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        return dst.usePinned {
            val copySize = minOf(length, readRemaining)
            memcpy(it.addressOf(offset), content + readPosition, copySize.toLong())
            readPosition += copySize
            copySize
        }
    }

    actual final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        val copySize = minOf(length, readRemaining shr 1)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, copySize.toLong() * 2)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<ShortVar>()
            for (i in 0 .. copySize - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }

        readPosition += copySize * 2

        return copySize
    }

    actual final override fun readAvailable(dst: IntArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        val copySize = minOf(length, readRemaining shr 2)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, copySize.toLong() * 4)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<IntVar>()
            for (i in 0 .. copySize - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }

        readPosition += copySize * 4

        return copySize
    }

    actual final override fun readAvailable(dst: LongArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        val copySize = minOf(length, readRemaining shr 3)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, copySize.toLong() * 8)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<LongVar>()
            for (i in 0 .. copySize - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }

        readPosition += copySize * 8

        return copySize
    }

    actual final override fun readAvailable(dst: FloatArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        val copySize = minOf(length, readRemaining shr 2)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, copySize.toLong() * 4)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<FloatVar>()
            for (i in 0 .. copySize - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }

        readPosition += copySize * 4

        return copySize
    }

    actual final override fun readAvailable(dst: DoubleArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        val copySize = minOf(length, readRemaining shr 3)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, copySize.toLong() * 8)
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<DoubleVar>()
            for (i in 0 .. copySize - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }

        readPosition += copySize * 8

        return copySize
    }

    actual final override fun readAvailable(dst: BufferView, length: Int): Int {
        require(length <= dst.writeRemaining) { "Not enough space in the dst buffer to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        val copySize = minOf(length, readRemaining)
        memcpy(dst.content + dst.writePosition, content + readPosition, copySize.toLong())
        readPosition += length
        dst.writePosition += length

        return copySize
    }

    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }

        val copySize = minOf(length, readRemaining)
        memcpy(dst + offset, content + readPosition, copySize.toLong())
        readPosition += length

        return copySize
    }

    internal fun writeDirect(block: (CPointer<ByteVar>) -> Int) {
        val rc = block((content + writePosition)!!)
        check(rc >= 0)
        check(rc <= writeRemaining)
        writePosition += rc
    }

    internal fun readDirect(block: (CPointer<ByteVar>) -> Int) {
        val rc = block((content + readPosition)!!)
        check(rc >= 0)
        check(rc <= readRemaining)
        readPosition += rc
    }

    @Deprecated("Use writeFully instead")
    actual final fun write(array: ByteArray, offset: Int, length: Int) {
        writeFully(array, offset, length)
    }

    actual final override fun writeFully(src: ByteArray, offset: Int, length: Int) {
        require(length <= writeRemaining) { "Not enough space to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        src.usePinned {
            val address = it.addressOf(offset)
            memcpy(content + writePosition, address, length.toLong())
        }

        writePosition += length
    }

    actual final override fun writeFully(src: ShortArray, offset: Int, length: Int) {
        require(length * 2 <= writeRemaining) { "Not enough space to write $length short int numbers (16)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), length.toLong() * 2L)
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<ShortVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 2
    }

    actual final override fun writeFully(src: IntArray, offset: Int, length: Int) {
        require(length * 4 <= writeRemaining) { "Not enough space to write $length int numbers (32)"}
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), length.toLong() * 4L)
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<IntVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 4
    }

    actual final override fun writeFully(src: LongArray, offset: Int, length: Int) {
        require(length * 8 <= writeRemaining) { "Not enough space to write $length long int numbers (64)"}
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), length.toLong() * 8L)
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<LongVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 8
    }

    actual final override fun writeFully(src: FloatArray, offset: Int, length: Int) {
        require(length * 4 <= writeRemaining) { "Not enough space to write $length float numbers (32)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), length.toLong() * 4L)
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<FloatVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 4
    }

    actual final override fun writeFully(src: DoubleArray, offset: Int, length: Int) {
        require(length * 8 <= writeRemaining) { "Not enough space to write $length double numbers (64)"}
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), length.toLong() * 8L)
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<DoubleVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 8
    }

    actual final override fun writeFully(src: BufferView, length: Int) {
        require(length <= src.readRemaining) { "length is too large: not enough bytes to read $length > ${src.readRemaining}"}
        require(length <= writeRemaining) { "length is too large: not enough room to write $length > $writeRemaining" }

        memcpy(content + writePosition, src.content + src.readPosition, length.toLong())

        src.readPosition += length
        writePosition += length
    }

    actual final override fun fill(n: Long, v: Byte) {
        require(n <= writeRemaining.toLong())
        require(n >= 0) { "n shouldn't be negative: $n" }

        memset(content + writePosition, v.toInt() and 0xff, n)
        writePosition += n.toInt()
    }

    actual final override fun readLong(): Long {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a long")
        val m = 0xffffffff
        val a = readInt().toLong() and m
        val b = readInt().toLong() and m

        return if (byteOrder === ByteOrder.LITTLE_ENDIAN) {
            (b shl 32) or a
        } else {
            (a shl 32) or b
        }
    }

    actual final override fun writeLong(v: Long) {
        if (writeRemaining < 8) throw IllegalStateException("Not enough space left to write a long")
        val m = 0xffffffff
        val a = (v shr 32).toInt()
        val b = (v and m).toInt()

        if (byteOrder === ByteOrder.LITTLE_ENDIAN) {
            writeInt(b)
            writeInt(a)
        } else {
            writeInt(a)
            writeInt(b)
        }
    }

    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(src, length)"))
    actual fun writeBuffer(src: BufferView, length: Int): Int {
        writeFully(src, length)
        return length
    }

    internal actual fun writeBufferPrepend(other: BufferView) {
        val size = other.readRemaining
        require(size <= startGap) { "size should be greater than startGap (size = $size, startGap = $startGap)" }

        memcpy(content + (readPosition - size), other.content + readPosition, size.toLong())

        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: BufferView, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap) { "should should be greater than write space + end gap (size = $size, " +
                "writeRemaining = $writeRemaining, endGap = $endGap, rem+gap = ${writeRemaining + endGap}" }


        memcpy(content + writePosition, other.content + readPosition, size.toLong())

        writePosition += size
        if (writePosition > limit) {
            limit = writePosition
        }
        other.readPosition += size
    }

    actual fun discardExact(n: Int) {
        if (discard(n.toLong()) != n.toLong()) throw EOFException("Unable to discard $n")
    }

    actual final override fun discard(n: Long): Long {
        val step = minOf(readRemaining.toLong(), n).toInt()
        readPosition += step
        return step.toLong()
    }

    actual fun resetForWrite() {
        resetForWrite(contentCapacity)
    }

    actual fun resetForWrite(limit: Int) {
        require(limit <= contentCapacity) { "Limit shouldn't be greater than buffers capacity" }
        readPosition = 0
        writePosition = 0
        this.limit = limit
    }

    actual fun resetForRead() {
        readPosition = 0
        limit = contentCapacity
        writePosition = limit
    }

    actual fun pushBack(n: Int) {
        if (readPosition == 0) throw IllegalStateException("Nothing to push back")
        readPosition--
    }

    internal fun unlink() {
        if (refCount != 0) throw IllegalStateException("Unable to unlink buffers: buffer view is in use")
        content = EmptyBuffer
        resetForWrite(0)
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

    actual fun isExclusivelyOwned(): Boolean = refCount == 1

    actual fun makeView(): BufferView {
        val o = origin ?: this
        o.acquire()

        val view = BufferView(content, contentCapacity, o)
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

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$updateRemaining$`(remaining: Int) {
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$ensureNext$`(current: BufferView): BufferView? {
        return null
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$prepareRead$`(minSize: Int): BufferView? {
        return if (readRemaining >= minSize) this else null
    }

    actual final override fun flush() {
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Short): Short = (((s.toInt() and 0xff) shl 8) or ((s.toInt() and 0xffff) ushr 8)).toShort()
    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Int): Int = (swap((s and 0xffff).toShort()).toInt() shl 16) or (swap((s ushr 16).toShort()).toInt() and 0xffff)
    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Long): Long = (swap((s and 0xffffffff).toInt()).toLong() shl 32) or (swap((s ushr 32).toInt()).toLong() and 0xffffffff)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Float): Float = Float.fromBits(swap(s.bits()))

    @Suppress("NOTHING_TO_INLINE")
    private inline fun swap(s: Double): Double = Double.fromBits(swap(s.bits()))

    actual companion object {
        private val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        actual val Empty = BufferView(EmptyBuffer, 0, null)

        actual val Pool: ObjectPool<BufferView> = object: DefaultPool<BufferView>(BUFFER_VIEW_POOL_SIZE) {
            override fun produceInstance(): BufferView {
                val buffer = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
                return BufferView(buffer, BUFFER_VIEW_SIZE, null)
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
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }

                nativeHeap.free(instance.content)
            }
        }

        actual val NoPool: ObjectPool<BufferView> = object : NoPoolImpl<BufferView>() {
            override fun borrow(): BufferView {
                val content = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
                return BufferView(content, BUFFER_VIEW_SIZE, null)
            }

            override fun recycle(instance: BufferView) {
                require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
                require(instance.content !== EmptyBuffer) { "Couldn't dispose empty buffer" }
                nativeHeap.free(instance.content)
            }
        }
    }
}
