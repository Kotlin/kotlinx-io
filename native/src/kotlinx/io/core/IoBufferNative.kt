@file:Suppress("RedundantModalityModifier")

package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy
import platform.posix.memset
import platform.posix.size_t
import kotlinx.io.core.internal.*
import kotlin.native.concurrent.ThreadLocal

@PublishedApi
@SharedImmutable
internal val MAX_SIZE: size_t = size_t.MAX_VALUE

actual class IoBuffer internal constructor(
        internal var content: CPointer<ByteVar>,
        private val contentCapacity: Int,
        internal actual val origin: IoBuffer?
) : Input, Output {
    @Deprecated(
        "Suppress warning.",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("unused")
    actual final override val doNotImplementInputButExtendAbstractInputInstead: Nothing
        get() = error("Should be never accessed.")

    @Deprecated(
        "Suppress warning.",
        level = DeprecationLevel.HIDDEN
    )
    @Suppress("unused")
    actual final override val doNotImplementOutputButExtendAbstractOutputInstead: Nothing
        get() = error("Should be never accessed.")

    internal var refCount = 1

    constructor(content: CPointer<ByteVar>, contentCapacity: Int) : this(content, contentCapacity, null)

    internal var readPosition = 0
    internal var writePosition = 0
    private var limit = contentCapacity

    private var platformEndian = ByteOrder.BIG_ENDIAN === ByteOrder.nativeOrder()

    @ExperimentalIoApi
    actual var attachment: Any? = null
    actual var next: IoBuffer? = null

    actual val capacity: Int get() = contentCapacity
    actual val readRemaining: Int get() = writePosition - readPosition
    actual val writeRemaining: Int get() = limit - writePosition

    actual fun canRead() = writePosition > readPosition
    actual fun canWrite() = writePosition < limit

    override val endOfInput: Boolean get() = !canRead()

    init {
        require(contentCapacity >= 0) { "contentCapacity shouln't be negative: $contentCapacity" }
        require(this !== origin) { "origin shouldn't point to itself" }
    }

    @Deprecated("Read/write with readXXXLittleEndian/writeXXXLittleEndian or " +
        "do readXXX/writeXXX with X.reverseByteOrder() instead.")
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
        val readPosition = readPosition
        val value = content[readPosition]
        this.readPosition = readPosition + 1
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
        val value = if (platformEndian) v else swap(v)
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

    final override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= readRemaining.toLong())
        require(length >= 0L) { "length shouldn't be negative: $length" }
        require(length <= Int.MAX_VALUE) { "length shouldn't be greater than Int.MAX_VALUE" }

        memcpy(dst + offset, content + readPosition, length.convert<size_t>())
        readPosition += length.toInt()
    }

    final override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(dst + offset, content + readPosition, length.convert<size_t>())
        readPosition += length
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= writeRemaining) { "Not enough space available to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(content + writePosition, src + offset, length.convert<size_t>())
        writePosition += length
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= writeRemaining.toLong()) { "Not enough space available to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(length.convert<size_t>() <= size_t.MAX_VALUE) { "length shouldn't be greater than ${size_t.MAX_VALUE}" }

        memcpy(content + writePosition, src + offset, length.convert<size_t>())
        writePosition += length.toInt()
    }

    @Deprecated("Use readFully instead", ReplaceWith("readFully(dst, offset, length)"), level = DeprecationLevel.ERROR)
    actual fun read(dst: ByteArray, offset: Int, length: Int) {
        readFully(dst, offset, length)
    }

    actual final override fun readFully(dst: ByteArray, offset: Int, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (length == 0) return

        dst.usePinned {
            val address = it.addressOf(offset)
            memcpy(address, content + readPosition, length.convert<size_t>())
        }

        readPosition += length
    }

    actual final override fun readFully(dst: ShortArray, offset: Int, length: Int) {
        require(length * 2 <= readRemaining) { "Not enough bytes available to read $length short int numbers (16)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (length == 0) return

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (length * 2).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (length * 4).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (length * 8).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (length * 4).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (length * 8).convert<size_t>())
            }
        } else {
            val ptr = (content + readPosition)!!.reinterpret<DoubleVar>()
            for (i in 0 .. length - 1) {
                dst[offset + i] = swap(ptr[i])
            }
        }
        readPosition += length * 8
    }

    actual final override fun readFully(dst: IoBuffer, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length <= dst.writeRemaining) { "Not enough space in the destination buffer to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(dst.content + dst.writePosition, content + readPosition, length.convert<size_t>())
        readPosition += length
        dst.writePosition += length
    }


    actual final override fun readAvailable(dst: ByteArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (length == 0) return 0

        return dst.usePinned {
            val copySize = minOf(length, readRemaining)
            memcpy(it.addressOf(offset), content + readPosition, copySize.convert<size_t>())
            readPosition += copySize
            copySize
        }
    }

    actual final override fun readAvailable(dst: ShortArray, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= dst.size) { "offset ($offset) + length ($length) > dst.size (${dst.size})" }

        if (length == 0) return 0
        val copySize = minOf(length, readRemaining shr 1)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (copySize * 2).convert<size_t>())
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

        if (length == 0) return 0
        val copySize = minOf(length, readRemaining shr 2)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (copySize * 4).convert<size_t>())
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

        if (length == 0) return 0
        val copySize = minOf(length, readRemaining shr 3)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (copySize * 8).convert<size_t>())
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

        if (length == 0) return 0
        val copySize = minOf(length, readRemaining shr 2)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (copySize * 4).convert<size_t>())
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

        if (length == 0) return 0
        val copySize = minOf(length, readRemaining shr 3)

        if (platformEndian) {
            dst.usePinned {
                memcpy(it.addressOf(offset), content + readPosition, (copySize * 8).convert<size_t>())
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

    actual final override fun readAvailable(dst: IoBuffer, length: Int): Int {
        require(length <= dst.writeRemaining) { "Not enough space in the dst buffer to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        val copySize = minOf(length, readRemaining)
        memcpy(dst.content + dst.writePosition, content + readPosition, copySize.convert<size_t>())
        readPosition += length
        dst.writePosition += length

        return copySize
    }

    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Int, length: Int): Int {
        require(length >= 0) { "length shouldn't be negative: $length" }

        val copySize = minOf(length, readRemaining)
        memcpy(dst + offset, content + readPosition, copySize.convert<size_t>())
        readPosition += length

        return copySize
    }

    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(length <= Int.MAX_VALUE) { "length shouldn't be greater than Int.MAX_VALUE" }

        val copySize = minOf(length, readRemaining.toLong())
        memcpy(dst + offset, content + readPosition, copySize.convert<size_t>())
        readPosition += length.toInt()

        return copySize
    }

    /**
     * Apply [block] to a native pointer for writing to the buffer. Lambda should return number of bytes were written.
     * @return number of bytes written
     */
    fun writeDirect(block: (CPointer<ByteVar>) -> Int): Int {
        val rc = block((content + writePosition)!!)
        check(rc >= 0) { "block function should return non-negative results: $rc" }
        check(rc <= writeRemaining)
        writePosition += rc
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
        readPosition += rc
        return rc
    }

    @Deprecated("Use writeFully instead", level = DeprecationLevel.ERROR)
    actual final fun write(array: ByteArray, offset: Int, length: Int) {
        writeFully(array, offset, length)
    }

    actual final override fun writeFully(src: ByteArray, offset: Int, length: Int) {
        require(length <= writeRemaining) { "Not enough space to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (length == 0) return

        src.usePinned {
            val address = it.addressOf(offset)
            memcpy(content + writePosition, address, length.convert<size_t>())
        }

        writePosition += length
    }

    actual final override fun writeFully(src: ShortArray, offset: Int, length: Int) {
        require(length * 2 <= writeRemaining) { "Not enough space to write $length short int numbers (16)" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(offset >= 0) { "offset shouldn't be negative: $offset" }
        require(offset + length <= src.size) { "offset ($offset) + length ($length) > src.size (${src.size})" }

        if (length == 0) return

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), (length * 2).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), (length * 4).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), (length * 8).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), (length * 4).convert<size_t>())
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

        if (length == 0) return

        if (platformEndian) {
            src.usePinned {
                memcpy(content + writePosition, it.addressOf(offset), (length * 8).convert<size_t>())
            }
        } else {
            val buffer = (content + writePosition)!!.reinterpret<DoubleVar>()
            for (i in 0 .. length - 1){
                buffer[i] = swap(src[i + offset])
            }
        }

        writePosition += length * 8
    }

    actual final override fun writeFully(src: IoBuffer, length: Int) {
        require(length <= src.readRemaining) { "length is too large: not enough bytes to read $length > ${src.readRemaining}"}
        require(length <= writeRemaining) { "length is too large: not enough room to write $length > $writeRemaining" }

        memcpy(content + writePosition, src.content + src.readPosition, length.convert<size_t>())

        src.readPosition += length
        writePosition += length
    }

    actual final override fun fill(n: Long, v: Byte) {
        require(n <= writeRemaining.toLong())
        require(n >= 0) { "n shouldn't be negative: $n" }
        require(n < Int.MAX_VALUE)

        memset(content + writePosition, v.toInt() and 0xff, n.convert<size_t>())
        writePosition += n.convert<Int>()
    }

    actual final override fun readLong(): Long {
        if (readRemaining < 8) throw IllegalStateException("Not enough bytes available to read a long")
        val m = 0xffffffff
        val a = readInt().toLong() and m
        val b = readInt().toLong() and m

        @Suppress("DEPRECATION")
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

        @Suppress("DEPRECATION")
        if (byteOrder === ByteOrder.LITTLE_ENDIAN) {
            writeInt(b)
            writeInt(a)
        } else {
            writeInt(a)
            writeInt(b)
        }
    }

    actual final override fun append(csq: CharSequence?, start: Int, end: Int): Appendable {
        val idx = appendChars(csq ?: "null", start, end)
        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    actual final override fun append(csq: CharSequence?): Appendable {
        return if (csq == null) append("null") else append(csq, 0, csq.length)
    }

    actual final override fun append(csq: CharArray, start: Int, end: Int): Appendable {
        val idx = appendChars(csq, start, end)

        if (idx != end) throw IllegalStateException("Not enough free space to append char sequence")
        return this
    }

    actual override fun append(c: Char): Appendable {
        val wp = writePosition
        val s = content.putUtf8Char(c.toInt(), limit - wp, wp)
        if (s == 0) notEnoughFreeSpace(c)
        writePosition = wp + s
        return this
    }

    private fun notEnoughFreeSpace(c: Char): Nothing {
        throw IllegalStateException("Not Enough free space to append character '$c', remaining $writeRemaining bytes")
    }

    actual fun appendChars(csq: CharArray, start: Int, end: Int): Int {
        val i8 = content
        var wp = writePosition
        val l = limit
        var rc = end

        if (start == end || wp == l) return start

        for (idx in start until end) {
            val ch = csq[idx].toInt()
            if (ch > 0x7f || wp >= l) {
                rc = idx
                break
            }

            i8[wp++] = ch.toByte()
        }

        if (rc >= end || wp == l) {
            writePosition = wp
            return rc
        }

        return appendCharsUtf8(csq, rc, end, wp)
    }

    private fun appendCharsUtf8(csq: CharArray, start: Int, end: Int, wp0: Int): Int {
        val i8 = content
        val l = limit
        var wp = wp0
        var idx = start

        while (idx < end) {
            val ch = csq[idx++].toInt()

            val size = if (ch.isSurrogateCodePoint()) i8.putUtf8CharSurrogate(ch, csq[idx++].toInt(), l - wp, wp)
            else i8.putUtf8Char(ch, l - wp, wp)

            if (size == 0) {
                return appendCharFailed(ch, idx, wp)
            }

            wp += size
        }

        writePosition = wp
        return end
    }

    actual fun appendChars(csq: CharSequence, start: Int, end: Int): Int {
        val i8 = content
        var wp = writePosition
        val l = limit
        var rc = end

        if (start == end || wp == l) return start

        for (idx in start until end) {
            val ch = csq[idx].toInt()
            if (ch > 0x7f || wp >= l) {
                rc = idx
                break
            }

            i8[wp++] = ch.toByte()
        }

        if (rc >= end || wp == limit) {
            writePosition = wp
            return rc
        }

        return appendCharsUtf8(csq, rc, end, wp)
    }

    private fun appendCharsUtf8(csq: CharSequence, start: Int, end: Int, wp0: Int): Int {
        val i8 = content
        val l = limit
        var wp = wp0
        var idx = start

        while (idx < end) {
            val ch = csq[idx++].toInt()
            val remaining = l - wp
            val size = if (ch.isSurrogateCodePoint()) i8.putUtf8CharSurrogate(ch, csq[idx++].toInt(), remaining, wp)
            else i8.putUtf8Char(ch, remaining, wp)

            if (size == 0) {
                return appendCharFailed(ch, idx, wp)
            }

            wp += size
        }

        writePosition = wp
        return end
    }

    private fun appendCharFailed(ch: Int, idx: Int, wp: Int): Int {
        writePosition = wp
        return if (ch.isSurrogateCodePoint()) idx - 2 else idx - 1
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun CPointer<ByteVar>.putUtf8Char(v: Int, remaining: Int, wp: Int): Int {
        return when {
            v in 1..0x7f -> {
                if (remaining < 1) return 0
                this[wp] = v.toByte()
                1
            }
            v > 0xffff -> {
                if (remaining < 4) return 0
                this[wp    ] = (0xf0 or ((v shr 18) and 0x3f)).toByte()
                this[wp + 1] = (0x80 or ((v shr 12) and 0x3f)).toByte()
                this[wp + 2] = (0x80 or ((v shr  6) and 0x3f)).toByte()
                this[wp + 3] = (0x80 or ( v         and 0x3f)).toByte()
                4
            }
            v > 0x7ff -> {
                if (remaining < 3) return 0
                this[wp    ] = (0xe0 or ((v shr 12) and 0x0f)).toByte()
                this[wp + 1] = (0x80 or ((v shr  6) and 0x3f)).toByte()
                this[wp + 2] = (0x80 or ( v         and 0x3f)).toByte()
                3
            }
            else -> {
                if (remaining < 2) return 0
                this[wp    ] = (0xc0 or ((v shr  6) and 0x1f)).toByte()
                this[wp + 1] = (0x80 or ( v         and 0x3f)).toByte()
                2
            }
        }
    }

    private fun CPointer<ByteVar>.putUtf8CharSurrogate(high: Int, low: Int, remaining: Int, wp: Int): Int {
        val highValue = (high and 0x7ff) shl 10
        val lowValue = (low and 0x3ff)
        val value = 0x010000 or (highValue or lowValue)

        return putUtf8Char(value, remaining, wp)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun Int.isSurrogateCodePoint() = this in 55296..57343

    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(src, length)"), level = DeprecationLevel.ERROR)
    actual fun writeBuffer(src: IoBuffer, length: Int): Int {
        writeFully(src, length)
        return length
    }

    internal actual fun restoreStartGap(n: Int) {
        val rp = readPosition
        if (rp < n) {
            throw IllegalArgumentException("Can't restore start gap: $n bytes were not reserved before")
        }

        readPosition = rp - n
    }

    internal actual fun restoreEndGap(n: Int) {
        val newLimit = limit - n
        limit = newLimit
        if (writePosition > newLimit) {
            writePosition = newLimit
        }
        if (readPosition > newLimit) {
            readPosition = newLimit
        }
    }

    internal actual fun writeBufferPrepend(other: IoBuffer) {
        val size = other.readRemaining
        require(size <= startGap) { "size should be greater than startGap (size = $size, startGap = $startGap)" }

        memcpy(content + (readPosition - size), other.content + other.readPosition, size.convert<size_t>())

        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: IoBuffer, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap) { "size should be greater than write space + end gap (size = $size, " +
                "writeRemaining = $writeRemaining, endGap = $endGap, rem+gap = ${writeRemaining + endGap}" }

        memcpy(content + writePosition, other.content + other.readPosition, size.convert<size_t>())

        writePosition += size
        if (writePosition > limit) {
            limit = writePosition
        }
        other.readPosition += size
    }

    actual final override fun tryPeek(): Int {
        val readPosition = readPosition
        val writePosition = writePosition
        if (readPosition == writePosition) return -1

        return content[readPosition].toInt() and 0xff
    }

    @Deprecated("Binary compatibility.", level = DeprecationLevel.HIDDEN)
    actual final override fun peekTo(buffer: IoBuffer): Int {
        return peekTo(buffer)
    }

    actual fun discardExact(n: Int) {
        if (discard(n.toLong()) != n.toLong()) throw EOFException("Unable to discard $n")
    }

    @Deprecated("Use discardExact instead.")
    actual final override fun discard(n: Long): Long {
        val step = minOf(readRemaining.toLong(), n).toInt()
        readPosition += step
        return step.toLong()
    }

    actual fun resetForWrite() {
        resetForWrite(contentCapacity)
    }

    actual fun resetForWrite(limit: Int) {
        require(limit >= 0) { "Limit shouldn't be negative: $limit" }
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
        if (readPosition < n) throw IllegalStateException("Nothing to push back")
        readPosition -= n
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
        if (this === Empty) throw IllegalStateException("attempted to release IoBuffer.Empty")

        val v = refCount
        if (v == 0) throw IllegalStateException("Unable to release: buffer has been already released")
        val newCount = v - 1
        refCount = newCount
        return newCount == 0
    }

    @ExperimentalIoApi
    actual fun isExclusivelyOwned(): Boolean = refCount == 1

    actual fun makeView(): IoBuffer {
        if (this === Empty) return this

        val o = origin ?: this
        o.acquire()

        val view = IoBuffer(content, contentCapacity, o)
        view.attachment = attachment
        view.readPosition = readPosition
        view.writePosition = writePosition
        view.limit = limit

        return view
    }

    actual fun release(pool: ObjectPool<IoBuffer>) {
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
        @Deprecated("This implementation detail is going to become internal.")
        actual val ReservedSize: Int = 8

        internal val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        actual val Empty = IoBuffer(EmptyBuffer, 0, null)

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
private object BufferPoolNativeWorkaround : DefaultPool<IoBuffer>(BUFFER_VIEW_POOL_SIZE) {
    override fun produceInstance(): IoBuffer {
        val buffer = nativeHeap.allocArray<ByteVar>(BUFFER_VIEW_SIZE)
        return IoBuffer(buffer, BUFFER_VIEW_SIZE, null)
    }

    override fun clearInstance(instance: IoBuffer): IoBuffer {
        return super.clearInstance(instance).apply {
            instance.resetForWrite()
            instance.next = null
            instance.attachment = null

            if (instance.refCount != 0) throw IllegalStateException("Unable to clear instance: refCount is ${instance.refCount} != 0")
            instance.refCount = 1
        }
    }

    override fun validateInstance(instance: IoBuffer) {
        super.validateInstance(instance)

        require(instance.refCount == 0) { "unable to recycle buffer: buffer view is in use (refCount = ${instance.refCount})"}
        require(instance.origin == null) { "Unable to recycle buffer view: view copy shouldn't be recycled" }
    }

    override fun disposeInstance(instance: IoBuffer) {
        require(instance.refCount == 0) { "Couldn't dispose buffer: it is still in-use: refCount = ${instance.refCount}" }
        require(instance.content !== IoBuffer.EmptyBuffer) { "Couldn't dispose empty buffer" }

        nativeHeap.free(instance.content)
    }
}
