package kotlinx.io.core

import kotlinx.cinterop.*
import kotlinx.io.pool.*
import platform.posix.memcpy
import platform.posix.memset

actual class IoBuffer internal constructor(
        internal var content: CPointer<ByteVar>,
        private val contentCapacity: Int,
        internal actual val origin: IoBuffer?
) : Input, Output {
    internal var refCount = 1

    internal var readPosition = 0
    internal var writePosition = 0
    private var limit = contentCapacity

    private var platformEndian = ByteOrder.BIG_ENDIAN === ByteOrder.nativeOrder()

    actual var attachment: Any? = null
    actual var next: IoBuffer? = null

    actual val capacity: Int get() = contentCapacity
    actual val readRemaining: Int get() = writePosition - readPosition
    actual val writeRemaining: Int get() = limit - writePosition

    actual fun canRead() = writePosition > readPosition
    actual fun canWrite() = writePosition < limit

    override val endOfInput: Boolean get() = !canRead()

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

    final override fun readFully(dst: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= readRemaining.toLong())
        require(length >= 0L) { "length shouldn't be negative: $length" }
        require(length <= Int.MAX_VALUE) { "length shouldn't be greater than Int.MAX_VALUE" }

        memcpy(dst + offset, content + readPosition, length.toLong())
        readPosition += length.toInt()
    }

    final override fun readFully(dst: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= readRemaining) { "Not enough bytes available to read $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }
        
        memcpy(dst + offset, content + readPosition, length.toLong())
        readPosition += length
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Int, length: Int) {
        require(length <= writeRemaining) { "Not enough space available to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(content + writePosition, src + offset, length.toLong())
        writePosition += length
    }

    final override fun writeFully(src: CPointer<ByteVar>, offset: Long, length: Long) {
        require(length <= writeRemaining.toLong()) { "Not enough space available to write $length bytes" }
        require(length >= 0) { "length shouldn't be negative: $length" }

        memcpy(content + writePosition, src + offset, length.toLong())
        writePosition += length.toInt()
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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

    actual final override fun readFully(dst: IoBuffer, length: Int) {
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

        if (length == 0) return 0

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

        if (length == 0) return 0
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

        if (length == 0) return 0
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

        if (length == 0) return 0
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

        if (length == 0) return 0
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

        if (length == 0) return 0
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

    actual final override fun readAvailable(dst: IoBuffer, length: Int): Int {
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

    final override fun readAvailable(dst: CPointer<ByteVar>, offset: Long, length: Long): Long {
        require(length >= 0) { "length shouldn't be negative: $length" }
        require(length <= Int.MAX_VALUE) { "length shouldn't be greater than Int.MAX_VALUE" }

        val copySize = minOf(length, readRemaining.toLong())
        memcpy(dst + offset, content + readPosition, copySize.toLong())
        readPosition += length.toInt()

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

        if (length == 0) return

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

    actual final override fun writeFully(src: IoBuffer, length: Int) {
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
        var rc = end

        for (idx in start until end) {
            val ch = csq[idx].toInt()
            val size = i8.putUtf8Char(ch, l - wp, wp)
            if (size == 0) {
                rc = idx
                break
            }
            wp += size
        }

        writePosition = wp
        return rc
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
        var rc = end

        for (idx in start until end) {
            val ch = csq[idx].toInt()
            val size = i8.putUtf8Char(ch, l - wp, wp)
            if (size == 0) {
                rc = idx
                break
            }
            wp += size
        }

        writePosition = wp
        return rc
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun CPointer<ByteVar>.putUtf8Char(v: Int, remaining: Int, wp: Int): Int {
        return when {
            v in 1..0x7f -> {
                if (remaining < 1) return 0
                this[wp] = v.toByte()
                1
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

    @Deprecated("Use writeFully instead", ReplaceWith("writeFully(src, length)"))
    actual fun writeBuffer(src: IoBuffer, length: Int): Int {
        writeFully(src, length)
        return length
    }

    internal actual fun writeBufferPrepend(other: IoBuffer) {
        val size = other.readRemaining
        require(size <= startGap) { "size should be greater than startGap (size = $size, startGap = $startGap)" }

        memcpy(content + (readPosition - size), other.content + other.readPosition, size.toLong())

        readPosition -= size
        other.readPosition += size
    }

    internal actual fun writeBufferAppend(other: IoBuffer, maxSize: Int) {
        val size = minOf(other.readRemaining, maxSize)
        require(size <= writeRemaining + endGap) { "size should be greater than write space + end gap (size = $size, " +
                "writeRemaining = $writeRemaining, endGap = $endGap, rem+gap = ${writeRemaining + endGap}" }

        memcpy(content + writePosition, other.content + other.readPosition, size.toLong())

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

    actual fun isExclusivelyOwned(): Boolean = refCount == 1

    actual fun makeView(): IoBuffer {
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

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$updateRemaining$`(remaining: Int) {
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$ensureNext$`(current: IoBuffer): IoBuffer? {
        return null
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$prepareRead$`(minSize: Int): IoBuffer? {
        return if (readRemaining >= minSize) this else null
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$afterWrite$`() {
    }

    @Deprecated("Non-public API. Use takeWhile or takeWhileSize instead", level = DeprecationLevel.ERROR)
    actual final override fun `$prepareWrite$`(n: Int): IoBuffer {
        if (writeRemaining >= n) return this
        throw IllegalArgumentException("Not enough space in the chunk")
    }

    actual final override fun flush() {
    }

    actual override fun close() {
        throw UnsupportedOperationException("close for buffer view is not supported")
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
        internal val EmptyBuffer = nativeHeap.allocArray<ByteVar>(0)

        actual val Empty = IoBuffer(EmptyBuffer, 0, null)

        actual val Pool: ObjectPool<IoBuffer> get() = BufferPoolNativeWorkaround

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

        actual val EmptyPool: ObjectPool<IoBuffer> = EmptyBufferViewPoolImpl
    }
}

private val BufferPoolNativeWorkaround: ObjectPool<IoBuffer> = object: DefaultPool<IoBuffer>(BUFFER_VIEW_POOL_SIZE) {
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